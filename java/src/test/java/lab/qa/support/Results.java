package lab.qa.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * result.json en el formato común del lab (el mismo que escribe qa-playwright). Las capturas van
 * en evidence_path; shared/publish-results.js las sube y cierra el run. Se reescribe tras cada
 * test: si la ejecución se corta, lo que ya terminó no se pierde.
 */
public final class Results {
  public record TestResult(String name, String status, long duration_ms, String role, String error, String evidence_path) {}

  private static final Instant STARTED = Instant.now();
  private static final List<TestResult> TESTS = new ArrayList<>();
  private static final File FILE = new File("result.json");

  private Results() {}

  public static synchronized void add(TestResult result) {
    TESTS.add(result);
    write();
  }

  private static void write() {
    Instant now = Instant.now();
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("total", TESTS.size());
    summary.put("passed", count("passed"));
    summary.put("failed", count("failed"));
    summary.put("skipped", count("skipped"));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("tool", "selenium-java");
    out.put("target", "tactical-calendar");
    out.put("suite", System.getenv().getOrDefault("SUITE", "local"));
    out.put("started_at", STARTED.toString());
    out.put("finished_at", now.toString());
    out.put("duration_ms", now.toEpochMilli() - STARTED.toEpochMilli());
    out.put("summary", summary);
    out.put("tests", TESTS);
    try {
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(FILE, out);
    } catch (IOException e) {
      System.err.println("[qa-lab] No se pudo escribir result.json: " + e.getMessage());
    }
  }

  private static long count(String status) {
    return TESTS.stream().filter(t -> t.status().equals(status)).count();
  }
}
