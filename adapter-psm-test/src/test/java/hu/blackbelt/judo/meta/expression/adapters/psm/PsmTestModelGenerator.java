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

import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import hu.blackbelt.judo.meta.psm.data.AssociationEnd;
import hu.blackbelt.judo.meta.psm.data.EntityType;
import hu.blackbelt.judo.meta.psm.measure.DurationType;
import hu.blackbelt.judo.meta.psm.measure.Measure;
import hu.blackbelt.judo.meta.psm.measure.Unit;
import hu.blackbelt.judo.meta.psm.namespace.Model;
import hu.blackbelt.judo.meta.psm.namespace.Package;
import hu.blackbelt.judo.meta.psm.runtime.PsmModel;
import hu.blackbelt.judo.meta.psm.type.Primitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newDecimalArithmeticExpressionBuilder;
import static hu.blackbelt.judo.meta.psm.data.util.builder.DataBuilders.*;
import static hu.blackbelt.judo.meta.psm.measure.util.builder.MeasureBuilders.*;
import static hu.blackbelt.judo.meta.psm.namespace.util.builder.NamespaceBuilders.*;
import static hu.blackbelt.judo.meta.psm.runtime.PsmModel.buildPsmModel;
import static hu.blackbelt.judo.meta.psm.type.util.builder.TypeBuilders.*;

/**
 * Utility class for generating test models with rackinspect-like characteristics.
 *
 * <p>Generated model characteristics (matching rackinspect complexity):
 * <ul>
 *   <li>70 entity types</li>
 *   <li>14 attributes per entity average</li>
 *   <li>6 relations per entity average</li>
 *   <li>5 derived members per entity average</li>
 *   <li>Total: 10,000+ elements</li>
 * </ul>
 */
public class PsmTestModelGenerator {

    private static final int ENTITY_COUNT = 70;
    private static final int ATTRIBUTES_PER_ENTITY = 14;
    private static final int RELATIONS_PER_ENTITY = 6;
    private static final int EXPRESSIONS_PER_ENTITY = 5;

    private final Random random = new Random(42); // Fixed seed for reproducibility

    /**
     * Generate a PSM model with rackinspect-like characteristics.
     *
     * @return a populated PsmModel
     */
    public PsmModel generatePsmModel() {
        PsmModel psmModel = buildPsmModel().build();

        Model model = newModelBuilder().withName("generated").build();
        psmModel.addContent(model);

        // Create packages
        Package types = newPackageBuilder().withName("types").build();
        Package measures = newPackageBuilder().withName("measures").build();
        Package entities = newPackageBuilder().withName("entities").build();
        useModel(model).withPackages(types, measures, entities).build();

        // Create primitive types
        List<Primitive> primitiveTypes = createPrimitiveTypes(types, psmModel);

        // Create measures
        createMeasures(measures);

        // Create entity types
        List<EntityType> entityTypes = createEntityTypes(entities, primitiveTypes);

        // Create relations between entities
        createRelations(entityTypes);

        return psmModel;
    }

