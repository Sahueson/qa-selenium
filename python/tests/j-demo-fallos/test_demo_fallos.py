"""Suite J · Demo de fallos: escenarios que FALLAN A PROPÓSITO (requisitos que la app no cumple) para enseñar cómo sale
un fallo y su reporte de bug. No modifican nada. Mismos casos que qa-playwright/tests/j-demo-fallos."""
import pytest

from qa.app_page import button, heading, text

pytestmark = pytest.mark.usefixtures("reset_staging")


@pytest.mark.case("TC-J01 El jugador MAD ve las partidas del equipo FOE (requisito inexistente)")
def test_tc_j01(app, login_as):
    login_as("player_mad")
    app.expect_visible(text("QA FOE · Partida próxima"))


@pytest.mark.case("TC-J02 El jugador FOE ve las partidas del equipo MAD (requisito inexistente)")
def test_tc_j02(app, login_as):
    login_as("player_foe")
    app.expect_visible(text("QA MAD · Partida próxima"))


@pytest.mark.case("TC-J03 El super admin dispone de un panel de métricas")
def test_tc_j03(app, login_as):
    login_as("superadmin")
    app.expect_visible(heading("Panel de métricas"))


@pytest.mark.case("TC-J04 El jugador sin equipo puede crear partidas")
def test_tc_j04(app, login_as):
    login_as("sinequipo")
    app.expect_visible(button("Crear partida"))


@pytest.mark.case("TC-J05 El jugador bloqueado sigue viendo el calendario de su equipo")
def test_tc_j05(app, login_as):
    login_as("blocked")
    app.expect_visible(text("QA MAD · Partida próxima"))


@pytest.mark.case("TC-J06 El admin MAD tiene una pestaña de Facturación")
def test_tc_j06(app, login_as):
    login_as("admin_mad")
    app.expect_visible(button("Facturación"))


@pytest.mark.case("TC-J07 El jugador MAD puede exportar el calendario a PDF")
def test_tc_j07(app, login_as):
    login_as("player_mad")
    app.expect_visible(button("Exportar calendario (PDF)"))
