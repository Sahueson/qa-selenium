package lab.qa.pages;

import java.time.Duration;
import java.util.List;
import lab.qa.support.Env;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object de Tactical Calendar, equivalente al de qa-playwright (pages/app-page.ts). La app
 * solo tiene data-testid en el login QA; el resto se localiza por texto visible (XPath), como lo
 * haría un usuario. Las esperas son explícitas: 10 s, como el expect de Playwright.
 */
public class AppPage {
  public static final By LOGIN_BUTTON = By.cssSelector("[data-testid='qa-login-btn']");
  public static final By NAV = By.cssSelector("nav[aria-label='Navegación principal']");
  public static final By NEW_EVENT_BUTTON = button("+ Nueva partida");
  public static final By LOGOUT_BUTTON = By.xpath("//*[contains(@class,'app-header')]//button[normalize-space(.)='Salir']");
  public static final By PROFILE_BUTTON = By.cssSelector("[title='Editar mi ficha de operador']");
  public static final By EVENT_CARD = By.cssSelector("article.event-card");

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private final WebDriver driver;

  public AppPage(WebDriver driver) {
    this.driver = driver;
  }

  public WebDriver driver() {
    return driver;
  }

  // ---------- Localizadores por texto (equivalentes a getByRole de Playwright) ----------

  /** Botón cuyo texto visible es exactamente `text`. */
  public static By button(String text) {
    return By.xpath("//button[normalize-space(.)=" + xpathLiteral(text) + "]");
  }

  /** Botón cuyo texto visible empieza por `prefix`. */
  public static By buttonStartingWith(String prefix) {
    return By.xpath("//button[starts-with(normalize-space(.)," + xpathLiteral(prefix) + ")]");
  }

