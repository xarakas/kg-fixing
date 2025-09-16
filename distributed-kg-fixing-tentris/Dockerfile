# Dockerfile
FROM ubuntu:22.04

# Avoid interactive prompts during install
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y \
    python3 python3-pip \
    git curl vim \
    && apt-get clean

# Copy entrypoint script
COPY entrypoint.sh /mnt/entrypoint.sh
COPY truncate_log.sh /mnt/truncate_log.sh

# Make truncate_log executable
RUN chmod +x /mnt/truncate_log.sh

# Make entrypoint executable
RUN chmod +x /mnt/entrypoint.sh

# Default workdir
WORKDIR /mnt

# Set the entrypoint
ENTRYPOINT ["/mnt/entrypoint.sh"]
