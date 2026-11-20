"""Page object de Tactical Calendar, equivalente al de qa-playwright (pages/app-page.ts).

La app solo tiene data-testid en el login QA; el resto se localiza por texto visible (XPath),
como lo haría un usuario. Las esperas son explícitas: 10 s, como el expect de Playwright.
"""
import time

from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webdriver import WebDriver
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from qa.env import base_url

TIMEOUT = 10
Locator = tuple[str, str]


def xpath_literal(s: str) -> str:
    """Comillas seguras en XPath 1.0 (los textos de la app llevan comillas y apóstrofos)."""
    if "'" not in s:
        return f"'{s}'"
    if '"' not in s:
        return f'"{s}"'
    return "concat('" + s.replace("'", "',\"'\",'") + "')"


# ---------- Localizadores por texto (equivalentes a getByRole de Playwright) ----------

def button(text: str) -> Locator:
    """Botón cuyo texto visible es exactamente `text`."""
    return By.XPATH, f"//button[normalize-space(.)={xpath_literal(text)}]"


def heading(text: str) -> Locator:
    """Encabezado (h1-h4) con exactamente ese texto."""
    return By.XPATH, (f"//*[self::h1 or self::h2 or self::h3 or self::h4]"
                      f"[normalize-space(.)={xpath_literal(text)}]")


def contains_text_xpath(t: str) -> str:
    """XPath relativo del elemento MÁS INTERNO cuyo texto completo contiene `t` (como getByText
    de Playwright). No vale text()[contains(...)]: React parte textos con variables en varios
    nodos ("…(iniciada por", " ", "QA Admin MAD", ")")."""
    lit = xpath_literal(t)
    return f"//*[contains(normalize-space(.),{lit})][not(*[contains(normalize-space(.),{lit})])]"


def text(t: str) -> Locator:
    """Cualquier elemento cuyo texto contiene `t`."""
    return By.XPATH, contains_text_xpath(t)


def nav_tab(label: str) -> Locator:
    return By.XPATH, f"//nav[@aria-label='Navegación principal']//button[normalize-space(.)={xpath_literal(label)}]"


LOGIN_BUTTON: Locator = (By.CSS_SELECTOR, "[data-testid='qa-login-btn']")
NAV: Locator = (By.CSS_SELECTOR, "nav[aria-label='Navegación principal']")
NEW_EVENT_BUTTON: Locator = button("+ Nueva partida")
LOGOUT_BUTTON: Locator = (By.XPATH, "//*[contains(@class,'app-header')]//button[normalize-space(.)='Salir']")
PROFILE_BUTTON: Locator = (By.CSS_SELECTOR, "[title='Editar mi ficha de operador']")
EVENT_CARD: Locator = (By.CSS_SELECTOR, "article.event-card")
MODAL = "//*[contains(concat(' ',@class,' '),' modal ')]"


def modal_with_heading(title: str) -> str:
    """XPath del modal abierto que contiene un encabezado con ese texto (para componer)."""
    return f"{MODAL}[.//*[self::h1 or self::h2 or self::h3][normalize-space(.)={xpath_literal(title)}]]"


def event_card(title: str) -> str:
    """XPath de la tarjeta de partida (article.event-card) que contiene ese título."""
    return f"//article[contains(@class,'event-card')][.//*[contains(normalize-space(.),{xpath_literal(title)})]]"


CARD = "contains(concat(' ',@class,' '),' tactical-card ')"


def player_card(nickname: str) -> str:
    """XPath de la ficha de un jugador (pestaña Jugadores). El título lleva " [BLOQUEADO]" si
    está bloqueado o " (Tú)" si es el usuario con sesión."""
    names = " or ".join(f"normalize-space(.)={xpath_literal(nickname + suf)}" for suf in ("", " [BLOQUEADO]", " (Tú)"))
    return f"//*[{CARD}][.//*[self::h2 or self::h3 or self::h4][{names}]][not(.//*[{CARD}])]"


def attendance_of(nickname: str) -> Locator:
    """Bloque de asistencia de la ficha: "Asistencia 2026 · 60% (1,5/2,5)" + "Cumple mínimo..."."""
    return By.XPATH, (player_card(nickname) + "//*[contains(@class,'roster-card__label')]"
                      "[starts-with(normalize-space(.),'Asistencia ')]/..")


