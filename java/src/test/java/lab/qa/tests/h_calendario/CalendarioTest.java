package lab.qa.tests.h_calendario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import static lab.qa.pages.AppPage.*;

import lab.qa.support.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;

/**
 * Suite H · Calendario: fechas de vacaciones / "No se juega" (team_blackouts). Mismos casos que
 * qa-playwright/tests/h-calendario. Seed: "QA · Sin partida FOE" (+25 a +26) y "QA · Sin partida MAD" (+27).
 */
@Tag("h-calendario")
@DisplayName("Suite H · Calendario y fechas sin partida")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarioTest extends BaseTest {
  private static final By MARK_BUTTON = button("Marcar vacaciones / \"No se juega\"");
  private static final String FORM = "//form[.//button[normalize-space(.)='Guardar fechas sin partida']]";
  private static final String DATES = FORM + "//input[@type='date']";
  private static final By REASON = By.xpath(FORM + "//input[@placeholder='Ej. Vacaciones de verano / Puente']");
  private static final By SAVE = By.xpath(FORM + "//button[normalize-space(.)='Guardar fechas sin partida']");

  @BeforeAll
  static void seed() {
    resetStaging();
  }

  private static String inDays(int days) {
    return LocalDate.now().plusDays(days).toString();
  }

  /** Fila del listado "Vacaciones / Sin partida": fila > botón "fechas · motivo" + Quitar. */
  private static String blackoutRow(String reason) {
    return "//button[contains(normalize-space(.)," + xpathLiteralPublic("· " + reason) + ")]/..";
  }

  private static String xpathLiteralPublic(String s) {
    return s.contains("'") ? "\"" + s + "\"" : "'" + s + "'";
  }

  private void openForm() {
    app.openTab("Calendario");
    app.click(MARK_BUTTON);
  }

  @Test
  @Order(1)
  @DisplayName("TC-H01 Un admin marca fechas sin partida para su equipo")
  void tcH01_marcar() {
    loginAs("admin_mad");
    openForm();
    String reason = "QA E2E · Puente " + System.currentTimeMillis();
    app.setValue(By.xpath("(" + DATES + ")[1]"), inDays(40));
    app.setValue(By.xpath("(" + DATES + ")[2]"), inDays(42));
    app.setValue(REASON, reason);
    app.click(SAVE);

    app.expectToast("Fechas de vacaciones / \"No se juega\" guardadas.");
    app.expectHidden(By.xpath(FORM));
    app.expectContainsText(By.xpath(blackoutRow(reason)), "[MAD Irregulars] " + inDays(40) + " → " + inDays(42));
  }

  @Test
  @Order(2)
  @DisplayName("TC-H02 La fecha de fin no puede ser anterior a la de inicio")
  void tcH02_finAntesQueInicio() {
    loginAs("admin_mad");
    openForm();
    String reason = "QA E2E · Fechas al revés " + System.currentTimeMillis();
    app.setValue(By.xpath("(" + DATES + ")[1]"), inDays(50));
    app.setValue(By.xpath("(" + DATES + ")[2]"), inDays(45));
    app.setValue(REASON, reason);
    app.click(SAVE);

    // "Hasta" lleva min = "Desde": la validación del propio navegador impide enviar el formulario
    // (el mensaje de la app no llega a salir). No se guarda nada.
    Object underflow = app.js("return arguments[0].validity.rangeUnderflow;",
        app.driver().findElement(By.xpath("(" + DATES + ")[2]")));
    assertEquals(Boolean.TRUE, underflow);
    app.expectVisible(By.xpath(FORM));
    app.expectCount(By.xpath(blackoutRow(reason)), 0);
  }

  @Test
  @Order(3)
  @DisplayName("TC-H03 Un admin de un solo equipo no puede marcar fechas para ambos")
  void tcH03_adminUnEquipo() {
    loginAs("admin_mad");
    openForm();
    assertEquals(List.of("MAD Irregulars"), app.dropdownOptions(By.xpath(FORM + "//*[@data-testid='blackout-squad-select']")));
  }

  @Test
  @Order(4)
  @DisplayName("TC-H04 El super admin puede marcar fechas para ambos equipos")
  void tcH04_superAmbos() {
    loginAs("superadmin");
    openForm();
    assertEquals(List.of("MAD Irregulars", "FOE", "Ambos equipos (MAD Irregulars y FOE)"),
        app.dropdownOptions(By.xpath(FORM + "//*[@data-testid='blackout-squad-select']")));
    // Y ve las fechas de los dos equipos.
    app.expectVisible(By.xpath(blackoutRow("QA · Sin partida FOE")));
    app.expectVisible(By.xpath(blackoutRow("QA · Sin partida MAD")));
  }

  @Test
  @Order(5)
  @DisplayName("TC-H05 Cada equipo solo ve sus fechas y un jugador no puede quitarlas")
  void tcH05_visibilidad() {
    loginAs("player_mad");
    app.openTab("Calendario");
    String mad = blackoutRow("QA · Sin partida MAD");
    app.expectVisible(By.xpath(mad));
    app.expectCount(in(mad, btn("Quitar")), 0);
    app.expectCount(By.xpath(blackoutRow("QA · Sin partida FOE")), 0);
    app.expectHidden(MARK_BUTTON);
  }

  @Test
  @Order(6)
  @DisplayName("TC-H06 Un admin quita las fechas de su equipo y no persisten al recargar")
  void tcH06_quitar() {
    loginAs("admin_mad");
    app.openTab("Calendario");
    app.click(in(blackoutRow("QA · Sin partida MAD"), btn("Quitar")));

    app.expectToast("Fechas eliminadas del calendario.");
    app.expectCount(By.xpath(blackoutRow("QA · Sin partida MAD")), 0);
    app.driver().navigate().refresh();
    app.waitForQuietNetwork();
    app.openTab("Calendario");
    app.expectVisible(MARK_BUTTON);
    app.expectCount(By.xpath(blackoutRow("QA · Sin partida MAD")), 0);
  }

  @Test
  @Order(7)
  @DisplayName("TC-H07 Un día sin partida se avisa al seleccionarlo en el calendario")
  void tcH07_avisoEnElDia() {
    loginAs("admin_foe");
    app.openTab("Calendario");
    // Pulsar la fila lleva el calendario a ese mes y selecciona el primer día.
    app.click(By.xpath("//button[contains(normalize-space(.),'· QA · Sin partida FOE')]"));
    String aviso = containsTextXpath("No se juega (FOE):");
    app.expectVisible(By.xpath(aviso));
    app.expectContainsText(By.xpath(aviso + "/.."), "QA · Sin partida FOE");
  }
}
