FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SDKMAN_DIR="/root/.sdkman"

# ---- Versions & paths ----
ARG HADOOP_VERSION=3.4.0
ARG SPARK_VERSION=3.5.6
ENV HADOOP_VERSION=${HADOOP_VERSION}
ENV SPARK_VERSION=${SPARK_VERSION}

ARG DEFAULT_SPARK_PROFILE=dev-1
ENV SPARK_PROFILE=${DEFAULT_SPARK_PROFILE}

ENV HADOOP_HOME=/opt/hadoop
ENV HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
ENV JAVA_HOME=/root/.sdkman/candidates/java/current
ENV JAVA_HOME21=/root/.sdkman/candidates/java/21.0.6-amzn
ENV SPARK_HOME=/opt/spark
ENV PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$SPARK_HOME/bin:$SPARK_HOME/sbin:$PATH

# Apache CDN
ENV APACHE_MIRROR=https://dlcdn.apache.org

# Install dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      curl wget ca-certificates gnupg2 sudo software-properties-common \
      zip unzip tar bash openssh-server coreutils && \
    rm -rf /var/lib/apt/lists/*

# Set up SSH
RUN mkdir /var/run/sshd && \
    echo "root:root" | chpasswd && \
    ssh-keygen -t rsa -P "" -f /root/.ssh/id_rsa && \
    cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys && \
    chmod 0600 /root/.ssh/authorized_keys && \
    echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config

# Install SDKMAN and Java 11.0.26-zulu 8.0.442-amzn
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && \
             sdk install java 11.0.26-zulu && \
             sdk install java 21.0.6-amzn && \
             sdk default java 11.0.26-zulu"

# ---- Download & install Hadoop ----
RUN set -eux; \
    HADOOP_TGZ="hadoop-${HADOOP_VERSION}.tar.gz"; \
    HADOOP_URL="${APACHE_MIRROR}/hadoop/common/hadoop-${HADOOP_VERSION}/${HADOOP_TGZ}"; \
    curl -fSL --retry 5 --retry-delay 2 -o "/tmp/${HADOOP_TGZ}" "${HADOOP_URL}"; \
    curl -fSL --retry 5 --retry-delay 2 -o "/tmp/${HADOOP_TGZ}.sha512" "${HADOOP_URL}.sha512"; \
    cd /tmp && sha512sum -c "${HADOOP_TGZ}.sha512"; \
    tar -xzf "/tmp/${HADOOP_TGZ}" -C /opt/; \
    mv "/opt/hadoop-${HADOOP_VERSION}" "${HADOOP_HOME}"; \
    rm -f "/tmp/${HADOOP_TGZ}" "/tmp/${HADOOP_TGZ}.sha512"

# ---- Download & install Spark ----
RUN set -eux; \
    SPARK_TGZ="spark-${SPARK_VERSION}-bin-hadoop3.tgz"; \
    SPARK_URL="${APACHE_MIRROR}/spark/spark-${SPARK_VERSION}/${SPARK_TGZ}"; \
    curl -fSL --retry 5 --retry-delay 2 -o "/tmp/${SPARK_TGZ}" "${SPARK_URL}"; \
    curl -fSL --retry 5 --retry-delay 2 -o "/tmp/${SPARK_TGZ}.sha512" "${SPARK_URL}.sha512"; \
    cd /tmp && sha512sum -c "${SPARK_TGZ}.sha512"; \
    tar -xzf "/tmp/${SPARK_TGZ}" -C /opt/; \
    ln -s "/opt/spark-${SPARK_VERSION}-bin-hadoop3" "${SPARK_HOME}"; \
    # create a writable work dir for logs that you can mount from host
    mkdir -p "${SPARK_HOME}/work"; \
    rm -f "/tmp/${SPARK_TGZ}" "/tmp/${SPARK_TGZ}.sha512"

# Copy config files
COPY hadoop-configs/core-site.xml $HADOOP_CONF_DIR/
COPY hadoop-configs/hdfs-site.xml $HADOOP_CONF_DIR/
COPY hadoop-configs/mapred-site.xml $HADOOP_CONF_DIR/
COPY hadoop-configs/yarn-site.xml $HADOOP_CONF_DIR/

# ---- Spark profiles ----
COPY spark-profiles/ /opt/spark-profiles/
COPY select-spark-profile.sh /usr/local/bin/select-spark-profile
RUN chmod +x /usr/local/bin/select-spark-profile && sed -i 's/\r$//' /usr/local/bin/select-spark-profile


# ---- App artifacts ----
COPY tbox-files/LUBM_with_disj-Altered.ttl /root/
COPY tbox-files/dbpedia_2016-10.nt /root/
COPY tbox-files/food_insp.nt /root/

COPY kg-fixing-jars/ComputeExpansions-1.0-SNAPSHOT-jar-with-dependencies.jar /root/
COPY kg-fixing-jars/SparkKG-1.0-SNAPSHOT.jar /root/
COPY kg-fixing-jars/lib /root/lib
COPY native-libs /root/native-libs

RUN mkdir -p /root/exported-output

# Configure hadoop-env.sh to point to JAVA_HOME
RUN echo "export JAVA_HOME=${JAVA_HOME}" >> $HADOOP_CONF_DIR/hadoop-env.sh

COPY entrypoint.sh /root/entrypoint.sh
RUN chmod +x /root/entrypoint.sh && sed -i 's/\r$//' /root/entrypoint.sh

# Expose HDFS/YARN Web UI ports if needed
EXPOSE 7077 9870 8088 4040 8080 8081 8082

CMD ["/root/entrypoint.sh"]
