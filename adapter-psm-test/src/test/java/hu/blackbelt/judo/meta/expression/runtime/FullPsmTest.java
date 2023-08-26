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

import org.slf4j.Logger;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static hu.blackbelt.judo.meta.expression.adapters.psm.ExpressionEpsilonValidatorOnPsm.validateExpressionOnPsm;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FullPsmTest extends ExecutionContextOnPsmTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        expressionModel = ExpressionModelForTest.createExpressionModel();
        log.info(expressionModel.getDiagnosticsAsString());
        assertTrue(expressionModel.isValid());
    }

    @Test
    void test() throws Exception {
        try (BufferedSlf4jLogger bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnPsm(bufferedLog, psmModel, expressionModel, calculateExpressionValidationScriptURI());
        }
    }
}
