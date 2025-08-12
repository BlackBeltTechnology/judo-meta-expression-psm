package hu.blackbelt.judo.meta.expression.adapters.psm;

/*-
 * #%L
 * JUDO :: Expression :: PSM Adapter
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

import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidationException;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidator;
import org.slf4j.Logger;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.psm.runtime.PsmModel;

import java.util.Collection;

import static java.util.Collections.emptyList;

public class ExpressionValidatorOnPsm {

    public static void validateExpressionOnPsm(Logger log, PsmModel psmModel, ExpressionModel expressionModel)
            throws ExpressionValidationException {
        validateExpressionOnPsm(log, psmModel, expressionModel, emptyList(), emptyList());
    }

    public static void validateExpressionOnPsm(Logger log, PsmModel psmModel, ExpressionModel expressionModel,
                                               Collection<String> expectedErrors, Collection<String> expectedWarnings)
            throws ExpressionValidationException {
        ExpressionValidator.validateExpression(log, expressionModel,
                new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                "PSM", psmModel.getResource(), "MEASURES", psmModel.getResource(),
                expectedErrors, expectedWarnings);
    }
}
