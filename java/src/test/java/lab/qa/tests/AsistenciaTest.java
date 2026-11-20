package lab.qa.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Suite D · Motor de asistencia (Art. 33 y 34). Mismos casos que qa-playwright/tests/d-asistencia.
 * El % se calcula sobre las partidas computables FOE del año (seed: finalizada 1, pasada abierta 1
 * y MilSim pasada 1,5 = 3,5).
 */
@Tag("d-asistencia")
@DisplayName("Suite D · Asistencia")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsistenciaTest extends BaseTest {
  @BeforeAll
  static void seed() {
    resetStaging(); // otras suites alteran ese total
  }

  private static By regCheckbox(String nickname) {
    return in(MODAL, "//*[contains(@class,'event-detail__reg-row')][contains(normalize-space(.),'" + nickname + "')]"
        + "//input[@type='checkbox']");
  }

  @Test
  @Order(1)
  @DisplayName("TC-D01 El % anual se calcula sobre las partidas ya jugadas")
  void porcentajeAnual() {
    loginAs("admin_foe");
    app.openTab("Jugadores");
    By att = attendanceOf("QA Jugador FOE"); // asistió a 1 de 3,5
    app.expectContainsText(att, "29% (1/3,5)");
    app.expectContainsText(att, "Por debajo del mínimo anual FOE (60% · Art. 33)");
  }

  @Test
  @Order(2)
  @DisplayName("TC-D02 Una MilSim pesa x1,5 en la asistencia")
  void milsimPesaMas() {
    loginAs("admin_foe");
    app.openTab("Jugadores");
    // junta2 solo fue a la MilSim pasada: suma 1,5, no 1.
    app.expectContainsText(attendanceOf("QA Junta 2"), "(1,5/");
  }

  @Test
  @Order(3)
  @DisplayName("TC-D03 Las partidas durante una excedencia aprobada no restan")
  void excedenciaNoResta() {
    loginAs("admin_foe");
    app.openTab("Jugadores");
    // Sin la excedencia serían 1,5/3,5 = 43 %; con ella la pasada abierta no computa.
    By att = attendanceOf("QA Junta 2");
    app.expectContainsText(att, "60% (1,5/2,5)");
    app.expectContainsText(att, "Cumple mínimo anual FOE (60%)");
  }

  @Test
  @Order(4)
  @DisplayName("TC-D04 Cerrar el acta con asistencia actualiza el % del jugador")
  void cerrarActaActualiza() {
    loginAs("admin_foe");
    app.openEvent("QA FOE · Partida pasada abierta");
    By box = regCheckbox("QA Jugador FOE");
    app.click(box);
    app.expectChecked(box, true); // sigue marcada al guardar
    app.click(in(MODAL, btn("Guardar asistencia y bloquear partida")));
    app.expectToast("Asistencia guardada y partida bloqueada.");
    app.click(in(MODAL, btn("Cerrar")));

    app.openTab("Jugadores");
    app.expectContainsText(attendanceOf("QA Jugador FOE"), "57% (2/3,5)");
  }

  // Regresión del bug (c), corregido en bfdc0e2: al abrir el detalle la app recarga los datos y
  // esa recarga reiniciaba el borrador de asistencia, borrando lo ya marcado. Para que la recarga
  // llegue SIEMPRE después de la marca, el script inyectado (Browser) la retrasa 1,5 s.
  @Test
  @Order(5)
  @DisplayName("TC-D05 Una marca hecha nada más abrir la partida no se pierde con la recarga")
  void marcaNoSePierde() {
    loginAs("junta2");
    app.openTab("Partidas");
    app.js("window.__qa.delay = { pattern: '/rest/v1/event_registrations', ms: 1500 };");

    app.click(By.xpath(eventCard("QA FOE · Partida finalizada") + "//button[contains(@class,'tactical-btn--blue')]"));
    By box = regCheckbox("QA Admin FOE");
    // El borrador se rellena justo después de pintar el modal (en el primer instante sale vacío).
    app.expectChecked(box, true);
    app.click(box);
    assertEquals(0L, ((Number) app.js("return window.__qa.delayed || 0;")).longValue(),
        "La recarga llegó antes de la marca: el test no prueba la carrera");

    app.waitForQuietNetwork();
    assertTrue(((Number) app.js("return window.__qa.delayed || 0;")).longValue() > 0, "La recarga retrasada no llegó");
    app.expectChecked(box, false);
    app.js("window.__qa.delay = null;");
    app.click(in(MODAL, btn("Guardar asistencia y bloquear partida")));
    app.expectToast("Asistencia guardada y partida bloqueada.");

    // Lo guardado en la base de datos, no solo en pantalla.
    app.driver().navigate().refresh();
    app.waitForQuietNetwork();
    app.openTab("Jugadores");
    app.expectContainsText(attendanceOf("QA Admin FOE"), "0% (0/3,5)");
  }
}
