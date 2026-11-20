"""Suite A · Visibilidad por rol (RBAC): lo que impone el RLS del staging, visto en la interfaz.

Mismos casos que qa-playwright/tests/a-auth/rbac.spec.ts. Se comprueba qué se ve y qué no, sin
contar el total, para que añadir partidas al seed no rompa estos tests.
"""
import pytest

from qa.app_page import button, nav_tab, text

MAD = ["QA MAD · Partida próxima", "QA MAD · Partida en excedencia"]
FOE = ["QA FOE · Partida próxima", "QA FOE · MilSim próxima", "QA FOE · Partida en menos de 48 h",
       "QA FOE · Partida finalizada", "QA FOE · Inscripción abierta"]
VOTACION_DIRECTIVA = "QA · Votación de la Junta Directiva"


def assert_sees(titles: list[str], expected: list[str]) -> None:
    for t in expected:
        assert any(t in x for x in titles), f"No se ve la partida «{t}»"


@pytest.mark.case("TC-A06 Jugador MAD solo ve partidas MAD y no tiene pestaña FOE")
def test_tc_a06_jugador_mad(app, login_as):
    login_as("player_mad")
    titles = app.visible_event_titles()
    assert_sees(titles, MAD)
    assert all("QA MAD" in x for x in titles), f"Ve partidas que no son MAD: {titles}"
    app.expect_hidden(nav_tab("FOE · Votos y Faltas"))


@pytest.mark.case("TC-A07 Jugador FOE ve las partidas FOE y ninguna MAD")
def test_tc_a07_jugador_foe(app, login_as):
    login_as("player_foe")
    titles = app.visible_event_titles()
    assert_sees(titles, FOE)
    assert all("QA FOE" in x for x in titles), f"Ve partidas que no son FOE: {titles}"
    app.expect_visible(nav_tab("FOE · Votos y Faltas"))


@pytest.mark.case("TC-A08 Super admin ve las partidas de ambos equipos")
def test_tc_a08_super_admin(app, login_as):
    login_as("superadmin")
    assert_sees(app.visible_event_titles(), MAD + FOE)


@pytest.mark.case("TC-A09 Jugador FOE sin cargo no ve la Junta Directiva")
def test_tc_a09_jugador_foe_sin_junta(app, login_as):
    login_as("player_foe")
    app.open_tab("FOE · Votos y Faltas")
    app.expect_visible(button("Faltas y Sanciones"))
    app.expect_hidden(button("Votos · Junta Directiva"))
    app.expect_hidden(text(VOTACION_DIRECTIVA))


@pytest.mark.case("TC-A10 Miembro de la Junta Directiva ve su votación")
def test_tc_a10_miembro_junta(app, login_as):
    login_as("admin_foe")
    app.open_tab("FOE · Votos y Faltas")
    app.click(button("Votos · Junta Directiva"))
    app.expect_visible(text(VOTACION_DIRECTIVA))
