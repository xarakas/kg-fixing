#!/usr/bin/env bash
set -euo pipefail

: "${SPARK_HOME:?SPARK_HOME not set}"
PROFILE="${SPARK_PROFILE:-dev}"
SRC_DIR="/opt/spark-profiles/${PROFILE}"
CONF_DIR="${SPARK_HOME}/conf"

if [ ! -d "$SRC_DIR" ]; then
  echo "Spark profile '$PROFILE' not found at $SRC_DIR"
  echo "   Available profiles:" && ls -1 /opt/spark-profiles || true
  exit 1
fi

echo "Activating Spark profile: $PROFILE"
mkdir -p "$CONF_DIR"

# copy profile files into SPARK_HOME/conf
for f in spark-defaults.conf spark-env.sh log4j2.properties metrics.properties; do
  [ -f "${SRC_DIR}/${f}" ] && cp -f "${SRC_DIR}/${f}" "${CONF_DIR}/"
done

# ensure spark-env.sh is executable if present
[ -f "${CONF_DIR}/spark-env.sh" ] && chmod +x "${CONF_DIR}/spark-env.sh"

echo "Spark profile '$PROFILE' activated."
