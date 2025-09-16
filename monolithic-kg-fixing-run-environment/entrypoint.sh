#!/bin/bash
set -euo pipefail

# Use Java 21 installed via SDKMAN
JAVA_CMD="${JAVA_HOME21}/bin/java"

exec "$JAVA_CMD" \
  -Djava.library.path=/root/native-libs \
  -jar /root/MonolithicKGFixer-1.0-SNAPSHOT.jar \
  -t "${TBOX_FILE_PATH}" \
  -se "${SPARQL_ENDPOINT}" \
  -r 2 \
  -graph http://enexa.eu \
  -hdfs . \
  -outputDir /root/exported-output \
  -inputDir /root/Input \
  -mode 1 \
  -fixS 1 \
  -mcd \
  -or 1 \
  -expLim 8 \
  -reasoner-timeout 1200000 \
  -fixing-timeout 1200000
