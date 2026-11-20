package lab.qa.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

/** Usuarios QA: la fuente única es users.json de ivan-qa-lab-shared (el mismo que usa el reset). */
public final class QaUsers {
  public record QaUser(String key, String email, String nickname) {}

  private static final List<QaUser> USERS = load();

  private QaUsers() {}

  private static List<QaUser> load() {
    try {
      ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      QaUser[] users = json.readValue(Env.sharedDir().resolve("reset-staging/users.json").toFile(), QaUser[].class);
      return Arrays.asList(users);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo leer users.json de shared", e);
    }
  }

  public static QaUser get(String key) {
    return USERS.stream().filter(u -> u.key().equals(key)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Usuario QA desconocido: " + key));
  }
}
