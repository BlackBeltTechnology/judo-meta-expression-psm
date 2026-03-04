# adapter-psm Specification

## Purpose

Provides the core PSM model adapter that bridges the JUDO Expression Language to PSM metamodel elements, enabling type resolution, attribute/reference navigation, measure support, and expression validation against PSM-defined schemas.

## Architecture

The module contains three classes in `hu.blackbelt.judo.meta.expression.adapters.psm`:

- **PsmModelAdapter** — Implements `ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit>`. Delegates measure operations to `PsmMeasureProvider` via a `MeasureAdapter` bridge.
- **PsmMeasureProvider** — Implements `MeasureProvider<Measure, Unit>`. Traverses PSM `ResourceSet` to resolve measures, units, derived measures, and duration types. Registers an `EContentAdapter` for change notification.
- **ExpressionValidatorOnPsm** — Static facade that creates a `PsmModelAdapter` and delegates to `ExpressionValidator.validateExpression()`.

## Requirements

### Requirement: Type Name Resolution

The adapter SHALL resolve symbolic `TypeName` objects to PSM `NamespaceElement` instances by matching namespace (fully-qualified with `::` separator) and element name.

#### Scenario: Resolve entity type by fully-qualified name
- **GIVEN** a PSM model containing `EntityType` named `Order` in namespace `demo::entities`
- **WHEN** `get(TypeName)` is called with namespace `demo::entities` and name `Order`
- **THEN** the adapter returns an `Optional` containing the `Order` EntityType

#### Scenario: Resolve with dot-separated namespace
- **GIVEN** a `TypeName` with namespace `demo.entities` (dot-separated)
- **WHEN** `get(TypeName)` is called
- **THEN** the adapter converts dots to `::` separators and resolves correctly

#### Scenario: Namespace not found
- **GIVEN** a `TypeName` with a non-existent namespace
- **WHEN** `get(TypeName)` is called
- **THEN** the adapter logs a warning and returns `null`

### Requirement: Type Name Building

The adapter SHALL build `TypeName` objects from PSM `NamespaceElement` instances.

#### Scenario: Build type name from entity type
- **GIVEN** a PSM `EntityType` contained in a namespace
- **WHEN** `buildTypeName(NamespaceElement)` is called
- **THEN** a `TypeName` is returned with the fully-qualified namespace and element name

### Requirement: Type Classification

The adapter SHALL correctly classify PSM `Primitive` types into their categories by delegating to the Primitive's own classification methods.

#### Scenario: Classify numeric type
- **WHEN** `isNumeric(primitive)` is called on a numeric primitive
- **THEN** it returns `true`

#### Scenario: Classify all type categories
- **WHEN** any of `isInteger`, `isDecimal`, `isBoolean`, `isString`, `isEnumeration`, `isDate`, `isTimestamp`, `isTime`, `isCustom` is called
- **THEN** each correctly delegates to the corresponding `Primitive` method

#### Scenario: Custom type detection
- **GIVEN** a primitive that is not boolean, numeric, string, enumeration, date, timestamp, or time
- **WHEN** `isCustom(primitive)` is called
- **THEN** it returns `true`

### Requirement: Object Type and Primitive Type Identification

The adapter SHALL distinguish between object types (EntityType) and primitive types using `instanceof` checks.

#### Scenario: Identify entity type
- **GIVEN** a `NamespaceElement` that is an `EntityType`
- **WHEN** `isObjectType(namespaceElement)` is called
- **THEN** it returns `true`

#### Scenario: Identify primitive type
- **GIVEN** a `NamespaceElement` that is a `Primitive`
- **WHEN** `isPrimitiveType(namespaceElement)` is called
- **THEN** it returns `true`

### Requirement: Attribute Resolution

The adapter SHALL resolve attributes from entity types by name.

#### Scenario: Resolve existing attribute
- **GIVEN** an `EntityType` with an attribute named `quantity`
- **WHEN** `getAttribute(entityType, "quantity")` is called
- **THEN** the attribute's `PrimitiveTypedElement` is returned

#### Scenario: Resolve attribute type
- **GIVEN** an `EntityType` with attribute `price` of type `Decimal`
- **WHEN** `getAttributeType(entityType, "price")` is called
- **THEN** the `Decimal` primitive type is returned

### Requirement: Reference Resolution

The adapter SHALL resolve references (relations) from entity types and determine their collection status.

#### Scenario: Resolve reference by name
- **GIVEN** an `EntityType` with a reference named `items`
- **WHEN** `getReference(entityType, "items")` is called
- **THEN** the `ReferenceTypedElement` is returned

#### Scenario: Check collection reference
- **GIVEN** a reference with `isCollection() == true`
- **WHEN** `isCollectionReference(reference)` is called
- **THEN** it returns `true`

#### Scenario: Get reference target
- **GIVEN** a reference pointing to `EntityType` `OrderItem`
- **WHEN** `getTarget(reference)` is called
- **THEN** `OrderItem` entity type is returned

