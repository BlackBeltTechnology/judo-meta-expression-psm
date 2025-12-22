<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Judo Expression PSM Adapter - Project Documentation

## Project Overview

**Repository:** BlackBeltTechnology/judo-meta-expression-psm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4+ with Tycho (Eclipse build tooling)

This is an Eclipse/Tycho-based adapter project that:
1. **Adapts** PSM (Platform Service Model) models for use with JUDO Expression Language
2. **Provides** measure support and type mappings between Expression and PSM metamodels
3. **Includes** JQL (Judo Query Language) expression builder integration
4. **Supports** dual validation testing (EVL and Java/Zeta validators)
5. **Distributes** via both Maven Central and Eclipse P2 repositories

## Directory Structure

```
judo-meta-expression-psm/
├── adapter-psm/                    # Core PSM adapter implementation
├── adapter-psm-test/               # Test utilities and validation tests
├── builder-jql-psm-test/           # JQL builder tests
├── feature-adapter-psm/            # Eclipse feature packaging
├── feature-builder-jql-psm/        # JQL builder feature
├── osgi-itest/                     # OSGi integration tests (Pax Exam)
├── site/                           # P2 update site
├── docs/                           # Documentation
│   └── validation/                 # Validation testing documentation
└── openspec/                       # OpenSpec change management
```

## Core Modules

| Module | Type | Purpose |
|--------|------|---------|
| `adapter-psm/` | eclipse-plugin | Core PSM adapter: PsmModelAdapter, PsmMeasureProvider, ExpressionValidatorOnPsm |
| `adapter-psm-test/` | bundle | Test utilities and dual validation test infrastructure |
| `builder-jql-psm-test/` | bundle | JQL expression builder tests |
| `feature-adapter-psm/` | eclipse-feature | Eclipse feature packaging |
| `osgi-itest/` | bundle | Pax Exam integration tests for Karaf container |
| `site/` | eclipse-repository | P2 update site assembly |

## Key Components

### Adapter Classes (adapter-psm module)

| Class | Purpose |
|-------|---------|
| `PsmModelAdapter` | Adapts PSM models for expression processing |
| `PsmMeasureProvider` | Provides measure support for PSM models |
| `ExpressionValidatorOnPsm` | Validates expressions on PSM models with EVL and Zeta support |

### Validation Test Infrastructure (adapter-psm-test module)

| Class | Purpose |
|-------|---------|
| `ValidatorType` | Enum for selecting EVL or Java validator |
| `AbstractExpressionPsmValidationTest` | Base class for dual validation tests |
| `ExpressionPsmValidationTest` | Dual validation test cases |
| `ExpressionPsmValidationPerformanceTest` | Performance comparison tests |
| `PsmTestModelGenerator` | Generates test models for performance benchmarks |

## Validation Architecture

This project imports and executes validators from [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression):

- **EVL Validation:** `ExpressionValidator.validateExpression()` - Epsilon Validation Language
- **Java/Zeta Validation:** `ExpressionZetaValidator.validateExpression()` - Zeta validation framework

**No validators are implemented in this project** - only test infrastructure and adapter wrappers.

### Validation Entry Points

```java
// EVL validation (default)
ExpressionValidatorOnPsm.validateExpressionOnPsm(log, psmModel, expressionModel);

// Java/Zeta validation
ExpressionValidatorOnPsm.validateExpressionOnPsmWithZeta(log, psmModel, expressionModel);
```

### Dual Validation Testing

Tests run with both EVL and Java validators to ensure parity:

```java
@ParameterizedTest(name = "testValidation [{0}]")
@EnumSource(ValidatorType.class)
void testValidation(ValidatorType type) throws Exception {
    this.validatorType = type;
    initModels();

    runValidation(
        ImmutableList.of(),  // expected errors
        ImmutableList.of()   // expected warnings
    );
}
```

## Technology Stack

### Core Technologies
- **Eclipse Modeling Framework (EMF)** 2.38.0+ - Metamodel foundation
- **Ecore** - Model definition language
- **Tycho** 4.0.13 - Eclipse plugin build
- **Epsilon** 2.8.0 - Model validation (EVL)
- **Zeta Framework** 1.0.0 - Java validation framework

### Dependencies
- **judo-meta-expression** - Expression metamodel and validators
- **judo-meta-psm** - Platform Service Model metamodel
- **judo-meta-jql** - JQL metamodel
- **judo-meta-measure** - Measure metamodel
- **judo-zeta** - Zeta validation framework

### Runtime
- **Apache Karaf** 4.4.7 - OSGi container
- **Pax Exam** 4.13.5 - OSGi testing

## Build Commands

```bash
# Standard build
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Run performance tests
./mvnw test -Dgroups=performance

# Memory requirements (configured in .mvn/jvm.config)
# -Xms1024m -Xmx2048m
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Includes all submodules (default) |
| `sign-artifacts` | GPG signing for release |
| `release-central` | Maven Central deployment |
| `release-judong` | Internal Judo repository |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM with module definitions and Zeta version |
| `.mvn/jvm.config` | JVM arguments for Maven build |
| `adapter-psm/META-INF/MANIFEST.MF` | OSGi bundle manifest |

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+ (or use ./mvnw wrapper)

**Optional:**
- Eclipse IDE with m2e and OSGi plugins
- IntelliJ IDEA with Maven and OSGi plugins

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** SNAPSHOT-based development
- **Release Process:** CI/CD with Maven Central and P2 deployment

## Related Documentation

- `docs/validation/README.md` - Validation testing overview
- `openspec/AGENTS.md` - OpenSpec workflow for spec-driven development
- `openspec/project.md` - Project conventions for OpenSpec
- [Zeta Framework Documentation](https://github.com/BlackBeltTechnology/judo-zeta) - Java validation framework
- [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression) - Expression metamodel
- [judo-meta-psm](https://github.com/BlackBeltTechnology/judo-meta-psm) - PSM metamodel
