package lab.qa.tests.g_jugadores;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Suite G · Gestión de jugadores. Solo el super admin cambia rol, bloqueo, equipos y órganos FOE;
 * el efecto se comprueba entrando con el jugador afectado. Mismos casos que qa-playwright/tests/g-jugadores.
 */
@Tag("g-jugadores")
@DisplayName("Suite G · Gestión de jugadores")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JugadoresTest extends BaseTest {
  private static final String ROLE_SELECT = "//select[option[@value='super_admin']]";
  private static final String ADD_SQUAD_SELECT = "//select[option[normalize-space(.)='+ Añadir otro equipo']]";

  @BeforeAll
  static void seed() {
    resetStaging();
  }

  @Test
  @Order(1)
  @DisplayName("TC-G01 El super admin asciende a un jugador a admin y este gestiona partidas")
  void tcG01_ascender() {
    loginAs("superadmin");
    app.openTab("Jugadores");
    app.selectByValue(in(playerCard("QA Jugador MAD"), ROLE_SELECT), "admin");
    app.expectToast("Ficha actualizada para QA Jugador MAD.");

    loginAs("player_mad");
    app.openTab("Partidas");
    String card = eventCard("QA MAD · Partida próxima");
    app.expectVisible(in(card, btn("Editar")));
    app.expectVisible(in(card, btn("Eliminar")));
  }

  @Test
  @Order(2)
  @DisplayName("TC-G02 El super admin bloquea a un jugador y este pierde el acceso")
  void tcG02_bloquear() {
    loginAs("superadmin");
    app.openTab("Jugadores");
    String card = playerCard("QA Jugador FOE");
    app.click(in(card, btn("Bloquear")));
    app.expectToast("Ficha actualizada para QA Jugador FOE.");
    app.expectVisible(in(card, btn("Desbloquear")));
    app.expectVisible(heading("QA Jugador FOE [BLOQUEADO]"));

    loginAs("player_foe");
    app.expectVisible(heading("Acceso denegado"));
    app.expectHidden(NAV);
  }

  @Test
  @Order(3)
  @DisplayName("TC-G03 Asignar equipo a un jugador sin equipo le da acceso a ese equipo")
  void tcG03_asignarEquipo() {
    loginAs("superadmin");
    app.openTab("Jugadores");
    String card = playerCard("QA Sin Equipo");
    app.expectVisible(in(card, "//*[normalize-space(.)='Sin equipo asignado']"));
    app.selectByValue(in(card, ADD_SQUAD_SELECT), "FOE");
    app.expectToast("Ficha actualizada para QA Sin Equipo.");
    app.expectVisible(in(card, "//*[@title='Quitar de FOE']"));

    loginAs("sinequipo");
    app.expectVisible(NAV);
    app.expectVisible(navTab("FOE · Votos y Faltas"));
    List<String> titles = app.visibleEventTitles();
    assertTrue(titles.stream().anyMatch(t -> t.contains("QA FOE · Partida próxima")), titles.toString());
    assertTrue(titles.stream().noneMatch(t -> t.contains("QA MAD")), titles.toString());
  }

  @Test
  @Order(4)
  @DisplayName("TC-G04 Quitar la Junta Directiva a un miembro le oculta sus votaciones")
  void tcG04_quitarJunta() {
    loginAs("superadmin");
    app.openTab("Jugadores");
    String card = playerCard("QA Junta 2");
    app.click(in(card, btn("✓ Junta Directiva")));
    app.expectToast("Ficha actualizada para QA Junta 2.");
    app.expectVisible(in(card, btn("+ Junta Directiva")));

    loginAs("junta2");
    app.openTab("FOE · Votos y Faltas");
    app.expectVisible(button("Votos · Junta Operativa"));
    app.expectHidden(button("Votos · Junta Directiva"));
  }

  @Test
  @Order(5)
  @DisplayName("TC-G05 Un admin que no es super admin no puede cambiar permisos")
  void tcG05_adminSinPermisos() {
    loginAs("admin_foe");
    app.openTab("Jugadores");
    String card = playerCard("QA Junta 2");
    app.expectVisible(By.xpath(card));
    app.expectCount(in(card, ROLE_SELECT), 0);
    app.expectCount(in(card, "//button[normalize-space(.)='Bloquear' or normalize-space(.)='Desbloquear']"), 0);
    app.expectCount(in(card, "//*[@title='Quitar de FOE']"), 0);
    app.expectEnabled(in(card, btnContaining("Junta Operativa")), false);
  }

  @Test
  @Order(6)
  @DisplayName("TC-G06 El super admin no puede cambiarse el rol ni bloquearse a sí mismo")
  void tcG06_superNoSeToca() {
    loginAs("superadmin");
    app.openTab("Jugadores");
    String card = playerCard("QA Super Admin");
    app.expectVisible(By.xpath(card));
    app.expectCount(in(card, ROLE_SELECT), 0);
    app.expectCount(in(card, btn("Bloquear")), 0);
  }

  @Test
  @Order(7)
  @DisplayName("TC-G07 Un admin de equipo solo ve a los jugadores de su equipo")
  void tcG07_adminVeSuEquipo() {
    loginAs("admin_mad");
    app.openTab("Jugadores");
    app.expectVisible(By.xpath(playerCard("QA Jugador MAD")));
    for (String nick : new String[] {"QA Jugador FOE", "QA Admin FOE", "QA Sin Equipo"}) {
      app.expectCount(By.xpath(playerCard(nick)), 0);
    }
  }
}
