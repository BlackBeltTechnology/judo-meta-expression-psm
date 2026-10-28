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

import hu.blackbelt.epsilon.runtime.execution.ExecutionContext;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionEvaluator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionScriptUriProvider;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidationException;
import hu.blackbelt.judo.meta.expression.validation.ExpressionZetaValidator;
import org.eclipse.epsilon.common.util.UriUtil;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static hu.blackbelt.epsilon.runtime.execution.ExecutionContext.executionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.contexts.EvlExecutionContext.evlExecutionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.model.emf.WrappedEmfModelContext.wrappedEmfModelContextBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Abstract base class for Expression validation tests on PSM.
 *
 * <p>Provides common infrastructure for testing both EVL and Java validators
 * with the same test cases using JUnit 5 parameterized tests.</p>
 *
 * <p>The key challenge is that EVL and Java validators use different expected error formats:
 * <ul>
 *   <li>EVL uses: {@code "ConstraintName|Full error message with context"}</li>
 *   <li>Java uses: {@code "ConstraintName"} (just the constraint name)</li>
 * </ul>
 *
 * <p>This base class handles this difference by running EVL validation with empty
 * expected collections to force it to throw an exception with all results, then
 * extracting constraint names for comparison.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class ExpressionPsmValidationTest extends AbstractExpressionPsmValidationTest {
 *
 *     @ParameterizedTest(name = "testValidation [{0}]")
 *     @EnumSource(ValidatorType.class)
 *     void testValidation(ValidatorType type) throws Exception {
 *         this.validatorType = type;
 *         initModels();
 *
 *         // ... build expression model ...
 *
 *         runValidation(
 *             ImmutableList.of(),  // expected errors
 *             ImmutableList.of()   // expected warnings
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see ValidatorType
 * @see ExpressionZetaValidator
 * @see ExpressionEpsilonValidator
 */
public abstract class AbstractExpressionPsmValidationTest extends ExecutionContextOnPsmTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ValidatorType validatorType;

    /**
     * Initialize the models for testing.
     * Call this at the start of each parameterized test method.
     *
     * @throws Exception if model initialization fails
     */
    protected void initModels() throws Exception {
        super.setUp();
        expressionModel = ExpressionModel.buildExpressionModel()
                .name("test-expression")
                .build();
    }

    /**
     * Run validation using the selected validator type.
     *
     * <p>For EVL validation, this method handles the format difference by:
     * <ol>
     *   <li>Running EVL with empty expected collections to force exception</li>
     *   <li>Extracting constraint names from the exception</li>
     *   <li>Comparing against expected constraint names</li>
     * </ol>
     *
     * @param expectedErrors expected error constraint names (null means no errors expected)
     * @param expectedWarnings expected warning constraint names (null means warnings are ignored)
     * @throws Exception if validation fails unexpectedly
     */
    protected void runValidation(
            Collection<String> expectedErrors,
            Collection<String> expectedWarnings
    ) throws Exception {
        log.debug("PSM diagnostics: {}", psmModel.getDiagnosticsAsString());
        Assertions.assertTrue(psmModel.isValid(), "PSM model should be structurally valid");

        log.debug("Expression diagnostics: {}", expressionModel.getDiagnosticsAsString());
        Assertions.assertTrue(expressionModel.isValid(), "Expression model should be structurally valid");

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            switch (validatorType) {
                case EVL:
                    log.info("Running EVL validation...");
                    runEvlValidation(bufferedLogger, expectedErrors, expectedWarnings);
                    break;
                case JAVA:
                    log.info("Running Java validation...");
                    ExpressionZetaValidator.validateExpression(
                            bufferedLogger,
                            expressionModel,
                            new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                            expectedErrors,
                            expectedWarnings,
                            false // Sequential for deterministic results
                    );
                    break;
                default:
                    throw new IllegalStateException("Unknown validator type: " + validatorType);
            }
        } catch (ExpressionValidationException ex) {
            log.error("{} validation failed", validatorType, ex);
            throw ex;
        }
    }

    /**
     * Run EVL validation with constraint name matching (not full key matching).
     *
     * <p>This method runs EVL validation with empty expected collections to force
     * it to throw an exception containing the actual results. We then extract
     * constraint names from the unsatisfied constraints for comparison.</p>
     */
    private void runEvlValidation(
            BufferedSlf4jLogger bufferedLogger,
            Collection<String> expectedErrors,
            Collection<String> expectedWarnings
    ) throws Exception {
        // Prepare injections - both evaluator and modelAdapter are required by expression.evl
        final Map<String, Object> injections = new HashMap<>();
        injections.put("evaluator", new ExpressionEvaluator());
        injections.put("modelAdapter", new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()));

        // Build and execute EVL with empty expected collections to force exception
        ExecutionContext executionContext = executionContextBuilder()
                .log(bufferedLogger)
                .resourceSet(expressionModel.getResourceSet())
                .metaModels(emptyList())
                .modelContexts(asList(
                        wrappedEmfModelContextBuilder()
                                .log(bufferedLogger)
                                .name("EXPR")
                                .resource(expressionModel.getResource())
                                .validateModel(false)
                                .useCache(false)
                                .build(),
                        wrappedEmfModelContextBuilder()
                                .log(bufferedLogger)
                                .name("PSM")
                                .resource(psmModel.getResource())
                                .validateModel(false)
                                .useCache(false)
                                .build()
                ))
                .injectContexts(injections)
                .build();

        Set<String> actualErrorNames = new HashSet<>();
        Set<String> actualWarningNames = new HashSet<>();

        try {
            // Load the model
            executionContext.load();

            // Execute EVL with empty expected collections
            // This forces EVL to throw exception if there are any errors/warnings
            // so we can capture the actual results
            executionContext.executeProgram(
                    evlExecutionContextBuilder()
                            .source(UriUtil.resolve("expression.evl",
                                    ExpressionScriptUriProvider.calculateExpressionValidationScriptURI()))
                            .expectedErrors(Collections.emptyList()) // Empty = any error is unexpected
                            .expectedWarnings(Collections.emptyList()) // Empty = any warning is unexpected
                            .parallel(false) // Sequential for deterministic results
                            .build()
            );

            // If we get here, no errors or warnings were found
            log.debug("EVL validation completed with no errors or warnings");
        } catch (EvlScriptExecutionException ex) {
            // Extract constraint names from the exception
            if (ex.getUnsatisfiedErrors() != null) {
                for (UnsatisfiedConstraint uc : ex.getUnsatisfiedErrors()) {
                    actualErrorNames.add(uc.getConstraint().getName());
                    log.debug("EVL error: {} - {}", uc.getConstraint().getName(), uc.getMessage());
                }
            }
            if (ex.getUnsatisfiedWarnings() != null) {
                for (UnsatisfiedConstraint uc : ex.getUnsatisfiedWarnings()) {
                    actualWarningNames.add(uc.getConstraint().getName());
                    log.debug("EVL warning: {} - {}", uc.getConstraint().getName(), uc.getMessage());
                }
            }
        } finally {
            executionContext.commit();
            try {
                executionContext.close();
            } catch (Exception e) {
                log.warn("Unable to close EVL execution context", e);
            }
        }

        // Now compare with expected constraint names
        Set<String> expectedErrorSet = expectedErrors != null
                ? new HashSet<>(expectedErrors)
                : Collections.emptySet();

        Set<String> expectedWarningSet = expectedWarnings != null
                ? new HashSet<>(expectedWarnings)
                : null; // null means "don't care about warnings"

        // Check errors
        Set<String> unexpectedErrors = new HashSet<>(actualErrorNames);
        unexpectedErrors.removeAll(expectedErrorSet);

        Set<String> missingErrors = new HashSet<>(expectedErrorSet);
        missingErrors.removeAll(actualErrorNames);

        // Check warnings only if expectedWarnings was explicitly provided
        Set<String> unexpectedWarnings = Collections.emptySet();
        Set<String> missingWarnings = Collections.emptySet();
        if (expectedWarningSet != null) {
            unexpectedWarnings = new HashSet<>(actualWarningNames);
            unexpectedWarnings.removeAll(expectedWarningSet);
            missingWarnings = new HashSet<>(expectedWarningSet);
            missingWarnings.removeAll(actualWarningNames);
        }

        boolean hasIssues = !unexpectedErrors.isEmpty()
                || !missingErrors.isEmpty()
                || !unexpectedWarnings.isEmpty()
                || !missingWarnings.isEmpty();

        if (hasIssues) {
            log.error("EVL validation result mismatch:");
            log.error("  Actual errors: {}", actualErrorNames);
            log.error("  Expected errors: {}", expectedErrorSet);
            log.error("  Actual warnings: {}", actualWarningNames);
            log.error("  Expected warnings: {}", expectedWarningSet);
            if (!unexpectedErrors.isEmpty()) {
                log.error("  Unexpected errors: {}", unexpectedErrors);
            }
            if (!missingErrors.isEmpty()) {
                log.error("  Missing errors: {}", missingErrors);
            }
            if (!unexpectedWarnings.isEmpty()) {
                log.error("  Unexpected warnings: {}", unexpectedWarnings);
            }
            if (!missingWarnings.isEmpty()) {
                log.error("  Missing warnings: {}", missingWarnings);
            }
            throw new ExpressionValidationException("EVL validation result mismatch");
        }

        // Log success
        log.info("EVL validation passed: {} errors, {} warnings",
                actualErrorNames.size(), actualWarningNames.size());
    }
}
