#!/bin/bash

LOGFILE="/mnt/logs/serve_logs/serve.log"
MAXSIZE=104857600   # 100 MB in bytes
INTERVAL=60         # Check every 60 seconds

while true; do
    if [ -f "$LOGFILE" ]; then
        actualsize=$(stat -c%s "$LOGFILE")
        if [ "$actualsize" -gt "$MAXSIZE" ]; then
            echo "[$(date)] Truncating $LOGFILE (current size: $actualsize bytes)..."
            : > "$LOGFILE"
        fi
    fi
    sleep "$INTERVAL"
done
