package lab.qa.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Eventos en vivo para la pantalla de ejecución de la web (tabla run_events).
 * Tipos: plan (nº de tests), test_started, test_passed, test_failed, test_skipped y log.
 *
 * Nunca rompe ni retrasa un test: sin RUN_ID/credenciales no hace nada, los eventos salen en cola
 * desde un hilo (en el orden en que ocurren), con tope de 5 s y los errores se ignoran.
 * LIVE_DEBUG=1 los imprime en vez de enviarlos.
 */
public final class LiveEvents {
  private static final String URL = System.getenv("SUPABASE_URL");
  private static final String KEY = System.getenv("SUPABASE_SERVICE_KEY");
  private static final String RUN_ID = System.getenv("RUN_ID");
  private static final boolean DEBUG = System.getenv("LIVE_DEBUG") != null;
  private static final boolean ENABLED = notEmpty(URL) && notEmpty(KEY) && notEmpty(RUN_ID);

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private static final ExecutorService QUEUE = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "live-events");
    t.setDaemon(true);
    return t;
  });

  private LiveEvents() {}

  private static boolean notEmpty(String s) {
    return s != null && !s.isEmpty();
  }

  public static void emit(String type, String message) {
    String text = clean(message);
    if (DEBUG) {
      System.out.println("[live] " + type + ": " + text);
      return;
    }
    if (!ENABLED) return;
    QUEUE.submit(() -> {
      try {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("run_id", RUN_ID);
        body.put("type", type);
        body.put("message", text);
        body.put("created_at", Instant.now().toString());
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL + "/rest/v1/run_events"))
            .timeout(Duration.ofSeconds(5))
            .header("apikey", KEY)
            .header("Authorization", "Bearer " + KEY)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
            .build();
        HTTP.send(request, HttpResponse.BodyHandlers.discarding());
      } catch (Exception e) {
        // los eventos son best-effort
      }
    });
  }

  /** Espera (hasta 10 s) a que salgan los eventos pendientes. */
  public static void flush() {
    if (!ENABLED) return;
    try {
      QUEUE.submit(() -> {}).get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      // sin esperar más
    }
  }

  private static String clean(String text) {
    String s = (text == null ? "" : text).replaceAll("\u001B\\[[0-9;]*m", "").replaceAll("\\s+", " ").trim();
    String pwd = System.getenv("QA_PASSWORD");
    if (notEmpty(pwd)) s = s.replace(pwd, "***");
    return s.length() > 300 ? s.substring(0, 297) + "..." : s;
  }
}
