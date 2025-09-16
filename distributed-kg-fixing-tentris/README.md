# Distributed KG Fixing Tentris Environment

This directory provides a Dockerized environment for running Tentris, a high-performance triple store, as part of the distributed knowledge graph fixing pipeline. It includes all necessary scripts, configuration files, and binaries for setup and usage.

## Directory Structure

- **docker-compose.yml**: Docker Compose configuration for orchestrating the Tentris environment.
- **Dockerfile**: Docker image definition for Tentris.
- **entrypoint.sh**: Entrypoint script executed when the container starts.
- **truncate_log.sh**: Utility script to truncate Tentris logs.
- **data/**: Directory where the user will put all the files to be inserted in the triple store.
- **tentris-bin/**: Contains Tentris binaries and configuration files:
  - `tentris`: Tentris server executable.
  - `tentris-server-config.toml`: Tentris server configuration file.
  - `README.html`: Documentation for Tentris.
- **README.md**: This documentation file.

## Quick Start

1. **Build the Docker image:**  
   ```sh
   docker build -t tentris-server .
   ```

2. **Start the Tentris environment:**  
   ```sh
   docker-compose up
   ```

3. **Monitor logs:**  
   ```sh
   docker-compose logs -f
   ```

## Configuration

- Edit `tentris-bin/tentris-server-config.toml` to customize Tentris server settings as needed.
- Data persistence and storage are managed in the `data/` directory.
- Use `truncate_log.sh` to clear Tentris logs if needed.

## Troubleshooting

- Ensure the Tentris binary is executable (`chmod +x tentris-bin/tentris`).
- Check file and directory permissions if you encounter issues running shell scripts.
- Review Tentris logs for errors or warnings.