### Requirement: Super Type Resolution

The adapter SHALL resolve all super entity types using `PsmUtils.getAllSuperEntityTypes()`.

#### Scenario: Get super types
- **GIVEN** an `EntityType` that extends another entity type
- **WHEN** `getSuperTypes(entityType)` is called
- **THEN** all ancestor entity types are returned

### Requirement: Containment Resolution

The adapter SHALL find container types of a given entity type by scanning all entity types for `Containment` relations.

#### Scenario: Find container types
- **GIVEN** `EntityType` `OrderItem` contained by `Order` via a `Containment` relation
- **WHEN** `getContainerTypesOf(OrderItem)` is called
- **THEN** `Order` and its super types are returned

### Requirement: Derived Attribute and Reference Support

The adapter SHALL identify derived attributes (`PrimitiveAccessor`) and derived references (`ReferenceAccessor`) and extract their getter expressions.

#### Scenario: Identify derived attribute
- **GIVEN** an attribute that is a `PrimitiveAccessor`
- **WHEN** `isDerivedAttribute(attribute)` is called
- **THEN** it returns `true`

#### Scenario: Get attribute getter expression
- **GIVEN** a `PrimitiveAccessor` with a getter expression
- **WHEN** `getAttributeGetter(attribute)` is called
- **THEN** the getter expression string is returned

#### Scenario: Identify derived reference
- **GIVEN** a reference that is a `ReferenceAccessor`
- **WHEN** `isDerivedReference(reference)` is called
- **THEN** it returns `true`

### Requirement: Transfer Object Type Support

The adapter SHALL support `TransferObjectType` operations including attribute lookup, relation navigation, and mapped entity type resolution.

#### Scenario: Get transfer attribute
- **GIVEN** a `TransferObjectType` with attribute `name`
- **WHEN** `getTransferAttribute(transferObject, "name")` is called
- **THEN** the `TransferAttribute` is returned

#### Scenario: Get mapped entity type
- **GIVEN** a `MappedTransferObjectType` mapped to `EntityType` `Customer`
- **WHEN** `getMappedEntityType(transferObject)` is called
- **THEN** `Customer` entity type is returned

#### Scenario: Get filter expression
- **GIVEN** a `MappedTransferObjectType` with a filter expression
- **WHEN** `getFilter(transferObject)` is called
- **THEN** the filter expression string is returned

### Requirement: Mixin Compatibility Check

The adapter SHALL determine if one transfer object type is a mixin of another by comparing all attributes and relations for name, type, binding, and cardinality equality.

#### Scenario: Compatible mixin
- **GIVEN** two transfer object types with identical attribute names, types, and bindings
- **WHEN** `isMixin(included, mixin)` is called
- **THEN** it returns `true`

### Requirement: Actor Type Support

The adapter SHALL enumerate all `AbstractActorType` instances and resolve their principal `TransferObjectType`.

#### Scenario: Get all actor types
- **WHEN** `getAllActorTypes()` is called
- **THEN** all `AbstractActorType` instances from the PSM model are returned

#### Scenario: Get principal of actor type
- **GIVEN** an `AbstractActorType` with an associated transfer object type
- **WHEN** `getPrincipal(actorType)` is called
- **THEN** the associated `TransferObjectType` is returned

### Requirement: Enumeration Member Lookup

The adapter SHALL check if an enumeration contains a given member name.

#### Scenario: Check enum member exists
- **GIVEN** an `EnumerationType` with member `ACTIVE`
- **WHEN** `contains(enumeration, "ACTIVE")` is called
- **THEN** it returns `true`

### Requirement: Sequence Support

The adapter SHALL resolve static sequences (namespace-level) and entity-level sequences.

#### Scenario: Get all static sequences
- **WHEN** `getAllStaticSequences()` is called
- **THEN** all `NamespaceSequence` instances are returned as `NamespaceElement`

#### Scenario: Get entity sequence
- **GIVEN** an `EntityType` with a sequence named `orderSeq`
- **WHEN** `getSequence(entityType, "orderSeq")` is called
- **THEN** the sequence is returned

### Requirement: Measure Resolution from PSM

`PsmMeasureProvider` SHALL resolve measures and units by namespace and name from the PSM resource set.

#### Scenario: Resolve measure by namespace and name
- **GIVEN** a `Measure` named `Mass` in namespace `demo::measures`
- **WHEN** `getMeasure("demo::measures", "Mass")` is called
- **THEN** the `Mass` measure is returned

#### Scenario: Resolve unit by name or symbol
- **GIVEN** a unit named `kilogram` with symbol `kg`
- **WHEN** `getUnitByNameOrSymbol(Optional.of(measure), "kg")` is called
- **THEN** the `kilogram` unit is returned

#### Scenario: Ambiguous unit symbol without measure
- **GIVEN** two measures with units sharing the same symbol
- **WHEN** `getUnitByNameOrSymbol(Optional.empty(), symbol)` is called
- **THEN** an `IllegalStateException` is thrown

