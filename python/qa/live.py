"""Eventos en vivo para la pantalla de ejecución de la web (tabla run_events).

Tipos: plan (nº de tests), test_started, test_passed, test_failed, test_skipped y log.
Nunca rompe ni retrasa un test: sin RUN_ID/credenciales no hace nada, los eventos salen en
cola desde un hilo (en el orden en que ocurren), con tope de 5 s y los errores se ignoran.
LIVE_DEBUG=1 los imprime en vez de enviarlos (probar en local sin gastar runs).
"""
import json
import os
import queue
import re
import threading
import time
import urllib.request
from datetime import datetime, timezone

_queue: "queue.Queue[tuple[str, str] | None]" = queue.Queue()
_thread: threading.Thread | None = None


def _enabled() -> bool:
    return all(os.environ.get(k) for k in ("RUN_ID", "SUPABASE_URL", "SUPABASE_SERVICE_KEY"))


def _clean(text: str) -> str:
    s = re.sub(r"\s+", " ", re.sub(r"\x1b\[[0-9;]*m", "", str(text or ""))).strip()
    pwd = os.environ.get("QA_PASSWORD")
    if pwd:
        s = s.replace(pwd, "***")
    return s if len(s) <= 300 else s[:297] + "..."


def _post(type_: str, message: str) -> None:
    key = os.environ["SUPABASE_SERVICE_KEY"]
    body = json.dumps({
        "run_id": os.environ["RUN_ID"], "type": type_, "message": message,
        "created_at": datetime.now(timezone.utc).isoformat(),
    }).encode()
    req = urllib.request.Request(
        f"{os.environ['SUPABASE_URL']}/rest/v1/run_events", data=body, method="POST",
        headers={"apikey": key, "Authorization": f"Bearer {key}",
                 "Content-Type": "application/json", "Prefer": "return=minimal"})
    urllib.request.urlopen(req, timeout=5).close()


def _worker() -> None:
    while True:
        item = _queue.get()
        try:
            if item is not None:
                _post(*item)
        except Exception:  # noqa: BLE001 - los eventos son best-effort
            pass
        finally:
            _queue.task_done()


def emit(type_: str, message: str) -> None:
    global _thread
    if os.environ.get("LIVE_DEBUG"):
        print(f"[live] {type_}: {_clean(message)}")
        return
    if not _enabled():
        return
    if _thread is None:
        _thread = threading.Thread(target=_worker, daemon=True)
        _thread.start()
    _queue.put((type_, _clean(message)))


def flush(timeout: float = 10.0) -> None:
    """Espera (hasta `timeout` s) a que salgan los eventos pendientes."""
    end = time.monotonic() + timeout
    while _queue.unfinished_tasks and time.monotonic() < end:
        time.sleep(0.05)
