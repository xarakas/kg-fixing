# Monolithic KG Fixer Module

This module contains the source code and build configuration for the Monolithic KG Fixer Java application. It is responsible for compiling the main JAR and packaging all required dependencies for standalone execution or containerization.

## Directory Structure

- **pom.xml**: Maven build configuration file, specifying dependencies and build instructions.
- **src/main/java/**: Java source code for the Monolithic KG Fixer.
- **target/**: Output directory for compiled classes, JAR files, and build artifacts.
  - `MonolithicKGFixer-1.0-SNAPSHOT.jar`: Thin JAR containing only the application code.
  - `MonolithicKGFixer-1.0-SNAPSHOT-jar-with-dependencies.jar`: Fat JAR including all dependencies for standalone execution.
  - `lib/`: All third-party JAR dependencies (used for thin JAR deployment).
- **.idea/**: IDE configuration files (IntelliJ IDEA).

## Building the Project

To build the Monolithic KG Fixer and its dependencies, run:

```sh
mvn clean install
```

This will generate both the thin and fat JAR files in the `target/` directory.

## Usage

- Use the **fat JAR** (`MonolithicKGFixer-1.0-SNAPSHOT-jar-with-dependencies.jar`) for standalone execution:
  ```sh
  java -jar target/MonolithicKGFixer-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
- Use the **thin JAR** (`MonolithicKGFixer-1.0-SNAPSHOT.jar`) with the `lib/` folder for environments that manage dependencies separately (e.g., Docker containers).

## Customization

- Modify `pom.xml` to add or update dependencies as needed.
- Place additional Java source files in `src/main/java/`.

## Troubleshooting

- If build errors occur, ensure all dependencies are available and Maven is properly installed.
- For runtime issues, check that all required libraries are present in the `lib/` folder (for thin JAR usage).
