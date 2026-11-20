"""Suite D · Motor de asistencia (Art. 33 y 34 de los Estatutos). Mismos casos que
qa-playwright/tests/d-asistencia/asistencia.spec.ts. El % se calcula sobre las partidas
computables FOE del año (seed: finalizada 1, pasada abierta 1 y MilSim pasada 1,5 = 3,5)."""
import pytest
from selenium.webdriver.common.by import By

from qa.app_page import MODAL, attendance_of, btn, event_card, in_

pytestmark = pytest.mark.usefixtures("reset_staging")  # otras suites alteran ese total


def reg_checkbox(nickname: str):
    return in_(MODAL, f"//*[contains(@class,'event-detail__reg-row')][contains(normalize-space(.),'{nickname}')]"
                      "//input[@type='checkbox']")


@pytest.mark.case("TC-D01 El % anual se calcula sobre las partidas ya jugadas")
def test_porcentaje_anual(app, login_as):
    login_as("admin_foe")
    app.open_tab("Jugadores")
    att = attendance_of("QA Jugador FOE")  # asistió a 1 de 3,5
    app.expect_contains_text(att, "29% (1/3,5)")
    app.expect_contains_text(att, "Por debajo del mínimo anual FOE (60% · Art. 33)")


@pytest.mark.case("TC-D02 Una MilSim pesa x1,5 en la asistencia")
def test_milsim_pesa_mas(app, login_as):
    login_as("admin_foe")
    app.open_tab("Jugadores")
    # junta2 solo fue a la MilSim pasada: suma 1,5, no 1.
    app.expect_contains_text(attendance_of("QA Junta 2"), "(1,5/")


@pytest.mark.case("TC-D03 Las partidas durante una excedencia aprobada no restan")
def test_excedencia_no_resta(app, login_as):
    login_as("admin_foe")
    app.open_tab("Jugadores")
    # Sin la excedencia serían 1,5/3,5 = 43 %; con ella la pasada abierta no computa.
    att = attendance_of("QA Junta 2")
    app.expect_contains_text(att, "60% (1,5/2,5)")
    app.expect_contains_text(att, "Cumple mínimo anual FOE (60%)")


@pytest.mark.case("TC-D04 Cerrar el acta con asistencia actualiza el % del jugador")
def test_cerrar_acta_actualiza(app, login_as):
    login_as("admin_foe")
    app.open_event("QA FOE · Partida pasada abierta")
    box = reg_checkbox("QA Jugador FOE")
    app.click(box)
    app.expect_checked(box)  # sigue marcada al guardar
    app.click(in_(MODAL, btn("Guardar asistencia y bloquear partida")))
    app.expect_toast("Asistencia guardada y partida bloqueada.")
    app.click(in_(MODAL, btn("Cerrar")))

    app.open_tab("Jugadores")
    app.expect_contains_text(attendance_of("QA Jugador FOE"), "57% (2/3,5)")


# Regresión del bug (c), corregido en bfdc0e2: al abrir el detalle la app recarga los datos y esa
# recarga reiniciaba el borrador de asistencia, borrando lo ya marcado. Para que la recarga llegue
# SIEMPRE después de la marca, el script inyectado (browser.py) la retrasa 1,5 s.
@pytest.mark.case("TC-D05 Una marca hecha nada más abrir la partida no se pierde con la recarga")
def test_marca_no_se_pierde(app, login_as):
    login_as("junta2")
    app.open_tab("Partidas")
    app.driver.execute_script("window.__qa.delay = { pattern: '/rest/v1/event_registrations', ms: 1500 };")
    delayed = lambda: app.driver.execute_script("return window.__qa.delayed || 0;")  # noqa: E731

    app.click((By.XPATH, event_card("QA FOE · Partida finalizada") + "//button[contains(@class,'tactical-btn--blue')]"))
    box = reg_checkbox("QA Admin FOE")
    # El borrador se rellena justo después de pintar el modal (en el primer instante sale vacío).
    app.expect_checked(box)
    app.click(box)
    assert delayed() == 0, "La recarga llegó antes de la marca: el test no prueba la carrera"

    app.wait_for_quiet_network()
    assert delayed() > 0, "La recarga retrasada no llegó"
    app.expect_checked(box, False)
    app.driver.execute_script("window.__qa.delay = null;")
    app.click(in_(MODAL, btn("Guardar asistencia y bloquear partida")))
    app.expect_toast("Asistencia guardada y partida bloqueada.")

    # Lo guardado en la base de datos, no solo en pantalla.
    app.driver.refresh()
    app.wait_for_quiet_network()
    app.open_tab("Jugadores")
    app.expect_contains_text(attendance_of("QA Admin FOE"), "0% (0/3,5)")
