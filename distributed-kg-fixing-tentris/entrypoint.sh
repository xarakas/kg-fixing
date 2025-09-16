#!/bin/bash
set -e

/mnt/truncate_log.sh &


echo ""
echo "####################################################"
echo "Loading all .nt and .ttl files into Tentris..."
echo "####################################################"
echo ""

# Gather all .nt and .ttl files into an array
mapfile -t files < <(find ./data -maxdepth 1 -type f \( -name "*.nt" -o -name "*.ttl" \) | sort)

total_files=${#files[@]}

if [ "$total_files" -eq 0 ]; then
  echo "No .nt or .ttl files found in /mnt/data"
  exit 1
fi

# sysctl vm.dirty_writeback_centisecs=30000
# sysctl vm.dirty_ration=90
# sysctl vm.dirty_background_ratio=80
# sysctl vm.dirty_expire_centisecs=300000000

for i in "${!files[@]}"; do
  file="${files[$i]}"
  extension="${file##*.}"
  filename=$(basename "$file" ."$extension")

  echo ""
  echo "[$((i+1))/$total_files] Loading $file ..."

  output_log="/mnt/logs/load_logs/load_${filename}.${extension}.log"

  if [ "$total_files" -eq 1 ] || [ "$i" -eq $((total_files - 1)) ]; then
    # Only file OR last file — allow snapshot
    nohup ./tentris-bin/tentris load --into-graph "http://enexa.eu" < "$file" \
      > "$output_log" 2>&1
  else
    # All other files — skip snapshot
    nohup ./tentris-bin/tentris load --force-no-snapshot --into-graph "http://enexa.eu" < "$file" \
      > "$output_log" 2>&1
  fi
done

echo ""
echo "All files have been loaded."
echo ""

# Wait a moment to be sure
sleep 2

# Start the Tentris server
echo ""
echo "######################################"
echo "Starting Tentris server..."
echo "######################################"
echo ""

nohup ./tentris-bin/tentris --config ./tentris-bin/tentris-server-config.toml serve \
  > /mnt/logs/serve_logs/serve.log 2>&1 &

# Tail the server log so the container doesn't exit
tail -f /mnt/logs/serve_logs/serve.log
