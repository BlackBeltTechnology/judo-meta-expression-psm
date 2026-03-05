# Expression PSM Adapter

The **JUDO Expression PSM Adapter** bridges the JUDO Expression Language with PSM (Platform Specific Model) metamodels. It enables expression evaluation, type resolution, and validation against PSM-defined entity types, primitives, enumerations, measures, and transfer objects.

This module is part of the [JUDO framework](https://github.com/BlackBeltTechnology) by BlackBelt Technology.

## What It Does

1. **Type Resolution** — Resolves symbolic type names in expressions to actual PSM `EntityType`, `Primitive`, or `EnumerationType` definitions
2. **Attribute & Reference Navigation** — Validates and resolves attribute access and reference navigation expressions against the PSM schema
3. **Measurement Support** — Handles dimensional analysis, measure resolution, and unit conversions (e.g. length, mass, time, derived measures like velocity and density)
4. **Transfer Object Mapping** — Supports service/DTO layer mappings for `TransferAttribute` and `TransferObjectRelation`
5. **Expression Validation** — Validates complete expression models against PSM schemas, reporting errors and warnings

## Module Overview

```mermaid
graph TD
    A["adapter-psm<br/><i>eclipse-plugin</i><br/>Core implementation"] --> B["feature-adapter-psm<br/><i>eclipse-feature</i><br/>Eclipse packaging"]
    B --> C["site<br/><i>eclipse-repository</i><br/>P2 update site"]
    D["adapter-psm-test<br/><i>jar</i><br/>Unit tests"] -.->|tests| A
    E["osgi-itest<br/><i>jar</i><br/>OSGi integration tests"] -.->|tests| A
```

| Module | Packaging | Description |
|--------|-----------|-------------|
| `adapter-psm` | eclipse-plugin | Core adapter: `PsmModelAdapter`, `PsmMeasureProvider`, `ExpressionValidatorOnPsm` |
| `adapter-psm-test` | jar | JUnit 5 unit and integration tests |
| `feature-adapter-psm` | eclipse-feature | Packages the plugin into an Eclipse feature for P2 distribution |
| `osgi-itest` | jar | OSGi integration tests running in a Karaf 4.4.7 container via Pax Exam |
| `site` | eclipse-repository | Builds the Eclipse P2 update site repository |

## Core Architecture

All production code resides in a single package: `hu.blackbelt.judo.meta.expression.adapters.psm`.

```mermaid
classDiagram
    class PsmModelAdapter {
        +getTypeName(NamespaceElement) TypeName
        +get(EntityType) Collection~Attribute~
        +getReference(EntityType) Collection~Reference~
        +isNumericType(Primitive) boolean
        +isBooleanType(Primitive) boolean
        +isStringType(Primitive) boolean
        +isDateType(Primitive) boolean
        +isMeasuredType(Primitive) boolean
        +getSuperTypes(EntityType) Collection
        +getFilter(MappedTransferObjectType) Expression
    }

    class PsmMeasureProvider {
        +getMeasure(String namespace, String name) Optional~Measure~
        +getUnit(Measure, String name) Optional~Unit~
        +getUnitRateDividend(Unit) BigDecimal
        +getUnitRateDivisor(Unit) BigDecimal
        +getBaseMeasure(DerivedMeasure) Map
        +isDurationSupportingAddition(Unit) boolean
    }

    class ExpressionValidatorOnPsm {
        +validateExpressionOnPsm(Logger, PsmModel, ExpressionModel)$
        +validateExpressionOnPsm(Logger, PsmModel, ExpressionModel, Collection errors, Collection warnings)$
    }

    PsmModelAdapter --> PsmMeasureProvider : delegates measure ops
    ExpressionValidatorOnPsm --> PsmModelAdapter : creates & uses
```

### Data Flow

```mermaid
sequenceDiagram
    participant Client
    participant Validator as ExpressionValidatorOnPsm
    participant Adapter as PsmModelAdapter
    participant MeasProv as PsmMeasureProvider
    participant PSM as PSM Model
    participant Expr as Expression Model

    Client->>Validator: validateExpressionOnPsm(psmModel, exprModel)
    Validator->>Adapter: new PsmModelAdapter(psmModel, measureModel)
    Adapter->>MeasProv: new PsmMeasureProvider(psmResourceSet)
    Validator->>Expr: validate expressions
    Expr->>Adapter: resolve type / attribute / reference
    Adapter->>PSM: lookup NamespaceElement
    PSM-->>Adapter: EntityType / Primitive / Enum
    Adapter-->>Expr: resolved type
    Expr->>MeasProv: resolve measure / unit
    MeasProv->>PSM: lookup Measure / Unit
    PSM-->>MeasProv: result
    MeasProv-->>Expr: measure info
    Validator-->>Client: diagnostics (errors, warnings)
```

### External Dependencies

```mermaid
graph LR
    subgraph "JUDO Metamodels"
        ExprModel["judo-meta-expression<br/>Expression metamodel"]
        PsmModel["judo-meta-psm<br/>PSM metamodel"]
        MeasureModel["judo-meta-measure<br/>Measure metamodel"]
        MeasureAdapter["expression-adapter-measure<br/>Measure adapter bridge"]
    end
    subgraph "Eclipse / OSGi"
        EMF["Eclipse EMF<br/>Ecore 2.21+"]
        Epsilon["Epsilon Runtime<br/>Model validation"]
        Tycho["Eclipse Tycho 4.0.13<br/>Plugin build"]
    end
    subgraph "This Project"
        Core["adapter-psm"]
    end
    Core --> ExprModel
    Core --> PsmModel
    Core --> MeasureModel
    Core --> MeasureAdapter
    Core --> EMF
    Core --> Epsilon
```

## Build

Requires **JDK 21+** and **Maven 3.9.9+**. The project uses [SDKMAN!](https://sdkman.io/) for Java version management.

```bash
# Full build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run only unit tests
mvn test -pl adapter-psm-test

# Run a single test class
mvn test -pl adapter-psm-test -Dtest=PsmModelAdapterTest

# Run OSGi integration tests (Karaf container)
mvn test -pl osgi-itest
```

> **Note:** Build success requires `[INFO] BUILD SUCCESS` with no `[ERROR]` lines in the output.

## License

[Eclipse Public License 2.0 (EPL-2.0)](LICENSE.txt)
