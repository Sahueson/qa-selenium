#!/usr/bin/env bash
# Ejecuta una o varias suites, opcionalmente solo algunos tests, con el staging reservado.
#   SUITE=a-auth[,b-eventos...]  GREP='tcA01_x|tcB02_y' (opcional)  ./run.sh
# Las suites son @Tag (-Dgroups admite varias separadas por comas) y GREP son nombres de método
# (-Dtest=*#m1+m2). SUITE y GREP llegan de la web: se limpian a caracteres seguros.
set -euo pipefail
cd "$(dirname "$0")"
suites=$(printf %s "${SUITE:?Falta SUITE}" | tr -cd 'a-z0-9,-')
grep=$(printf %s "${GREP:-}" | tr -cd 'A-Za-z0-9_|')
args=(-B test "-Dgroups=$suites")
[ -n "$grep" ] && args+=("-Dtest=*Test#${grep//|/+}" -Dsurefire.failIfNoSpecifiedTests=false)
exec node ../shared/reset-staging/with-staging.js -- mvn "${args[@]}"
