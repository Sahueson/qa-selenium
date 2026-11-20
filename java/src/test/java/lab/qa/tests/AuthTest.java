package lab.qa.tests;

import static lab.qa.pages.AppPage.*;

import lab.qa.support.BaseTest;
import lab.qa.support.QaUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;

/** Suite A · Autenticación y perfiles. Mismos casos que qa-playwright/tests/a-auth/auth.spec.ts. */
@Tag("a-auth")
@DisplayName("Suite A · Autenticación")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthTest extends BaseTest {

  private static final String FICHA = "//*[contains(concat(' ',@class,' '),' modal ')]"
      + "[.//*[self::h1 or self::h2 or self::h3][normalize-space(.)='Tu ficha de operador']]";

  private static By fichaButton(String prefix) {
    return By.xpath(FICHA + "//button[starts-with(normalize-space(.),'" + prefix + "')]");
  }

  @Test
  @Order(1)
  @DisplayName("TC-SMOKE La app carga y muestra el login QA")
  void smoke() {
    role("Visitante");
    app.open();
    app.expectTitleNotEmpty();
    app.expectVisible(LOGIN_BUTTON);
  }

  @Test
  @Order(2)
  @DisplayName("TC-A01 Jugador con equipo entra y ve la navegación completa")
  void jugadorConEquipoEntra() {
    QaUsers.QaUser user = loginAs("player_mad");
    app.expectContainsText(PROFILE_BUTTON, user.nickname());
    for (String tab : new String[] {"Calendario", "Partidas", "Jugadores", "Normas"}) {
      app.expectVisible(navTab(tab));
    }
    app.expectVisible(NEW_EVENT_BUTTON);
  }

  @Test
  @Order(3)
  @DisplayName("TC-A01b Usuario sin equipo queda pendiente de asignación")
  void sinEquipoPendiente() {
    loginAs("sinequipo");
    app.expectVisible(heading("Pendiente de asignación de equipo"));
    app.expectHidden(NAV);
    app.expectHidden(NEW_EVENT_BUTTON);
  }

  // Detectó un bug real en Playwright (19-nov-2026, ya corregido en el staging): la RLS no dejaba
  // a un bloqueado leer su propio perfil y veía "Pendiente de asignación".
  @Test
  @Order(4)
  @DisplayName("TC-A02 Usuario bloqueado ve acceso denegado")
  void bloqueadoAccesoDenegado() {
    loginAs("blocked");
    app.expectVisible(heading("Acceso denegado"));
  }

  @Test
  @Order(5)
  @DisplayName("TC-A02b Usuario bloqueado no accede a ningún dato")
  void bloqueadoSinDatos() {
    loginAs("blocked");
    app.expectVisible(By.xpath("//*[text()[contains(.,'Pendiente de asignación') or contains(.,'Acceso denegado')]]"));
    app.expectHidden(NAV);
    app.expectHidden(NEW_EVENT_BUTTON);
    app.expectCount(EVENT_CARD, 0);
  }

  @Test
  @Order(6)
  @DisplayName("TC-A04 Contraseña incorrecta muestra error y no entra")
  void passwordIncorrecta() {
    role("Visitante");
    app.open();
    app.login(QaUsers.get("player_mad").email(), "contraseña-incorrecta");
    app.expectToast("Error al iniciar sesión");
    app.expectVisible(LOGIN_BUTTON);
    app.expectHidden(NAV);
  }

  @Test
  @Order(7)
  @DisplayName("TC-A05 Cerrar sesión vuelve a la pantalla de login")
  void cerrarSesion() {
    loginAs("player_mad");
    app.click(LOGOUT_BUTTON);
    app.expectVisible(LOGIN_BUTTON);
    app.expectHidden(NAV);
  }

  @Test
  @Order(8)
  @DisplayName("TC-A03 Editar la ficha de operador guarda equipaciones y roles")
  void editarFicha() {
    QaUsers.QaUser user = loginAs("player_foe");
    app.click(PROFILE_BUTTON);
    app.expectVisible(By.xpath(FICHA));
    app.click(fichaButton("MultiCam"));
    app.click(fichaButton("Médico"));
    app.click(By.xpath(FICHA + "//button[normalize-space(.)='Guardar perfil']"));
    app.expectToast("Perfil actualizado: " + user.nickname() + ".");
    app.expectHidden(By.xpath(FICHA));

    // Persistencia: tras recargar, la ficha conserva lo marcado.
    app.driver().navigate().refresh();
    app.waitForQuietNetwork();
    app.click(PROFILE_BUTTON);
    app.expectVisible(fichaButton("MultiCam ✓"));
    app.expectVisible(fichaButton("Médico ✓"));
  }
}
