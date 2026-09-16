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

import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidator;
import hu.blackbelt.judo.meta.expression.validation.ExpressionZetaValidator;
import hu.blackbelt.judo.meta.psm.runtime.PsmModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance comparison tests for EVL and Java validation.
 *
 * <p>These tests generate large models with rackinspect-like characteristics
 * and compare validation performance between EVL and Java validators.</p>
 *
 * <p>Run with: {@code ./mvnw test -Dgroups=performance}</p>
 *
 * @see PsmTestModelGenerator
 */
@Tag("performance")
public class ExpressionPsmValidationPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ExpressionPsmValidationPerformanceTest.class);

    private PsmModel psmModel;
    private ExpressionModel expressionModel;
    private PsmTestModelGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PsmTestModelGenerator();
        psmModel = generator.generatePsmModel();
        expressionModel = generator.generateExpressionModel();

        log.info("Generated model with {} expected elements", generator.getExpectedElementCount());
        assertTrue(psmModel.isValid(), "PSM model should be valid");
        assertTrue(expressionModel.isValid(), "Expression model should be valid");
    }

    /**
     * Benchmark EVL validation performance.
     */
    @Test
    void benchmarkEvlValidation() throws Exception {
        log.info("=== EVL Validation Benchmark ===");

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            // Warm-up run
            log.info("Warm-up run...");
            long warmupStart = System.currentTimeMillis();
            ExpressionValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    "PSM", psmModel.getResource(),
                    "MEASURES", psmModel.getResource(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            long warmupTime = System.currentTimeMillis() - warmupStart;
            log.info("Warm-up completed in {} ms", warmupTime);

            // Benchmark runs
            int runs = 3;
            long totalTime = 0;
            for (int i = 1; i <= runs; i++) {
                long start = System.currentTimeMillis();
                ExpressionValidator.validateExpression(
                        bufferedLogger,
                        expressionModel,
                        new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                        "PSM", psmModel.getResource(),
                        "MEASURES", psmModel.getResource(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
                long elapsed = System.currentTimeMillis() - start;
                totalTime += elapsed;
                log.info("Run {}: {} ms", i, elapsed);
            }

            long avgTime = totalTime / runs;
            log.info("EVL average time over {} runs: {} ms", runs, avgTime);
        }
    }

    /**
     * Benchmark Java sequential validation performance.
     */
    @Test
    void benchmarkJavaSequentialValidation() throws Exception {
        log.info("=== Java Sequential Validation Benchmark ===");

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            // Warm-up run
            log.info("Warm-up run...");
            long warmupStart = System.currentTimeMillis();
            ExpressionZetaValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false // Sequential
            );
            long warmupTime = System.currentTimeMillis() - warmupStart;
            log.info("Warm-up completed in {} ms", warmupTime);

            // Benchmark runs
            int runs = 3;
            long totalTime = 0;
            for (int i = 1; i <= runs; i++) {
                long start = System.currentTimeMillis();
                ExpressionZetaValidator.validateExpression(
                        bufferedLogger,
                        expressionModel,
                        new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        false // Sequential
                );
                long elapsed = System.currentTimeMillis() - start;
                totalTime += elapsed;
                log.info("Run {}: {} ms", i, elapsed);
            }

            long avgTime = totalTime / runs;
            log.info("Java sequential average time over {} runs: {} ms", runs, avgTime);
        }
    }

    /**
     * Benchmark Java parallel validation performance.
     */
    @Test
    void benchmarkJavaParallelValidation() throws Exception {
        log.info("=== Java Parallel Validation Benchmark ===");

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            // Warm-up run
            log.info("Warm-up run...");
            long warmupStart = System.currentTimeMillis();
            ExpressionZetaValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    true // Parallel
            );
            long warmupTime = System.currentTimeMillis() - warmupStart;
            log.info("Warm-up completed in {} ms", warmupTime);

            // Benchmark runs
            int runs = 3;
            long totalTime = 0;
            for (int i = 1; i <= runs; i++) {
                long start = System.currentTimeMillis();
                ExpressionZetaValidator.validateExpression(
                        bufferedLogger,
                        expressionModel,
                        new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        true // Parallel
                );
                long elapsed = System.currentTimeMillis() - start;
                totalTime += elapsed;
                log.info("Run {}: {} ms", i, elapsed);
            }

            long avgTime = totalTime / runs;
            log.info("Java parallel average time over {} runs: {} ms", runs, avgTime);
        }
    }

    /**
     * Compare all validation modes in a single test.
     */
    @Test
    void compareAllValidationModes() throws Exception {
        log.info("=== Validation Mode Comparison ===");
        log.info("Model size: {} elements", generator.getExpectedElementCount());

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            // EVL
            long evlStart = System.currentTimeMillis();
            ExpressionValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    "PSM", psmModel.getResource(),
                    "MEASURES", psmModel.getResource(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            long evlTime = System.currentTimeMillis() - evlStart;

            // Java Sequential
            long javaSeqStart = System.currentTimeMillis();
            ExpressionZetaValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false
            );
            long javaSeqTime = System.currentTimeMillis() - javaSeqStart;

            // Java Parallel
            long javaParStart = System.currentTimeMillis();
            ExpressionZetaValidator.validateExpression(
                    bufferedLogger,
                    expressionModel,
                    new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet()),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    true
            );
            long javaParTime = System.currentTimeMillis() - javaParStart;

            log.info("");
            log.info("========================================");
            log.info("Validation Performance Comparison");
            log.info("========================================");
            log.info("EVL:             {} ms", evlTime);
            log.info("Java Sequential: {} ms", javaSeqTime);
            log.info("Java Parallel:   {} ms", javaParTime);
            log.info("========================================");

            if (javaSeqTime > 0) {
                log.info("Java Seq vs EVL speedup: {}x", String.format("%.2f", (double) evlTime / javaSeqTime));
            }
            if (javaParTime > 0) {
                log.info("Java Par vs EVL speedup: {}x", String.format("%.2f", (double) evlTime / javaParTime));
            }
            if (javaParTime > 0 && javaSeqTime > 0) {
                log.info("Parallel vs Sequential: {}x", String.format("%.2f", (double) javaSeqTime / javaParTime));
            }
            log.info("========================================");
        }
    }
}
