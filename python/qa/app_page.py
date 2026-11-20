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


def text(t: str) -> Locator:
    """Cualquier elemento cuyo texto propio contiene `t`."""
    return By.XPATH, f"//*[text()[contains(normalize-space(.),{xpath_literal(t)})]]"


def nav_tab(label: str) -> Locator:
    return By.XPATH, f"//nav[@aria-label='Navegación principal']//button[normalize-space(.)={xpath_literal(label)}]"


LOGIN_BUTTON: Locator = (By.CSS_SELECTOR, "[data-testid='qa-login-btn']")
NAV: Locator = (By.CSS_SELECTOR, "nav[aria-label='Navegación principal']")
NEW_EVENT_BUTTON: Locator = button("+ Nueva partida")
LOGOUT_BUTTON: Locator = (By.XPATH, "//*[contains(@class,'app-header')]//button[normalize-space(.)='Salir']")
PROFILE_BUTTON: Locator = (By.CSS_SELECTOR, "[title='Editar mi ficha de operador']")
EVENT_CARD: Locator = (By.CSS_SELECTOR, "article.event-card")


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

    def visible_event_titles(self) -> list[str]:
        """Títulos de las partidas que el usuario ve en la pestaña Partidas."""
        self.open_tab("Partidas")
        self.expect_visible(EVENT_CARD)
        return [e.text for e in self.driver.find_elements(By.CSS_SELECTOR, "article.event-card .event-card__title")]
