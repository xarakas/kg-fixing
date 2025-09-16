# Monolithic KG Fixer Run Environment

This module provides a ready-to-use Dockerized environment for deploying the Monolithic KG Fixer. It includes all necessary scripts, configuration files, compiled Java code, and dependencies for easy setup and execution.

## Directory Structure

- **kg-fixing-jars/**: Contains the main JAR file (`MonolithicKGFixer-1.0-SNAPSHOT.jar`) and a `lib/` folder with all required Java dependencies.
- **native-libs/**: Native libraries required for the Fact++ reasoner (e.g., `libFaCTPlusPlusJNI.so`).
- **tbox-files/**: TBox files for reasoning and knowledge graph fixing. Select the desired file in the `.env` configuration.
- **.env**: Environment variables to configure runtime options before starting the application.
- **docker-compose.yml**: Docker Compose configuration for orchestrating the environment.
- **Dockerfile**: Docker image definition for the Monolithic KG Fixer.
- **start-kg-fixer.sh**: Script to build the Docker image and launch the Monolithic KG Fixer application.
- **entrypoint.sh**: Entrypoint script executed when the container starts (if present).

## Quick Start

1. **Set environment variables:**  
   Edit the `.env` file to configure runtime options, such as the TBox file to use.

2. **Start the environment (includes building the Docker image):**  
   ```sh
   ./start-kg-fixer.sh
   ```

## Troubleshooting

- Check file and directory permissions if you encounter issues running shell scripts.
- Verify that all dependencies are correctly placed in the `kg-fixing-jars/` and `native-libs/` folders.