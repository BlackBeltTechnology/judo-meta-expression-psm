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

import com.google.common.collect.ImmutableList;
import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newDecimalArithmeticExpressionBuilder;

/**
 * Dual validation tests for Expression models on PSM.
 *
 * <p>These tests run with both EVL and Java validators to ensure validation parity
 * between the two implementations.</p>
 *
 * @see AbstractExpressionPsmValidationTest
 * @see ValidatorType
 */
public class ExpressionPsmValidationTest extends AbstractExpressionPsmValidationTest {

    /**
     * Test valid decimal arithmetic expressions.
     */
    @ParameterizedTest(name = "testValidDecimalArithmetic [{0}]")
    @EnumSource(ValidatorType.class)
    void testValidDecimalArithmetic(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModels();

        ExpressionModelResourceSupport expressionSupport = expressionModel.getExpressionModelResourceSupport();

        // Simple decimal addition: 1 + 10
        expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newDecimalConstantBuilder().withValue(BigDecimal.ONE).build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());

        // Measured decimal multiplication: 1kg * 10
        expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("kg").build())
                .withOperator(DecimalOperator.MULTIPLY)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());

        // No errors or warnings expected
        runValidation(
                ImmutableList.of(),
                ImmutableList.of()
        );
    }

    /**
     * Test measured addition with compatible units.
     */
    @ParameterizedTest(name = "testMeasuredAdditionCompatible [{0}]")
    @EnumSource(ValidatorType.class)
    void testMeasuredAdditionCompatible(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModels();

        ExpressionModelResourceSupport expressionSupport = expressionModel.getExpressionModelResourceSupport();

        // Addition of compatible measured values: 1cm + 10m (both are Length)
        expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("cm").build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.TEN).withUnitName("m").build())
                .build());

        // No errors expected - compatible units within same measure
        runValidation(
                ImmutableList.of(),
                ImmutableList.of()
        );
    }

    /**
     * Test measured addition with incompatible units (should produce validation error).
     *
     * <p>Both EVL and Zeta validators use the same constraint name: {@code MeasureOfAdditionIsValid}</p>
     */
    @ParameterizedTest(name = "testMeasuredAdditionIncompatible [{0}]")
    @EnumSource(ValidatorType.class)
    void testMeasuredAdditionIncompatible(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModels();

        ExpressionModelResourceSupport expressionSupport = expressionModel.getExpressionModelResourceSupport();

        // Addition of incompatible measured values: 1kg + 10 (measured + unmeasured)
        expressionSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("kg").build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());

        // Expect validation error for incompatible operands
        runValidation(
                ImmutableList.of("MeasureOfAdditionIsValid"),
                ImmutableList.of()
        );
    }
}
