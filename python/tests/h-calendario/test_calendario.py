"""Suite H · Calendario: fechas de vacaciones / "No se juega" (team_blackouts). Mismos casos que
qa-playwright/tests/h-calendario/calendario.spec.ts. Seed: "QA · Sin partida FOE" (+25 a +26)
y "QA · Sin partida MAD" (+27)."""
import time

import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from qa.app_page import btn, button, contains_text_xpath, in_, xpath_literal
from qa.dates import in_days

pytestmark = pytest.mark.usefixtures("reset_staging")

MARK_BUTTON = button('Marcar vacaciones / "No se juega"')
FORM = "//form[.//button[normalize-space(.)='Guardar fechas sin partida']]"
DATES = f"{FORM}//input[@type='date']"
REASON = (By.XPATH, f"{FORM}//input[@placeholder='Ej. Vacaciones de verano / Puente']")
SAVE = (By.XPATH, f"{FORM}//button[normalize-space(.)='Guardar fechas sin partida']")


def blackout_row(reason: str) -> str:
    """Fila del listado "Vacaciones / Sin partida": <fila><botón fechas · motivo/><Quitar/></fila>."""
    return f"//button[contains(normalize-space(.),{xpath_literal('· ' + reason)})]/.."


def open_form(app) -> None:
    app.open_tab("Calendario")
    app.click(MARK_BUTTON)


@pytest.mark.case("TC-H01 Un admin marca fechas sin partida para su equipo")
def test_tc_h01_marcar(app, login_as):
    login_as("admin_mad")
    open_form(app)
    reason = f"QA E2E · Puente {int(time.time() * 1000)}"
    app.set_value((By.XPATH, f"({DATES})[1]"), in_days(40))
    app.set_value((By.XPATH, f"({DATES})[2]"), in_days(42))
    app.set_value(REASON, reason)
    app.click(SAVE)

    app.expect_toast('Fechas de vacaciones / "No se juega" guardadas.')
    app.expect_hidden((By.XPATH, FORM))
    app.expect_contains_text((By.XPATH, blackout_row(reason)), f"[MAD Irregulars] {in_days(40)} → {in_days(42)}")


@pytest.mark.case("TC-H02 La fecha de fin no puede ser anterior a la de inicio")
def test_tc_h02_fin_antes_que_inicio(app, login_as):
    login_as("admin_mad")
    open_form(app)
    reason = f"QA E2E · Fechas al revés {int(time.time() * 1000)}"
    app.set_value((By.XPATH, f"({DATES})[1]"), in_days(50))
    app.set_value((By.XPATH, f"({DATES})[2]"), in_days(45))
    app.set_value(REASON, reason)
    app.click(SAVE)

    # "Hasta" lleva min = "Desde": la validación del propio navegador impide enviar el formulario
    # (el mensaje de la app no llega a salir). No se guarda nada.
    end = app.driver.find_element(By.XPATH, f"({DATES})[2]")
    assert app.driver.execute_script("return arguments[0].validity.rangeUnderflow;", end) is True
    app.expect_visible((By.XPATH, FORM))
    app.expect_count((By.XPATH, blackout_row(reason)), 0)


@pytest.mark.case("TC-H03 Un admin de un solo equipo no puede marcar fechas para ambos")
def test_tc_h03_admin_un_equipo(app, login_as):
    login_as("admin_mad")
    open_form(app)
    options = [o.text for o in Select(app.expect_visible((By.XPATH, f"{FORM}//select"))).options]
    assert options == ["MAD Irregulars"], options


@pytest.mark.case("TC-H04 El super admin puede marcar fechas para ambos equipos")
def test_tc_h04_super_ambos(app, login_as):
    login_as("superadmin")
    open_form(app)
    options = [o.text for o in Select(app.expect_visible((By.XPATH, f"{FORM}//select"))).options]
    assert options == ["MAD Irregulars", "FOE", "Ambos equipos (MAD Irregulars y FOE)"], options
    # Y ve las fechas de los dos equipos.
    app.expect_visible((By.XPATH, blackout_row("QA · Sin partida FOE")))
    app.expect_visible((By.XPATH, blackout_row("QA · Sin partida MAD")))


@pytest.mark.case("TC-H05 Cada equipo solo ve sus fechas y un jugador no puede quitarlas")
def test_tc_h05_visibilidad(app, login_as):
    login_as("player_mad")
    app.open_tab("Calendario")
    mad = blackout_row("QA · Sin partida MAD")
    app.expect_visible((By.XPATH, mad))
    app.expect_count(in_(mad, btn("Quitar")), 0)
    app.expect_count((By.XPATH, blackout_row("QA · Sin partida FOE")), 0)
    app.expect_hidden(MARK_BUTTON)


@pytest.mark.case("TC-H06 Un admin quita las fechas de su equipo y no persisten al recargar")
def test_tc_h06_quitar(app, login_as):
    login_as("admin_mad")
    app.open_tab("Calendario")
    app.click(in_(blackout_row("QA · Sin partida MAD"), btn("Quitar")))

    app.expect_toast("Fechas eliminadas del calendario.")
    app.expect_count((By.XPATH, blackout_row("QA · Sin partida MAD")), 0)
    app.driver.refresh()
    app.wait_for_quiet_network()
    app.open_tab("Calendario")
    app.expect_visible(MARK_BUTTON)
    app.expect_count((By.XPATH, blackout_row("QA · Sin partida MAD")), 0)


@pytest.mark.case("TC-H07 Un día sin partida se avisa al seleccionarlo en el calendario")
def test_tc_h07_aviso_en_el_dia(app, login_as):
    login_as("admin_foe")
    app.open_tab("Calendario")
    # Pulsar la fila lleva el calendario a ese mes y selecciona el primer día.
    app.click((By.XPATH, "//button[contains(normalize-space(.),'· QA · Sin partida FOE')]"))
    aviso = contains_text_xpath("No se juega (FOE):")
    app.expect_visible((By.XPATH, aviso))
    app.expect_contains_text((By.XPATH, aviso + "/.."), "QA · Sin partida FOE")
