FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SDKMAN_DIR="/root/.sdkman"

ENV JAVA_HOME=/root/.sdkman/candidates/java/current
ENV JAVA_HOME21=/root/.sdkman/candidates/java/21.0.6-amzn
ENV PATH=$JAVA_HOME/bin:$JAVA_HOME21/bin:$PATH

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

# ---- App artifacts ----
COPY tbox-files/LUBM_with_disj-Altered.ttl /root/
COPY tbox-files/dbpedia_2016-10.nt /root/
COPY tbox-files/food_insp.nt /root/

COPY kg-fixing-jars/MonolithicKGFixer-1.0-SNAPSHOT.jar /root/
COPY kg-fixing-jars/lib /root/lib
COPY native-libs /root/native-libs

RUN mkdir -p /root/exported-output

COPY entrypoint.sh /root/entrypoint.sh
RUN chmod +x /root/entrypoint.sh && sed -i 's/\r$//' /root/entrypoint.sh

CMD ["/root/entrypoint.sh"]
