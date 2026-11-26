package lab.qa.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * Ciclo de vida de cada test: abre Chrome, y al terminar (pase o falle) hace una captura JPEG
 * -la única evidencia que se publica: nunca logs, que podrían llevar la contraseña- cierra el
 * navegador y anota el resultado en result.json.
 */
public class QaLabExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {
  private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(QaLabExtension.class);
  private static final File EVIDENCE_DIR = new File("target/evidence");

  @Override
  public void beforeEach(ExtensionContext ctx) {
    LiveEvents.emit("test_started", ctx.getDisplayName());
    BaseTest test = (BaseTest) ctx.getRequiredTestInstance();
    ChromeDriver driver = Browser.start();
    test.attach(driver);
    ctx.getStore(NS).put("start", System.currentTimeMillis());
  }

  @Override
  public void afterEach(ExtensionContext ctx) {
    BaseTest test = (BaseTest) ctx.getRequiredTestInstance();
    WebDriver driver = test.driver();
    if (driver == null) return;
    try {
      File file = new File(EVIDENCE_DIR, slug(ctx.getDisplayName()) + ".jpg");
      saveJpeg(((ChromeDriver) driver).getScreenshotAs(OutputType.BYTES), file);
      ctx.getStore(NS).put("evidence", "target/evidence/" + file.getName());
      try (FileInputStream in = new FileInputStream(file)) {
        Allure.attachment("Captura final", "image/jpeg", in, AttachmentOptions.withFileExtension(".jpg"));
      }
    } catch (Exception e) {
      System.err.println("[qa-lab] Sin captura para " + ctx.getDisplayName() + ": " + e.getMessage());
    } finally {
      ctx.getStore(NS).put("end", System.currentTimeMillis());
      driver.quit();
    }
  }

  @Override
  public void testSuccessful(ExtensionContext ctx) {
    record(ctx, "passed", null);
  }

  @Override
  public void testFailed(ExtensionContext ctx, Throwable cause) {
    record(ctx, "failed", cause);
  }

  @Override
  public void testAborted(ExtensionContext ctx, Throwable cause) {
    record(ctx, "skipped", null);
  }

  @Override
  public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
    Results.add(new Results.TestResult(ctx.getDisplayName(), "skipped", 0, null, null, null));
    LiveEvents.emit("test_skipped", ctx.getDisplayName());
  }

  private void record(ExtensionContext ctx, String status, Throwable cause) {
    ExtensionContext.Store store = ctx.getStore(NS);
    Long start = store.get("start", Long.class);
    Long end = store.get("end", Long.class);
    long duration = start == null ? 0 : (end != null ? end : System.currentTimeMillis()) - start;
    BaseTest test = (BaseTest) ctx.getTestInstance().orElse(null);
    Results.add(new Results.TestResult(
        ctx.getDisplayName(), status, duration,
        test == null ? null : test.role(),
        cause == null ? null : firstLine(cause),
        store.get("evidence", String.class)));
    LiveEvents.emit("test_" + status, ctx.getDisplayName());
    if (cause != null) LiveEvents.emit("log", "   ↳ " + firstLine(cause));
  }

  private static String firstLine(Throwable t) {
    String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    String line = msg.lines().findFirst().orElse(msg);
    String pwd = System.getenv("QA_PASSWORD");
    return pwd != null && !pwd.isEmpty() ? line.replace(pwd, "***") : line;
  }

  // Selenium da PNG; se guarda como JPEG (como en Playwright) para que pese ~10 veces menos.
  private static void saveJpeg(byte[] png, File file) throws IOException {
    BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
    BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
    rgb.createGraphics().drawImage(src, 0, 0, null);
    file.getParentFile().mkdirs();
    ImageIO.write(rgb, "jpg", file);
  }

  static String slug(String s) {
    String plain = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    String slug = plain.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    return slug.length() > 80 ? slug.substring(0, 80) : slug;
  }
}
