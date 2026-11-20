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
    const args = arguments, self = this;
    // apikey pública con la que la app habla con Supabase (para que un test pregunte a Storage).
    try { const k = new Headers((init && init.headers) || (input && input.headers) || {}).get("apikey"); if (k) qa.apikey = k; } catch (e) {}
    qa.inflight++; qa.total++; qa.last = Date.now();
    // Retraso opcional para tests de carreras: window.__qa.delay = { pattern, ms } (solo GET).
    const method = (init && init.method) || (input && input.method) || "GET";
    const ms = qa.delay && method === "GET" && new RegExp(qa.delay.pattern).test(url) ? qa.delay.ms : 0;
    const run = () => original.apply(self, args);
    const p = ms ? new Promise((r) => setTimeout(r, ms)).then(run).then((res) => { qa.delayed = (qa.delayed || 0) + 1; return res; }) : run();
    return p.finally(() => { qa.inflight--; qa.last = Date.now(); });
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
