package lab.qa.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
 * Suite I · Normas (PDF del equipo en el bucket PRIVADO team-documents). Mismos casos que
 * qa-playwright/tests/i-normas: subida, enlace firmado, la URL pública no sirve, borrado (fila +
 * fichero) y quién puede subir y ver. El reset borra las filas y sus ficheros.
 */
@Tag("i-normas")
@DisplayName("Suite I · Normas del equipo")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NormasTest extends BaseTest {
  // PDF mínimo válido (una página en blanco).
  private static final String PDF = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
      + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
      + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n";
  private static final String FORM = "//form[.//button[normalize-space(.)='Publicar PDF']]";
  private static final By FILE_INPUT = By.xpath(FORM + "//input[@type='file']");
  private static final By TITLE = By.xpath(FORM + "//input[@placeholder='Ej. Estatutos Oficiales FOE']");
  private static final By PUBLISH = By.xpath(FORM + "//button[normalize-space(.)='Publicar PDF']");
  private static final By ERROR = By.xpath(FORM + "//*[contains(@class,'modal__error')]");
  private static final By FULLSCREEN = By.xpath("//a[normalize-space(.)='Pantalla completa']");
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static Path pdf;
  private static Path txt;

  @BeforeAll
  static void seed() throws IOException {
    resetStaging();
    pdf = Files.createTempFile("normas-qa", ".pdf");
    Files.writeString(pdf, PDF, StandardCharsets.ISO_8859_1);
    txt = Files.createTempFile("notas", ".txt");
    Files.writeString(txt, "hola");
  }

  private static HttpResponse<byte[]> http(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void uploadStatutes(String title) {
    app.openTab("Normas");
    app.click(button("+ Subir PDF"));
    app.driver().findElement(FILE_INPUT).sendKeys(pdf.toString());
    app.setValue(TITLE, title);
    app.click(PUBLISH);
    app.expectToast("Documento PDF publicado correctamente.");
    app.expectHidden(By.xpath(FORM));
  }

  @Test
  @Order(1)
  @DisplayName("TC-I01 Subir unos estatutos: se ven con enlace firmado y la URL pública no los sirve")
  void subir() {
    loginAs("admin_foe");
    String title = "QA E2E · Estatutos " + System.currentTimeMillis();
    uploadStatutes(title);

    app.expectVisible(text(title));
    String href = app.expectVisible(FULLSCREEN).getDomAttribute("href");
    assertTrue(href.matches(".*/storage/v1/object/sign/team-documents/foe/statutes/.+\\.pdf\\?token=.*"), href);
    HttpResponse<byte[]> signed = http(HttpRequest.newBuilder(URI.create(href)).build());
    assertEquals(200, signed.statusCode());
    assertEquals("%PDF-", new String(Arrays.copyOf(signed.body(), 5), StandardCharsets.ISO_8859_1));
    // Mismo objeto por la ruta pública (sin firma): el bucket es privado.
    String publicUrl = href.replace("/object/sign/", "/object/public/").replaceAll("\\?.*$", "");
    assertNotEquals(200, http(HttpRequest.newBuilder(URI.create(publicUrl)).build()).statusCode());
  }

  @Test
  @Order(2)
  @DisplayName("TC-I02 Eliminar un documento borra también el fichero")
  void eliminar() {
    loginAs("admin_foe");
    String title = "QA E2E · Estatutos a borrar " + System.currentTimeMillis();
    uploadStatutes(title);
    String href = app.expectVisible(FULLSCREEN).getDomAttribute("href");

    app.click(button("Eliminar"));
    app.expectToast("Documento eliminado.");
    app.expectCount(text(title), 0);

    // ¿Sigue el fichero en Storage? No se reutiliza el enlace firmado (la CDN puede seguir
    // sirviéndolo ~60 s): se pide al servidor un enlace NUEVO; si el fichero no existe, lo rechaza.
    URI uri = URI.create(href);
    String path = uri.getPath().replaceFirst("^.*/object/sign/team-documents/", "");
    @SuppressWarnings("unchecked")
    List<String> auth = (List<String>) app.js("""
        const key = Object.keys(localStorage).find((k) => k.endsWith('-auth-token'));
        return [key ? JSON.parse(localStorage.getItem(key)).access_token : null, window.__qa.apikey || null];""");
    assertNotNull(auth.get(0));
    assertNotNull(auth.get(1));
    HttpResponse<byte[]> resign = http(HttpRequest.newBuilder(
            URI.create(uri.getScheme() + "://" + uri.getHost() + "/storage/v1/object/sign/team-documents/" + path))
        .header("apikey", auth.get(1)).header("Authorization", "Bearer " + auth.get(0))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\":60}")).build());
    assertNotEquals(200, resign.statusCode(), "Storage aún firma " + path + ": el fichero no se borró");
  }

  @Test
  @Order(3)
  @DisplayName("TC-I03 El formulario exige título y solo acepta PDF")
  void validacion() {
    loginAs("admin_foe");
    app.openTab("Normas");
    app.click(button("+ Subir PDF"));

    app.click(PUBLISH);
    app.expectText(ERROR, "Indica un título para el documento.");
    app.driver().findElement(FILE_INPUT).sendKeys(txt.toString());
    app.expectText(ERROR, "Solo se permiten archivos en formato PDF (.pdf).");
    app.setValue(TITLE, "QA E2E · Sin fichero");
    app.click(PUBLISH);
    app.expectText(ERROR, "Selecciona un archivo PDF.");
  }

  @Test
  @Order(4)
  @DisplayName("TC-I04 Un jugador ve las normas de su equipo pero no puede subir ni borrar")
  void jugadorVe() {
    loginAs("admin_foe");
    String title = "QA E2E · Normas para jugadores " + System.currentTimeMillis();
    uploadStatutes(title);

    loginAs("player_foe");
    app.openTab("Normas");
    app.expectVisible(text(title));
    app.expectCount(button("+ Subir PDF"), 0);
    app.expectCount(button("Eliminar"), 0);
  }

  @Test
  @Order(5)
  @DisplayName("TC-I05 Un jugador de otro equipo no ve las normas del FOE")
  void otroEquipoNoVe() {
    loginAs("admin_foe");
    String title = "QA E2E · Normas solo FOE " + System.currentTimeMillis();
    uploadStatutes(title);

    loginAs("player_mad");
    app.openTab("Normas");
    app.expectVisible(text("Todavía no hay estatutos o normativa publicados para MAD Irregulars."));
    app.expectCount(By.cssSelector("#doc-squad-select"), 0);
    app.expectCount(text(title), 0);
  }
}