### Requirement: Derived Measure Base Calculation

`PsmMeasureProvider` SHALL recursively compute base measures and their exponents for derived measures.

#### Scenario: Get base measures of derived measure
- **GIVEN** a `DerivedMeasure` "Velocity" defined as Length^1 * Time^-1
- **WHEN** `getBaseMeasures(velocity)` is called
- **THEN** a map is returned with Length → 1 and Time → -1

#### Scenario: Base measure returns itself
- **GIVEN** a non-derived `Measure` "Length"
- **WHEN** `getBaseMeasures(length)` is called
- **THEN** a singleton map with Length → 1 is returned

### Requirement: Duration Unit Addition Support

`PsmMeasureProvider` SHALL identify which duration units support temporal addition (millisecond through week, excluding month and year).

#### Scenario: Duration supporting addition
- **GIVEN** a `DurationUnit` with type `HOUR`
- **WHEN** `isDurationSupportingAddition(unit)` is called
- **THEN** it returns `true`

#### Scenario: Duration not supporting addition
- **GIVEN** a `DurationUnit` with type `MONTH` or `YEAR`
- **WHEN** `isDurationSupportingAddition(unit)` is called
- **THEN** it returns `false`

### Requirement: Duration Base Ratio Computation

`PsmModelAdapter` SHALL compute duration-to-second and duration-to-day ratios for `DurationUnit` instances, accounting for the base unit's rate.

#### Scenario: Compute second ratio
- **GIVEN** a `DurationUnit` of type `DAY` with rate relative to base
- **WHEN** `getBaseDurationRatio(unit, DurationType.SECOND)` is called
- **THEN** the correct `UnitFraction` for seconds is returned

#### Scenario: Unsupported duration type
- **WHEN** `getBaseDurationRatio(unit, DurationType.MINUTE)` is called
- **THEN** an `IllegalArgumentException` is thrown (only SECOND and DAY are supported)

#### Scenario: Non-duration unit
- **GIVEN** a unit that is not a `DurationUnit`
- **WHEN** `getBaseDurationRatio(unit, DurationType.SECOND)` is called
- **THEN** an `IllegalArgumentException` is thrown

### Requirement: Measure Change Notification

`PsmMeasureProvider` SHALL register an `EContentAdapter` on the resource set that notifies a `MeasureChangedHandler` when measures are added, removed, or their terms change.

#### Scenario: Measure added notification
- **GIVEN** a measure change handler is registered
- **WHEN** a new `Measure` is added to the resource set
- **THEN** `measureAdded()` is called on the handler

#### Scenario: Derived measure terms changed
- **GIVEN** a measure change handler is registered
- **WHEN** the terms of a `DerivedMeasure` are modified
- **THEN** `measureChanged()` is called on the handler

### Requirement: Expression Validation

`ExpressionValidatorOnPsm` SHALL validate expression models against PSM models, reporting errors and warnings.

#### Scenario: Validate valid expression
- **GIVEN** a valid `ExpressionModel` and `PsmModel`
- **WHEN** `validateExpressionOnPsm(log, psmModel, expressionModel)` is called
- **THEN** no `ExpressionValidationException` is thrown

#### Scenario: Validate with expected errors
- **GIVEN** an `ExpressionModel` with known issues
- **WHEN** `validateExpressionOnPsm(log, psmModel, expressionModel, expectedErrors, expectedWarnings)` is called
- **THEN** validation passes when actual errors match the expected errors

#### Scenario: Validate invalid expression
- **GIVEN** an `ExpressionModel` with type resolution errors
- **WHEN** `validateExpressionOnPsm(log, psmModel, expressionModel)` is called
- **THEN** an `ExpressionValidationException` is thrown with diagnostic details

### Requirement: Measured Type Support

The adapter SHALL detect measured types and resolve their associated measure and store unit.

#### Scenario: Detect measured primitive
- **GIVEN** a `Primitive` with `isMeasured() == true`
- **WHEN** `isMeasuredType(primitive)` is called
- **THEN** it returns `true`

#### Scenario: Get measure of measured type
- **GIVEN** a `MeasuredType` with store unit belonging to measure "Mass"
- **WHEN** `getMeasureOfType(primitive)` is called
- **THEN** the "Mass" measure is returned

#### Scenario: Get unit of non-measured type
- **GIVEN** a `Primitive` with `isMeasured() == false`
- **WHEN** `getUnitOfType(primitive)` is called
- **THEN** `Optional.empty()` is returned

### Requirement: Dimension Computation

The adapter SHALL compute measure dimensions for numeric expressions by delegating to `MeasureAdapter.getDimension()` and resolving measure IDs to actual `Measure` objects.

#### Scenario: Get dimension of numeric expression
- **GIVEN** a numeric expression involving measured attributes
- **WHEN** `getDimension(numericExpression)` is called
- **THEN** a map of `Measure → Integer` exponents is returned