def poll_card(title: str) -> str:
    """XPath de la tarjeta de una votación por (parte de) su título."""
    return (f"//*[{CARD}][.//*[self::h2 or self::h3 or self::h4][contains(normalize-space(.),{xpath_literal(title)})]]"
            f"[not(.//*[{CARD}])]")


def leave_row(reason: str) -> str:
    """XPath de la fila de excedencias por su motivo: <fila><info><motivo/></info><botones/></fila>."""
    return f"//*[text()[normalize-space(.)={xpath_literal(reason)}]]/../.."


def btn_containing(t: str) -> str:
    """XPath relativo de un botón cuyo texto contiene `t`."""
    return f"//button[contains(normalize-space(.),{xpath_literal(t)})]"


def in_(container_xpath: str, inner_xpath: str) -> Locator:
    """Localizador dentro de un contenedor: in_(MODAL, "//button[...]")."""
    return By.XPATH, container_xpath + inner_xpath


def btn(text: str) -> str:
    """XPath relativo de un botón con ese texto exacto (para componer con in_)."""
    return f"//button[normalize-space(.)={xpath_literal(text)}]"


class AppPage:
    def __init__(self, driver: WebDriver):
        self.driver = driver

    # ---------- Esperas ----------

    def expect_visible(self, locator: Locator):
        try:
            return WebDriverWait(self.driver, TIMEOUT, 0.2).until(EC.visibility_of_element_located(locator))
        except Exception:
            raise AssertionError(f"No se ve {locator[1]}") from None

    def expect_hidden(self, locator: Locator) -> None:
        """Oculto o inexistente."""
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(EC.invisibility_of_element_located(locator))
        except Exception:
            raise AssertionError(f"Sigue visible {locator[1]}") from None

    def expect_count(self, locator: Locator, count: int) -> None:
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(lambda d: len(d.find_elements(*locator)) == count)
        except Exception:
            raise AssertionError(f"No hay {count} elemento(s) {locator[1]}") from None

    def expect_contains_text(self, locator: Locator, t: str) -> None:
        """Compara con el texto del DOM (textContent), como toContainText de Playwright. El .text de
        Selenium es el texto renderizado: con text-transform: uppercase (p. ej. el nombre en el
        botón de la ficha) devolvería "QA JUGADOR MAD" en vez de "QA Jugador MAD"."""
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(
                lambda d: t in (d.find_element(*locator).get_attribute("textContent") or ""))
        except Exception:
            raise AssertionError(f"{locator[1]} no contiene «{t}»") from None

    def expect_title_not_empty(self) -> None:
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(lambda d: bool((d.title or "").strip()))
        except Exception:
            raise AssertionError("La página no tiene título") from None

    def expect_toast(self, t: str) -> None:
        """Toast (.tactical-toast) que contiene el texto."""
        self.expect_visible((By.XPATH, f"//*[contains(@class,'tactical-toast')][contains(normalize-space(.),{xpath_literal(t)})]"))

    def click(self, locator: Locator) -> None:
        try:
            el = WebDriverWait(self.driver, TIMEOUT, 0.2).until(EC.element_to_be_clickable(locator))
        except Exception:
            raise AssertionError(f"No se puede pulsar {locator[1]}") from None
        el.click()

    def wait_for_quiet_network(self, quiet_ms: int = 700, timeout_ms: int = 15_000) -> None:
        """Espera a que la app no tenga peticiones a Supabase en vuelo durante `quiet_ms`.

        El contador lo inyecta browser.start() por CDP (window.__qa) antes de que cargue la app.
        """
        deadline = time.monotonic() + timeout_ms / 1000
        while True:
            inflight, idle = self.driver.execute_script(
                "return window.__qa ? [window.__qa.inflight, Date.now() - window.__qa.last] : [0, 1e9];")
            if inflight <= 0 and idle >= quiet_ms:
                return
            if time.monotonic() > deadline:
                raise AssertionError(f"La app no dejó de hacer peticiones en {timeout_ms} ms")
            time.sleep(0.1)

    def requests_started(self) -> int:
        """Peticiones a Supabase lanzadas desde que cargó la página (contador de browser.py)."""
        return self.driver.execute_script("return window.__qa ? window.__qa.total : 0;")

    def wait_for_request_after(self, before: int, timeout_s: float = 15) -> None:
        """Espera a que se lance al menos una petición nueva desde `before`."""
        try:
            WebDriverWait(self.driver, timeout_s, 0.1).until(lambda d: self.requests_started() > before)
        except Exception:
            raise AssertionError("La app no lanzó la petición esperada") from None

    def set_value(self, locator: Locator, value: str) -> None:
        """Escribe en un input como lo haría el usuario, también en <input type=date/time>
        (send_keys en un date depende del formato del idioma del navegador). Usa el setter nativo
        y dispara input/change para que React se entere (equivale a fill() de Playwright)."""
        el = self.expect_visible(locator)
        self.driver.execute_script("""
            const [el, v] = arguments;
            const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
            Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, v);
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        """, el, value)

    def expect_text(self, locator: Locator, t: str) -> None:
        """El elemento tiene exactamente ese texto (sin espacios sobrantes)."""
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(
                lambda d: (d.find_element(*locator).get_attribute("textContent") or "").strip() == t)
        except Exception:
            raise AssertionError(f"{locator[1]} no dice «{t}»") from None

    def expect_value(self, locator: Locator, value: str) -> None:
        """El input/select tiene ese valor (como toHaveValue de Playwright)."""
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(
                lambda d: d.find_element(*locator).get_property("value") == value)
        except Exception:
            raise AssertionError(f"{locator[1]} no vale «{value}»") from None

    def expect_enabled(self, locator: Locator, enabled: bool = True) -> None:
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(lambda d: d.find_element(*locator).is_enabled() == enabled)
        except Exception:
            raise AssertionError(f"{locator[1]} no está {'habilitado' if enabled else 'deshabilitado'}") from None

    def expect_checked(self, locator: Locator, checked: bool = True) -> None:
        try:
            WebDriverWait(self.driver, TIMEOUT, 0.2).until(lambda d: d.find_element(*locator).is_selected() == checked)
        except Exception:
            raise AssertionError(f"{locator[1]} no está {'marcado' if checked else 'desmarcado'}") from None

    def open_leaves_panel(self) -> None:
        self.open_tab("Jugadores")
        self.click((By.XPATH, "//button[starts-with(normalize-space(.),'Excedencias FOE')]"))

    def open_foe_section(self, label: str) -> None:
        self.open_tab("FOE · Votos y Faltas")
        self.click(button(label))

    # ---------- Login QA ----------

    def open(self) -> None:
        self.driver.get(base_url() + "/")

    def login(self, email: str, password: str) -> None:
        email_input = self.expect_visible((By.CSS_SELECTOR, "[data-testid='qa-email-input']"))
        email_input.clear()
        email_input.send_keys(email)
        pwd_input = self.driver.find_element(By.CSS_SELECTOR, "[data-testid='qa-password-input']")
        pwd_input.clear()
        pwd_input.send_keys(password)
        self.click(LOGIN_BUTTON)

    # ---------- Navegación y partidas ----------

    def open_tab(self, label: str) -> None:
        self.click(nav_tab(label))

    def open_event(self, title: str) -> None:
        """Abre el detalle de una partida. El modal recarga los datos al abrirse (loadCloudData):
        se espera a la petición lanzada tras el clic y a que la red quede en reposo."""
        self.open_tab("Partidas")
        before = self.requests_started()
        self.click((By.XPATH, event_card(title) + "//button[contains(@class,'tactical-btn--blue')]"))
        self.expect_visible((By.XPATH, modal_with_heading("Detalle de partida")))
        self.wait_for_request_after(before)
        self.wait_for_quiet_network()

    # Formulario de inscripción del detalle de partida: equipación y rol.
    REGISTRATION_GEAR: Locator = (By.XPATH, f"({MODAL}//*[contains(@class,'inscribir-form')]//select)[1]")
    REGISTRATION_ROLE: Locator = (By.XPATH, f"({MODAL}//*[contains(@class,'inscribir-form')]//select)[2]")

    def visible_event_titles(self) -> list[str]:
        """Títulos de las partidas que el usuario ve en la pestaña Partidas."""
        self.open_tab("Partidas")
        self.expect_visible(EVENT_CARD)
        return [e.text for e in self.driver.find_elements(By.CSS_SELECTOR, "article.event-card .event-card__title")]
