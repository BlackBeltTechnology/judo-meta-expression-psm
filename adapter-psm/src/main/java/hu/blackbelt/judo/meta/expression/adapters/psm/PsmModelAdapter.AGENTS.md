# `PsmModelAdapter.java`

Binds the generic expression `ModelAdapter` SPI to the PSM metamodel. Sole implementation of
`ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement,
ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence,
Measure, Unit>` — the twelve type parameters are the PSM classifiers the ASM and ESM sibling
adapters bind differently, so a row or a change here is PSM-specific by construction.

## Construction

`PsmModelAdapter(ResourceSet psmResourceSet, ResourceSet measureResourceSet)` — builds a
`PsmMeasureProvider` over `measureResourceSet` and wraps it in a `MeasureAdapter`. The measure
resource set MUST be the one the PSM resource actually references; passing an unrelated set makes
every `getMeasure`/`getUnit` lookup silently return empty rather than fail.

## Name resolution

`buildTypeName(NamespaceElement)`, `get(TypeName)`, `get(MeasureName)`, `buildMeasureName(Measure)`,
`getFqName(Object)`, `getName(Object)`. PSM namespaces nest via `Package` and are joined with
`::`; expression `TypeName.namespace` uses `.`, so `get` rewrites `.` → `::` before matching.
`get(TypeName)` returns **`null`, not `Optional.empty()`**, when the namespace itself is missing —
callers dereferencing the result without a null check NPE on an unknown namespace. `getFqName`
dispatches to `PsmUtils.attributeToString` / `relationToString` / `transferAttributeToString` /
`transferObjectRelationToString` and returns `null` for anything else.

## Entity vs. transfer-object split (PSM-specific)

PSM separates the persistence side (`EntityType`, `Attribute`, `Relation`, `Sequence`) from the
exposed side (`TransferObjectType`, `TransferAttribute`, `TransferObjectRelation`), so the adapter
carries paired accessors: `getAttribute`/`getTransferAttribute`, `getReference`/`getTransferRelation`,
`getTarget`/`getTransferRelationTarget`, `isCollectionReference`/`isCollectionTransferRelation`,
`isDerivedAttribute`/`isDerivedTransferAttribute`, `isDerivedReference`/`isDerivedTransferRelation`,
plus the getter/setter/default/range accessors on both sides. `getMappedEntityType` unwraps only a
`MappedTransferObjectType`; an unmapped transfer object yields empty.
`getEntityTypeOfTransferObjectRelationTarget(TypeName, String)` and
`isCollectionReference(TypeName, String)` accept either kind of `TypeName` and branch on the
resolved element — an unmapped target degrades to empty/`false` instead of throwing.
Collection views: `getAllEntityTypes`, `getAllEnums`, `getAllPrimitiveTypes`, `getAllMeasures`,
`getAllStaticSequences`, `getAllTransferObjectTypes`, `getAllMappedTransferObjectTypes`,
`getAllUnmappedTransferObjectTypes`, `getAllActorTypes`, `getContainerTypesOf(EntityType)`,
`getPrincipal(NamespaceElement)`, `getFilter(TransferObjectType)`.

`isMixin(included, mixin)` is structural, not nominal: every attribute of `included` must have a
same-name, same-`dataType`, same-`binding` counterpart in `mixin`, and every relation a same-name
counterpart with equal `target`, equal cardinality `lower`/`upper`, and equal `binding`. Renaming a
mixin member silently breaks the mixin relation.

## Type predicates

`isObjectType`, `isPrimitiveType`, `isSequence`, `isMeasuredType`, `isNumeric`, `isInteger`,
`isDecimal`, `isBoolean`, `isString`, `isEnumeration`, `isDate`, `isTimestamp`, `isTime`,
`isCustom`, `contains(EnumerationType, memberName)` — delegated to the PSM `Primitive` flags
(`primitive.isNumeric()` etc.), so they follow the model, not a Java instance-class list.

## Measure / unit bridge

`isMeasured(NumericExpression)`, `getMeasure`, `getUnit`, `getUnits(Measure)`, `getUnitName`,
`getUnitRates`, `getDimension`, `isDurationSupportingAddition`. `getUnit(NumericExpression)` handles
`NumericAttribute` (reads `MeasuredType.getStoreUnit()` off the attribute's data type),
`MeasuredDecimal` and `MeasuredDecimalEnvironmentVariable` (delegate to `MeasureAdapter`); anything
else is empty. `getUnitRates` wraps `Unit.rateDividend`/`rateDivisor` into a `UnitFraction`.
`getDimension` maps `MeasureAdapter.MeasureId` back to PSM `Measure` instances and drops ids the
provider cannot resolve.

`getBaseDurationRatio(Unit, DurationType)` throws `IllegalArgumentException` on three distinct
violations: unit not a `DurationUnit`; unit type `MONTH` or `YEAR` (no fixed ratio exists); target
type other than `SECOND` or `DAY`. It converts PSM `DurationType` to the expression `DurationType`,
then rescales `getSecondUnitFraction()`/`getDayUnitFraction()` by `rateDivisor/rateDividend` so the
returned fraction is expressed in the measure's base unit, not in seconds.
