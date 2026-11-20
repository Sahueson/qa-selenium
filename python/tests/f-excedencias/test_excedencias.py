"""Suite F · Excedencias FOE (Art. 18 y 19): las solicita el jugador, las concede la Junta
Directiva y nadie se concede la suya. Duración entre 1 mes y 1 año. Mismos casos que
qa-playwright/tests/f-excedencias/excedencias.spec.ts."""
import pytest
from selenium.webdriver.common.by import By

from qa.app_page import btn, button, in_, leave_row, text
from qa.dates import in_days

pytestmark = pytest.mark.usefixtures("reset_staging")

DATES = "//form//input[@type='date']"
REASON = (By.XPATH, "//input[contains(@placeholder,'Motivos laborales / Lesión')]")
SUBMIT = button("Solicitar excedencia a Junta Directiva")


def request_leave(app, start: int, end: int, reason: str) -> None:
    app.open_leaves_panel()
    app.set_value((By.XPATH, f"({DATES})[1]"), in_days(start))
    app.set_value((By.XPATH, f"({DATES})[2]"), in_days(end))
    app.set_value(REASON, reason)
    app.click(SUBMIT)


@pytest.mark.case("TC-F01 Un jugador solicita una excedencia y queda pendiente de la Junta")
def test_tc_f01_solicitar(app, login_as):
    login_as("player_foe")
    request_leave(app, 40, 80, "Excedencia E2E · lesión")
    app.expect_toast("Solicitud de excedencia enviada.")
    row = leave_row("Excedencia E2E · lesión")
    app.expect_contains_text((By.XPATH, row), "PENDIENTE DE JUNTA")
    app.expect_hidden(in_(row, btn("Conceder")))


@pytest.mark.case("TC-F02 Una excedencia de menos de 1 mes se rechaza")
def test_tc_f02_menos_de_un_mes(app, login_as):
    login_as("player_foe")
    request_leave(app, 40, 45, "Excedencia E2E · corta")
    app.expect_toast("La excedencia debe tener una duración mínima de 1 mes")
    app.expect_hidden(text("Excedencia E2E · corta"))


@pytest.mark.case("TC-F03 La Junta Directiva concede una solicitud pendiente")
def test_tc_f03_junta_concede(app, login_as):
    login_as("admin_foe")
    app.open_leaves_panel()
    row = leave_row("Solicitud QA pendiente")  # de QA Jugador FOE (seed)
    app.expect_contains_text((By.XPATH, row), "PENDIENTE DE JUNTA")
    app.click(in_(row, btn("Conceder")))
    app.expect_toast("Excedencia concedida.")
    app.expect_contains_text((By.XPATH, row), "CONCEDIDA")


@pytest.mark.case("TC-F04 Nadie puede concederse su propia excedencia")
def test_tc_f04_no_autoconcesion(app, login_as):
    login_as("admin_foe")  # es de la Junta Directiva, pero la solicitud es suya
    app.open_leaves_panel()
    row = leave_row("Solicitud QA propia")
    app.expect_contains_text((By.XPATH, row), "PENDIENTE DE JUNTA")
    app.expect_hidden(in_(row, btn("Conceder")))


@pytest.mark.case("TC-F05 Un jugador sin cargo no puede conceder excedencias")
def test_tc_f05_jugador_no_concede(app, login_as):
    login_as("player_foe")
    app.open_leaves_panel()
    app.expect_count(button("Conceder"), 0)
    app.expect_count(button("Conceder y aplicar excedencia"), 0)
