package lab.qa.tests.j_demo_fallos;

import static lab.qa.pages.AppPage.*;

import lab.qa.support.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Suite J · Demo de fallos: escenarios que FALLAN A PROPÓSITO (requisitos que la app no cumple) para enseñar cómo sale un
 * fallo y su reporte de bug. No modifican nada. Mismos casos que qa-playwright/tests/j-demo-fallos.
 */
@Tag("j-demo-fallos")
@DisplayName("Suite J · Demo de fallos (fallan a propósito)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoFallosTest extends BaseTest {
  @BeforeAll
  static void seed() {
    resetStaging();
  }

  @Test
  @Order(1)
  @DisplayName("TC-J01 El jugador MAD ve las partidas del equipo FOE (requisito inexistente)")
  void tcJ01() {
    loginAs("player_mad");
    app.expectVisible(text("QA FOE · Partida próxima"));
  }

  @Test
  @Order(2)
  @DisplayName("TC-J02 El jugador FOE ve las partidas del equipo MAD (requisito inexistente)")
  void tcJ02() {
    loginAs("player_foe");
    app.expectVisible(text("QA MAD · Partida próxima"));
  }

  @Test
  @Order(3)
  @DisplayName("TC-J03 El super admin dispone de un panel de métricas")
  void tcJ03() {
    loginAs("superadmin");
    app.expectVisible(heading("Panel de métricas"));
  }

  @Test
  @Order(4)
  @DisplayName("TC-J04 El jugador sin equipo puede crear partidas")
  void tcJ04() {
    loginAs("sinequipo");
    app.expectVisible(button("Crear partida"));
  }

  @Test
  @Order(5)
  @DisplayName("TC-J05 El jugador bloqueado sigue viendo el calendario de su equipo")
  void tcJ05() {
    loginAs("blocked");
    app.expectVisible(text("QA MAD · Partida próxima"));
  }

  @Test
  @Order(6)
  @DisplayName("TC-J06 El admin MAD tiene una pestaña de Facturación")
  void tcJ06() {
    loginAs("admin_mad");
    app.expectVisible(button("Facturación"));
  }

  @Test
  @Order(7)
  @DisplayName("TC-J07 El jugador MAD puede exportar el calendario a PDF")
  void tcJ07() {
    loginAs("player_mad");
    app.expectVisible(button("Exportar calendario (PDF)"));
  }
}
