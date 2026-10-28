# Expression PSM Validation Testing

This document describes the dual validation testing infrastructure for Expression models on PSM.

## Overview

The judo-meta-expression-psm project supports two validation engines for Expression models:

1. **EVL Validation** - Epsilon Validation Language based validation
2. **Java/Zeta Validation** - Native Java validation using the Zeta framework

Both validators are imported from [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression) and executed through adapter wrappers in this project.

## Validation Entry Points

The `ExpressionValidatorOnPsm` class provides the main entry points for validation:

```java
// EVL validation (default)
ExpressionValidatorOnPsm.validateExpressionOnPsm(log, psmModel, expressionModel);

// EVL validation with expected results
ExpressionValidatorOnPsm.validateExpressionOnPsm(log, psmModel, expressionModel,
    expectedErrors, expectedWarnings);

// Java/Zeta validation
ExpressionValidatorOnPsm.validateExpressionOnPsmWithZeta(log, psmModel, expressionModel);

// Java/Zeta validation with expected results and parallel option
ExpressionValidatorOnPsm.validateExpressionOnPsmWithZeta(log, psmModel, expressionModel,
    expectedErrors, expectedWarnings, parallel);
```

## Test Infrastructure

### ValidatorType Enum

Used to select which validator to use in parameterized tests:

```java
public enum ValidatorType {
    EVL,   // Epsilon Validation Language
    JAVA   // Native Java/Zeta validation
}
```

### AbstractExpressionPsmValidationTest

Base class for dual validation tests. Provides:
- Model initialization (`initModels()`)
- Unified validation execution (`runValidation()`)
- Automatic handling of EVL vs Java result format differences

### ExpressionPsmValidationTest

Parameterized tests that run with both validators:

```java
@ParameterizedTest(name = "testValidation [{0}]")
@EnumSource(ValidatorType.class)
void testValidation(ValidatorType type) throws Exception {
    this.validatorType = type;
    initModels();

    // Build expression model...

    runValidation(
        ImmutableList.of("ExpectedError1"),
        ImmutableList.of()
    );
}
```

## Performance Testing

### ExpressionPsmValidationPerformanceTest

Benchmarks validation performance across all modes:
- EVL validation
- Java sequential validation
- Java parallel validation

Run performance tests with:

```bash
./mvnw test -Dgroups=performance
```

### PsmTestModelGenerator

Generates large test models with rackinspect-like characteristics:
- 70 entity types
- 14 attributes per entity
- 6 relations per entity
- 5 expressions per entity
- Total: 10,000+ elements

## Key Differences: EVL vs Java

| Aspect | EVL | Java/Zeta |
|--------|-----|-----------|
| Language | Epsilon Validation Language | Native Java |
| IDE Support | Limited | Full IntelliJ/Eclipse |
| Debugging | Script-level | Step-through debugging |
| Performance | Slower startup | Faster, especially parallel |
| Error Format | `ConstraintName\|Message` | `ConstraintName` |

## Validation Result Handling

The test infrastructure handles format differences automatically:

1. EVL produces: `ConstraintName|Full error message with context`
2. Java produces: `ConstraintName` (constraint name only)

The `AbstractExpressionPsmValidationTest` extracts constraint names from EVL results for comparison, enabling the same expected error/warning lists to work with both validators.

## Dependencies

Zeta dependencies are managed in the parent `pom.xml`:

```xml
<properties>
    <judo-zeta-version>1.0.0....</judo-zeta-version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>hu.blackbelt.judo.zeta</groupId>
            <artifactId>hu.blackbelt.judo.zeta.validation-core</artifactId>
            <version>${judo-zeta-version}</version>
        </dependency>
        <!-- Additional Zeta dependencies -->
    </dependencies>
</dependencyManagement>
```

## Adding New Validation Tests

1. Extend `AbstractExpressionPsmValidationTest`
2. Initialize models with `initModels()`
3. Build expression model content
4. Call `runValidation(expectedErrors, expectedWarnings)`
5. Use `@ParameterizedTest` with `@EnumSource(ValidatorType.class)`

Example:

```java
public class MyValidationTest extends AbstractExpressionPsmValidationTest {

    @ParameterizedTest(name = "testMyConstraint [{0}]")
    @EnumSource(ValidatorType.class)
    void testMyConstraint(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModels();

        // Add expression content that should trigger error
        expressionModel.getExpressionModelResourceSupport().addContent(
            // ... build invalid expression ...
        );

        runValidation(
            ImmutableList.of("MyConstraintName"),
            ImmutableList.of()
        );
    }
}
```

## Related Documentation

- [Zeta Framework](https://github.com/BlackBeltTechnology/judo-zeta) - Java validation framework
- [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression) - Expression metamodel and validators
- [Epsilon](https://www.eclipse.org/epsilon/) - Epsilon platform for EVL
