# JUDO Expression PSM Adapter - Project Documentation

## Project Overview

**Repository:** BlackBeltTechnology/judo-meta-expression-psm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven with Eclipse Tycho 4.0.13 (hybrid OSGi/standard modules)

1. Bridges the JUDO Expression Language to the PSM (Platform Specific Model) metamodel, enabling expression type resolution, attribute/reference navigation, and validation against PSM-defined schemas
2. Provides measurement and unit support (dimensional analysis, unit conversions, derived measures) through a dedicated measure provider
3. Packages as an OSGi bundle (Eclipse plugin) with full Karaf container deployment support via P2 update site
4. Part of the JUDO framework by BlackBelt Technology — works alongside `judo-meta-expression`, `judo-meta-psm`, and `judo-meta-measure` metamodel projects

## Directory Structure

```
├── adapter-psm/                  # Core implementation (eclipse-plugin)
│   ├── META-INF/MANIFEST.MF      # OSGi bundle manifest
│   └── src/main/java/             # 3 production classes
├── adapter-psm-test/             # Unit & integration tests (jar)
│   └── src/test/java/             # 11 test classes
├── feature-adapter-psm/          # Eclipse feature packaging
│   └── feature.xml                # Feature descriptor
├── osgi-itest/                   # OSGi integration tests (Karaf/Pax Exam)
│   ├── src/test/java/             # Karaf container tests
│   └── src/test/resources/        # test-features.xml, logback config
├── site/                         # Eclipse P2 update site
│   └── category.xml              # Site category descriptor
├── .github/                      # CI workflows and templates
│   ├── CIFLOW.md                 # Branching and CI/CD documentation
│   └── ISSUE_TEMPLATE/           # Bug, feature, docs templates
├── pom.xml                       # Root POM (parent for all modules)
├── CONTRIBUTING.md               # Contribution guidelines
└── README.md                     # Project overview
```

## Core Modules

### Implementation Layer

| Module | Type | Artifact ID | Purpose |
|--------|------|-------------|---------|
| `adapter-psm/` | eclipse-plugin | `hu.blackbelt.judo.meta.expression.model.adapter.psm` | Core adapter implementing `ModelAdapter` interface — type resolution, attribute/reference navigation, measure support, transfer object mapping, expression validation |

### Test Layer

| Module | Type | Artifact ID | Purpose |
|--------|------|-------------|---------|
| `adapter-psm-test/` | jar | `hu.blackbelt.judo.meta.expression.psm.model.test` | JUnit 5 unit tests for adapter methods, measure support, dimensions, and expression validation |
| `osgi-itest/` | jar | `hu.blackbelt.judo.meta.expression.psm.osgi.test` | OSGi integration tests running in a live Karaf 4.4.7 container via Pax Exam |

### Packaging Layer

| Module | Type | Artifact ID | Purpose |
|--------|------|-------------|---------|
| `feature-adapter-psm/` | eclipse-feature | `hu.blackbelt.judo.meta.expression.adapter.psm.feature` | Bundles the adapter plugin into an Eclipse feature for P2 distribution |
| `site/` | eclipse-repository | `hu.blackbelt.judo.meta.expression.psm.site` | Builds the Eclipse P2 update site repository |

## Technology Stack

### Core Technologies
- **Eclipse EMF** (Ecore 2.21+) — metamodel framework for Expression, PSM, and Measure models
- **Eclipse Tycho 4.0.13** — builds eclipse-plugin and eclipse-feature modules as OSGi bundles
- **Epsilon Runtime** — model validation and transformation engine
- **OSGi Framework** (1.8+) — runtime module system for the adapter bundle
- **Apache Karaf 4.4.7** — OSGi container for integration testing

### Build & Quality
- **Maven** with CI-friendly versioning (`${revision}` = 1.0.5-SNAPSHOT)
- **JUnit 5** — unit testing framework
- **Pax Exam 4.13.5** — Karaf container test framework
- **JaCoCo 0.8.12** — code coverage
- **Maven Surefire 3.5.1** — test execution
- **SLF4J 2.0.16** + **Logback 1.5.12** — logging

### Key JUDO Dependencies
- `hu.blackbelt.judo.meta.expression.model` — Expression language metamodel
- `hu.blackbelt.judo.meta.psm.model` — PSM metamodel (EntityType, Primitive, Measure, etc.)
- `hu.blackbelt.judo.meta.measure.model` — Measurement/unit metamodel
- `hu.blackbelt.judo.meta.expression.model.adapter.measure` — Measure adapter bridge
- `hu.blackbelt.epsilon:epsilon-runtime-execution` — Epsilon runtime

