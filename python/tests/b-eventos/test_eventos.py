"""Suite B · Ciclo de vida de partidas. Mismos casos que qa-playwright/tests/b-eventos/eventos.spec.ts:
crear, cancelar, reactivar, editar, eliminar, cerrar el acta y desbloquearla (aprobación de los
demás responsables, RPC del servidor)."""
import time
from datetime import date, timedelta

import pytest
from selenium.webdriver.common.by import By

from qa.app_page import (MODAL, NEW_EVENT_BUTTON, btn, contains_text_xpath, event_card, in_,
                         modal_with_heading)

pytestmark = pytest.mark.usefixtures("reset_staging")  # la suite parte del seed

TITLE_INPUT = "//input[@placeholder='Ej. Dominical CQB / Operación Tormenta']"
FIELD_INPUT = "//input[@placeholder='Ej. El Cortijo / Bunker / Campo Norte']"
DATE_INPUT = "//input[@type='date']"
TIME_INPUT = "//input[@type='time']"
NEW_FORM = modal_with_heading("Nueva partida")
EDIT_FORM = modal_with_heading("Editar partida")
LOCKED = "Partida cerrada y bloqueada"


def future(days: int) -> str:
    return (date.today() + timedelta(days=days)).isoformat()


def modal_text(t: str):
    return in_(MODAL, contains_text_xpath(t))


def modal_button(t: str):
    return in_(MODAL, btn(t))


def card_button(title: str, t: str):
    return in_(event_card(title), btn(t))


@pytest.mark.case("TC-B01 Crear partida valida los campos obligatorios")
def test_crear_valida_campos(app, login_as):
    login_as("admin_mad")
    app.click(NEW_EVENT_BUTTON)
    submit, error = in_(NEW_FORM, btn("Crear partida")), in_(NEW_FORM, "//*[contains(@class,'modal__error')]")

    # El formulario propone una fecha por defecto: se vacía para probar su validación.
    app.set_value(in_(NEW_FORM, DATE_INPUT), "")
    app.click(submit)
    app.expect_text(error, "El título de la partida es obligatorio.")

    app.set_value(in_(NEW_FORM, TITLE_INPUT), "QA E2E · Validación")
    app.click(submit)
    app.expect_text(error, "Selecciona la fecha de la partida.")

    app.set_value(in_(NEW_FORM, DATE_INPUT), future(15))
    app.click(submit)
    app.expect_text(error, "Indica el campo de juego.")


@pytest.mark.case("TC-B01b Un admin crea una partida y aparece en la lista")
def test_crear_partida(app, login_as):
    login_as("admin_mad")
    title = f"QA E2E · Partida creada {int(time.time() * 1000)}"
    app.click(NEW_EVENT_BUTTON)
    app.set_value(in_(NEW_FORM, TITLE_INPUT), title)
    app.set_value(in_(NEW_FORM, DATE_INPUT), future(15))
    app.set_value(in_(NEW_FORM, TIME_INPUT), "10:00")
    app.set_value(in_(NEW_FORM, FIELD_INPUT), "Campo QA E2E")
    app.click(in_(NEW_FORM, btn("Crear partida")))

    app.expect_toast("Partida creada para MAD Irregulars.")
    app.expect_hidden((By.XPATH, NEW_FORM))
    app.open_tab("Partidas")
    app.expect_visible((By.XPATH, event_card(title)))


@pytest.mark.case("TC-B02 Cancelar una partida sin contar asistencia")
def test_cancelar_sin_asistencia(app, login_as):
    login_as("admin_mad")
    app.open_event("QA MAD · Partida próxima")
    app.click(modal_button("Cancelar partida"))
    app.click(in_(MODAL, "//label[contains(normalize-space(.),'No contar asistencia')]//input"))
    app.set_value(in_(MODAL, "//input[@placeholder='Ej. Lluvia / Campo cerrado / Falta de jugadores']"), "Lluvia (QA)")
    app.click(modal_button("Confirmar cancelación"))

    app.expect_toast("Partida cancelada y descontada del total de partidas.")
    app.expect_visible(modal_text("CANCELADA · NO CUENTA"))
    app.expect_visible(modal_button("Reactivar partida"))


@pytest.mark.case("TC-B02b Cancelar una partida contando asistencia permite pasar lista")
def test_cancelar_con_asistencia(app, login_as):
    login_as("admin_foe")
    app.open_event("QA FOE · Partida próxima")
    app.click(modal_button("Cancelar partida"))
    app.click(in_(MODAL, "//label[contains(normalize-space(.),'Sí, contar asistencia')]//input"))
    app.click(modal_button("Confirmar cancelación"))

    app.expect_toast("Partida cancelada (cuenta asistencia")
    app.expect_visible(modal_text("CANCELADA · CUENTA ASISTENCIA"))


@pytest.mark.case("TC-B03 Cerrar el acta bloquea la partida en solo lectura")
def test_cerrar_acta(app, login_as):
    login_as("admin_foe")
    app.open_event("QA FOE · Partida finalizada")
    app.click(modal_button("Guardar asistencia y bloquear partida"))

    app.expect_toast("Asistencia guardada y partida bloqueada.")
    app.expect_visible(in_(MODAL, "//*[normalize-space(text())='BLOQUEADA']"))
    app.expect_visible(modal_text(LOCKED))
    app.expect_hidden(modal_button("Editar partida"))
    app.expect_hidden(modal_button("Cancelar partida"))


@pytest.mark.case("TC-B04 Un jugador no puede editar ni cancelar partidas ajenas")
def test_jugador_no_edita(app, login_as):
    login_as("player_foe")
    app.open_event("QA FOE · MilSim próxima")
    app.expect_hidden(modal_button("Editar partida"))
    app.expect_hidden(modal_button("Cancelar partida"))


