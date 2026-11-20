package lab.qa.tests;

import static lab.qa.pages.AppPage.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import lab.qa.support.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Suite A · Visibilidad por rol (RBAC): lo que impone el RLS del staging, visto en la interfaz.
 * Mismos casos que qa-playwright/tests/a-auth/rbac.spec.ts. Se comprueba qué se ve y qué no,
 * sin contar el total, para que añadir partidas al seed no rompa estos tests.
 */
@Tag("a-auth")
@DisplayName("Suite A · Visibilidad por rol")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RbacTest extends BaseTest {
  private static final List<String> MAD = List.of("QA MAD · Partida próxima", "QA MAD · Partida en excedencia");
  private static final List<String> FOE = List.of(
      "QA FOE · Partida próxima", "QA FOE · MilSim próxima", "QA FOE · Partida en menos de 48 h",
      "QA FOE · Partida finalizada", "QA FOE · Inscripción abierta");
  private static final String VOTACION_DIRECTIVA = "QA · Votación de la Junta Directiva";

  // Las clases van por orden alfabético (AuthTest, EventosTest, RbacTest...): si se ejecutan
  // varias suites juntas, esta no debe ver lo que haya creado la B.
  @BeforeAll
  static void seed() {
    resetStaging();
  }

  private static void assertSees(List<String> titles, List<String> expected) {
    for (String t : expected) {
      assertTrue(titles.stream().anyMatch(x -> x.contains(t)), "No se ve la partida «" + t + "»");
    }
  }

  @Test
  @Order(1)
  @DisplayName("TC-A06 Jugador MAD solo ve partidas MAD y no tiene pestaña FOE")
  void jugadorMad() {
    loginAs("player_mad");
    List<String> titles = app.visibleEventTitles();
    assertSees(titles, MAD);
    assertTrue(titles.stream().allMatch(x -> x.contains("QA MAD")), "Ve partidas que no son MAD: " + titles);
    app.expectHidden(navTab("FOE · Votos y Faltas"));
  }

  @Test
  @Order(2)
  @DisplayName("TC-A07 Jugador FOE ve las partidas FOE y ninguna MAD")
  void jugadorFoe() {
    loginAs("player_foe");
    List<String> titles = app.visibleEventTitles();
    assertSees(titles, FOE);
    assertTrue(titles.stream().allMatch(x -> x.contains("QA FOE")), "Ve partidas que no son FOE: " + titles);
    app.expectVisible(navTab("FOE · Votos y Faltas"));
  }

  @Test
  @Order(3)
  @DisplayName("TC-A08 Super admin ve las partidas de ambos equipos")
  void superAdmin() {
    loginAs("superadmin");
    assertSees(app.visibleEventTitles(), Stream.concat(MAD.stream(), FOE.stream()).toList());
  }

  @Test
  @Order(4)
  @DisplayName("TC-A09 Jugador FOE sin cargo no ve la Junta Directiva")
  void jugadorFoeSinJunta() {
    loginAs("player_foe");
    app.openTab("FOE · Votos y Faltas");
    app.expectVisible(button("Faltas y Sanciones"));
    app.expectHidden(button("Votos · Junta Directiva"));
    app.expectHidden(text(VOTACION_DIRECTIVA));
  }

  @Test
  @Order(5)
  @DisplayName("TC-A10 Miembro de la Junta Directiva ve su votación")
  void miembroJunta() {
    loginAs("admin_foe");
    app.openTab("FOE · Votos y Faltas");
    app.click(button("Votos · Junta Directiva"));
    app.expectVisible(text(VOTACION_DIRECTIVA));
  }
}