  /** Encabezado (h1-h4) con exactamente ese texto. */
  public static By heading(String text) {
    return By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4][normalize-space(.)=" + xpathLiteral(text) + "]");
  }

  /**
   * XPath relativo del elemento MÁS INTERNO cuyo texto completo contiene `text` (como getByText de
   * Playwright). No vale text()[contains(...)]: React parte textos con variables en varios nodos
   * ("…(iniciada por", " ", "QA Admin MAD", ")").
   */
  public static String containsTextXpath(String text) {
    String lit = xpathLiteral(text);
    return "//*[contains(normalize-space(.)," + lit + ")][not(*[contains(normalize-space(.)," + lit + ")])]";
  }

  /** Cualquier elemento cuyo texto contiene `text`. */
  public static By text(String text) {
    return By.xpath(containsTextXpath(text));
  }

  public static By navTab(String label) {
    return By.xpath("//nav[@aria-label='Navegación principal']//button[normalize-space(.)=" + xpathLiteral(label) + "]");
  }

  /** Modal abierto que contiene un encabezado con ese texto. */
  public static By modalWithHeading(String title) {
    return By.xpath("//*[contains(concat(' ',@class,' '),' modal ')][.//*[self::h1 or self::h2 or self::h3][normalize-space(.)="
        + xpathLiteral(title) + "]]");
  }

  // ---------- Piezas XPath para componer (contenedor + elemento) ----------

  public static final String MODAL = "//*[contains(concat(' ',@class,' '),' modal ')]";

  /** XPath del modal abierto que contiene un encabezado con ese texto. */
  public static String modalXpath(String title) {
    return MODAL + "[.//*[self::h1 or self::h2 or self::h3][normalize-space(.)=" + xpathLiteral(title) + "]]";
  }

  /** XPath de la tarjeta de partida (article.event-card) que contiene ese título. */
  public static String eventCard(String title) {
    return "//article[contains(@class,'event-card')][.//*[contains(normalize-space(.)," + xpathLiteral(title) + ")]]";
  }

  /** XPath relativo de un botón con ese texto exacto. */
  public static String btn(String text) {
    return "//button[normalize-space(.)=" + xpathLiteral(text) + "]";
  }

  /** Localizador dentro de un contenedor: in(MODAL, btn("Guardar")). */
  public static By in(String containerXpath, String innerXpath) {
    return By.xpath(containerXpath + innerXpath);
  }

  /** Comillas seguras en XPath 1.0 (los textos de la app llevan comillas y apóstrofos). */
  static String xpathLiteral(String s) {
    if (!s.contains("'")) return "'" + s + "'";
    if (!s.contains("\"")) return "\"" + s + "\"";
    return "concat('" + s.replace("'", "',\"'\",'") + "')";
  }

  // ---------- Esperas ----------

  public WebElement expectVisible(By locator) {
    return new WebDriverWait(driver, TIMEOUT)
        .withMessage("No se ve " + locator)
        .until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  /** Oculto o inexistente. */
  public void expectHidden(By locator) {
    new WebDriverWait(driver, TIMEOUT)
        .withMessage("Sigue visible " + locator)
        .until(ExpectedConditions.invisibilityOfElementLocated(locator));
  }

  public void expectCount(By locator, int count) {
    new WebDriverWait(driver, TIMEOUT)
        .withMessage("No hay " + count + " elemento(s) " + locator)
        .until(d -> d.findElements(locator).size() == count);
  }

  /**
   * Compara con el texto del DOM (textContent), como toContainText de Playwright. getText() de
   * Selenium es el texto renderizado: con text-transform: uppercase (p. ej. el nombre en el botón
   * de la ficha) devolvería "QA JUGADOR MAD" en vez de "QA Jugador MAD".
   */
  public void expectContainsText(By locator, String text) {
    new WebDriverWait(driver, TIMEOUT)
        .withMessage(locator + " no contiene «" + text + "»")
        .ignoring(org.openqa.selenium.NoSuchElementException.class)
        .until(d -> {
          String content = d.findElement(locator).getDomProperty("textContent");
          return content != null && content.contains(text);
        });
  }

  public void expectTitleNotEmpty() {
    new WebDriverWait(driver, TIMEOUT).withMessage("La página no tiene título")
        .until(d -> d.getTitle() != null && !d.getTitle().isBlank());
  }

  /** Toast (.tactical-toast) que contiene el texto. */
  public void expectToast(String text) {
    expectVisible(By.xpath("//*[contains(@class,'tactical-toast')][contains(normalize-space(.)," + xpathLiteral(text) + ")]"));
  }

  public void click(By locator) {
    new WebDriverWait(driver, TIMEOUT)
        .withMessage("No se puede pulsar " + locator)
        .until(ExpectedConditions.elementToBeClickable(locator))
        .click();
  }

  /**
   * Espera a que la app no tenga peticiones a Supabase en vuelo durante `quietMs`. El contador lo
   * inyecta Browser por CDP (window.__qa) antes de que cargue la app.
   */
  public void waitForQuietNetwork() {
    waitForQuietNetwork(700, 15_000);
  }

  public void waitForQuietNetwork(long quietMs, long timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    JavascriptExecutor js = (JavascriptExecutor) driver;
    while (true) {
      @SuppressWarnings("unchecked")
      List<Number> state = (List<Number>) js.executeScript(
          "return window.__qa ? [window.__qa.inflight, Date.now() - window.__qa.last] : [0, 1e9];");
      if (state.get(0).longValue() <= 0 && state.get(1).longValue() >= quietMs) return;
      if (System.currentTimeMillis() > deadline) {
        throw new IllegalStateException("La app no dejó de hacer peticiones en " + timeoutMs + " ms");
      }
      sleep(100);
    }
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Peticiones a Supabase lanzadas desde que cargó la página (contador de Browser). */
  public long requestsStarted() {
    Object n = ((JavascriptExecutor) driver).executeScript("return window.__qa ? window.__qa.total : 0;");
    return ((Number) n).longValue();
  }

  /** Espera a que se lance al menos una petición nueva desde `before`. */
  public void waitForRequestAfter(long before) {
    new WebDriverWait(driver, Duration.ofSeconds(15))
        .withMessage("La app no lanzó la petición esperada")
        .until(d -> requestsStarted() > before);
  }

  /**
   * Escribe en un input como lo haría el usuario, también en input type=date/time (sendKeys en un
   * date depende del formato del idioma del navegador). Usa el setter nativo y dispara
   * input/change para que React se entere (equivale a fill() de Playwright).
   */
  public void setValue(By locator, String value) {
    WebElement el = expectVisible(locator);
    ((JavascriptExecutor) driver).executeScript("""
        const [el, v] = arguments;
        const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
        Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, v);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        """, el, value);
  }

  /** El elemento tiene exactamente ese texto (sin espacios sobrantes). */
  public void expectText(By locator, String text) {
    new WebDriverWait(driver, TIMEOUT)
        .withMessage(locator + " no dice «" + text + "»")
        .ignoring(org.openqa.selenium.NoSuchElementException.class)
        .until(d -> {
          String content = d.findElement(locator).getDomProperty("textContent");
          return content != null && content.strip().equals(text);
        });
  }

  // ---------- Login QA ----------

  public void open() {
    driver.get(Env.baseUrl() + "/");
  }

  public void login(String email, String password) {
    WebElement emailInput = expectVisible(By.cssSelector("[data-testid='qa-email-input']"));
    emailInput.clear();
    emailInput.sendKeys(email);
    WebElement pwdInput = driver.findElement(By.cssSelector("[data-testid='qa-password-input']"));
    pwdInput.clear();
    pwdInput.sendKeys(password);
    click(LOGIN_BUTTON);
  }

  // ---------- Navegación y partidas ----------

  public void openTab(String label) {
    click(navTab(label));
  }

  /**
   * Abre el detalle de una partida. El modal recarga los datos al abrirse (loadCloudData): se
   * espera a la petición lanzada tras el clic y a que la red quede en reposo.
   */
  public void openEvent(String title) {
    openTab("Partidas");
    long before = requestsStarted();
    click(By.xpath(eventCard(title) + "//button[contains(@class,'tactical-btn--blue')]"));
    expectVisible(By.xpath(modalXpath("Detalle de partida")));
    waitForRequestAfter(before);
    waitForQuietNetwork();
  }

  /** Títulos de las partidas que el usuario ve en la pestaña Partidas. */
  public List<String> visibleEventTitles() {
    openTab("Partidas");
    expectVisible(EVENT_CARD);
    return driver.findElements(By.cssSelector("article.event-card .event-card__title")).stream()
        .map(WebElement::getText).toList();
  }
}
