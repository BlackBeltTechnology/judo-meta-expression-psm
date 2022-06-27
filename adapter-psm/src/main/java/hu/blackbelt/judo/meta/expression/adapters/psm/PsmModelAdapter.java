package hu.blackbelt.judo.meta.expression.adapters.psm;

import hu.blackbelt.judo.meta.expression.MeasureName;
import hu.blackbelt.judo.meta.expression.NumericExpression;
import hu.blackbelt.judo.meta.expression.ReferenceSelector;
import hu.blackbelt.judo.meta.expression.TypeName;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.expression.numeric.NumericAttribute;
import hu.blackbelt.judo.meta.expression.variable.MeasuredDecimalEnvironmentVariable;
import hu.blackbelt.judo.meta.psm.PsmUtils;
import hu.blackbelt.judo.meta.psm.accesspoint.AbstractActorType;
import hu.blackbelt.judo.meta.psm.data.*;
import hu.blackbelt.judo.meta.psm.derived.PrimitiveAccessor;
import hu.blackbelt.judo.meta.psm.derived.ReferenceAccessor;
import hu.blackbelt.judo.meta.psm.measure.DurationUnit;
import hu.blackbelt.judo.meta.psm.measure.Measure;
import hu.blackbelt.judo.meta.psm.measure.MeasuredType;
import hu.blackbelt.judo.meta.psm.measure.Unit;
import hu.blackbelt.judo.meta.psm.namespace.NamedElement;
import hu.blackbelt.judo.meta.psm.namespace.Namespace;
import hu.blackbelt.judo.meta.psm.namespace.NamespaceElement;
import hu.blackbelt.judo.meta.psm.namespace.Package;
import hu.blackbelt.judo.meta.psm.service.MappedTransferObjectType;
import hu.blackbelt.judo.meta.psm.service.TransferAttribute;
import hu.blackbelt.judo.meta.psm.service.TransferObjectRelation;
import hu.blackbelt.judo.meta.psm.service.TransferObjectType;
import hu.blackbelt.judo.meta.psm.type.EnumerationType;
import hu.blackbelt.judo.meta.psm.type.Primitive;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newMeasureNameBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static java.util.stream.Collectors.toList;

/**
 * Model adapter for PSM models.
 */
public class PsmModelAdapter implements ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit> {

