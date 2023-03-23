package hu.blackbelt.judo.meta.expression.adapters.psm;

/*-
 * #%L
 * JUDO :: Expression :: PSM Adapter :: Test
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newInstanceBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newIntegerConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newIntegerAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newMeasureNameBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static hu.blackbelt.judo.meta.psm.data.util.builder.DataBuilders.newEntityTypeBuilder;
import static hu.blackbelt.judo.meta.psm.type.util.builder.TypeBuilders.newStringTypeBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import hu.blackbelt.judo.meta.psm.service.TransferAttribute;
import hu.blackbelt.judo.meta.psm.service.TransferObjectRelation;
import org.eclipse.emf.common.notify.Notifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import hu.blackbelt.judo.meta.expression.MeasureName;
import hu.blackbelt.judo.meta.expression.TypeName;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import hu.blackbelt.judo.meta.expression.constant.IntegerConstant;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.expression.numeric.NumericAttribute;
import hu.blackbelt.judo.meta.psm.PsmUtils;
import hu.blackbelt.judo.meta.psm.data.Attribute;
import hu.blackbelt.judo.meta.psm.data.EntityType;
import hu.blackbelt.judo.meta.psm.data.PrimitiveTypedElement;
import hu.blackbelt.judo.meta.psm.data.ReferenceTypedElement;
import hu.blackbelt.judo.meta.psm.data.Relation;
import hu.blackbelt.judo.meta.psm.data.Sequence;
import hu.blackbelt.judo.meta.psm.measure.Measure;
import hu.blackbelt.judo.meta.psm.measure.Unit;
import hu.blackbelt.judo.meta.psm.namespace.Namespace;
import hu.blackbelt.judo.meta.psm.namespace.NamespaceElement;
import hu.blackbelt.judo.meta.psm.namespace.Package;
import hu.blackbelt.judo.meta.psm.service.TransferObjectType;
import hu.blackbelt.judo.meta.psm.type.EnumerationType;
import hu.blackbelt.judo.meta.psm.type.Primitive;
import hu.blackbelt.judo.meta.psm.type.StringType;

public class PsmModelAdapterTest extends ExecutionContextOnPsmTest{

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PsmModelAdapterTest.class);

    private ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit> modelAdapter;

    private static final Logger logger = LoggerFactory.getLogger(PsmModelAdapterTest.class);

    @BeforeEach
    public void setUp() throws Exception {

        super.setUp();
        modelAdapter = new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet());
    }

    @AfterEach
    public void tearDown() {
        modelAdapter = null;
    }

    @Test
    public void testGetTypeName() {
        log.info("Testing: getTypeName(NE): TypeName");

        Optional<TypeName> categoryTypeName = modelAdapter.buildTypeName(getEntityTypeByName("Category").get());

        assertTrue(categoryTypeName.isPresent());
        assertThat(categoryTypeName.get().getName(), is("Category"));
        assertThat(categoryTypeName.get().getNamespace(), is("demo::entities"));
    }

    @Test
    void testGetNamespaceElementByTypeName() {
        log.info("Testing: get(TypeName): NE");
        final TypeName orderTypeName = newTypeNameBuilder()
                .withNamespace("demo::entities")
                .withName("Order")
                .build();


        final Optional<? extends NamespaceElement> orderNamespaceElement = modelAdapter.get(orderTypeName);
        assertTrue(orderNamespaceElement.isPresent());
        assertThat(orderNamespaceElement.get(), instanceOf(EntityType.class));
        assertThat(orderNamespaceElement.get().getName(), is("Order"));
        assertThat(getNamespaceFQName(PsmUtils.getNamespaceOfNamespaceElement(orderNamespaceElement.get()).get()), is("demo::entities"));

        final TypeName negtest_name_TypeName = newTypeNameBuilder()
                .withNamespace("demo::entities")
                .withName("negtest")
                .build();
        final Optional<? extends NamespaceElement> negtest_name_NamespaceElement = modelAdapter.get(negtest_name_TypeName);
        assertThat(negtest_name_NamespaceElement.isPresent(), is(Boolean.FALSE));

        //TODO: remove b\c not needed?
        final TypeName negtest_namespace_TypeName = newTypeNameBuilder()
                .withNamespace("demo::negtest")
                .withName("negtest")
                .build();
        assertTrue(modelAdapter.get(negtest_namespace_TypeName) == null);
    }

    @Test
    void testGetMeasureByMeasureName() {
        log.info("Testing: get(MeasureName): Opt<Measure>");
        final MeasureName measureName = newMeasureNameBuilder()
                .withNamespace("demo::measures")
                .withName("Mass")
                .build();
        final Optional<? extends Measure> measure = modelAdapter.get(measureName);

        assertThat(measure.isPresent(), is(Boolean.TRUE));
        assertThat(measure.get(), instanceOf(Measure.class));
        assertThat(measure.get().getName(), is("Mass"));
    }

    @Test
    void testIsObjectType() {
        log.info("Testing: isObjectType(NamespaceElement): boolean");

        EntityType entityType = newEntityTypeBuilder().withName("EntityType").build();
        StringType stringType = newStringTypeBuilder().withName("String").withMaxLength(-3).build();

        assertTrue(modelAdapter.isObjectType(entityType));
        assertFalse(modelAdapter.isObjectType(stringType));
    }


    @Test
    void testGetReference() {
        log.info("Testing: getReference(ET, String): Opt<RefTypEl>");

        Optional<EntityType> entityType = getEntityTypeByName("Category");
        Optional<Relation> productsRelation = entityType.get().getRelations().stream().filter(r -> "products".equals(r.getName())).findAny();

        assertTrue(entityType.isPresent());
        assertTrue(productsRelation.isPresent());
        assertThat(modelAdapter.getReference(entityType.get(), "products"), is(productsRelation));

        assertThat(modelAdapter.getReference(entityType.get(), "store"), is(Optional.empty()));
    }

    @Test
    void testGetTarget() {
        log.debug("Testing: getTarget(RTE): EntityType");
        Optional<EntityType> containerEntityType = getEntityTypeByName("Category");
        Optional<Relation> productsRelation = containerEntityType.get().getRelations().stream().filter(r -> "products".equals(r.getName())).findAny();
        Optional<EntityType> targetEntityType = getEntityTypeByName("Product");

        assertTrue(containerEntityType.isPresent());
        assertTrue(targetEntityType.isPresent());
        assertTrue(productsRelation.isPresent());
        assertThat(modelAdapter.getTarget(productsRelation.get()), is(targetEntityType.get()));
    }

    @Test
    void testGetAttribute() {
        log.debug("Testing: getAttribute(ET, attributeName): Opt<PTE>");

        Optional<EntityType> entityType = getEntityTypeByName("Category");
        Optional<Attribute> categoryNameAttribute = entityType.get().getAttributes().stream().filter(a -> "categoryName".equals(a.getName())).findAny();

        assertTrue(entityType.isPresent());
        assertTrue(categoryNameAttribute.isPresent());
        assertThat(modelAdapter.getAttribute(entityType.get(), "categoryName"), is(categoryNameAttribute));
        assertThat(modelAdapter.getAttribute(entityType.get(), "productName"), is(Optional.empty()));
    }

    @Test
    void testGetAttributeType() {
        log.debug("Testing: getAttributeType(EntityType clazz, String attributeName): Optional<? extends Primitive>");

        Optional<EntityType> entityType = getEntityTypeByName("Category");
        Optional<PrimitiveTypedElement> primitiveTypedElement = getPrimitiveTypedElementByName("categoryName");

        assertTrue(entityType.isPresent());
        assertTrue(primitiveTypedElement.isPresent());
        assertThat(
                modelAdapter.getAttributeType(entityType.get(), "categoryName").get(), is(primitiveTypedElement.get().getDataType())
                //modelAdapter.getAttribute(entityType.get(), "categoryName").get().getDataType(), instanceOf(StringType.class) //TODO: ask what was this
        );
    }

    @Test
    void testGetSuperTypes() {
        log.debug("Testing: getSuperTypes(ET): Coll<? ext ET>");
        Optional<EntityType> childEntity = getEntityTypeByName("Company");
        Optional<EntityType> superEntity = getEntityTypeByName("Customer");

        assertTrue(superEntity.isPresent());
        assertTrue(childEntity.isPresent());
        assertTrue(modelAdapter.getSuperTypes(childEntity.get()).contains(superEntity.get()));

        Optional<EntityType> negtestEntity = getEntityTypeByName("Order");
        assertTrue(negtestEntity.isPresent());
        assertFalse(modelAdapter.getSuperTypes(childEntity.get()).contains(negtestEntity.get()));
        assertFalse(modelAdapter.getSuperTypes(negtestEntity.get()).contains(superEntity.get()));
    }

    @Test
    void testContains() {
        log.debug("Testing: boolean contains(EnumType, memberName)");

        Optional<EnumerationType> countries = getPsmElement(EnumerationType.class).filter(e -> "Countries".equals(e.getName())).findAny();
        assertTrue(countries.isPresent());
        assertTrue(modelAdapter.contains(countries.get(), "HU"));
        assertFalse(modelAdapter.contains(countries.get(), "MS"));

        Optional<EnumerationType> titles = getPsmElement(EnumerationType.class).filter(e -> "Titles".equals(e.getName())).findAny();
        assertTrue(titles.isPresent());
        assertFalse(modelAdapter.contains(titles.get(), "HU"));
    }

    @Test
    void testIsDurationSupportingAddition() {
        log.debug("Testing: boolean isDurSuppAdd(Unit)...");

        Optional<Measure> time = getMeasureByName("Time");
        Optional<Unit> day = time.get().getUnits().stream().filter(u -> "day".equals(u.getName())).findAny();
        Optional<Unit> microsecond = time.get().getUnits().stream().filter(u -> "microsecond".equals(u.getName())).findAny();

        assertTrue(time.isPresent());
        assertTrue(day.isPresent());
        assertTrue(modelAdapter.isDurationSupportingAddition(day.get()));
        assertTrue(microsecond.isPresent());
        assertFalse(modelAdapter.isDurationSupportingAddition(microsecond.get()));
    }

    //TODO: [MeasuredDecimal & MeasuredInteger].getMeasure()==null => return Optional.empty()?
    @Test
    void testGetUnit() {
        log.debug("Testing: Opt<Unit> getUnit(NumExpr)...");
        //--------------------------
        TypeName type = modelAdapter.buildTypeName(getEntityTypeByName("Product").get()).get();
        Instance instance = newInstanceBuilder().withElementName(type).build();

        Optional<Unit> kilogram = getMeasureElement(Unit.class).filter(u -> "kilogram".equals(u.getName())).findAny();
        NumericAttribute numericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("weight")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertTrue(modelAdapter.getUnit(numericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(numericAttribute).get(), is(kilogram.get()));

        NumericAttribute nonMeasureNumericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("quantityPerUnit")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertFalse(modelAdapter.getUnit(nonMeasureNumericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(nonMeasureNumericAttribute), is(Optional.empty()));

        NumericAttribute notFoundAttributeNumericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("somethingNonExistent")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertFalse(modelAdapter.getUnit(notFoundAttributeNumericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(notFoundAttributeNumericAttribute), is(Optional.empty()));
        //--------------------------
        Optional<Unit> inch = getMeasureElement(Unit.class).filter(u -> "inch".equals(u.getName())).findAny();
        assertTrue(inch.isPresent());
        //5cm téll hossz, 5m as dist -> length-e

        MeasuredDecimal inchMeasuredDecimal = newMeasuredDecimalBuilder()
                .withUnitName(
                        "inch"
                ).withMeasure(
                        newMeasureNameBuilder().withNamespace("demo::measures").withName("Length").build()
                ).build();
        assertThat(modelAdapter.getUnit(inchMeasuredDecimal).get(), is(inch.get()));
        //--------------------------
        MeasuredDecimal measuredDecimal = newMeasuredDecimalBuilder()
                .withUnitName("TODO")
                .withMeasure(
                        newMeasureNameBuilder().withName("TODO").build()
                ).build();
        assertFalse(modelAdapter.getUnit(measuredDecimal).isPresent());
        assertThat(modelAdapter.getUnit(measuredDecimal), is(Optional.empty()));

        IntegerConstant integerConstant = newIntegerConstantBuilder().build();
        assertThat(modelAdapter.getUnit(integerConstant), is(Optional.empty()));
    }

    private Optional<EntityType> getEntityTypeByName(final String entityTypeName) {
        final Iterable<Notifier> psmContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(psmContents.spliterator(), true)
                .filter(e -> EntityType.class.isAssignableFrom(e.getClass())).map(e -> (EntityType) e)
                .filter(entityType -> Objects.equals(entityType.getName(), entityTypeName))
                .findAny();
    }

    private Optional<Measure> getMeasureByName(final String measureName) {
        final Iterable<Notifier> psmContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(psmContents.spliterator(), true)
                .filter(m -> Measure.class.isAssignableFrom(m.getClass())).map(m -> (Measure) m)
                .filter(measure -> Objects.equals(measure.getName(), measureName))
                .findAny();
    }

    private Optional<PrimitiveTypedElement> getPrimitiveTypedElementByName(final String pteName) {
        return getPsmElement(PrimitiveTypedElement.class)
                .filter(pte -> Objects.equals(pte.getName(), pteName))
                .findAny();
    }

    <T> Stream<T> getPsmElement(final Class<T> clazz) {
        final Iterable<Notifier> psmContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(psmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    <T> Stream<T> getMeasureElement(final Class<T> clazz) {
        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    private String getNamespaceFQName(final Namespace namespace) {
        final Optional<Namespace> containerNamespace;
        if (namespace instanceof Package) {
            containerNamespace = PsmUtils.getNamespaceOfPackage((Package) namespace);
        } else {
            containerNamespace = Optional.empty();
        }

        return (containerNamespace.isPresent() ? getNamespaceFQName(containerNamespace.get()) + "::" : "") + namespace.getName();
    }
}
