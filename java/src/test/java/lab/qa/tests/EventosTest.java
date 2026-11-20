package lab.qa.tests;

import static lab.qa.pages.AppPage.*;

import java.time.LocalDate;
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
 * Suite B · Ciclo de vida de partidas. Mismos casos que qa-playwright/tests/b-eventos/eventos.spec.ts:
 * crear, cancelar, reactivar, editar, eliminar, cerrar el acta y desbloquearla (aprobación de los
 * demás responsables, RPC del servidor).
 */
@Tag("b-eventos")
@DisplayName("Suite B · Ciclo de vida de partidas")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventosTest extends BaseTest {
  private static final String TITLE_INPUT = "//input[@placeholder='Ej. Dominical CQB / Operación Tormenta']";
  private static final String FIELD_INPUT = "//input[@placeholder='Ej. El Cortijo / Bunker / Campo Norte']";
  private static final String DATE_INPUT = "//input[@type='date']";
  private static final String TIME_INPUT = "//input[@type='time']";
  private static final String NEW_FORM = modalXpath("Nueva partida");
  private static final String EDIT_FORM = modalXpath("Editar partida");
  private static final String LOCKED = "Partida cerrada y bloqueada";

  @BeforeAll
  static void seed() {
    resetStaging(); // la suite parte del seed
  }

  private static String future(int days) {
    return LocalDate.now().plusDays(days).toString();
  }

  private static By modalText(String text) {
    return in(MODAL, containsTextXpath(text));
  }

  private static By modalButton(String text) {
    return in(MODAL, btn(text));
  }

  private static By cardButton(String title, String text) {
    return in(eventCard(title), btn(text));
  }

  @Test
  @Order(1)
  @DisplayName("TC-B01 Crear partida valida los campos obligatorios")
  void crearValidaCampos() {
    loginAs("admin_mad");
    app.click(NEW_EVENT_BUTTON);
    By submit = in(NEW_FORM, btn("Crear partida"));
    By error = in(NEW_FORM, "//*[contains(@class,'modal__error')]");

    // El formulario propone una fecha por defecto: se vacía para probar su validación.
    app.setValue(in(NEW_FORM, DATE_INPUT), "");
    app.click(submit);
    app.expectText(error, "El título de la partida es obligatorio.");

    app.setValue(in(NEW_FORM, TITLE_INPUT), "QA E2E · Validación");
    app.click(submit);
    app.expectText(error, "Selecciona la fecha de la partida.");

    app.setValue(in(NEW_FORM, DATE_INPUT), future(15));
    app.click(submit);
    app.expectText(error, "Indica el campo de juego.");
  }

  @Test
  @Order(2)
  @DisplayName("TC-B01b Un admin crea una partida y aparece en la lista")
  void crearPartida() {
    loginAs("admin_mad");
    String title = "QA E2E · Partida creada " + System.currentTimeMillis();
    app.click(NEW_EVENT_BUTTON);
    app.setValue(in(NEW_FORM, TITLE_INPUT), title);
    app.setValue(in(NEW_FORM, DATE_INPUT), future(15));
    app.setValue(in(NEW_FORM, TIME_INPUT), "10:00");
    app.setValue(in(NEW_FORM, FIELD_INPUT), "Campo QA E2E");
    app.click(in(NEW_FORM, btn("Crear partida")));

    app.expectToast("Partida creada para MAD Irregulars.");
    app.expectHidden(By.xpath(NEW_FORM));
    app.openTab("Partidas");
    app.expectVisible(By.xpath(eventCard(title)));
  }

  @Test
  @Order(3)
  @DisplayName("TC-B02 Cancelar una partida sin contar asistencia")
  void cancelarSinAsistencia() {
    loginAs("admin_mad");
    app.openEvent("QA MAD · Partida próxima");
    app.click(modalButton("Cancelar partida"));
    app.click(in(MODAL, "//label[contains(normalize-space(.),'No contar asistencia')]//input"));
    app.setValue(in(MODAL, "//input[@placeholder='Ej. Lluvia / Campo cerrado / Falta de jugadores']"), "Lluvia (QA)");
    app.click(modalButton("Confirmar cancelación"));

    app.expectToast("Partida cancelada y descontada del total de partidas.");
    app.expectVisible(modalText("CANCELADA · NO CUENTA"));
    app.expectVisible(modalButton("Reactivar partida"));
  }

  @Test
  @Order(4)
  @DisplayName("TC-B02b Cancelar una partida contando asistencia permite pasar lista")
  void cancelarConAsistencia() {
    loginAs("admin_foe");
    app.openEvent("QA FOE · Partida próxima");
    app.click(modalButton("Cancelar partida"));
    app.click(in(MODAL, "//label[contains(normalize-space(.),'Sí, contar asistencia')]//input"));
    app.click(modalButton("Confirmar cancelación"));

    app.expectToast("Partida cancelada (cuenta asistencia");
    app.expectVisible(modalText("CANCELADA · CUENTA ASISTENCIA"));
  }

  @Test
  @Order(5)
  @DisplayName("TC-B03 Cerrar el acta bloquea la partida en solo lectura")
  void cerrarActa() {
    loginAs("admin_foe");
    app.openEvent("QA FOE · Partida finalizada");
    app.click(modalButton("Guardar asistencia y bloquear partida"));

    app.expectToast("Asistencia guardada y partida bloqueada.");
    app.expectVisible(in(MODAL, "//*[normalize-space(text())='BLOQUEADA']"));
    app.expectVisible(modalText(LOCKED));
    app.expectHidden(modalButton("Editar partida"));
    app.expectHidden(modalButton("Cancelar partida"));
  }

  @Test
  @Order(6)
  @DisplayName("TC-B04 Un jugador no puede editar ni cancelar partidas ajenas")
  void jugadorNoEdita() {
    loginAs("player_foe");
    app.openEvent("QA FOE · MilSim próxima");
    app.expectHidden(modalButton("Editar partida"));
    app.expectHidden(modalButton("Cancelar partida"));
  }

  @Test
  @Order(7)
  @DisplayName("TC-B05 Un admin edita una partida de su equipo")
  void editar() {
    loginAs("admin_mad");
    String title = "QA MAD · Partida editada " + System.currentTimeMillis();
    app.openTab("Partidas");
    app.click(cardButton("QA MAD · Partida a editar", "Editar"));
    app.setValue(in(EDIT_FORM, TITLE_INPUT), title);
    app.click(in(EDIT_FORM, btn("Guardar cambios")));

    app.expectToast("Partida actualizada.");
    app.expectHidden(By.xpath(EDIT_FORM));
    app.expectVisible(By.xpath(eventCard(title)));
    app.expectCount(By.xpath(eventCard("QA MAD · Partida a editar")), 0);
  }

  @Test
  @Order(8)
  @DisplayName("TC-B06 Reactivar una partida cancelada")
  void reactivar() {
    loginAs("admin_foe");
    app.openEvent("QA FOE · Partida cancelada");
    app.click(modalButton("Reactivar partida"));

    app.expectToast("Partida reactivada.");
    app.expectHidden(in(MODAL, "//*[starts-with(normalize-space(text()),'CANCELADA')]"));
    app.expectVisible(modalButton("Cancelar partida"));
  }

  @Test
  @Order(9)
  @DisplayName("TC-B07 Eliminar una partida pide confirmación y la borra del servidor")
  void eliminar() {
    loginAs("admin_mad");
    app.openTab("Partidas");
    String card = "QA MAD · Partida a eliminar";
    app.click(cardButton(card, "Eliminar"));
    // Primer clic solo muestra la confirmación; el botón pasa a "Cancelar".
    app.expectVisible(cardButton(card, "Cancelar"));
    app.click(cardButton(card, "Confirmar eliminar partida"));

    app.expectToast("Partida eliminada.");
    app.expectCount(By.xpath(eventCard(card)), 0);
    // Tras recargar sigue sin estar: se borró en la base de datos, no solo en pantalla.
    app.driver().navigate().refresh();
    app.waitForQuietNetwork();
    app.openTab("Partidas");
    app.expectVisible(By.xpath(eventCard("QA MAD · Partida próxima")));
    app.expectCount(By.xpath(eventCard(card)), 0);
  }

  @Test
  @Order(10)
  @DisplayName("TC-B08 Una partida cerrada no se puede editar ni eliminar desde la lista")
  void cerradaSinBotones() {
    loginAs("admin_mad");
    app.openTab("Partidas");
    String card = "QA MAD · Partida cerrada";
    app.expectVisible(cardButton(card, "Ver acta / Desbloquear"));
    app.expectCount(cardButton(card, "Editar"), 0);
    app.expectCount(cardButton(card, "Eliminar"), 0);
  }

  @Test
  @Order(11)
  @DisplayName("TC-B09 Un jugador no puede pedir el desbloqueo de un acta")
  void jugadorNoDesbloquea() {
    loginAs("player_mad");
    app.openEvent("QA MAD · Partida cerrada");
    app.expectVisible(modalText(LOCKED));
    app.expectHidden(modalButton("Solicitar desbloqueo para editar"));
  }

  @Test
  @Order(12)
  @DisplayName("TC-B10 Desbloquear un acta MAD: la pide su admin y la aprueba el super admin")
  void desbloqueoMad() {
    loginAs("admin_mad");
    app.openEvent("QA MAD · Partida cerrada");
    app.click(modalButton("Solicitar desbloqueo para editar"));
    // Responsables MAD aparte de quien lo pide: solo el super admin.
    app.expectToast("Solicitud enviada. Pendiente de aprobación por 1 responsable.");
    app.expectVisible(modalText("Solicitud de desbloqueo activa (iniciada por QA Admin MAD)"));
    app.expectVisible(modalText("Aprobaciones necesarias (0/1):"));

    loginAs("superadmin");
    app.openEvent("QA MAD · Partida cerrada");
    app.click(modalButton("Aceptar desbloqueo"));
    app.expectToast("Todos han aprobado. Partida desbloqueada.");
    app.expectHidden(modalText(LOCKED));
    app.expectVisible(modalButton("Editar partida"));
  }

  @Test
  @Order(13)
  @DisplayName("TC-B11 Desbloquear un acta FOE necesita a los otros dos responsables y se puede cancelar")
  void desbloqueoFoe() {
    String title = "QA FOE · Partida pasada abierta";
    loginAs("admin_foe");
    app.openEvent(title);
    app.click(modalButton("Guardar asistencia y bloquear partida"));
    app.expectToast("Asistencia guardada y partida bloqueada.");
    app.click(modalButton("Solicitar desbloqueo para editar"));
    // Responsables FOE aparte de admin_foe: super admin y QA Junta 2 (Junta Operativa).
    app.expectToast("Solicitud enviada. Pendiente de aprobación por 2 responsables.");

    loginAs("junta2");
    app.openEvent(title);
    // Solo quien lo pidió (o el super admin) puede cancelar la solicitud.
    app.expectHidden(modalButton("Cancelar solicitud"));
    app.click(modalButton("Aceptar desbloqueo"));
    app.expectToast("Tu aprobación ha quedado registrada.");
    app.expectVisible(modalText("Aprobaciones necesarias (1/2):"));
    app.expectVisible(modalText(LOCKED));

    loginAs("admin_foe");
    app.openEvent(title);
    app.click(modalButton("Cancelar solicitud"));
    app.expectToast("Solicitud de desbloqueo cancelada.");
    app.expectVisible(modalButton("Solicitar desbloqueo para editar"));
    app.expectVisible(modalText(LOCKED));
  }
}
