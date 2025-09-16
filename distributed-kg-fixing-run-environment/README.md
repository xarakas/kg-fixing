# Distributed KG Fixing Run Environment

This directory provides a complete Dockerized environment for running distributed knowledge graph fixing using Hadoop and Spark modules. It includes all necessary scripts, configuration files, dependencies, and instructions for setup and usage in a distributed setting.

## Directory Structure

- **docker-compose.yml**: Docker Compose configuration for orchestrating the distributed environment.
- **Dockerfile**: Docker image definition for the distributed KG Fixer.
- **entrypoint.sh**: Entrypoint script executed when the container starts.
- **start-kg-fixer.sh**: Script to launch the distributed KG Fixer process inside the container.
- **select-spark-profile.sh**: Script to select the appropriate Spark profile for execution.
- **hadoop-configs/**: Contains Hadoop configuration files (`core-site.xml`, `hdfs-site.xml`, `mapred-site.xml`, `yarn-site.xml`).
- **kg-fixing-jars/**: Contains the Hadoop and Spark module JARs and all required Java dependencies.
  - `DistributedKGFixerHadoopModule-1.0-SNAPSHOT-jar-with-dependencies.jar`
  - `DistributedKGFixerSparkModule-1.0-SNAPSHOT.jar`
  - `lib/`: All third-party JAR dependencies.
- **native-libs/**: Native libraries required by the KG Fixer (e.g., `libFaCTPlusPlusJNI.so`).
- **spark-profiles/**: Contains Spark profile configurations for different environments (e.g., `dev-1/`, `dev-21/`).
- **tbox-files/**: TBox files for reasoning and knowledge graph fixing (e.g., `dbpedia_2016-10.nt`).
- **.env**: Environment variables to configure runtime options before starting the application.

## Quick Start

1. **Set environment variables:**  
   Edit the `.env` file to configure runtime options, such as the TBox file to use.

2. **Start the environment (includes building the Docker image):**  
   ```sh
   ./start-kg-fixer.sh
   ```

## Configuration

- Update Hadoop and Spark configuration files in `hadoop-configs/` and `spark-profiles/` as needed for your cluster or environment.
- Ensure all required JARs and native libraries are present in their respective folders.
- For Java memory or performance tuning, adjust JVM options in the relevant scripts or Dockerfile.

## Troubleshooting

- Check file and directory permissions if you encounter issues running shell scripts.
- Verify that all dependencies are correctly placed in the `kg-fixing-jars/` and `native-libs/` folders.
- Ensure your Hadoop and Spark cluster settings match the configuration files provided.