    /**
     * Generate an Expression model with many expressions for performance testing.
     *
     * @return a populated ExpressionModel
     */
    public ExpressionModel generateExpressionModel() {
        ExpressionModel expressionModel = ExpressionModel.buildExpressionModel()
                .name("perf-test-expression")
                .build();

        ExpressionModelResourceSupport expressionSupport = expressionModel.getExpressionModelResourceSupport();

        // Create expressions based on entity count
        int expressionCount = ENTITY_COUNT * EXPRESSIONS_PER_ENTITY;
        for (int i = 0; i < expressionCount; i++) {
            // Mix of expression types
            if (i % 3 == 0) {
                // Simple decimal addition
                expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                        .withLeft(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(i)).build())
                        .withOperator(DecimalOperator.ADD)
                        .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                        .build());
            } else if (i % 3 == 1) {
                // Decimal multiplication
                expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                        .withLeft(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(i)).build())
                        .withOperator(DecimalOperator.MULTIPLY)
                        .withRight(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(2)).build())
                        .build());
            } else {
                // Measured decimal with unit
                expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                        .withLeft(newMeasuredDecimalBuilder()
                                .withValue(BigDecimal.valueOf(i))
                                .withUnitName("kg")
                                .build())
                        .withOperator(DecimalOperator.MULTIPLY)
                        .withRight(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(2)).build())
                        .build());
            }
        }

        return expressionModel;
    }

    /**
     * Get the total expected element count for the generated model.
     */
    public int getExpectedElementCount() {
        // Entities + attributes + relations + expressions
        return ENTITY_COUNT
                + (ENTITY_COUNT * ATTRIBUTES_PER_ENTITY)
                + (ENTITY_COUNT * RELATIONS_PER_ENTITY)
                + (ENTITY_COUNT * EXPRESSIONS_PER_ENTITY);
    }

    private List<Primitive> createPrimitiveTypes(Package types, PsmModel psmModel) {
        List<Primitive> primitives = new ArrayList<>();

        Primitive stringType = newStringTypeBuilder()
                .withName("String")
                .withMaxLength(256)
                .build();
        primitives.add(stringType);

        Primitive integerType = newNumericTypeBuilder()
                .withName("Integer")
                .withPrecision(10)
                .build();
        primitives.add(integerType);

        Primitive doubleType = newNumericTypeBuilder()
                .withName("Double")
                .withPrecision(18)
                .withScale(7)
                .build();
        primitives.add(doubleType);

        Primitive booleanType = newBooleanTypeBuilder()
                .withName("Boolean")
                .build();
        primitives.add(booleanType);

        Primitive timestampType = newTimestampTypeBuilder()
                .withName("Timestamp")
                .build();
        primitives.add(timestampType);

        usePackage(types).withElements(primitives.toArray(new Primitive[0])).build();

        return primitives;
    }

    private void createMeasures(Package measures) {
        Measure mass = newMeasureBuilder().withName("Mass").withUnits(
                newUnitBuilder().withName("kilogram").withSymbol("kg")
                        .withRateDividend(1.0).withRateDivisor(1.0).build(),
                newUnitBuilder().withName("gram").withSymbol("g")
                        .withRateDividend(0.001).withRateDivisor(1.0).build()
        ).build();

        Measure length = newMeasureBuilder().withName("Length").withUnits(
                newUnitBuilder().withName("metre").withSymbol("m")
                        .withRateDividend(1.0).withRateDivisor(1.0).build(),
                newUnitBuilder().withName("centimetre").withSymbol("cm")
                        .withRateDividend(0.01).withRateDivisor(1.0).build()
        ).build();

        Measure time = newMeasureBuilder().withName("Time").withUnits(
                newDurationUnitBuilder().withName("second").withSymbol("s")
                        .withRateDividend(1.0).withRateDivisor(1.0)
                        .withUnitType(DurationType.SECOND).build(),
                newDurationUnitBuilder().withName("hour").withSymbol("h")
                        .withRateDividend(3600.0).withRateDivisor(1.0)
                        .withUnitType(DurationType.HOUR).build()
        ).build();

        usePackage(measures).withElements(mass, length, time).build();
    }

    private List<EntityType> createEntityTypes(Package entities, List<Primitive> primitives) {
        List<EntityType> entityTypes = new ArrayList<>();

        for (int i = 0; i < ENTITY_COUNT; i++) {
            EntityType entity = newEntityTypeBuilder()
                    .withName("Entity" + i)
                    .build();

            // Add attributes
            for (int j = 0; j < ATTRIBUTES_PER_ENTITY; j++) {
                Primitive type = primitives.get(j % primitives.size());
                useEntityType(entity).withAttributes(
                        newAttributeBuilder()
                                .withName("attr" + j)
                                .withDataType(type)
                                .build()
                ).build();
            }

            entityTypes.add(entity);
        }

        usePackage(entities).withElements(entityTypes.toArray(new EntityType[0])).build();
        return entityTypes;
    }

    private void createRelations(List<EntityType> entityTypes) {
        for (int i = 0; i < entityTypes.size(); i++) {
            EntityType source = entityTypes.get(i);

            // Create relations to other entities
            for (int j = 0; j < RELATIONS_PER_ENTITY; j++) {
                int targetIndex = (i + j + 1) % entityTypes.size();
                EntityType target = entityTypes.get(targetIndex);

                AssociationEnd relation = newAssociationEndBuilder()
                        .withName("rel" + j + "To" + target.getName())
                        .withTarget(target)
                        .withCardinality(newCardinalityBuilder()
                                .withLower(0)
                                .withUpper(j % 2 == 0 ? 1 : -1) // Mix of single and collection
                                .build())
                        .build();

                useEntityType(source).withRelations(relation).build();
            }
        }
    }
}
