"""Suite A · Autenticación y perfiles. Mismos casos que qa-playwright/tests/a-auth/auth.spec.ts."""
import pytest
from selenium.webdriver.common.by import By

from qa.app_page import (EVENT_CARD, LOGIN_BUTTON, LOGOUT_BUTTON, NAV, NEW_EVENT_BUTTON,
                         PROFILE_BUTTON, heading, nav_tab)
from qa.users import qa_user

pytestmark = pytest.mark.usefixtures("reset_staging")  # la suite parte del seed

FICHA = ("//*[contains(concat(' ',@class,' '),' modal ')]"
         "[.//*[self::h1 or self::h2 or self::h3][normalize-space(.)='Tu ficha de operador']]")


def ficha_button(prefix: str):
    return By.XPATH, f"{FICHA}//button[starts-with(normalize-space(.),'{prefix}')]"


@pytest.mark.case("TC-SMOKE La app carga y muestra el login QA")
def test_tc_smoke_smoke(app, role):
    role("Visitante")
    app.open()
    app.expect_title_not_empty()
    app.expect_visible(LOGIN_BUTTON)


@pytest.mark.case("TC-A01 Jugador con equipo entra y ve la navegación completa")
def test_tc_a01_jugador_con_equipo_entra(app, login_as):
    user = login_as("player_mad")
    app.expect_contains_text(PROFILE_BUTTON, user.nickname)
    for tab in ("Calendario", "Partidas", "Jugadores", "Normas"):
        app.expect_visible(nav_tab(tab))
    app.expect_visible(NEW_EVENT_BUTTON)


@pytest.mark.case("TC-A01b Usuario sin equipo queda pendiente de asignación")
def test_tc_a01b_sin_equipo_pendiente(app, login_as):
    login_as("sinequipo")
    app.expect_visible(heading("Pendiente de asignación de equipo"))
    app.expect_hidden(NAV)
    app.expect_hidden(NEW_EVENT_BUTTON)


# Detectó un bug real en Playwright (19-nov-2026, ya corregido en el staging): la RLS no dejaba
# a un bloqueado leer su propio perfil y veía "Pendiente de asignación".
@pytest.mark.case("TC-A02 Usuario bloqueado ve acceso denegado")
def test_tc_a02_bloqueado_acceso_denegado(app, login_as):
    login_as("blocked")
    app.expect_visible(heading("Acceso denegado"))


@pytest.mark.case("TC-A02b Usuario bloqueado no accede a ningún dato")
def test_tc_a02b_bloqueado_sin_datos(app, login_as):
    login_as("blocked")
    app.expect_visible((By.XPATH, "//*[text()[contains(.,'Pendiente de asignación') or contains(.,'Acceso denegado')]]"))
    app.expect_hidden(NAV)
    app.expect_hidden(NEW_EVENT_BUTTON)
    app.expect_count(EVENT_CARD, 0)


@pytest.mark.case("TC-A04 Contraseña incorrecta muestra error y no entra")
def test_tc_a04_password_incorrecta(app, role):
    role("Visitante")
    app.open()
    app.login(qa_user("player_mad").email, "contraseña-incorrecta")
    app.expect_toast("Error al iniciar sesión")
    app.expect_visible(LOGIN_BUTTON)
    app.expect_hidden(NAV)


@pytest.mark.case("TC-A05 Cerrar sesión vuelve a la pantalla de login")
def test_tc_a05_cerrar_sesion(app, login_as):
    login_as("player_mad")
    app.click(LOGOUT_BUTTON)
    app.expect_visible(LOGIN_BUTTON)
    app.expect_hidden(NAV)


@pytest.mark.case("TC-A03 Editar la ficha de operador guarda equipaciones y roles")
def test_tc_a03_editar_ficha(app, login_as):
    user = login_as("player_foe")
    app.click(PROFILE_BUTTON)
    app.expect_visible((By.XPATH, FICHA))
    app.click(ficha_button("MultiCam"))
    app.click(ficha_button("Médico"))
    app.click((By.XPATH, f"{FICHA}//button[normalize-space(.)='Guardar perfil']"))
    app.expect_toast(f"Perfil actualizado: {user.nickname}.")
    app.expect_hidden((By.XPATH, FICHA))

    # Persistencia: tras recargar, la ficha conserva lo marcado.
    app.driver.refresh()
    app.wait_for_quiet_network()
    app.click(PROFILE_BUTTON)
    app.expect_visible(ficha_button("MultiCam ✓"))
    app.expect_visible(ficha_button("Médico ✓"))
