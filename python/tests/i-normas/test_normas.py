"""Suite I · Normas (PDF del equipo en el bucket PRIVADO team-documents). Mismos casos que
qa-playwright/tests/i-normas/normas.spec.ts: subida, enlace firmado, la URL pública no sirve,
borrado (fila + fichero) y quién puede subir y ver. El reset borra las filas y sus ficheros."""
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

import pytest
from selenium.webdriver.common.by import By

from qa.app_page import btn, button, contains_text_xpath

pytestmark = pytest.mark.usefixtures("reset_staging")

# PDF mínimo válido (una página en blanco).
PDF = (b"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
       b"3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n")
FORM = "//form[.//button[normalize-space(.)='Publicar PDF']]"
FILE_INPUT = (By.XPATH, f"{FORM}//input[@type='file']")
TITLE = (By.XPATH, f"{FORM}//input[@placeholder='Ej. Estatutos Oficiales FOE']")
PUBLISH = (By.XPATH, f"{FORM}//button[normalize-space(.)='Publicar PDF']")
ERROR = (By.XPATH, f"{FORM}//*[contains(@class,'modal__error')]")
FULLSCREEN = (By.XPATH, "//a[normalize-space(.)='Pantalla completa']")


@pytest.fixture
def files(tmp_path: Path):
    pdf, txt = tmp_path / "normas-qa.pdf", tmp_path / "notas.txt"
    pdf.write_bytes(PDF)
    txt.write_text("hola")
    return {"pdf": str(pdf), "txt": str(txt)}


def http_status(url: str, method: str = "GET", headers: dict | None = None, body: bytes | None = None) -> tuple[int, bytes]:
    req = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            return res.status, res.read()
    except urllib.error.HTTPError as e:
        return e.code, b""


def upload_statutes(app, files, title: str) -> None:
    app.open_tab("Normas")
    app.click(button("+ Subir PDF"))
    app.driver.find_element(*FILE_INPUT).send_keys(files["pdf"])
    app.set_value(TITLE, title)
    app.click(PUBLISH)
    app.expect_toast("Documento PDF publicado correctamente.")
    app.expect_hidden((By.XPATH, FORM))


def doc_title(title: str):
    return By.XPATH, contains_text_xpath(title)


@pytest.mark.case("TC-I01 Subir unos estatutos: se ven con enlace firmado y la URL pública no los sirve")
def test_tc_i01_subir(app, login_as, files):
    login_as("admin_foe")
    title = f"QA E2E · Estatutos {int(time.time() * 1000)}"
    upload_statutes(app, files, title)

    app.expect_visible(doc_title(title))
    href = app.expect_visible(FULLSCREEN).get_attribute("href")
    assert re.search(r"/storage/v1/object/sign/team-documents/foe/statutes/.+\.pdf\?token=", href), href
    status, body = http_status(href)
    assert status == 200 and body[:5] == b"%PDF-", status
    # Mismo objeto por la ruta pública (sin firma): el bucket es privado.
    public = href.replace("/object/sign/", "/object/public/").split("?")[0]
    assert http_status(public)[0] != 200


@pytest.mark.case("TC-I02 Eliminar un documento borra también el fichero")
def test_tc_i02_eliminar(app, login_as, files):
    login_as("admin_foe")
    title = f"QA E2E · Estatutos a borrar {int(time.time() * 1000)}"
    upload_statutes(app, files, title)
    href = app.expect_visible(FULLSCREEN).get_attribute("href")

    app.click(button("Eliminar"))
    app.expect_toast("Documento eliminado.")
    app.expect_count(doc_title(title), 0)

    # ¿Sigue el fichero en Storage? No se reutiliza el enlace firmado (la CDN puede seguir
    # sirviéndolo ~60 s): se pide al servidor un enlace NUEVO; si el fichero no existe, lo rechaza.
    origin = re.match(r"https?://[^/]+", href).group(0)
    path = re.sub(r"^.*/object/sign/team-documents/", "", href.split("?")[0])
    token, apikey = app.driver.execute_script("""
        const key = Object.keys(localStorage).find((k) => k.endsWith('-auth-token'));
        return [key ? JSON.parse(localStorage.getItem(key)).access_token : null, window.__qa.apikey || null];""")
    assert token and apikey
    status, _ = http_status(f"{origin}/storage/v1/object/sign/team-documents/{path}", "POST",
                            {"apikey": apikey, "Authorization": f"Bearer {token}", "Content-Type": "application/json"},
                            json.dumps({"expiresIn": 60}).encode())
    assert status != 200, f"Storage aún firma {path}: el fichero no se borró"


@pytest.mark.case("TC-I03 El formulario exige título y solo acepta PDF")
def test_tc_i03_validacion(app, login_as, files):
    login_as("admin_foe")
    app.open_tab("Normas")
    app.click(button("+ Subir PDF"))

    app.click(PUBLISH)
    app.expect_text(ERROR, "Indica un título para el documento.")
    app.driver.find_element(*FILE_INPUT).send_keys(files["txt"])
    app.expect_text(ERROR, "Solo se permiten archivos en formato PDF (.pdf).")
    app.set_value(TITLE, "QA E2E · Sin fichero")
    app.click(PUBLISH)
    app.expect_text(ERROR, "Selecciona un archivo PDF.")


@pytest.mark.case("TC-I04 Un jugador ve las normas de su equipo pero no puede subir ni borrar")
def test_tc_i04_jugador_ve(app, login_as, files):
    login_as("admin_foe")
    title = f"QA E2E · Normas para jugadores {int(time.time() * 1000)}"
    upload_statutes(app, files, title)

    login_as("player_foe")
    app.open_tab("Normas")
    app.expect_visible(doc_title(title))
    app.expect_count(button("+ Subir PDF"), 0)
    app.expect_count(button("Eliminar"), 0)


@pytest.mark.case("TC-I05 Un jugador de otro equipo no ve las normas del FOE")
def test_tc_i05_otro_equipo_no_ve(app, login_as, files):
    login_as("admin_foe")
    title = f"QA E2E · Normas solo FOE {int(time.time() * 1000)}"
    upload_statutes(app, files, title)

    login_as("player_mad")
    app.open_tab("Normas")
    app.expect_visible((By.XPATH, contains_text_xpath("Todavía no hay estatutos o normativa publicados para MAD Irregulars.")))
    app.expect_count((By.CSS_SELECTOR, "#doc-squad-select"), 0)
    app.expect_count(doc_title(title), 0)
