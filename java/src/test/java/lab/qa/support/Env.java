package lab.qa.support;

import java.nio.file.Path;

/** Configuración por variables de entorno (las mismas que qa-playwright). */
public final class Env {
  private Env() {}

  public static String baseUrl() {
    String url = System.getenv("BASE_URL");
    if (url == null || url.isBlank()) throw new IllegalStateException("Falta BASE_URL (el demo de staging)");
    return url.replaceAll("/+$", "");
  }

  public static String qaPassword() {
    String pwd = System.getenv("QA_PASSWORD");
    if (pwd == null || pwd.isBlank()) throw new IllegalStateException("Falta QA_PASSWORD");
    return pwd;
  }

  /** Código compartido (ivan-qa-lab-shared): en CI se clona en ./shared; en local es un enlace. */
  public static Path sharedDir() {
    String dir = System.getenv("QA_SHARED_DIR");
    return Path.of(dir != null && !dir.isBlank() ? dir : "../shared").toAbsolutePath().normalize();
  }

  public static boolean headless() {
    return !"0".equals(System.getenv("HEADLESS"));
  }
}
