# adapter-psm-test Specification

## Purpose

Provides comprehensive unit and integration tests for the PSM model adapter, verifying type resolution, attribute/reference navigation, measure support, dimensional analysis, and expression validation against programmatically constructed PSM models.

## Architecture

Test classes are organized in two packages:

- `hu.blackbelt.judo.meta.expression.adapters.psm` — Unit tests for `PsmModelAdapter`, `PsmMeasureProvider`, and measure support
- `hu.blackbelt.judo.meta.expression.runtime` — Integration tests for end-to-end expression validation

Key base class: `ExecutionContextOnPsmTest` — Programmatically builds a complete PSM model with entity types, primitives, measures (time, mass, length), derived measures (velocity, area, volume, density, force), and validates the PSM model with Epsilon before running expression tests.

## Requirements

### Requirement: PSM Model Adapter Type Resolution Tests

The test suite SHALL verify that `PsmModelAdapter` correctly resolves types, attributes, and references from PSM models.

#### Scenario: Test getTypeName resolution
- **GIVEN** a PSM model with entity types in known namespaces
- **WHEN** `PsmModelAdapterTest#testGetTypeName` runs
- **THEN** type names are correctly resolved by namespace and name

#### Scenario: Test getAttribute resolution
- **GIVEN** a PSM model with entity types having typed attributes
- **WHEN** `PsmModelAdapterTest#testGetAttribute` runs
- **THEN** attributes are resolved and their types match expected primitives

#### Scenario: Test getReference resolution
- **GIVEN** a PSM model with entity types having relations
- **WHEN** `PsmModelAdapterTest#testGetReference` runs
- **THEN** references are resolved and their targets and collection flags are correct

### Requirement: Measure Provider Tests

The test suite SHALL verify that `PsmMeasureProvider` correctly resolves measures, units, and computed base measures.

#### Scenario: Test measure lookup by namespace and name
- **GIVEN** a PSM model with measures (Time, Mass, Length)
- **WHEN** `PsmMeasureProviderTest` runs measure lookup tests
- **THEN** measures are found by their fully-qualified namespace and name

#### Scenario: Test unit resolution by name or symbol
- **GIVEN** measures with units having both names and symbols
- **WHEN** `PsmMeasureProviderTest` tests unit lookup
- **THEN** units are found by either name or symbol

### Requirement: Measure Support Tests

The test suite SHALL verify measure-related operations including unit rates and duration support.

#### Scenario: Test measure support operations
- **GIVEN** a complete PSM model with measurement definitions
- **WHEN** `PsmMeasureSupportTest` runs
- **THEN** unit rate calculations, duration addition checks, and measured type detection are correct

### Requirement: Dimensional Analysis Tests

The test suite SHALL verify that measure dimension computation correctly handles derived measures.

#### Scenario: Test dimension computation for derived measures
- **GIVEN** derived measures like Velocity (Length/Time), Area (Length^2), Density (Mass/Volume)
- **WHEN** `PsmModelAdapterDimensionTest` runs
- **THEN** base measure maps contain the correct measures and exponents

### Requirement: Expression Validation Integration Tests

The test suite SHALL validate expressions against PSM models using `ExpressionValidatorOnPsm`.

#### Scenario: Minimal expression validation
- **GIVEN** a minimal PSM model and a set of basic expressions
- **WHEN** `MinimalPsmTest` runs validation
- **THEN** valid expressions pass and invalid expressions produce expected errors

#### Scenario: Full expression feature coverage
- **GIVEN** a comprehensive PSM model with all supported types
- **WHEN** `FullPsmTest` runs validation
- **THEN** all expression features (navigation, arithmetic, filtering, etc.) validate correctly

#### Scenario: Illegal expression detection
- **GIVEN** an `ExpressionModel` with deliberately invalid expressions
- **WHEN** `IllegalPsmTest` runs validation with expected errors
- **THEN** all expected error messages are reported

#### Scenario: Measured decimal expression validation
- **GIVEN** expressions involving measured decimal values
- **WHEN** `MeasuredTest` runs validation
- **THEN** unit compatibility and dimension checks pass

### Requirement: Test Model Construction

The test suite SHALL programmatically construct PSM models for testing rather than loading from files.

#### Scenario: ExecutionContextOnPsmTest builds complete PSM model
- **GIVEN** `ExecutionContextOnPsmTest` setUp runs
- **WHEN** the PSM model is built with entity types, primitives, and measures
- **THEN** the model passes Epsilon validation and is ready for expression testing

#### Scenario: MinimalExpressionFactory builds test expressions
- **GIVEN** a PSM model is available
- **WHEN** `MinimalExpressionFactory` creates expression models
- **THEN** well-formed expressions are produced for adapter testing

### Requirement: Test Framework Setup

The test suite SHALL use JUnit 5 and depend on the adapter module as a test-scoped dependency.

#### Scenario: Dependencies are test-scoped
- **GIVEN** `adapter-psm-test/pom.xml`
- **WHEN** dependencies are examined
- **THEN** all JUDO model dependencies (expression, PSM, measure, adapter) have `<scope>test</scope>`
