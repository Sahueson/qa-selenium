"""result.json en el formato común del lab (el mismo que escribe qa-playwright).

Las capturas van en evidence_path; shared/publish-results.js las sube y cierra el run. Se
reescribe tras cada test: si la ejecución se corta, lo que ya terminó no se pierde.
"""
import json
import os
import time
from datetime import datetime, timezone
from pathlib import Path

FILE = Path("result.json")
_started = datetime.now(timezone.utc)
_started_ms = time.time() * 1000
_tests: list[dict] = []


def add(name: str, status: str, duration_ms: int, role: str | None, error: str | None,
        evidence_path: str | None) -> None:
    _tests.append({"name": name, "status": status, "duration_ms": duration_ms, "role": role,
                   "error": error, "evidence_path": evidence_path})
    _write()


def _write() -> None:
    count = lambda s: sum(1 for t in _tests if t["status"] == s)  # noqa: E731
    now = datetime.now(timezone.utc)
    FILE.write_text(json.dumps({
        "tool": "selenium-python",
        "target": "tactical-calendar",
        "suite": os.environ.get("SUITE", "local"),
        "started_at": _started.isoformat(),
        "finished_at": now.isoformat(),
        "duration_ms": round(time.time() * 1000 - _started_ms),
        "summary": {"total": len(_tests), "passed": count("passed"),
                    "failed": count("failed"), "skipped": count("skipped")},
        "tests": _tests,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
