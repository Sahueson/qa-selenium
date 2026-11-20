# qa-selenium

Selenium WebDriver contra **Tactical Calendar** (staging), en **Java** y en **Python**, con los
mismos casos y nombres que [qa-playwright](https://github.com/Sahueson/qa-playwright) para poder
comparar herramientas y lenguajes. Forma parte de Ivan QA Lab: la web lanza cada suite en
GitHub Actions y muestra resultados y capturas.

```
java/      Java 21 + JUnit 6 + Selenium 4 (Maven). Suites = @Tag  -> mvn test -Dgroups=a-auth
python/    Python 3.12 + pytest + Selenium 4. Suites = carpetas   -> pytest tests/a-auth
.github/workflows/run-tests-java.yml · run-tests-python.yml
```

Las dos partes siguen el mismo diseño: un page object (`AppPage`), un navegador por test, una
captura JPEG al final de cada test y un `result.json` con el formato común del lab.

- **Staging compartido:** `shared/reset-staging/with-staging.js` (de ivan-qa-lab-shared) reserva
  el staging, lo resetea, ejecuta los tests y libera la reserva. Nunca hay dos ejecuciones a la vez.
- **Publicación:** `shared/publish-results.js` sube las capturas y envía `result.json` a la web.
  Nunca se publican logs: podrían llevar la contraseña QA.
- **Esperar a la red:** Selenium no tiene eventos de red como Playwright. Se inyecta por CDP un
  contador de peticiones `fetch` a Supabase (`window.__qa`) y `waitForQuietNetwork` espera a que
  quede en reposo.

## Ejecutar en local

Requisitos: Chrome, Node 22, JDK 21 + Maven o Python 3.12. `shared` es un enlace a
`../ivan-qa-lab-shared` (en CI se clona).

```bash
ln -s ../ivan-qa-lab-shared shared
set -a; . ~/.qa-lab.env; set +a
export BASE_URL=https://tactical-calendar-demo.vercel.app   # el demo, nunca PROD

# Python
cd python && python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
node ../shared/reset-staging/with-staging.js -- .venv/bin/pytest tests/a-auth

# Java
cd java && node ../shared/reset-staging/with-staging.js -- mvn -B test -Dgroups=a-auth
```

`HEADLESS=0` abre el navegador a la vista.

## Diferencias con Playwright que se notan en los tests

- **Texto:** el `getText()` / `.text` de Selenium es el texto *renderizado*. Con
  `text-transform: uppercase` devuelve mayúsculas ("QA JUGADOR MAD"). Para comparar como
  `toContainText` de Playwright se usa `textContent`.
- **Localizadores:** no hay `getByRole`; se usan XPath por texto visible (`button("Guardar")`,
  `heading("Acceso denegado")`).
- **Esperas:** no hay auto-wait; cada comprobación es una espera explícita de 10 s.