@pytest.mark.case("TC-B05 Un admin edita una partida de su equipo")
def test_editar(app, login_as):
    login_as("admin_mad")
    title = f"QA MAD · Partida editada {int(time.time() * 1000)}"
    app.open_tab("Partidas")
    app.click(card_button("QA MAD · Partida a editar", "Editar"))
    app.set_value(in_(EDIT_FORM, TITLE_INPUT), title)
    app.click(in_(EDIT_FORM, btn("Guardar cambios")))

    app.expect_toast("Partida actualizada.")
    app.expect_hidden((By.XPATH, EDIT_FORM))
    app.expect_visible((By.XPATH, event_card(title)))
    app.expect_count((By.XPATH, event_card("QA MAD · Partida a editar")), 0)


@pytest.mark.case("TC-B06 Reactivar una partida cancelada")
def test_reactivar(app, login_as):
    login_as("admin_foe")
    app.open_event("QA FOE · Partida cancelada")
    app.click(modal_button("Reactivar partida"))

    app.expect_toast("Partida reactivada.")
    app.expect_hidden(in_(MODAL, "//*[starts-with(normalize-space(text()),'CANCELADA')]"))
    app.expect_visible(modal_button("Cancelar partida"))


@pytest.mark.case("TC-B07 Eliminar una partida pide confirmación y la borra del servidor")
def test_eliminar(app, login_as):
    login_as("admin_mad")
    app.open_tab("Partidas")
    card = "QA MAD · Partida a eliminar"
    app.click(card_button(card, "Eliminar"))
    # Primer clic solo muestra la confirmación; el botón pasa a "Cancelar".
    app.expect_visible(card_button(card, "Cancelar"))
    app.click(card_button(card, "Confirmar eliminar partida"))

    app.expect_toast("Partida eliminada.")
    app.expect_count((By.XPATH, event_card(card)), 0)
    # Tras recargar sigue sin estar: se borró en la base de datos, no solo en pantalla.
    app.driver.refresh()
    app.wait_for_quiet_network()
    app.open_tab("Partidas")
    app.expect_visible((By.XPATH, event_card("QA MAD · Partida próxima")))
    app.expect_count((By.XPATH, event_card(card)), 0)


@pytest.mark.case("TC-B08 Una partida cerrada no se puede editar ni eliminar desde la lista")
def test_cerrada_sin_botones(app, login_as):
    login_as("admin_mad")
    app.open_tab("Partidas")
    card = "QA MAD · Partida cerrada"
    app.expect_visible(card_button(card, "Ver acta / Desbloquear"))
    app.expect_count(card_button(card, "Editar"), 0)
    app.expect_count(card_button(card, "Eliminar"), 0)


@pytest.mark.case("TC-B09 Un jugador no puede pedir el desbloqueo de un acta")
def test_jugador_no_desbloquea(app, login_as):
    login_as("player_mad")
    app.open_event("QA MAD · Partida cerrada")
    app.expect_visible(modal_text(LOCKED))
    app.expect_hidden(modal_button("Solicitar desbloqueo para editar"))


@pytest.mark.case("TC-B10 Desbloquear un acta MAD: la pide su admin y la aprueba el super admin")
def test_desbloqueo_mad(app, login_as):
    login_as("admin_mad")
    app.open_event("QA MAD · Partida cerrada")
    app.click(modal_button("Solicitar desbloqueo para editar"))
    # Responsables MAD aparte de quien lo pide: solo el super admin.
    app.expect_toast("Solicitud enviada. Pendiente de aprobación por 1 responsable.")
    app.expect_visible(modal_text("Solicitud de desbloqueo activa (iniciada por QA Admin MAD)"))
    app.expect_visible(modal_text("Aprobaciones necesarias (0/1):"))

    login_as("superadmin")
    app.open_event("QA MAD · Partida cerrada")
    app.click(modal_button("Aceptar desbloqueo"))
    app.expect_toast("Todos han aprobado. Partida desbloqueada.")
    app.expect_hidden(modal_text(LOCKED))
    app.expect_visible(modal_button("Editar partida"))


@pytest.mark.case("TC-B11 Desbloquear un acta FOE necesita a los otros dos responsables y se puede cancelar")
def test_desbloqueo_foe(app, login_as):
    title = "QA FOE · Partida pasada abierta"
    login_as("admin_foe")
    app.open_event(title)
    app.click(modal_button("Guardar asistencia y bloquear partida"))
    app.expect_toast("Asistencia guardada y partida bloqueada.")
    app.click(modal_button("Solicitar desbloqueo para editar"))
    # Responsables FOE aparte de admin_foe: super admin y QA Junta 2 (Junta Operativa).
    app.expect_toast("Solicitud enviada. Pendiente de aprobación por 2 responsables.")

    login_as("junta2")
    app.open_event(title)
    # Solo quien lo pidió (o el super admin) puede cancelar la solicitud.
    app.expect_hidden(modal_button("Cancelar solicitud"))
    app.click(modal_button("Aceptar desbloqueo"))
    app.expect_toast("Tu aprobación ha quedado registrada.")
    app.expect_visible(modal_text("Aprobaciones necesarias (1/2):"))
    app.expect_visible(modal_text(LOCKED))

    login_as("admin_foe")
    app.open_event(title)
    app.click(modal_button("Cancelar solicitud"))
    app.expect_toast("Solicitud de desbloqueo cancelada.")
    app.expect_visible(modal_button("Solicitar desbloqueo para editar"))
    app.expect_visible(modal_text(LOCKED))
