package hu.blackbelt.judo.meta.expression.runtime;

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

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.ScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.*;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static hu.blackbelt.judo.meta.expression.adapters.psm.ExpressionEpsilonValidatorOnPsm.validateExpressionOnPsm;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newInstanceBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectNavigationExpressionBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static hu.blackbelt.judo.meta.expression.string.util.builder.StringBuilders.newStringAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class IllegalPsmTest extends ExecutionContextOnPsmTest {

    ExpressionModel expressionModel;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        expressionModel = ExpressionModelForTest.createExpressionModel();

        final TypeName orderType = newTypeNameBuilder().withNamespace("demo::entities").withName("InternationalOrder").build();
        final Instance orderVar = newInstanceBuilder()
                .withElementName(orderType)
                .withName("self")
                .build();

        final StringExpression shipperName = newStringAttributeBuilder()
                .withObjectExpression(newObjectNavigationExpressionBuilder()
                        .withObjectExpression(newObjectVariableReferenceBuilder()
                                .withVariable(orderVar)
                                .build())
                        .withReferenceName("shippers")
                        .build())
                .withAttributeName("companyName")
                .build();

        expressionModel.addContent(orderType);
        expressionModel.addContent(orderVar);
        expressionModel.addContent(shipperName);

        log.info(expressionModel.getDiagnosticsAsString());
        assertTrue(expressionModel.isValid());
    }

    @Test
    void test() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            assertThrows(
                    ScriptExecutionException.class,
                    () -> validateExpressionOnPsm(bufferedLog, psmModel, expressionModel, calculateExpressionValidationScriptURI())
            );
        }
    }
}
