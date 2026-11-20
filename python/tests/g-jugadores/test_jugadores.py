"""Suite G · Gestión de jugadores (pestaña Jugadores). Solo el super admin cambia rol, bloqueo,
equipos y órganos FOE; el efecto se comprueba entrando con el jugador afectado. Mismos casos que
qa-playwright/tests/g-jugadores/jugadores.spec.ts."""
import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from qa.app_page import NAV, btn, btn_containing, button, event_card, heading, in_, nav_tab, player_card

pytestmark = pytest.mark.usefixtures("reset_staging")

ROLE_SELECT = "//select[option[@value='super_admin']]"
ADD_SQUAD_SELECT = "//select[option[normalize-space(.)='+ Añadir otro equipo']]"


@pytest.mark.case("TC-G01 El super admin asciende a un jugador a admin y este gestiona partidas")
def test_ascender(app, login_as):
    login_as("superadmin")
    app.open_tab("Jugadores")
    Select(app.expect_visible(in_(player_card("QA Jugador MAD"), ROLE_SELECT))).select_by_value("admin")
    app.expect_toast("Ficha actualizada para QA Jugador MAD.")

    login_as("player_mad")
    app.open_tab("Partidas")
    card = event_card("QA MAD · Partida próxima")
    app.expect_visible(in_(card, btn("Editar")))
    app.expect_visible(in_(card, btn("Eliminar")))


@pytest.mark.case("TC-G02 El super admin bloquea a un jugador y este pierde el acceso")
def test_bloquear(app, login_as):
    login_as("superadmin")
    app.open_tab("Jugadores")
    card = player_card("QA Jugador FOE")
    app.click(in_(card, btn("Bloquear")))
    app.expect_toast("Ficha actualizada para QA Jugador FOE.")
    app.expect_visible(in_(card, btn("Desbloquear")))
    app.expect_visible(heading("QA Jugador FOE [BLOQUEADO]"))

    login_as("player_foe")
    app.expect_visible(heading("Acceso denegado"))
    app.expect_hidden(NAV)


@pytest.mark.case("TC-G03 Asignar equipo a un jugador sin equipo le da acceso a ese equipo")
def test_asignar_equipo(app, login_as):
    login_as("superadmin")
    app.open_tab("Jugadores")
    card = player_card("QA Sin Equipo")
    app.expect_visible(in_(card, "//*[normalize-space(.)='Sin equipo asignado']"))
    Select(app.expect_visible(in_(card, ADD_SQUAD_SELECT))).select_by_value("FOE")
    app.expect_toast("Ficha actualizada para QA Sin Equipo.")
    app.expect_visible(in_(card, "//*[@title='Quitar de FOE']"))

    login_as("sinequipo")
    app.expect_visible(NAV)
    app.expect_visible(nav_tab("FOE · Votos y Faltas"))
    titles = app.visible_event_titles()
    assert any("QA FOE · Partida próxima" in t for t in titles), titles
    assert all("QA MAD" not in t for t in titles), titles


@pytest.mark.case("TC-G04 Quitar la Junta Directiva a un miembro le oculta sus votaciones")
def test_quitar_junta(app, login_as):
    login_as("superadmin")
    app.open_tab("Jugadores")
    card = player_card("QA Junta 2")
    app.click(in_(card, btn("✓ Junta Directiva")))
    app.expect_toast("Ficha actualizada para QA Junta 2.")
    app.expect_visible(in_(card, btn("+ Junta Directiva")))

    login_as("junta2")
    app.open_tab("FOE · Votos y Faltas")
    app.expect_visible(button("Votos · Junta Operativa"))
    app.expect_hidden(button("Votos · Junta Directiva"))


@pytest.mark.case("TC-G05 Un admin que no es super admin no puede cambiar permisos")
def test_admin_sin_permisos(app, login_as):
    login_as("admin_foe")
    app.open_tab("Jugadores")
    card = player_card("QA Junta 2")
    app.expect_visible((By.XPATH, card))
    app.expect_count(in_(card, ROLE_SELECT), 0)
    app.expect_count(in_(card, "//button[normalize-space(.)='Bloquear' or normalize-space(.)='Desbloquear']"), 0)
    app.expect_count(in_(card, "//*[@title='Quitar de FOE']"), 0)
    app.expect_enabled(in_(card, btn_containing("Junta Operativa")), False)


@pytest.mark.case("TC-G06 El super admin no puede cambiarse el rol ni bloquearse a sí mismo")
def test_super_no_se_toca(app, login_as):
    login_as("superadmin")
    app.open_tab("Jugadores")
    card = player_card("QA Super Admin")
    app.expect_visible((By.XPATH, card))
    app.expect_count(in_(card, ROLE_SELECT), 0)
    app.expect_count(in_(card, btn("Bloquear")), 0)


@pytest.mark.case("TC-G07 Un admin de equipo solo ve a los jugadores de su equipo")
def test_admin_ve_su_equipo(app, login_as):
    login_as("admin_mad")
    app.open_tab("Jugadores")
    app.expect_visible((By.XPATH, player_card("QA Jugador MAD")))
    for nick in ("QA Jugador FOE", "QA Admin FOE", "QA Sin Equipo"):
        app.expect_count((By.XPATH, player_card(nick)), 0)
