# Project Context

## Purpose
JUDO Expression PSM Adapter - An Eclipse/Tycho-based adapter project that:
1. Adapts PSM (Platform Specific Model) models for use with JUDO Expression Language
2. Provides measure support and type mappings between Expression and PSM metamodels
3. Supports expression validation via the judo-meta-expression validators
4. Distributes via both Maven Central and Eclipse P2 repositories

## Tech Stack
- Java 21
- Eclipse Modeling Framework (EMF) 2.38.0+
- Ecore - Model definition language
- Tycho 4.0.13 - Eclipse plugin build
- Epsilon 2.8.0 - Model validation (EVL)
- Zeta Framework - Java validation framework (to be integrated)
- Apache Karaf 4.4.7 - OSGi container
- Pax Exam 4.13.5 - OSGi testing

## Project Conventions

### Code Style
- Use Lombok for boilerplate reduction
- Follow EMF conventions for model classes
- Use static utility methods for validators
- Constants for constraint names follow pattern: `CONSTRAINT_*` or `*_IS_VALID`

### Architecture Patterns
- Adapter Pattern: PsmModelAdapter adapts PSM models for expression processing
- Provider Pattern: PsmMeasureProvider provides measure support
- Validator Delegation: ExpressionValidatorOnPsm delegates to judo-meta-expression validators
- Dual Validation: Both EVL and Java validators run in parallel for test verification

### Testing Strategy
- JUnit 5 for unit tests
- Parameterized tests with ValidatorType enum (EVL, JAVA) for dual validation
- Pax Exam for OSGi integration tests
- Performance tests comparing EVL vs Java validation

### Git Workflow
- Main branch: `develop`
- Feature branches: `feature/JNG-*`
- SNAPSHOT-based development (currently 1.0.5-SNAPSHOT)
- Release via Maven Central and P2 deployment

## Domain Context
This project is part of the JUDO ecosystem:
- **judo-meta-expression**: Core expression metamodel with EVL and Zeta validators
- **judo-meta-psm**: Platform Specific Model metamodel
- **judo-meta-measure**: Measure metamodel for units and measurements
- **judo-zeta**: Zeta validation framework (Java-based alternative to EVL)

## Important Constraints
- Must maintain OSGi compatibility (bundle manifests, P2 site)
- Validation must produce identical results between EVL and Java validators
- No new validation rules in this project - only adapter integration
- Import validators from judo-meta-expression as dependencies

## External Dependencies
- judo-meta-expression: Expression model and validators (EVL + Zeta)
- judo-meta-psm: PSM model runtime
- judo-meta-measure: Measure model runtime
- hu.blackbelt.judo.zeta: Zeta validation framework (annotations, common, validation-core)
