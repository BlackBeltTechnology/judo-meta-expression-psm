# Contributing to [Project Name] (LLM-Assisted)

## Objective

This document provides explicit, machine-readable instructions for contributing to
[Project Name]. It is intended to be parsed and followed by Large Language Models
(LLMs) acting as development assistants and by human contributors.

## 1. Project overview

### 1.1 Goal

The primary goal of [Project Name] is to [State the main purpose of the project,
e.g., "provide a set of tools for analyzing financial data"]. All contributions must
align with this goal.

## 2. Environment setup and build verification

### 2.1 Prerequisites

Ensure the following tools are installed and available in PATH:

- Java Development Kit (JDK): Version 21 or higher. Verify with:

  ```bash
  java -version
  ```

- Apache Maven: Version 3.9.9 or higher. Verify with:

  ```bash
  mvn -v
  ```

Generally in this project development using SdkMan to manage java version. The `sdk` command can be used to use specific java versions. With `PAGER=cat sdk list java` can de installed and installable versions. 

### 2.2 Full project build

To build the entire project from the project root, run:

```bash
mvn clean install
```

Success condition: The build is considered successful only if the command completes
with:

- log line: `[INFO] BUILD SUCCESS`
- and there are no `[ERROR]` lines in the build log.

All expected artifacts must be present in their respective `target/` directories.

## 3. Project structure and key files

A representative layout:

```text
├── eclipse/                # Parent for all Tycho modules
│   ├── plugins/            # Individual Eclipse plugins
│   ├── features/           # Eclipse features
│   └── sites/              # Eclipse update sites
├── maven/                  # Parent for standard Maven modules
│   └── [module-name]/      # A standard Java library or application
└── pom.xml                 # The root Maven Project Object Model
```

Key files referenced in this document:

- Root Maven POM: [`pom.xml`](pom.xml:1)
- Plugin MANIFEST: [`MANIFEST.MF`](adapter-psm/META-INF/MANIFEST.MF:1)
- This file: [`CONTRIBUTING.md`](CONTRIBUTING.md:1)

## 4. Dependency management rules

Dependencies are handled differently depending on module types. Follow these rules
exactly:

1. Rule 1 — Plain Maven -> Plain Maven  
   Action: Add a standard dependency block to the consuming module's [`pom.xml`](pom.xml:1).

   Example:

   ```xml
   <dependency>
     <groupId>com.example</groupId>
     <artifactId>example-lib</artifactId>
     <version>1.2.3</version>
   </dependency>
   ```

2. Rule 2 — Tycho -> Tycho (plugin -> plugin)  
   Action: Modify the consuming plugin's [`MANIFEST.MF`](adapter-psm/META-INF/MANIFEST.MF:1).
   Add the required bundle ID to the `Require-Bundle` section. Do NOT add a
   `<dependency>` to the plugin's `pom.xml`.

3. Rule 3 — Plain Maven -> Tycho  
   Action: To make a standard Java library available to an Eclipse plugin, add a
   standard `<dependency>` block to the Tycho plugin module's `pom.xml`. Tycho will
   wrap the JAR as an OSGi bundle automatically.

4. Rule 4 — Tycho -> Plain Maven  
   Action: To use a Tycho plugin artifact in a plain Maven module, add a standard
   `<dependency>` to the plain Maven module's `pom.xml` with the plugin's
   coordinates.

## 5. LLM contribution workflow

As an LLM assistant, follow this workflow for every contribution:

1. Declare intent  
   - State your intended change in one clear sentence.  
     Example:

     ```text
     I will add the method calculateAverage(List<Double> numbers) to MathUtils.java.
     ```

2. Implement changes  
   - Make code changes following the Coding Guidelines in section 6.

3. Verify build  
   - Run the full build from project root:

     ```bash
     mvn clean install
     ```

   - Proceed only if the Success Condition in section 2.2 is met. If the build fails,
     analyze and fix the errors before continuing.

4. Commit changes  
   - Use Conventional Commits. Template:

     ```text
     feat: A brief summary of the feature

     A more detailed optional description of the changes, explaining the what and
     why.

     Refs: #<issue_number>
     ```

   - Use `fix:` for bug fixes, `docs:` for documentation, `style:` for formatting-only
     changes, `refactor:` for refactors, `test:` for adding tests, etc.

## 6. Coding guidelines (mandatory)

### 6.1 Code style

- Indentation: 4 spaces (no tabs).
- Naming conventions:
  - Classes, enums, interfaces: PascalCase
  - Methods & variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- Braces: Opening brace `{` must be on the same line as the statement.

### 6.2 Documentation (JavaDoc)

- All public classes and methods must have JavaDoc.
- JavaDoc must include a description, `@param` for each parameter, and `@return`
  when applicable.

### 6.3 Testing

- All new public methods must have JUnit 5 tests.
- Bug fixes must include a regression test that fails before the fix and passes
  after.
- Test placement convention:
  - Implementation: `src/main/java/...` — example: [`src/main/java/...`](src/main/java/:1)
  - Test: `src/test/java/...` — example: [`src/test/java/...`](src/test/java/:1)
  - Test class name: `[ClassName]Test.java`

## 7. LLM scope of work

Allowed actions:

- Refactor code, add methods, fix documented bugs, write tests, and update JavaDoc.

Forbidden actions (require explicit human instruction):

- Do NOT change build configurations or top-level artifact wiring without explicit
  human approval. Examples:
  - [`pom.xml`](pom.xml:1)
  - `feature.xml`
  - `site.xml`
  - [`MANIFEST.MF`](adapter-psm/META-INF/MANIFEST.MF:1)
