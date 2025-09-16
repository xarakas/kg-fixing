# Distributed KG Fixer Hadoop Module

This module contains the source code and build configuration for the distributed knowledge graph fixing Hadoop application. It provides MapReduce-based components for scalable graph processing and fixing tasks in a distributed Hadoop environment.

## Directory Structure

- **pom.xml**: Maven build configuration file, specifying dependencies and build instructions.
- **src/main/java/org/nikolasparaskakis/**: Java source code for the Hadoop-based distributed KG Fixer, organized by functionality:
  - **deduplicator/**: Deduplication mappers and reducers.
  - **equalseteliminator/**: Equal set elimination mappers and reducers.
  - **khopextractor/**: K-hop extraction logic and mappers/reducers.
  - **onehopextractor/**: One-hop extraction logic, combiners, mappers, reducers, and input format classes.
  - **pipeline/**: Pipeline orchestration and argument handling.
  - **subseteliminator/**: Subset elimination mappers and reducers.
- **target/**: Output directory for compiled classes, JAR files, and build artifacts.
  - `DistributedKGFixerHadoopModule-1.0-SNAPSHOT-jar-with-dependencies.jar`: Fat JAR including all dependencies for standalone or cluster execution.
  - `archive-tmp/`, `classes/`, `generated-sources/`, `generated-test-sources/`, `test-classes/`: Build and test artifacts.
- **.idea/**: IDE configuration files (IntelliJ IDEA).

## Building the Project

To build the Hadoop module and its dependencies, run:

```sh
mvn clean compile assembly:single
```

This will generate the fat JAR file in the `target/` directory.

## Usage

- Use the **fat JAR** (`DistributedKGFixerHadoopModule-1.0-SNAPSHOT-jar-with-dependencies.jar`) for cluster execution:
  ```sh
  hadoop jar target/DistributedKGFixerHadoopModule-1.0-SNAPSHOT-jar-with-dependencies.jar <MainClass> <args>
  ```
- Refer to the source code for available main classes and arguments for different graph fixing tasks.

## Customization

- Modify `pom.xml` to add or update dependencies as needed.
- Place additional Java source files in the appropriate subdirectories under `src/main/java/org/nikolasparaskakis/`.

## Troubleshooting

- If build errors occur, ensure all dependencies are available and Maven is properly installed.
- For runtime issues, check that your Hadoop cluster is properly configured and accessible.
- Review logs for errors related to input formats, memory, or data partitioning.
