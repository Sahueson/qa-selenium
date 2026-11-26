"""Fixtures comunes: navegador por test, page object, login QA, captura y result.json.

Cada test, pase o falle, deja una captura JPEG: es la única evidencia que se publica (nunca
logs, que podrían llevar la contraseña QA).
"""
import io
import os
import re
import subprocess
import unicodedata
from pathlib import Path

import allure
import pytest
from PIL import Image

from qa import browser, live, results
from qa.app_page import LOGIN_BUTTON, AppPage
from qa.env import qa_password, shared_dir
from qa.users import QaUser, qa_user

EVIDENCE_DIR = Path("evidence")


def case_name(item) -> str:
    marker = item.get_closest_marker("case")
    return marker.args[0] if marker else item.name


def slug(s: str) -> str:
    plain = unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", plain).strip("-")[:80]


def first_line(excinfo) -> str:
    msg = str(excinfo.value).strip() or excinfo.typename
    line = msg.splitlines()[0]
    pwd = os.environ.get("QA_PASSWORD")
    return line.replace(pwd, "***") if pwd else line


def pytest_collection_modifyitems(items):
    """Nombre del caso en el informe de Allure (el mismo que en la web, no el de la función)."""
    for item in items:
        name = case_name(item)
        if name != item.name and hasattr(item, "obj"):
            item.obj.__allure_display_name__ = name.replace("{", "{{").replace("}", "}}")


@pytest.fixture
@allure.title("Abrir el navegador")
def driver(request):
    drv = browser.start()
    yield drv
    # Captura al final del test (pase o falle), antes de cerrar el navegador.
    try:
        EVIDENCE_DIR.mkdir(exist_ok=True)
        path = EVIDENCE_DIR / f"{slug(case_name(request.node))}.jpg"
        Image.open(io.BytesIO(drv.get_screenshot_as_png())).convert("RGB").save(path, "JPEG", quality=60)
        allure.attach.file(str(path), name="Captura final", attachment_type=allure.attachment_type.JPG)
        request.node.qa_evidence = str(path)
    except Exception as e:  # noqa: BLE001
        print(f"[qa-lab] Sin captura para {case_name(request.node)}: {e}")
    finally:
        drv.quit()


@pytest.fixture
@allure.title("Preparar la página de la app")
def app(driver) -> AppPage:
    return AppPage(driver)


@pytest.fixture
@allure.title("Registrar el rol del test")
def role(request):
    """Rol con el que corre el test (columna "role" de la web). Se queda el primero."""
    def set_role(description: str) -> None:
        if not getattr(request.node, "qa_role", None):
            request.node.qa_role = description
    return set_role


@pytest.fixture
@allure.title("Preparar el inicio de sesión QA")
def login_as(app, role):
    """Entra con un usuario QA. Admite cambiar de usuario dentro del mismo test."""
    def login(key: str) -> QaUser:
        user = qa_user(key)
        role(user.nickname)
        if (app.driver.current_url or "").startswith("http"):
            app.driver.execute_script("localStorage.clear(); sessionStorage.clear();")
        app.open()
        app.login(user.email, qa_password())
        app.expect_hidden(LOGIN_BUTTON)
        app.wait_for_quiet_network()  # carga inicial de datos tras el login
        return user
    return login


@pytest.fixture(scope="module")
@allure.title("Dejar el staging como el seed")
def reset_staging():
    """Vuelve a dejar el staging como el seed al empezar el módulo. La reserva ya la tiene
    with-staging.js (QA_LOCK_HELD=1): reset.js no la vuelve a pedir."""
    if os.environ.get("STG_URL"):
        subprocess.run(["node", str(shared_dir() / "reset-staging" / "reset.js")],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)


# ---------- Pantalla en vivo de la web (no hacen nada sin RUN_ID) ----------

def pytest_collection_finish(session):
    live.emit("plan", str(len(session.items)))


@pytest.hookimpl(tryfirst=True)
def pytest_runtest_setup(item):
    live.emit("test_started", case_name(item))


def pytest_sessionfinish(session, exitstatus):
    live.flush()


# ---------- Resultado de cada test -> result.json ----------

@pytest.hookimpl(wrapper=True)
def pytest_runtest_makereport(item, call):
    report = yield
    item.stash.setdefault(_REPORTS, {})[report.when] = (report, call)
    if report.when == "teardown":
        _record(item)
    return report


_REPORTS = pytest.StashKey[dict]()


def _record(item) -> None:
    reports = item.stash.get(_REPORTS, {})
    duration_ms = round(sum(r.duration for r, _ in reports.values()) * 1000)
    status, error = "passed", None
    for when in ("setup", "call", "teardown"):
        if when not in reports:
            continue
        report, call = reports[when]
        if report.skipped:
            status = "skipped"
            break
        if report.failed:
            status, error = "failed", first_line(call.excinfo) if call.excinfo else "Error"
            break
    results.add(case_name(item), status, duration_ms, getattr(item, "qa_role", None), error,
                getattr(item, "qa_evidence", None))
    live.emit({"passed": "test_passed", "failed": "test_failed"}.get(status, "test_skipped"), case_name(item))
    if error:
        live.emit("log", f"   ↳ {error}")
