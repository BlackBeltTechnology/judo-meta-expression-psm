# osgi-itest Specification

## Purpose

Verifies that the expression PSM adapter bundle installs and operates correctly in a live Apache Karaf 4.4.7 OSGi container, ensuring proper bundle resolution, service availability, and feature provisioning.

## Architecture

- **ExpressionWithPSMAdapterBundleITest** — Main integration test class using Pax Exam to start a Karaf container, install the adapter feature, and verify bundle/service availability
- **KarafFeatureProvider** — Utility class providing Karaf container configuration, feature repository URLs, and OSGi `ServiceTracker`-based service discovery helpers
- **test-features.xml** — Karaf feature descriptor defining the test feature with all required dependencies (OSGi utils, tinybundles, CXF, Eclipse EMF, etc.)

## Requirements

### Requirement: Bundle Installation

The adapter bundle SHALL install successfully in a Karaf 4.4.7 OSGi container with all required dependencies resolved.

#### Scenario: Install adapter feature
- **GIVEN** a Karaf 4.4.7 container with standard features installed
- **WHEN** the `hu.blackbelt.judo.meta.expression.adapter.psm.feature` is installed
- **THEN** the bundle `hu.blackbelt.judo.meta.expression.model.adapter.psm` reaches ACTIVE state

#### Scenario: Dependency resolution
- **GIVEN** the test feature defines prerequisites (EMF, Epsilon, SLF4J, JUDO metamodels)
- **WHEN** the feature is provisioned
- **THEN** all required bundles are resolved and active

### Requirement: Service Availability

The adapter's exported services SHALL be available in the OSGi service registry after bundle activation.

#### Scenario: Verify service registration
- **GIVEN** the adapter bundle is active in Karaf
- **WHEN** `ServiceTracker` queries for adapter services
- **THEN** the expected services are registered and reachable

### Requirement: Karaf Container Configuration

The test SHALL configure a Karaf container with proper feature repositories, logging, and port allocation.

#### Scenario: Container startup
- **GIVEN** `KarafFeatureProvider` configures the Karaf distribution and features
- **WHEN** Pax Exam starts the container
- **THEN** the container starts with correct feature repositories, logback logging, and allocated ports

### Requirement: Test Feature Definition

The test feature XML SHALL declare all prerequisite features needed to run the adapter in an OSGi environment.

#### Scenario: Feature prerequisites
- **GIVEN** `test-features.xml` defines the "test" feature
- **WHEN** the feature is examined
- **THEN** it includes wrap, shell, scr, osgi-utils, Eclipse EMF, and all JUDO metamodel features