    private static final String NAMESPACE_SEPARATOR = "::";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PsmModelAdapter.class);
    private final ResourceSet psmResourceSet;
    private final MeasureProvider<Measure, Unit> measureProvider;
    private final MeasureAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit> measureAdapter;

    /**
     * Create PSM model adapter for expressions.
     *
     * @param psmResourceSet     PSM resource set
     * @param measureResourceSet PSM resource set containing measures (must be the one that PSM resource is referencing)
     */
    public PsmModelAdapter(final ResourceSet psmResourceSet, final ResourceSet measureResourceSet) {
        this.psmResourceSet = psmResourceSet;
        measureProvider = new PsmMeasureProvider(measureResourceSet);
        measureAdapter = new MeasureAdapter<>(measureProvider, this);
    }

    @Override
    public Optional<TypeName> buildTypeName(final NamespaceElement namespaceElement) {
        return getPsmElement(Namespace.class)
                .filter(ns -> ns.getElements().contains(namespaceElement))
                .map(ns -> newTypeNameBuilder().withNamespace(getNamespaceFQName(ns)).withName(namespaceElement.getName()).build())
                .findAny();
    }

    @Override
    public Optional<? extends NamespaceElement> get(final TypeName elementName) {
        final Optional<Namespace> namespace = getPsmElement(Namespace.class)
                .filter(ns -> Objects.equals(getNamespaceFQName(ns), elementName.getNamespace().replace(".", NAMESPACE_SEPARATOR)))
                .findAny();
        if (namespace.isPresent()) {
            return namespace.get().getElements().stream()
                    .filter(e -> Objects.equals(e.getName(), elementName.getName()))
                    .findAny();
        } else {
            log.warn("Namespace not found: {}", elementName.getNamespace());
            return null;
        }
    }

    @Override
    public Optional<? extends Measure> get(final MeasureName measureName) {
        return measureProvider.getMeasure(measureName.getNamespace().replace(".", NAMESPACE_SEPARATOR), measureName.getName());
    }

    @Override
    public boolean isObjectType(final NamespaceElement namespaceElement) {
        return namespaceElement instanceof EntityType;
    }

    @Override
    public boolean isPrimitiveType(NamespaceElement namespaceElement) {
        return namespaceElement instanceof Primitive;
    }

    @Override
    public boolean isMeasuredType(Primitive primitiveType) {
        return primitiveType.isMeasured();
    }

    @Override
    public Optional<Measure> getMeasureOfType(Primitive primitiveType) {
        return getUnitOfType(primitiveType).map(Unit::getMeasure);
    }

    @Override
    public Optional<Unit> getUnitOfType(Primitive primitiveType) {
        if (isMeasuredType(primitiveType)) {
            return Optional.ofNullable(((MeasuredType) primitiveType).getStoreUnit());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getUnitName(Unit unit) {
        return unit.getName();
    }

    @Override
    public UnitFraction getUnitRates(Unit unit) {
        return new UnitFraction(BigDecimal.valueOf(unit.getRateDividend()), BigDecimal.valueOf(unit.getRateDivisor()));
    }

    @Override
    public UnitFraction getBaseDurationRatio(Unit unit, DurationType targetType) {
        if (!(unit instanceof DurationUnit)) {
            throw new IllegalArgumentException("Unit must be duration");
        }
        DurationUnit durationUnit = (DurationUnit) unit;
        // examples in comments when unit is day, the base unit is minute
        // example2 unit is millisecond, the base unit is minute
        BigDecimal dividendToBase = BigDecimal.valueOf(durationUnit.getRateDividend()); // 1440 |---| 1
        BigDecimal divisorToBase = BigDecimal.valueOf(durationUnit.getRateDivisor());  // 1 |---| 60000
        DurationType target;
        if (hu.blackbelt.judo.meta.psm.measure.DurationType.NANOSECOND.equals(durationUnit.getUnitType())) {
            target = DurationType.NANOSECOND;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.MICROSECOND.equals(durationUnit.getUnitType())) {
            target = DurationType.MICROSECOND;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.MILLISECOND.equals(durationUnit.getUnitType())) {
            target = DurationType.MILLISECOND;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.SECOND.equals(durationUnit.getUnitType())) {
            target = DurationType.SECOND;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.MINUTE.equals(durationUnit.getUnitType())) {
            target = DurationType.MINUTE;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.HOUR.equals(durationUnit.getUnitType())) {
            target = DurationType.HOUR;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.DAY.equals(durationUnit.getUnitType())) {
            target = DurationType.DAY;
        } else if (hu.blackbelt.judo.meta.psm.measure.DurationType.WEEK.equals(durationUnit.getUnitType())) {
            target = DurationType.WEEK;
        } else {
            throw new IllegalArgumentException("No duration ration is valid for month and year.");
        }
        if (targetType.equals(DurationType.SECOND)) {
            UnitFraction secondFraction = target.getSecondUnitFraction(); // 86400/1 |---| 1/1000
            // however, we need to return the ratio calculated in the base unit. The base unit is minute, so the result must be 60/1
            // so the result is: secondFraction * divisorToBase/dividendToBase, that is
            BigDecimal newDividend = secondFraction.getDividend().multiply(divisorToBase);
            BigDecimal newDivisor = secondFraction.getDivisor().multiply(dividendToBase);
            return new UnitFraction(newDividend, newDivisor);
        } else if (targetType.equals(DurationType.DAY)) {
            UnitFraction dayFraction = target.getDayUnitFraction(); // 86400/1 |---| 1/1000
            // however, we need to return the ratio calculated in the base unit. The base unit is minute, so the result must be 60/1
            // so the result is: dayFraction * divisorToBase/dividendToBase, that is
            BigDecimal newDividend = dayFraction.getDividend().multiply(divisorToBase);
            BigDecimal newDivisor = dayFraction.getDivisor().multiply(dividendToBase);
            return new UnitFraction(newDividend, newDivisor);
        } else {
            throw new IllegalArgumentException("Only second and day duration type is supported.");
        }
    }

    @Override
    public Optional<? extends ReferenceTypedElement> getReference(final EntityType clazz, final String referenceName) {
        return Optional.ofNullable(clazz.getReference(referenceName));
    }

    @Override
    public boolean isCollection(ReferenceSelector referenceSelector) {
        return ((ReferenceTypedElement) referenceSelector.getReference(this)).isCollection();
    }

    @Override
    public boolean isCollectionReference(ReferenceTypedElement reference) {
        return reference.isCollection();
    }

    @Override
    public Optional<TransferObjectType> getAttributeParameterType(PrimitiveTypedElement attribute) {
        if (attribute instanceof PrimitiveAccessor) {
            return Optional.ofNullable(((PrimitiveAccessor) attribute).getGetterExpression().getParameterType());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TransferObjectType> getReferenceParameterType(ReferenceTypedElement reference) {
        if (reference instanceof ReferenceAccessor) {
            return Optional.ofNullable(((ReferenceAccessor) reference).getGetterExpression().getParameterType());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TransferObjectType> getTransferAttributeParameterType(TransferAttribute attribute) {
        if (attribute.getBinding() instanceof PrimitiveAccessor) {
            return Optional.ofNullable(((PrimitiveAccessor) attribute.getBinding()).getGetterExpression().getParameterType());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TransferObjectType> getTransferRelationParameterType(TransferObjectRelation reference) {
        if (reference.getBinding() instanceof ReferenceAccessor) {
            return Optional.ofNullable(((ReferenceAccessor) reference.getBinding()).getGetterExpression().getParameterType());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public EntityType getTarget(ReferenceTypedElement reference) {
        return reference.getTarget();
    }

    @Override
    public Optional<? extends PrimitiveTypedElement> getAttribute(EntityType clazz, String attributeName) {
        return Optional.ofNullable(clazz.getAttribute(attributeName));
    }

    @Override
    public Optional<? extends TransferAttribute> getTransferAttribute(TransferObjectType transferObject, String attributeName) {
        return transferObject.getAttributes().stream().filter(transferAttribute -> transferAttribute.getName().equals(attributeName)).findAny();
    }

    @Override
    public Optional<? extends Primitive> getAttributeType(PrimitiveTypedElement attribute) {
        return Optional.ofNullable(attribute.getDataType());
    }

    @Override
    public Optional<? extends Primitive> getAttributeType(EntityType clazz, String attributeName) {
        return getAttribute(clazz, attributeName).map(PrimitiveTypedElement::getDataType);
    }

    @Override
    public Collection<? extends EntityType> getSuperTypes(EntityType clazz) {
        return PsmUtils.getAllSuperEntityTypes(clazz);
    }

    @Override
    public boolean isMixin(TransferObjectType included, TransferObjectType mixin) {
        if (included == null || mixin == null) {
            return false;
        } else if (EcoreUtil.equals(included, mixin)) {
            return true;
        }
        return included.getAttributes().stream().allMatch(ia -> mixin.getAttributes().stream().anyMatch(ma -> Objects.equals(ma.getName(), ia.getName())
                && Objects.equals(ma.getDataType(), ia.getDataType())
                && (ma.getBinding() == null && ia.getBinding() == null || EcoreUtil.equals(ma.getBinding(), ia.getBinding()))))
                && included.getRelations().stream().allMatch(ir -> mixin.getRelations().stream().anyMatch(mr -> Objects.equals(mr.getName(), ir.getName())
                && mr.getTarget() != null && ir.getTarget() != null && EcoreUtil.equals(mr.getTarget(), ir.getTarget())
                && mr.getCardinality() != null && ir.getCardinality() != null && mr.getCardinality().getLower() == ir.getCardinality().getLower() && mr.getCardinality().getUpper() == ir.getCardinality().getUpper()
                && (mr.getBinding() == null && mr.getBinding() == null || EcoreUtil.equals(mr.getBinding(), ir.getBinding()))));
    }

    @Override
    public boolean isNumeric(Primitive primitive) {
        return primitive.isNumeric();
    }

    @Override
    public boolean isInteger(Primitive primitive) {
        return primitive.isInteger();
    }

    @Override
    public boolean isDecimal(Primitive primitive) {
        return primitive.isDecimal();
    }

    @Override
    public boolean isBoolean(Primitive primitive) {
        return primitive.isBoolean();
    }

    @Override
    public boolean isString(Primitive primitive) {
        return primitive.isString();
    }

    @Override
    public boolean isEnumeration(Primitive primitive) {
        return primitive.isEnumeration();
    }

    @Override
    public boolean isDate(Primitive primitive) {
        return primitive.isDate();
    }

    @Override
    public boolean isTimestamp(Primitive primitive) {
        return primitive.isTimestamp();
    }

    @Override
    public boolean isTime(Primitive primitive) {
        return primitive.isTime();
    }

    @Override
    public boolean isCustom(Primitive primitive) {
        return !primitive.isBoolean()
                && !primitive.isNumeric()
                && !primitive.isString()
                && !primitive.isEnumeration()
                && !primitive.isDate()
                && !primitive.isTimestamp()
                && !primitive.isTime();

    }

    @Override
    public boolean isMeasured(NumericExpression numericExpression) {
        return measureAdapter.isMeasured(numericExpression);
    }

    @Override
    public boolean contains(EnumerationType enumeration, String memberName) {
        return enumeration.contains(memberName);
    }

    @Override
    public boolean isDurationSupportingAddition(Unit unit) {
        return measureProvider.isDurationSupportingAddition(unit);
    }

    @Override
    public Optional<Measure> getMeasure(NumericExpression numericExpression) {
        return measureAdapter.getMeasure(numericExpression);
    }

    @Override
    public Optional<Unit> getUnit(NumericExpression numericExpression) {
        if (numericExpression instanceof NumericAttribute) {
            final EntityType objectType = (EntityType) ((NumericAttribute) numericExpression).getObjectExpression().getObjectType(this);
            final String attributeName = ((NumericAttribute) numericExpression).getAttributeName();

            return getUnit(objectType, attributeName);
            //-------------------
        } else if (numericExpression instanceof MeasuredDecimal) {
            final MeasuredDecimal measuredDecimal = (MeasuredDecimal) numericExpression;
            return measureAdapter.getUnit(
                    measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace()) : Optional.empty(),
                    measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName()) : Optional.empty(),
                    measuredDecimal.getUnitName());
        } else if (numericExpression instanceof MeasuredDecimalEnvironmentVariable) {
            final MeasuredDecimalEnvironmentVariable measuredDecimal = (MeasuredDecimalEnvironmentVariable) numericExpression;
            return measureAdapter.getUnit(
                    measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace()) : Optional.empty(),
                    measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName()) : Optional.empty(),
                    measuredDecimal.getUnitName());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public EList<Unit> getUnits(Measure measure) {
        return measureProvider.getUnits(measure);
    }

    Optional<Unit> getUnit(final EntityType objectType, final String attributeName) {
        final Optional<Optional<Unit>> unit = objectType.getAttributes().stream()
                .filter(a -> Objects.equals(a.getName(), attributeName))
                .map(a -> getUnit(a))
                .findAny();
        if (unit.isPresent()) {
            return unit.get();
        } else {
            log.error("Attribute not found: {}", attributeName);
            return Optional.empty();
        }
    }

    Optional<Unit> getUnit(final Attribute attribute) {
        if (attribute.getDataType().isMeasured()) {
            return Optional.of(((MeasuredType) attribute.getDataType()).getStoreUnit());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Map<Measure, Integer>> getDimension(NumericExpression numericExpression) {
        return measureAdapter.getDimension(numericExpression).map(dimensions -> {
            Map<Measure, Integer> measureMap = new HashMap<>();
            dimensions.entrySet().stream().forEach(entry -> {
                MeasureAdapter.MeasureId measureId = entry.getKey();
                Optional<Measure> measure = measureProvider.getMeasure(measureId.getNamespace(), measureId.getName());
                measure.ifPresent(m -> measureMap.put(m, entry.getValue()));
            });
            return measureMap;
        });
    }

    @Override
    public EList<EntityType> getAllEntityTypes() {
        return ECollections.asEList(getPsmElement(EntityType.class).collect(toList()));
    }

    @Override
    public EList<EnumerationType> getAllEnums() {
        return ECollections.asEList(getPsmElement(EnumerationType.class).collect(toList()));
    }

    @Override
    public EList<Primitive> getAllPrimitiveTypes() {
        return ECollections.asEList(getPsmElement(Primitive.class).collect(toList()));
    }

    @Override
    public EList<NamespaceElement> getAllStaticSequences() {
        return ECollections.asEList(getPsmElement(NamespaceSequence.class).map(ns -> (NamespaceElement) ns).collect(toList()));
    }

    @Override
    public Optional<? extends Sequence> getSequence(EntityType clazz, String sequenceName) {
        return clazz.getSequences().stream().filter(sequence -> Objects.equals(sequence.getName(), sequenceName)).findAny();
    }

    @Override
    public boolean isSequence(NamespaceElement namespaceElement) {
        return namespaceElement instanceof Sequence;
    }

    @Override
    public boolean isDerivedAttribute(PrimitiveTypedElement attribute) {
        return attribute instanceof PrimitiveAccessor;
    }

    @Override
    public boolean isDerivedTransferAttribute(TransferAttribute attribute) {
        return attribute.getBinding() != null && isDerivedAttribute(attribute.getBinding());
    }

    @Override
    public Optional<String> getAttributeGetter(PrimitiveTypedElement attribute) {
        if (attribute instanceof PrimitiveAccessor) {
            return Optional.of(((PrimitiveAccessor) attribute).getGetterExpression().getExpression());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getTransferAttributeGetter(TransferAttribute attribute) {
        return getAttributeGetter(attribute.getBinding());
    }

    @Override
    public Optional<String> getAttributeSetter(PrimitiveTypedElement attribute) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getAttributeDefault(PrimitiveTypedElement attribute) {
        return Optional.empty();
    }

    @Override
    public boolean isDerivedReference(ReferenceTypedElement reference) {
        return reference instanceof ReferenceAccessor;
    }

    @Override
    public boolean isDerivedTransferRelation(TransferObjectRelation relation) {
        return relation.getBinding() != null && isDerivedReference(relation.getBinding());
    }

    @Override
    public Optional<String> getReferenceGetter(ReferenceTypedElement reference) {
        return Optional.ofNullable(reference)
                .filter(ref -> ref instanceof ReferenceAccessor)
                .map(ref -> ((ReferenceAccessor) ref).getGetterExpression().getExpression());
    }

    @Override
    public Optional<String> getTransferRelationGetter(TransferObjectRelation relation) {
        return getReferenceGetter(relation.getBinding());
    }

    @Override
    public Optional<String> getReferenceDefault(ReferenceTypedElement reference) {
        // TODO JNG-1556
        return Optional.empty();
    }

    @Override
    public Optional<String> getReferenceRange(ReferenceTypedElement reference) {
        // TODO JNG-1556
        return Optional.empty();
    }

    @Override
    public Optional<String> getReferenceSetter(ReferenceTypedElement reference) {
        return Optional.ofNullable(reference)
                .filter(ref -> ref instanceof ReferenceAccessor)
                .map(ref -> ((ReferenceAccessor) ref).getSetterExpression().getExpression());
    }

    @Override
    public Optional<String> getTransferRelationSetter(TransferObjectRelation relation) {
        return getReferenceSetter(relation.getBinding());
    }

    @Override
    public Optional<String> getFilter(TransferObjectType transferObjectType) {
        return Optional.of(transferObjectType)
                .filter(to -> to instanceof MappedTransferObjectType)
                .map(to -> ((MappedTransferObjectType) to).getFilter().getExpression());
    }

    @Override
    public EList<Measure> getAllMeasures() {
        return ECollections.asEList(measureProvider.getMeasures().collect(toList()));
    }

    <T> Stream<T> getPsmElement(final Class<T> clazz) {
        final Iterable<Notifier> psmContents = psmResourceSet::getAllContents;
        return StreamSupport.stream(psmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    /**
     * Get fully qualified name of a given namespace.
     *
     * @param namespace namespace
     * @return FQ name
     */
    private String getNamespaceFQName(final Namespace namespace) {
        final Optional<Namespace> containerNamespace;
        if (namespace instanceof Package) {
            containerNamespace = PsmUtils.getNamespaceOfPackage((Package) namespace);
        } else {
            containerNamespace = Optional.empty();
        }

        return (containerNamespace.isPresent() ? getNamespaceFQName(containerNamespace.get()) + NAMESPACE_SEPARATOR : "") + namespace.getName();
    }

    @Override
    public EList<EntityType> getContainerTypesOf(final EntityType entityType) {
        // TODO - change getRelations to getAllRelations
        return ECollections.asEList(getPsmElement(EntityType.class)
                .filter(container -> container.getRelations().stream().anyMatch(c -> (c instanceof Containment) && EcoreUtil.equals(c.getTarget(), entityType)))
                .flatMap(container -> Stream.concat(PsmUtils.getAllSuperEntityTypes(container).stream(), Collections.singleton(container).stream()))
                .collect(toList()));
    }

    @Override
    public Optional<MeasureName> buildMeasureName(Measure measure) {
        return measureProvider.getMeasures()
                .filter(mn -> Objects.equals(mn.getNamespace(), measure.getNamespace()) && Objects.equals(mn.getName(), measure.getName()))
                .findAny().map(m -> newMeasureNameBuilder().withName(m.getName()).withNamespace(m.getSymbol()).build());
    }

    @Override
    public EList<TransferObjectType> getAllTransferObjectTypes() {
        return ECollections.asEList(getPsmElement(TransferObjectType.class).collect(toList()));
    }

    @Override
    public EList<TransferObjectType> getAllMappedTransferObjectTypes() {
        return null;
    }

    @Override
    public EList<TransferObjectType> getAllUnmappedTransferObjectTypes() {
        return null;
    }

    public Optional<? extends TransferObjectRelation> getTransferRelation(TransferObjectType clazz, String referenceName) {
        return clazz.getRelations().stream().filter(r -> r.getName().equalsIgnoreCase(referenceName)).findAny();
    }

    @Override
    public TransferObjectType getTransferRelationTarget(TransferObjectRelation relation) {
        return relation.getTarget();
    }

    public boolean isCollectionTransferRelation(TransferObjectRelation relation) {
        return relation.isCollection();
    }

    @Override
    public Optional<EntityType> getEntityTypeOfTransferObjectRelationTarget(TypeName transferObjectTypeName, String transferObjectRelationName) {
        Optional<? extends NamespaceElement> transferObjectType = get(transferObjectTypeName);
        if (!transferObjectType.isPresent()) {
            return Optional.empty();
        } else if (transferObjectType.get() instanceof EntityType) {
            Optional<? extends ReferenceTypedElement> relation = getReference((EntityType) transferObjectType.get(), transferObjectRelationName);
            if (relation.isPresent()) {
                return Optional.of(getTarget(relation.get()));
            } else {
                return Optional.empty();
            }
        } else if (transferObjectType.get() instanceof TransferObjectType) {
            Optional<? extends TransferObjectRelation> transferObjectRelation = getTransferRelation((TransferObjectType) transferObjectType.get(), transferObjectRelationName);
            if (transferObjectRelation.isPresent()) {
                TransferObjectType transferObjectRelationTarget = getTransferRelationTarget(transferObjectRelation.get());
                if (transferObjectRelationTarget instanceof MappedTransferObjectType) {
                    return Optional.of((((MappedTransferObjectType) transferObjectRelationTarget).getEntityType()));
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean isCollectionReference(TypeName elementName, String referenceName) {
        Optional<? extends NamespaceElement> element = this.get(elementName);
        if (!element.isPresent()) {
            return false;
        } else if (element.get() instanceof EntityType) {
            Optional<? extends ReferenceTypedElement> relation = getReference((EntityType) element.get(), referenceName);
            if (relation.isPresent()) {
                return isCollectionReference(relation.get());
            } else {
                return false;
            }
        } else if (element.get() instanceof TransferObjectType) {
            Optional<? extends TransferObjectRelation> transferObjectRelation = getTransferRelation((TransferObjectType) element.get(), referenceName);
            if (transferObjectRelation.isPresent()) {
                return isCollectionTransferRelation(transferObjectRelation.get());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Optional<EntityType> getMappedEntityType(TransferObjectType mappedTransferObjectType) {
        return Optional.of(mappedTransferObjectType)
                .filter(to -> to instanceof MappedTransferObjectType)
                .map(to -> ((MappedTransferObjectType) to).getEntityType());
    }

    @Override
    public String getFqName(Object object) {
        if (object instanceof Attribute) {
            return PsmUtils.attributeToString((Attribute) object);
        } else if (object instanceof Relation) {
            return PsmUtils.relationToString((Relation) object);
        } else if (object instanceof TransferAttribute) {
            return PsmUtils.transferAttributeToString((TransferAttribute) object);
        } else if (object instanceof TransferObjectRelation) {
            return PsmUtils.transferObjectRelationToString((TransferObjectRelation) object);
        } else {
            return null;
        }
    }

    @Override
    public Optional<String> getName(Object object) {
        return Optional.of(object)
                .filter(o -> o instanceof NamedElement)
                .map(o -> ((NamedElement) o).getName());
    }

    @Override
    public Collection<Attribute> getAttributes(EntityType clazz) {
        return clazz.getAttributes();
    }

    @Override
    public Collection<Relation> getReferences(EntityType clazz) {
        return clazz.getRelations();
    }

    @Override
    public Collection<TransferAttribute> getTransferAttributes(TransferObjectType transferObjectType) {
        return transferObjectType.getAttributes();
    }

    @Override
    public Collection<TransferObjectRelation> getTransferRelations(TransferObjectType transferObjectType) {
        return transferObjectType.getRelations();
    }

    @Override
    public Primitive getTransferAttributeType(TransferAttribute transferAttribute) {
        return transferAttribute.getDataType();
    }

    @Override
    public List<NamespaceElement> getAllActorTypes() {
        return new ArrayList<NamespaceElement>(getPsmElement(AbstractActorType.class).collect(Collectors.toList()));
    }

    @Override
    public TransferObjectType getPrincipal(NamespaceElement actorType) {
        if (actorType instanceof AbstractActorType) {
            return ((AbstractActorType) actorType).getTransferObjectType();
        } else {
            throw new IllegalArgumentException(String.format("Not an actor type: %s", actorType));
        }
    }

}