"""Chrome para los tests. Selenium Manager descarga el chromedriver que corresponda."""
from selenium import webdriver

from qa.env import headless

# Cuenta las peticiones a Supabase en vuelo (fetch) desde antes de que cargue la app, para poder
# esperar a que la red quede en reposo (AppPage.wait_for_quiet_network). Selenium no tiene
# eventos de red como Playwright; se inyecta por CDP en cada documento nuevo.
NETWORK_COUNTER = r"""
(() => {
  if (window.__qa) return;
  const qa = window.__qa = { inflight: 0, total: 0, last: Date.now() };
  const isApi = (u) => /\/(rest|auth|storage)\/v1\//.test(String(u));
  const original = window.fetch;
  window.fetch = function (input, init) {
    const url = typeof input === "string" ? input : input && input.url;
    if (!isApi(url)) return original.apply(this, arguments);
    qa.inflight++; qa.total++; qa.last = Date.now();
    return original.apply(this, arguments).finally(() => { qa.inflight--; qa.last = Date.now(); });
  };
})();
"""


def start() -> webdriver.Chrome:
    options = webdriver.ChromeOptions()
    if headless():
        options.add_argument("--headless=new")
    # Mismo tamaño que el viewport por defecto de Playwright, para comparar capturas.
    for arg in ("--window-size=1280,720", "--lang=es-ES", "--no-sandbox", "--disable-dev-shm-usage"):
        options.add_argument(arg)
    driver = webdriver.Chrome(options=options)
    driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument", {"source": NETWORK_COUNTER})
    return driver
