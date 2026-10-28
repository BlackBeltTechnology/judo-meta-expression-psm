# Change: Add Zeta Dual Validation Support

## Why
The judo-meta-expression project now provides both EVL (Epsilon Validation Language) and Java-based Zeta validators. This project needs to integrate the dual validation capability to:
1. Enable Java-based validation as an alternative to EVL (better performance, IDE support, debugging)
2. Ensure validation parity between EVL and Java implementations through parameterized testing
3. Align with the pattern already established in judo-meta-esm

## What Changes

### Adapter Module (adapter-psm)
- Extend `ExpressionValidatorOnPsm` to support both EVL and Java (Zeta) validation modes
- Add method overloads that accept a `ValidatorType` parameter

### Test Module (adapter-psm-test)
- Add `ValidatorType` enum with `EVL` and `JAVA` values
- Create `AbstractExpressionPsmValidationTest` base class for dual validation testing
- Modify existing validation tests to run as parameterized tests with both validator types
- Add performance comparison test with 10,000+ elements (rackinspect-like model characteristics)

### OSGi Integration (osgi-itest)
- Add Zeta framework bundles to test-features.xml
- Add epsilon-runtime feature repository
- Verify both validation modes work in Karaf container

### Maven Configuration
- Add `judo-zeta-version` property to parent pom.xml
- Add Zeta dependencies (validation-core, annotations, common) to dependency management
- Update adapter-psm-test pom.xml with Zeta test dependencies

### Documentation
- Convert README.adoc to README.md (except pages directory)
- Convert .github/CIFLOW.adoc to markdown
- Convert PlantUML diagrams to Mermaid
- Create docs/validation/README.md with validation architecture documentation
- Reference Zeta framework documentation (do not copy)

## Impact
- **Affected specs**: dual-validation (new capability)
- **Affected code**:
  - `adapter-psm/src/main/java/.../ExpressionValidatorOnPsm.java`
  - `adapter-psm-test/src/test/java/.../*.java` (all validation tests)
  - `adapter-psm-test/pom.xml`
  - `osgi-itest/pom.xml`
  - `osgi-itest/src/test/resources/test-features.xml`
  - `pom.xml` (parent)
  - `README.adoc` -> `README.md`
  - `docs/` (new validation documentation)

## Key Design Decisions

### No New Validators
This project does NOT implement any validation rules. It imports and executes validators from judo-meta-expression:
- `ExpressionValidator.validateExpression()` - EVL validation
- `ExpressionZetaValidator.validateExpression()` - Java/Zeta validation

### Dual Validation Testing Pattern
Following the judo-meta-esm pattern:
```java
@ParameterizedTest(name = "testValidation [{0}]")
@EnumSource(ValidatorType.class)
void testValidation(ValidatorType type) throws Exception {
    this.validatorType = type;
    initModels(psmModel, measureModel);
    runValidation(expectedErrors, expectedWarnings);
}
```

### Performance Test Model Characteristics
Based on rackinspect analysis:
- 70+ entity types with 14 attributes average
- 400+ relations (aggregation/composition mix)
- 300+ derived members with expressions
- Target: 10,000+ total elements for stress testing

### Zeta Version
Use latest SNAPSHOT from develop branch, managed via `${judo-zeta-version}` property.
