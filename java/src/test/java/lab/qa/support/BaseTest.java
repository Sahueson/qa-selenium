package lab.qa.support;

import java.io.IOException;
import lab.qa.pages.AppPage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/** Base de todos los tests: navegador por test (QaLabExtension), page object y login QA. */
@ExtendWith(QaLabExtension.class)
public abstract class BaseTest {
  private WebDriver driver;
  private String role;
  protected AppPage app;

  void attach(WebDriver driver) {
    this.driver = driver;
    this.role = null;
    this.app = new AppPage(driver);
  }

  WebDriver driver() {
    return driver;
  }

  String role() {
    return role;
  }

  /** Rol con el que corre el test (columna "role" de la web). Se queda el primero. */
  protected void role(String description) {
    if (role == null) role = description;
  }

  /** Entra con un usuario QA. Admite cambiar de usuario dentro del mismo test. */
  protected QaUsers.QaUser loginAs(String key) {
    QaUsers.QaUser user = QaUsers.get(key);
    role(user.nickname());
    if (driver.getCurrentUrl() != null && driver.getCurrentUrl().startsWith("http")) {
      ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }
    app.open();
    app.login(user.email(), Env.qaPassword());
    app.expectHidden(AppPage.LOGIN_BUTTON);
    app.waitForQuietNetwork(); // carga inicial de datos tras el login
    return user;
  }

  /**
   * Vuelve a dejar el staging como el seed (al empezar cada suite que lo necesite). La reserva
   * ya la tiene with-staging.js (QA_LOCK_HELD=1): reset.js no la vuelve a pedir.
   */
  protected static void resetStaging() {
    if (System.getenv("STG_URL") == null) return;
    try {
      Process p = new ProcessBuilder("node", Env.sharedDir().resolve("reset-staging/reset.js").toString())
          .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
      if (p.waitFor() != 0) throw new IllegalStateException("El reset del staging falló (código " + p.exitValue() + ")");
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo lanzar el reset del staging", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
