"""Configuración por variables de entorno (las mismas que qa-playwright)."""
import os
from pathlib import Path


def base_url() -> str:
    url = os.environ.get("BASE_URL", "").rstrip("/")
    if not url:
        raise RuntimeError("Falta BASE_URL (el demo de staging)")
    return url


def qa_password() -> str:
    pwd = os.environ.get("QA_PASSWORD")
    if not pwd:
        raise RuntimeError("Falta QA_PASSWORD")
    return pwd


def shared_dir() -> Path:
    """Código compartido (ivan-qa-lab-shared): en CI se clona en ./shared; en local es un enlace."""
    return Path(os.environ.get("QA_SHARED_DIR") or "../shared").resolve()


def headless() -> bool:
    return os.environ.get("HEADLESS") != "0"
