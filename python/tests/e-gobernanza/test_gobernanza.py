"""Suite E · Gobernanza y régimen disciplinario FOE. Mismos casos que
qa-playwright/tests/e-gobernanza/gobernanza.spec.ts. El recuento, el cierre y el escalado los
decide el servidor (close_foe_poll). Junta Directiva = QA Super Admin, QA Admin FOE y QA Junta 2;
el seed deja cada expediente a falta del voto de junta2, salvo el de recusación."""
import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from qa.app_page import btn, btn_containing, button, contains_text_xpath, in_, poll_card

pytestmark = pytest.mark.usefixtures("reset_staging")  # la suite parte del seed

DIRECTIVA = "Votos · Junta Directiva"
A_FAVOR, EN_CONTRA = btn_containing("A favor de sancionar"), btn_containing("En contra de sancionar")


@pytest.mark.case("TC-E01 La Junta Directiva abre un expediente sancionador")
def test_tc_e01_abrir_expediente(app, login_as):
    login_as("admin_foe")
    app.open_foe_section(DIRECTIVA)
    app.click(button("+ Nueva votación"))

    form = "//form[contains(normalize-space(.),'Convocar votación en Junta Directiva')]"
    select = lambda option: Select(app.expect_visible(  # noqa: E731
        (By.XPATH, f"{form}//select[option[normalize-space(.)='{option}']]")))
    select("Votación de sanción a operador").select_by_visible_text("Votación de sanción a operador")
    select("QA Jugador FOE").select_by_visible_text("QA Jugador FOE")
    select("Falta Grave").select_by_value("grave")
    app.set_value(in_(form, "//*[self::input or self::textarea][contains(@placeholder,'Art. 51.i')]"), "Art. 36.2 — Baja < 48h")
    app.set_value(in_(form, "//*[self::input or self::textarea][contains(@placeholder,'Explica los hechos')]"),
                  "Baja sin aviso en la partida del domingo (E2E).")
    app.click(in_(form, btn("Publicar votación")))

    card = poll_card("Propuesta de Falta Grave a QA Jugador FOE")
    app.expect_visible((By.XPATH, card))
    app.expect_visible(in_(card, contains_text_xpath("Art. 36.2 — Baja < 48h")))
    app.expect_enabled(in_(card, A_FAVOR))


@pytest.mark.case("TC-E02 El propuesto para sanción queda recusado y no puede votar")
def test_tc_e02_recusado(app, login_as):
    login_as("junta2")
    app.open_foe_section(DIRECTIVA)
    card = poll_card("QA · Sanción a QA Junta 2 (recusación)")
    app.expect_visible(in_(card, contains_text_xpath("Recusación (Art. 53.4)")))
    app.expect_enabled(in_(card, A_FAVOR), False)
    app.expect_enabled(in_(card, EN_CONTRA), False)


@pytest.mark.case("TC-E03 El expediente se cierra solo al votar el último miembro habilitado")
def test_tc_e03_cierre_por_cuorum(app, login_as):
    login_as("junta2")
    app.open_foe_section(DIRECTIVA)
    card = poll_card("QA · Sanción con cuórum pendiente")
    app.click(in_(card, A_FAVOR))
    app.expect_toast("Sanción aprobada (2 a favor vs 0 en contra) y registrada")
    app.expect_visible(in_(card, contains_text_xpath("SANCIÓN APROBADA Y REGISTRADA")))


@pytest.mark.case("TC-E04 La 3ª falta leve en un año se escala a Grave")
def test_tc_e04_escalado_leve(app, login_as):
    login_as("junta2")
    app.open_foe_section(DIRECTIVA)
    app.click(in_(poll_card("QA · Tercera falta leve"), A_FAVOR))
    app.expect_toast("Escalada a Grave por 3ª falta leve en 1 año · Art. 51.k")


@pytest.mark.case("TC-E05 La 3ª falta grave en un año se escala a Muy Grave")
def test_tc_e05_escalado_grave(app, login_as):
    login_as("junta2")
    app.open_foe_section(DIRECTIVA)
    app.click(in_(poll_card("QA · Tercera falta grave"), A_FAVOR))
    app.expect_toast("Escalada a Muy Grave por 3ª falta grave en 1 año · Art. 50.m")


@pytest.mark.case("TC-E06 Con mayoría en contra el expediente se cierra sin sanción")
def test_tc_e06_desestimada(app, login_as):
    login_as("junta2")
    app.open_foe_section(DIRECTIVA)
    card = poll_card("QA · Sanción a desestimar")
    app.click(in_(card, EN_CONTRA))
    app.expect_toast("Votación cerrada (0 a favor vs 3 en contra). No se aplica sanción")
    app.expect_visible(in_(card, contains_text_xpath("SANCIÓN DESESTIMADA")))


@pytest.mark.case("TC-E07 Solo quien convoca (o el super admin) puede cerrar una votación a mano")
def test_tc_e07_solo_convocante_cierra(app, login_as):
    login_as("junta2")  # no convocó la votación del seed
    app.open_foe_section(DIRECTIVA)
    app.expect_hidden(in_(poll_card("QA · Votación de la Junta Directiva"), btn("Cerrar y resolver")))


@pytest.mark.case("TC-E08 Quien convocó la votación puede cerrarla a mano")
def test_tc_e08_convocante_cierra(app, login_as):
    login_as("admin_foe")
    app.open_foe_section(DIRECTIVA)
    card = poll_card("QA · Votación de la Junta Directiva")
    app.click(in_(card, btn("Cerrar y resolver")))
    app.expect_toast("Votación cerrada y resultado fijado.")
    app.expect_hidden(in_(card, btn("Cerrar y resolver")))
