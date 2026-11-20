#!/usr/bin/env bash
# Ejecuta una o varias suites, opcionalmente solo algunos tests, con el staging reservado.
#   SUITE=a-auth[,b-eventos...]  GREP='test_tc_a01_x|test_tc_b02_y' (opcional)  ./run.sh
# Lo usan el workflow y la ejecución local. SUITE y GREP llegan de la web (lo que elige un
# visitante): se limpian a caracteres seguros y nunca se interpolan en el shell.
set -euo pipefail
cd "$(dirname "$0")"
suites=$(printf %s "${SUITE:?Falta SUITE}" | tr -cd 'a-z0-9,-')
grep=$(printf %s "${GREP:-}" | tr -cd 'A-Za-z0-9_|')
args=()
IFS=',' read -ra list <<< "$suites"
for s in "${list[@]}"; do [ -n "$s" ] && args+=("tests/$s"); done
[ -n "$grep" ] && args+=(-k "${grep//|/ or }")
pytest_bin=$([ -x .venv/bin/pytest ] && echo .venv/bin/pytest || echo pytest)  # local: venv; CI: el instalado
exec node ../shared/reset-staging/with-staging.js -- "$pytest_bin" "${args[@]}"
