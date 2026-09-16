# Dual Validation Capability

## ADDED Requirements

### Requirement: Dual Validation Mode Support
The system SHALL support running expression validation using both EVL (Epsilon Validation Language) and Java/Zeta validators to ensure validation parity.

#### Scenario: EVL validation execution
- **WHEN** ExpressionValidatorOnPsm.validateExpressionOnPsm() is called
- **THEN** the EVL-based ExpressionValidator from judo-meta-expression is invoked
- **AND** validation errors and warnings are reported in EVL format

#### Scenario: Java/Zeta validation execution
- **WHEN** ExpressionValidatorOnPsm.validateExpressionOnPsmWithZeta() is called
- **THEN** the Java-based ExpressionZetaValidator from judo-meta-expression is invoked
- **AND** validation errors and warnings are reported with constraint names

#### Scenario: Validation result parity
- **WHEN** the same expression model is validated with both EVL and Java validators
- **THEN** both validators SHALL produce semantically equivalent results
- **AND** the same constraint names appear in both result sets

---

### Requirement: ValidatorType Parameterized Testing
The test infrastructure SHALL provide parameterized test support for running validation tests with both EVL and Java validators.

#### Scenario: Parameterized test execution
- **WHEN** a test class extends AbstractExpressionPsmValidationTest
- **AND** the test method is annotated with @ParameterizedTest and @EnumSource(ValidatorType.class)
- **THEN** the test runs twice: once with ValidatorType.EVL and once with ValidatorType.JAVA

#### Scenario: Constraint name extraction from EVL
- **WHEN** EVL validation produces errors in format "ConstraintName|Message"
- **THEN** the test infrastructure extracts "ConstraintName" for comparison with Java results

---

### Requirement: Performance Comparison Testing
The system SHALL provide performance tests comparing EVL and Java validation execution times with realistic model complexity.

#### Scenario: Performance test model generation
- **WHEN** a performance test is executed
- **THEN** a test model with 10,000+ elements is generated programmatically
- **AND** the model characteristics match rackinspect complexity (70 entities, 14 attributes/entity, 6 relations/entity)

#### Scenario: Performance benchmark execution
- **WHEN** the performance test runs
- **THEN** EVL validation time is measured and logged
- **AND** Java sequential validation time is measured and logged
- **AND** Java parallel validation time is measured and logged
- **AND** results are comparable for analysis

---

### Requirement: Zeta Framework OSGi Integration
The OSGi integration tests SHALL verify that Zeta validation framework bundles work correctly in Karaf container.

#### Scenario: Zeta bundles loaded in Karaf
- **WHEN** the OSGi integration test starts
- **THEN** hu.blackbelt.judo.zeta.validation-core bundle is loaded
- **AND** hu.blackbelt.judo.zeta.annotations bundle is loaded
- **AND** hu.blackbelt.judo.zeta.common bundle is loaded

#### Scenario: Java validation in OSGi
- **WHEN** ExpressionZetaValidator.validateExpression() is called in Karaf container
- **THEN** validation executes successfully
- **AND** results match EVL validation results

---

### Requirement: Zeta Version Property Management
All Zeta framework dependencies SHALL be managed through a single version property in the parent POM.

#### Scenario: Consistent Zeta version
- **WHEN** pom.xml references any Zeta artifact
- **THEN** the version is specified as ${judo-zeta-version}
- **AND** judo-zeta-version property is defined in parent pom.xml properties section

---

### Requirement: Documentation Updates
Project documentation SHALL be updated to reflect dual validation capability and converted to modern formats.

#### Scenario: AsciiDoc to Markdown conversion
- **WHEN** documentation exists as .adoc files (except under pages/)
- **THEN** it is converted to .md (Markdown) format

#### Scenario: PlantUML to Mermaid conversion
- **WHEN** documentation contains PlantUML diagrams
- **THEN** they are converted to Mermaid diagram format

#### Scenario: Validation documentation
- **WHEN** docs/validation/README.md is accessed
- **THEN** it documents the dual validation architecture
- **AND** it references Zeta framework documentation (links only, no copies)
- **AND** it documents all validation methods in ExpressionValidatorOnPsm
