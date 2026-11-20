"""Suite C · Inscripciones y reglas de negocio. Mismos casos que
qa-playwright/tests/c-inscripciones/inscripciones.spec.ts (partidas propias del seed)."""
import pytest
from selenium.webdriver.common.by import By

from qa.app_page import MODAL, AppPage, btn, contains_text_xpath, event_card, in_

pytestmark = pytest.mark.usefixtures("reset_staging")  # la suite parte del seed

GEAR, ROLE = AppPage.REGISTRATION_GEAR, AppPage.REGISTRATION_ROLE


def modal_text(t: str):
    return in_(MODAL, contains_text_xpath(t))


def modal_button(t: str):
    return in_(MODAL, btn(t))


@pytest.mark.case("TC-C01 Inscribirse preselecciona la equipación de la partida y avisa si no la tienes")
def test_preselecciona_equipacion_partida(app, login_as):
    # junta2 tiene Árido en su ficha; la partida recomienda Tiger Stripe. (Con la ficha vacía la
    # app no avisa: el aviso solo sale si tienes equipaciones marcadas y no es ninguna de ellas.)
    login_as("junta2")
    app.open_event("QA FOE · Inscripción abierta")

    app.expect_value(GEAR, "Tiger Stripe")
    app.expect_visible(modal_text("No tienes «Tiger Stripe» marcada en tu ficha"))

    app.click(modal_button("Apuntarse"))
    app.expect_toast("Inscripción guardada en la nube.")
    app.expect_visible(modal_text("Inscritos (1)"))
    app.expect_visible(modal_text("QA Junta 2 (Tú)"))
    app.expect_visible(modal_button("Actualizar inscripción"))


@pytest.mark.case("TC-C07 Sin equipación en la partida se preselecciona la de la ficha del jugador")
def test_preselecciona_equipacion_ficha(app, login_as):
    login_as("junta2")  # ficha: Árido + Médico
    app.open_event("QA FOE · MilSim próxima")  # sin equipación recomendada

    app.expect_value(GEAR, "Árido")
    app.expect_value(ROLE, "medico")
    app.expect_hidden(modal_text("No tienes «"))


@pytest.mark.case("TC-C04 Darse de baja a más de 48 h libera la plaza")
def test_baja_libera_plaza(app, login_as):
    login_as("player_foe")
    app.open_event("QA FOE · Inscripción abierta")
    app.click(modal_button("Apuntarse"))
    app.expect_toast("Inscripción guardada en la nube.")

    app.click(modal_button("Darse de baja"))
    app.expect_toast("Has cancelado tu inscripción.")
    app.expect_hidden(modal_text("QA Jugador FOE (Tú)"))
    app.expect_visible(modal_button("Apuntarse"))


@pytest.mark.case("TC-C02 A menos de 48 h no se permite darse de baja")
def test_menos_48h_sin_baja(app, login_as):
    login_as("player_foe")  # inscrito por el seed
    app.open_event("QA FOE · Partida en menos de 48 h")

    app.expect_visible(modal_text("Faltan menos de 48 horas para la partida"))
    app.expect_hidden(modal_button("Darse de baja"))
    # Sí puede actualizar equipación o rol.
    app.expect_visible(modal_button("Actualizar inscripción"))


@pytest.mark.case("TC-C03 Con una excedencia aprobada no se puede inscribir en esas fechas")
def test_excedencia_sin_inscripcion(app, login_as):
    login_as("player_mad")  # excedencia aprobada que cubre la partida
    app.open_tab("Partidas")
    app.expect_visible(in_(event_card("QA MAD · Partida en excedencia"), btn("En excedencia")))

    app.open_event("QA MAD · Partida en excedencia")
    app.expect_visible(modal_text("Tienes una excedencia concedida"))
    app.expect_hidden(modal_button("Apuntarse"))


@pytest.mark.case("TC-C05 Una partida ya finalizada no admite inscripciones")
def test_finalizada_sin_inscripcion(app, login_as):
    login_as("player_foe")
    app.open_event("QA FOE · Partida pasada abierta")

    app.expect_visible(modal_text("ya ha finalizado; las inscripciones y bajas están cerradas"))
    app.expect_hidden(modal_button("Apuntarse"))


@pytest.mark.case("TC-C06 Una partida cancelada no admite inscripciones")
def test_cancelada_sin_inscripcion(app, login_as):
    login_as("player_foe")
    app.open_event("QA FOE · Partida cancelada")

    app.expect_visible(modal_text("CANCELADA · NO CUENTA"))
    app.expect_visible(modal_text("está cancelada; no se permiten inscripciones"))
    app.expect_hidden(modal_button("Apuntarse"))
