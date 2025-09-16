#!/bin/bash

set -e

service ssh start

select-spark-profile

export HDFS_NAMENODE_USER=root
export HDFS_DATANODE_USER=root
export HDFS_SECONDARYNAMENODE_USER=root
export YARN_RESOURCEMANAGER_USER=root
export YARN_NODEMANAGER_USER=root

$HADOOP_HOME/bin/hdfs namenode -format -force
$HADOOP_HOME/sbin/start-dfs.sh
$HADOOP_HOME/sbin/start-yarn.sh

$SPARK_HOME/sbin/start-master.sh
$SPARK_HOME/sbin/start-worker.sh spark://localhost:7077

### ——————————————————————
###  Loop setup
### ——————————————————————
ITER=1

function copy_on_exit {
  echo "Exiting; grabbing any remaining Spark output..."
  hdfs dfs -get "/SparkOutput_${ITER}" /root/exported-output || true
}
trap copy_on_exit EXIT

### ——————————————————————
###  Iterative compute loop
### ——————————————————————
while [ ! -f "/root/exported-output/SparkOutput_$((ITER-1))/stop-loop-flag.txt" ]; do
  echo "====== Iteration $ITER ======"

  if [ "$ITER" -eq 1 ] && [ "${USE_OLD_HADOOP_OUTPUT}" = "true" ] && [ -n "${OLD_HADOOP_OUTPUT_DIR}" ]; then
    echo "Iteration 1: reusing precomputed Hadoop output from ${OLD_HADOOP_OUTPUT_DIR}"

    # clean & stage into HDFS
    hdfs dfs -rm -r -skipTrash /HadoopOutput_1 || true
    hdfs dfs -mkdir -p /HadoopOutput_1
    hdfs dfs -copyFromLocal -f "${OLD_HADOOP_OUTPUT_DIR}"/* /HadoopOutput_1

  else
    # for ITER>1 or fresh run, invoke the Hadoop job
    hdfs dfs -mkdir -p "/HadoopOutput_${ITER}"

    if [ "$ITER" -gt 1 ]; then
      PREV_DIR="/root/exported-output/SparkOutput_$((ITER-1))/position-tracker.json"
      if [ -f "${PREV_DIR}" ]; then
        echo "Copying previous positionTracker..."
        hdfs dfs -put "${PREV_DIR}" "/HadoopOutput_${ITER}"
      else
        echo "No positionTracker for iteration $((ITER-1))"
      fi
    fi

    hadoop jar /root/ComputeExpansions-1.0-SNAPSHOT-jar-with-dependencies.jar \
      -t "${TBOX_FILE_PATH}" \
      -se "${SPARQL_ENDPOINT}" \
      -graph http://enexa.eu \
      -b 100000 \
      -hdfs hdfs://localhost:9000 \
      -outputDir "HadoopOutput_${ITER}" \
      -mh "${MAX_HOP}" \
      -mi "${MAX_ITERATIONS}" -elimEqSets #-elimSub
  fi

  # Only pull HadoopOutput locally if we actually ran the job
  if [ "$ITER" -ne 1 ] || [ "${USE_OLD_HADOOP_OUTPUT}" != "true" ]; then
    hdfs dfs -get "/HadoopOutput_${ITER}" /root/exported-output
  fi

  # run Spark against HDFS:/HadoopOutput_$ITER
  if ! JAVA_HOME="$JAVA_HOME21" "$SPARK_HOME/bin/spark-submit" \
      --master spark://localhost:7077 \
      --deploy-mode client \
      --jars "$(find /root/lib -name '*.jar' | paste -sd, -)" \
      --class org.nikolasparaskakis.KGFixerMain \
      /root/SparkKG-1.0-SNAPSHOT.jar \
        -t "${TBOX_FILE_PATH}" \
        -se "${SPARQL_ENDPOINT}" \
        -pi \
        -d \
        -r "${REASONER}" \
        -graph http://enexa.eu \
        -hdfs hdfs://localhost:9000 \
        -outputDir "SparkOutput_${ITER}" \
        -inputDir "HadoopOutput_${ITER}" \
        -mode "${MODE}" \
        -fixS "${FIX_STRATEGY}" \
        -mcd \
        -or "${ITER}" \
        -parts "${PARTS}" \
        -redT "${REDUCER_TASKS}" \
        -e \
        -binCapacity 10 \
        -expLim 1
  then
    rc=$?
    echo "spark-submit failed with code $rc. Attempting to fetch any partial output..."
    # best-effort fetch; don't let errors here kill the script
    hdfs dfs -get -f "/SparkOutput_${ITER}" /root/exported-output || true
    # optionally dump Spark logs here if you want
    exit $rc
  fi

  # grab Spark output locally
  hdfs dfs -get "/SparkOutput_${ITER}" /root/exported-output

  echo "Finished iteration $ITER"
  sleep 3
  ((ITER++))
done