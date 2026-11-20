package lab.qa.tests.f_excedencias;

import java.time.LocalDate;
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
 * Suite F · Excedencias FOE (Art. 18 y 19): las solicita el jugador, las concede la Junta Directiva
 * y nadie se concede la suya. Duración entre 1 mes y 1 año. Mismos casos que qa-playwright/tests/f-excedencias.
 */
@Tag("f-excedencias")
@DisplayName("Suite F · Excedencias")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExcedenciasTest extends BaseTest {
  private static final String DATES = "//form//input[@type='date']";

  @BeforeAll
  static void seed() {
    resetStaging();
  }

  private void requestLeave(int start, int end, String reason) {
    app.openLeavesPanel();
    app.setValue(By.xpath("(" + DATES + ")[1]"), LocalDate.now().plusDays(start).toString());
    app.setValue(By.xpath("(" + DATES + ")[2]"), LocalDate.now().plusDays(end).toString());
    app.setValue(By.xpath("//input[contains(@placeholder,'Motivos laborales / Lesión')]"), reason);
    app.click(button("Solicitar excedencia a Junta Directiva"));
  }

  @Test
  @Order(1)
  @DisplayName("TC-F01 Un jugador solicita una excedencia y queda pendiente de la Junta")
  void tcF01_solicitar() {
    loginAs("player_foe");
    requestLeave(40, 80, "Excedencia E2E · lesión");
    app.expectToast("Solicitud de excedencia enviada.");
    String row = leaveRow("Excedencia E2E · lesión");
    app.expectContainsText(By.xpath(row), "PENDIENTE DE JUNTA");
    app.expectHidden(in(row, btn("Conceder")));
  }

  @Test
  @Order(2)
  @DisplayName("TC-F02 Una excedencia de menos de 1 mes se rechaza")
  void tcF02_menosDeUnMes() {
    loginAs("player_foe");
    requestLeave(40, 45, "Excedencia E2E · corta");
    app.expectToast("La excedencia debe tener una duración mínima de 1 mes");
    app.expectHidden(text("Excedencia E2E · corta"));
  }

  @Test
  @Order(3)
  @DisplayName("TC-F03 La Junta Directiva concede una solicitud pendiente")
  void tcF03_juntaConcede() {
    loginAs("admin_foe");
    app.openLeavesPanel();
    String row = leaveRow("Solicitud QA pendiente"); // de QA Jugador FOE (seed)
    app.expectContainsText(By.xpath(row), "PENDIENTE DE JUNTA");
    app.click(in(row, btn("Conceder")));
    app.expectToast("Excedencia concedida.");
    app.expectContainsText(By.xpath(row), "CONCEDIDA");
  }

  @Test
  @Order(4)
  @DisplayName("TC-F04 Nadie puede concederse su propia excedencia")
  void tcF04_noAutoconcesion() {
    loginAs("admin_foe"); // es de la Junta Directiva, pero la solicitud es suya
    app.openLeavesPanel();
    String row = leaveRow("Solicitud QA propia");
    app.expectContainsText(By.xpath(row), "PENDIENTE DE JUNTA");
    app.expectHidden(in(row, btn("Conceder")));
  }

  @Test
  @Order(5)
  @DisplayName("TC-F05 Un jugador sin cargo no puede conceder excedencias")
  void tcF05_jugadorNoConcede() {
    loginAs("player_foe");
    app.openLeavesPanel();
    app.expectCount(button("Conceder"), 0);
    app.expectCount(button("Conceder y aplicar excedencia"), 0);
  }
}
