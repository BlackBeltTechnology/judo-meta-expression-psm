package hu.blackbelt.judo.meta.expression.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static hu.blackbelt.judo.meta.expression.adapters.psm.ExpressionEpsilonValidatorOnPsm.validateExpressionOnPsm;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class MinimalPsmTest extends ExecutionContextOnPsmTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        final ExpressionModelResourceSupport expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .uri(URI.createURI("expr:test"))
                .build();

        MinimalExpressionFactory.createMinimalExpression().forEach(e -> expressionModelResourceSupport.addContent(e));

        expressionModel = ExpressionModel.buildExpressionModel()
                .name("expr")
                .expressionModelResourceSupport(expressionModelResourceSupport)
                .build();
        log.info(expressionModel.getDiagnosticsAsString());
    	assertTrue(expressionModel.isValid());
    }

    @Test
    void test() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnPsm(bufferedLog, psmModel, expressionModel, calculateExpressionValidationScriptURI());
        }
    }
}
