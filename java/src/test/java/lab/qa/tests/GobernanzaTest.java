package lab.qa.tests;

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
 * Suite E · Gobernanza y régimen disciplinario FOE. Mismos casos que qa-playwright/tests/e-gobernanza.
 * El recuento, el cierre y el escalado los decide el servidor (close_foe_poll). Junta Directiva =
 * QA Super Admin, QA Admin FOE y QA Junta 2; el seed deja cada expediente a falta del voto de
 * junta2, salvo el de recusación.
 */
@Tag("e-gobernanza")
@DisplayName("Suite E · Gobernanza FOE")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GobernanzaTest extends BaseTest {
  private static final String DIRECTIVA = "Votos · Junta Directiva";
  private static final String A_FAVOR = btnContaining("A favor de sancionar");
  private static final String EN_CONTRA = btnContaining("En contra de sancionar");

  @BeforeAll
  static void seed() {
    resetStaging();
  }

  @Test
  @Order(1)
  @DisplayName("TC-E01 La Junta Directiva abre un expediente sancionador")
  void abrirExpediente() {
    loginAs("admin_foe");
    app.openFoeSection(DIRECTIVA);
    app.click(button("+ Nueva votación"));

    String form = "//form[contains(normalize-space(.),'Convocar votación en Junta Directiva')]";
    app.selectByText(in(form, "//select[option[normalize-space(.)='Votación de sanción a operador']]"), "Votación de sanción a operador");
    app.selectByText(in(form, "//select[option[normalize-space(.)='QA Jugador FOE']]"), "QA Jugador FOE");
    app.selectByValue(in(form, "//select[option[normalize-space(.)='Falta Grave']]"), "grave");
    app.setValue(in(form, "//*[self::input or self::textarea][contains(@placeholder,'Art. 51.i')]"), "Art. 36.2 — Baja < 48h");
    app.setValue(in(form, "//*[self::input or self::textarea][contains(@placeholder,'Explica los hechos')]"),
        "Baja sin aviso en la partida del domingo (E2E).");
    app.click(in(form, btn("Publicar votación")));

    String card = pollCard("Propuesta de Falta Grave a QA Jugador FOE");
    app.expectVisible(By.xpath(card));
    app.expectVisible(in(card, containsTextXpath("Art. 36.2 — Baja < 48h")));
    app.expectEnabled(in(card, A_FAVOR), true);
  }

  @Test
  @Order(2)
  @DisplayName("TC-E02 El propuesto para sanción queda recusado y no puede votar")
  void recusado() {
    loginAs("junta2");
    app.openFoeSection(DIRECTIVA);
    String card = pollCard("QA · Sanción a QA Junta 2 (recusación)");
    app.expectVisible(in(card, containsTextXpath("Recusación (Art. 53.4)")));
    app.expectEnabled(in(card, A_FAVOR), false);
    app.expectEnabled(in(card, EN_CONTRA), false);
  }

  @Test
  @Order(3)
  @DisplayName("TC-E03 El expediente se cierra solo al votar el último miembro habilitado")
  void cierrePorCuorum() {
    loginAs("junta2");
    app.openFoeSection(DIRECTIVA);
    String card = pollCard("QA · Sanción con cuórum pendiente");
    app.click(in(card, A_FAVOR));
    app.expectToast("Sanción aprobada (2 a favor vs 0 en contra) y registrada");
    app.expectVisible(in(card, containsTextXpath("SANCIÓN APROBADA Y REGISTRADA")));
  }

  @Test
  @Order(4)
  @DisplayName("TC-E04 La 3ª falta leve en un año se escala a Grave")
  void escaladoLeve() {
    loginAs("junta2");
    app.openFoeSection(DIRECTIVA);
    app.click(in(pollCard("QA · Tercera falta leve"), A_FAVOR));
    app.expectToast("Escalada a Grave por 3ª falta leve en 1 año · Art. 51.k");
  }

  @Test
  @Order(5)
  @DisplayName("TC-E05 La 3ª falta grave en un año se escala a Muy Grave")
  void escaladoGrave() {
    loginAs("junta2");
    app.openFoeSection(DIRECTIVA);
    app.click(in(pollCard("QA · Tercera falta grave"), A_FAVOR));
    app.expectToast("Escalada a Muy Grave por 3ª falta grave en 1 año · Art. 50.m");
  }

  @Test
  @Order(6)
  @DisplayName("TC-E06 Con mayoría en contra el expediente se cierra sin sanción")
  void desestimada() {
    loginAs("junta2");
    app.openFoeSection(DIRECTIVA);
    String card = pollCard("QA · Sanción a desestimar");
    app.click(in(card, EN_CONTRA));
    app.expectToast("Votación cerrada (0 a favor vs 3 en contra). No se aplica sanción");
    app.expectVisible(in(card, containsTextXpath("SANCIÓN DESESTIMADA")));
  }

  @Test
  @Order(7)
  @DisplayName("TC-E07 Solo quien convoca (o el super admin) puede cerrar una votación a mano")
  void soloConvocanteCierra() {
    loginAs("junta2"); // no convocó la votación del seed
    app.openFoeSection(DIRECTIVA);
    app.expectHidden(in(pollCard("QA · Votación de la Junta Directiva"), btn("Cerrar y resolver")));
  }

  @Test
  @Order(8)
  @DisplayName("TC-E08 Quien convocó la votación puede cerrarla a mano")
  void convocanteCierra() {
    loginAs("admin_foe");
    app.openFoeSection(DIRECTIVA);
    String card = pollCard("QA · Votación de la Junta Directiva");
    app.click(in(card, btn("Cerrar y resolver")));
    app.expectToast("Votación cerrada y resultado fijado.");
    app.expectHidden(in(card, btn("Cerrar y resolver")));
  }
}
