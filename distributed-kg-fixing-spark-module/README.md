# Distributed KG Fixer Spark Module

This module contains the source code and build configuration for the distributed knowledge graph fixing Spark application. It is responsible for compiling the main JAR and packaging all required dependencies for distributed execution on Apache Spark clusters.

## Directory Structure

- **pom.xml**: Maven build configuration file, specifying dependencies and build instructions.
- **src/main/java/**: Java source code for the Spark-based distributed KG Fixer.
- **target/**: Output directory for compiled classes, JAR files, and build artifacts.
  - `DistributedKGFixerSparkModule-1.0-SNAPSHOT.jar`: Thin JAR containing only the application code.
  - `DistributedKGFixerSparkModule-1.0-SNAPSHOT-jar-with-dependencies.jar`: Fat JAR including all dependencies for standalone or cluster execution.
  - `lib/`: All third-party JAR dependencies (used for thin JAR deployment).
- **.idea/**: IDE configuration files (IntelliJ IDEA).

## Building the Project

To build the Spark module and its dependencies, run:

```sh
mvn clean install
```

This will generate both the thin and fat JAR files in the `target/` directory.

## Usage

- Use the **fat JAR** (`DistributedKGFixerSparkModule-1.0-SNAPSHOT-jar-with-dependencies.jar`) for standalone or cluster execution:
  ```sh
  spark-submit --class <MainClass> --master <master-url> target/DistributedKGFixerSparkModule-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
- Use the **thin JAR** (`DistributedKGFixerSparkModule-1.0-SNAPSHOT.jar`) with the `lib/` folder for environments that manage dependencies separately.

## Customization

- Modify `pom.xml` to add or update dependencies as needed.
- Place additional Java source files in `src/main/java/`.

## Troubleshooting

- If build errors occur, ensure all dependencies are available and Maven is properly installed.
- For runtime issues, check that all required libraries are present in the `lib/` folder (for thin JAR usage).
- Ensure your Spark cluster is properly configured and accessible.