## Build Commands

> **Note:** Use SDKMAN! (`sdk`) to manage Java versions. Requires JDK 21+ and Maven 3.9.9+.

```bash
# Full build (all modules)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run unit tests only (adapter-psm-test module)
mvn test -pl adapter-psm-test

# Run a specific test class
mvn test -pl adapter-psm-test -Dtest=PsmModelAdapterTest

# Run a specific test method
mvn test -pl adapter-psm-test -Dtest=PsmModelAdapterTest#testGetTypeName

# Run OSGi integration tests (starts Karaf container)
mvn test -pl osgi-itest

# Use Maven wrapper (if available)
./mvnw clean install
```

Build is successful when the output contains `[INFO] BUILD SUCCESS` with no `[ERROR]` lines.

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | **Default** — builds all 5 modules (active unless `-DskipModules=true`) |
| `sign-artifacts` | GPG-signs artifacts for release using simplify4u sign-maven-plugin |
| `release-dummy` | Test release to `/tmp` (file-based distribution) |
| `release-judong` | Deploy to JUDO.technology Nexus (staging) |
| `release-central` | Deploy to Maven Central via Sonatype OSSRH (auto-close enabled) |
| `generate-github-asciidoc-diagrams` | Generate documentation with Asciidoctor + PlantUML/Graphviz |
| `update-source-code-license` | Inject EPL-2.0 license headers into source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Root Maven POM — version properties, dependency management, profiles, plugin config |
| `adapter-psm/META-INF/MANIFEST.MF` | OSGi bundle manifest — exported packages, required bundles, import packages |
| `feature-adapter-psm/feature.xml` | Eclipse feature descriptor — plugin and dependency declarations |
| `osgi-itest/src/test/resources/test-features.xml` | Karaf feature definition for OSGi integration tests |
| `site/category.xml` | P2 update site category descriptor |
| `logback-test.xml` | Test logging configuration |
| `CONTRIBUTING.md` | Coding guidelines, dependency rules, contribution workflow |

## Development Environment

**Required:**
- Java 21 JDK (manage with SDKMAN!: `sdk use java 21.x.y-tem`)
- Maven 3.9.9+
- Git

**Recommended:**
- IDE with Tycho/OSGi support (Eclipse IDE or IntelliJ with Tycho plugin)
- Familiarity with EMF/Ecore metamodeling concepts

## Git Workflow

- **Main Branch:** `develop`
- **Stable Branch:** `master` (latest released version)
- **Versioning:** Semantic versioning via `${revision}` property (currently 1.0.5-SNAPSHOT)
- **Branch naming:** `feature/JNG-NUMBER_summary`, `bugfix/JNG-NUMBER_summary`, `release/X.Y.Z`
- **Commit format:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`)
- **Rule:** Every commit must reference a JIRA ticket (`JNG-xxx`)
- **CI/CD:** GitHub Actions — see [CIFLOW.md](.github/CIFLOW.md) for workflow details

## Important Notes

1. This project is a **consumer** of EMF metamodels — it contains no `.ecore` or `.genmodel` files. The Expression, PSM, and Measure metamodels are external dependencies.
2. The `adapter-psm` module uses **eclipse-plugin** packaging (Tycho). Dependencies between Tycho modules use `MANIFEST.MF` `Require-Bundle`/`Import-Package`, not POM `<dependency>` blocks.
3. All production code is in a **single package** (`hu.blackbelt.judo.meta.expression.adapters.psm`) with only 3 classes: `PsmModelAdapter`, `PsmMeasureProvider`, and `ExpressionValidatorOnPsm`.
4. Do NOT modify build configuration files (`pom.xml`, `feature.xml`, `MANIFEST.MF`, `category.xml`) without explicit approval — these affect artifact wiring and OSGi bundle resolution.
5. The OSGi integration tests (`osgi-itest`) spin up a real Karaf 4.4.7 container — they are slower and require network access for feature resolution.
6. Code style: 4-space indentation, opening brace on same line, JavaDoc on all public APIs.

## Related Documentation

- [README.md](README.md) — Project overview with architecture diagrams
- [CONTRIBUTING.md](CONTRIBUTING.md) — Coding guidelines, dependency rules, LLM contribution workflow
- [.github/CIFLOW.md](.github/CIFLOW.md) — Branching strategy and CI/CD workflow documentation
