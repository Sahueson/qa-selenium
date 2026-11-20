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

/** Suite C · Inscripciones y reglas de negocio. Mismos casos que qa-playwright/tests/c-inscripciones. */
@Tag("c-inscripciones")
@DisplayName("Suite C · Inscripciones")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InscripcionesTest extends BaseTest {
  @BeforeAll
  static void seed() {
    resetStaging();
  }

  private static By modalText(String text) {
    return in(MODAL, containsTextXpath(text));
  }

  private static By modalButton(String text) {
    return in(MODAL, btn(text));
  }

  @Test
  @Order(1)
  @DisplayName("TC-C01 Inscribirse preselecciona la equipación de la partida y avisa si no la tienes")
  void preseleccionaEquipacionPartida() {
    // junta2 tiene Árido en su ficha; la partida recomienda Tiger Stripe. (Con la ficha vacía la
    // app no avisa: el aviso solo sale si tienes equipaciones marcadas y no es ninguna de ellas.)
    loginAs("junta2");
    app.openEvent("QA FOE · Inscripción abierta");
    app.expectValue(REGISTRATION_GEAR, "Tiger Stripe");
    app.expectVisible(modalText("No tienes «Tiger Stripe» marcada en tu ficha"));

    app.click(modalButton("Apuntarse"));
    app.expectToast("Inscripción guardada en la nube.");
    app.expectVisible(modalText("Inscritos (1)"));
    app.expectVisible(modalText("QA Junta 2 (Tú)"));
    app.expectVisible(modalButton("Actualizar inscripción"));
  }

  @Test
  @Order(2)
  @DisplayName("TC-C07 Sin equipación en la partida se preselecciona la de la ficha del jugador")
  void preseleccionaEquipacionFicha() {
    loginAs("junta2"); // ficha: Árido + Médico
    app.openEvent("QA FOE · MilSim próxima"); // sin equipación recomendada
    app.expectValue(REGISTRATION_GEAR, "Árido");
    app.expectValue(REGISTRATION_ROLE, "medico");
    app.expectHidden(modalText("No tienes «"));
  }

  @Test
  @Order(3)
  @DisplayName("TC-C04 Darse de baja a más de 48 h libera la plaza")
  void bajaLiberaPlaza() {
    loginAs("player_foe");
    app.openEvent("QA FOE · Inscripción abierta");
    app.click(modalButton("Apuntarse"));
    app.expectToast("Inscripción guardada en la nube.");

    app.click(modalButton("Darse de baja"));
    app.expectToast("Has cancelado tu inscripción.");
    app.expectHidden(modalText("QA Jugador FOE (Tú)"));
    app.expectVisible(modalButton("Apuntarse"));
  }

  @Test
  @Order(4)
  @DisplayName("TC-C02 A menos de 48 h no se permite darse de baja")
  void menos48hSinBaja() {
    loginAs("player_foe"); // inscrito por el seed
    app.openEvent("QA FOE · Partida en menos de 48 h");
    app.expectVisible(modalText("Faltan menos de 48 horas para la partida"));
    app.expectHidden(modalButton("Darse de baja"));
    // Sí puede actualizar equipación o rol.
    app.expectVisible(modalButton("Actualizar inscripción"));
  }

  @Test
  @Order(5)
  @DisplayName("TC-C03 Con una excedencia aprobada no se puede inscribir en esas fechas")
  void excedenciaSinInscripcion() {
    loginAs("player_mad"); // excedencia aprobada que cubre la partida
    app.openTab("Partidas");
    app.expectVisible(in(eventCard("QA MAD · Partida en excedencia"), btn("En excedencia")));

    app.openEvent("QA MAD · Partida en excedencia");
    app.expectVisible(modalText("Tienes una excedencia concedida"));
    app.expectHidden(modalButton("Apuntarse"));
  }

  @Test
  @Order(6)
  @DisplayName("TC-C05 Una partida ya finalizada no admite inscripciones")
  void finalizadaSinInscripcion() {
    loginAs("player_foe");
    app.openEvent("QA FOE · Partida pasada abierta");
    app.expectVisible(modalText("ya ha finalizado; las inscripciones y bajas están cerradas"));
    app.expectHidden(modalButton("Apuntarse"));
  }

  @Test
  @Order(7)
  @DisplayName("TC-C06 Una partida cancelada no admite inscripciones")
  void canceladaSinInscripcion() {
    loginAs("player_foe");
    app.openEvent("QA FOE · Partida cancelada");
    app.expectVisible(modalText("CANCELADA · NO CUENTA"));
    app.expectVisible(modalText("está cancelada; no se permiten inscripciones"));
    app.expectHidden(modalButton("Apuntarse"));
  }
}
