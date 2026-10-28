# judo-meta-expression-psm — module agent doctrine

## Module purpose

`judo-meta-expression-psm` is the **PSM-side binding of the JUDO expression
engine**: it teaches the metamodel-agnostic expression evaluator how to read a
*Platform Specific Model* — the generation-facing model where the persisted
data layer (`psm.data`: `EntityType`, `Attribute`, `Relation`,
`PrimitiveTypedElement`, `ReferenceTypedElement`, `Sequence`) and the exposed
service layer (`psm.service`: `TransferObjectType`, `MappedTransferObjectType`,
`TransferAttribute`, `TransferObjectRelation`) are **separate, explicitly
related metamodel packages**. It owns no Ecore of its own. Its contract is one
class implementing someone else's interface: `PsmModelAdapter implements
ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType,
PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType,
TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit>`.

That separation is what makes this module *not* a rename of its ESM sibling.
Slot 4 is `EntityType` (a persisted entity, not a general classifier), and
slots 8/9 are `TransferAttribute` / `TransferObjectRelation` — distinct Java
types from the entity-side `PrimitiveTypedElement` / `ReferenceTypedElement`.
So the adapter dispatches on *type*, not on interrogation, and it can offer a
whole family of methods the entity/transfer-unified models cannot express:
`getAllMappedTransferObjectTypes` vs. `getAllUnmappedTransferObjectTypes`,
`getMappedEntityType(TransferObjectType)` to walk a mapped transfer object back
to the entity behind it, `getEntityTypeOfTransferObjectRelationTarget`,
`isCollectionTransferRelation`, plus generic `getFqName(Object)` /
`getName(Object)` helpers over `psm.namespace`. Derived members resolve through
the `psm.derived` accessors (`PrimitiveAccessor`, `ReferenceAccessor`) rather
than through inline getter strings, and `AbstractActorType` from
`psm.accesspoint` participates in type resolution. Naming uses the PSM `::`
namespace separator, generalization goes through `getSuperTypes(EntityType)`,
containment through `getContainerTypesOf(EntityType)`.

`PsmMeasureProvider` (a `MeasureProvider<Measure, Unit>`) resolves `Measure`,
`Unit`, `DurationType` and `DurationUnit` out of `psm.measure` by walking
`psm.namespace` `Package`/`Namespace` containment via `PsmUtils`, feeding a
`MeasureAdapter` for dimensional analysis and unit conversion, notifying
`MeasureChangedHandler` on live model edits, and whitelisting only
`MILLISECOND…WEEK` as duration units eligible for addition.
`ExpressionValidatorOnPsm` is the documented EVL entry point: it constructs a
`PsmModelAdapter` from `psmModel.getResourceSet()` and delegates to
`ExpressionValidator.validateExpression` with the resource registered under
both `"PSM"` and `"MEASURES"`, accepting `expectedErrors` / `expectedWarnings`
so tests assert on named constraints. Its JavaDoc is explicit that **Java/Zeta
validation is not routed through here** — callers wanting Zeta use
`ExpressionZetaValidator` from the test module directly.

Everything ships as one Eclipse plugin (`Export-Package:
hu.blackbelt.judo.meta.expression.adapters.psm`, bundle symbolic name
`hu.blackbelt.judo.meta.expression.model.adapter.psm`, singleton, lazy
activation, `JavaSE-21`), plus an Eclipse feature and a P2 update site.

**Repository:** BlackBeltTechnology/judo-meta-expression-psm ·
**Artifact:** `hu.blackbelt.judo.meta:hu.blackbelt.judo.meta.expression.psm`
(packaging `pom`, version `${revision}` = `1.0.5-SNAPSHOT`) ·
**License:** EPL-2.0 · **Java:** 21 · **Build:** Maven / Eclipse Tycho 4.0.13
(hybrid OSGi + standard modules).

## Reactor map

The root `pom.xml` declares its `<modules>` inside the `modules` profile, which
is active unless `-DskipModules=true` is passed. Build order is the declared
order — `adapter-psm` produces the artifact every other module consumes.

<modules>
  <module>adapter-psm</module>
  <module>adapter-psm-test</module>
  <module>feature-adapter-psm</module>
  <module>osgi-itest</module>
  <module>site</module>
</modules>

| Module | Artifact / packaging | What it contributes |
|---|---|---|
| `adapter-psm` | `…expression.model.adapter.psm`, `eclipse-plugin` | The entire shipped surface, one package `hu.blackbelt.judo.meta.expression.adapters.psm`, three classes: `PsmModelAdapter` (~756 lines — the 12-parameter `ModelAdapter` binding plus the mapped/unmapped transfer-object API), `PsmMeasureProvider` (~230 lines — namespace-walking measure/unit resolution with change notification), `ExpressionValidatorOnPsm` (EVL entry point). Tycho resolves its dependencies from `META-INF/MANIFEST.MF` `Import-Package` / `Require-Bundle`, not from `<dependencies>`. |
| `adapter-psm-test` | `…expression.psm.model.test`, `jar` | JUnit 5, split across three packages. `adapters/psm/` drives the adapter itself — `PsmModelAdapterTest`, `PsmModelAdapterDimensionTest` (dimensional analysis), `PsmMeasureProviderTest`, `PsmMeasureSupportTest`, the `ExpressionPsmValidation*` suite (incl. a performance variant) over `AbstractExpressionPsmValidationTest` / `ValidatorType`, with fixtures from `PsmTestModelGenerator`. `runtime/` exercises whole-model scenarios — `MinimalPsmTest`, `FullPsmTest`, `IllegalPsmTest` (negative), `MeasuredTest`, backed by `ExpressionModelForTest` and `MinimalExpressionFactory`. `ExecutionContextOnPsmTest` covers Epsilon execution-context wiring. |
| `feature-adapter-psm` | `…expression.adapter.psm.feature`, `eclipse-feature` | Installable Eclipse feature wrapping the adapter plugin for P2 distribution. Descriptor only — no sources. |
| `osgi-itest` | `…expression.psm.osgi.test`, `jar` | Pax Exam 4.13.5 boots a real Apache Karaf 4.4.7 container and asserts the bundle resolves and works there (`ExpressionWithPSMAdapterBundleITest`, `KarafFeatureProvider`), provisioned by `src/test/resources/test-features.xml` with container config under `src/test/resources/etc/` (`org.ops4j.pax.logging.cfg`, `org.ops4j.pax.url.mvn.cfg`). Slower than the unit tests and needs network access for feature resolution. |
| `site` | `…expression.psm.site`, `eclipse-repository` | Builds the P2 update site. `site/category.xml` publishes two categories — `psm_adapter` (`…adapter.psm.feature`) and `psm_adapters_source` (`…adapter.psm.feature.source`). |

Unlike the ESM sibling there is **no JQL builder test module here** — JQL-over-PSM
is not exercised in this reactor.

## Build commands

> Use SDKMAN! (`sdk`) to manage Java versions. Requires JDK 21+ and Maven 3.9.9+.
> A Maven wrapper (`./mvnw`) is checked in and is the preferred entry point;
> the equivalent bare-`mvn` forms are kept below verbatim.

```bash
# Full build (all modules)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run unit tests only (adapter-psm-test module)
mvn test -pl adapter-psm-test

# Run a specific test class
mvn test -pl adapter-psm-test -Dtest=PsmModelAdapterTest

# Run a specific test method
mvn test -pl adapter-psm-test -Dtest=PsmModelAdapterTest#testGetTypeName

# Run OSGi integration tests (starts Karaf container)
mvn test -pl osgi-itest

# Use Maven wrapper (if available)
./mvnw clean install
```

Build is successful when the output contains `[INFO] BUILD SUCCESS` with no
`[ERROR]` lines. To skip compiling tests entirely use `-Dmaven.test.skip`; to
let a whole reactor run and report every failure at the end use `-fae`.

### Maven profiles

| Profile | Purpose |
|---|---|
| `modules` | **Default** — builds all 5 modules (active unless `-DskipModules=true`) |
| `sign-artifacts` | GPG-signs artifacts for release using the simplify4u `sign-maven-plugin` |
| `release-dummy` | Test release to `/tmp` (file-based distribution) |
| `release-judong` | Deploy to JUDO.technology Nexus (staging) |
| `release-central` | Deploy to Maven Central via Sonatype OSSRH (auto-close enabled) |
| `generate-github-asciidoc-diagrams` | Generate documentation with Asciidoctor + PlantUML/Graphviz |
| `update-source-code-license` | Inject EPL-2.0 license headers into source files |

## Technology stack

- **Eclipse EMF** (Ecore 2.21+) — metamodel framework for the Expression, PSM and Measure models
- **Eclipse Tycho 4.0.13** — builds the `eclipse-plugin` / `eclipse-feature` / `eclipse-repository` modules
- **Epsilon Runtime** — EVL model validation and transformation engine
- **OSGi Framework** (1.8+) — runtime module system for the adapter bundle
- **Apache Karaf 4.4.7** + **Pax Exam 4.13.5** — OSGi container and container test framework
- **Maven** with CI-friendly versioning (`${revision}` = `1.0.5-SNAPSHOT`), **Surefire 3.5.1**, **JaCoCo 0.8.12**
- **JUnit 5** — unit testing framework
- **SLF4J 2.0.16** + **Logback 1.5.12** — logging
- **Flatten Maven Plugin** — resolves `${revision}` into the deployed POM

### Key JUDO dependencies

- `hu.blackbelt.judo.meta.expression.model` `1.0.5-SNAPSHOT` — the Expression metamodel and the `ModelAdapter` / `MeasureProvider` / `MeasureAdapter` interfaces implemented here
- `hu.blackbelt.judo.meta.psm.model` `1.3.0-SNAPSHOT` — the PSM metamodel being adapted (`data`, `service`, `derived`, `measure`, `namespace`, `type`, `accesspoint` packages plus `PsmUtils`, `PsmModel`)
- `hu.blackbelt.judo.meta.measure.model` `1.0.2-SNAPSHOT` — the standalone measurement/unit metamodel
- `hu.blackbelt.judo.meta.expression.model.adapter.measure` — the measure adapter bridge
- `hu.blackbelt.epsilon:epsilon-runtime-execution` — Epsilon runtime

## Architecture pointers

- `adapter-psm/src/main/java/hu/blackbelt/judo/meta/expression/adapters/psm/` — the three shipped classes; start at `PsmModelAdapter`.
- `adapter-psm/META-INF/MANIFEST.MF` — the real dependency declaration for the Tycho build: exported package, `Require-Bundle` (`org.eclipse.emf.ecore`, …) and `Import-Package`, with `Bundle-RequiredExecutionEnvironment: JavaSE-21` and lazy activation.
- `feature-adapter-psm/feature.xml` — Eclipse feature descriptor: plugin and dependency declarations.
- `site/category.xml` — P2 update site category descriptor.
- `osgi-itest/src/test/resources/test-features.xml` — Karaf feature definition provisioning the OSGi integration tests.
- The root `pom.xml` is the parent POM: `${revision}` versioning, version properties, dependency and plugin management, and every profile listed above.
- `logback-test.xml` at the repo root is the shared test logging configuration for every test module.
- [README.md](README.md) — project overview with architecture diagrams.
- [CONTRIBUTING.md](CONTRIBUTING.md) — coding guidelines, dependency rules, LLM contribution workflow.
- [.github/CIFLOW.md](.github/CIFLOW.md) — branching strategy and CI/CD workflow documentation; `.github/ISSUE_TEMPLATE/` holds the bug / feature / docs templates.

## Development environment

**Required:** Java 21 JDK (manage with SDKMAN!: `sdk use java 21.x.y-tem`) ·
Maven 3.9.9+ · Git · BlackBelt Nexus credentials in Maven `settings.xml` —
private BlackBelt artifacts do not resolve without them.

**Recommended:** an IDE with Tycho/OSGi support (Eclipse IDE, or IntelliJ with
the Tycho plugin) and familiarity with EMF/Ecore metamodeling concepts.

## Git workflow

- **Main branch:** `develop` · **Stable branch:** `master` (latest released version)
- **Versioning:** semantic versioning via the `${revision}` property (currently `1.0.5-SNAPSHOT`)
- **Branch naming:** `feature/JNG-NUMBER_summary`, `bugfix/JNG-NUMBER_summary`, `release/X.Y.Z`
- **Commit format:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`)
- **Rule:** every commit must reference a JIRA ticket (`JNG-xxx`)
- **CI/CD:** GitHub Actions — see [CIFLOW.md](.github/CIFLOW.md)

## Scope guard — invariants an edit must not break

1. **This project is a metamodel *consumer*.** It contains no `.ecore` and no `.genmodel`. The Expression, PSM and Measure metamodels are external dependencies — never try to regenerate them here.
2. **`adapter-psm` uses Tycho `eclipse-plugin` packaging.** Dependencies between Tycho modules go through `MANIFEST.MF` `Require-Bundle` / `Import-Package`, not POM `<dependency>` blocks. Adding a Java import without adding the package to the manifest compiles locally and fails in Tycho.
3. **All production code lives in one package** — `hu.blackbelt.judo.meta.expression.adapters.psm`, exactly three classes: `PsmModelAdapter`, `PsmMeasureProvider`, `ExpressionValidatorOnPsm`. A fourth class is a design decision, not a detail.
4. **Do NOT modify build configuration files** — `pom.xml`, `feature.xml`, `MANIFEST.MF`, `category.xml` — without explicit approval; they drive artifact wiring and OSGi bundle resolution.
5. **The entity/transfer type split is the contract.** `EntityType` + `PrimitiveTypedElement` / `ReferenceTypedElement` on the data side; `TransferObjectType` + `TransferAttribute` / `TransferObjectRelation` on the service side. Collapsing any pair breaks `getMappedEntityType`, `getAllMappedTransferObjectTypes` / `getAllUnmappedTransferObjectTypes` and `getEntityTypeOfTransferObjectRelationTarget`.
6. **`ExpressionValidatorOnPsm` is the EVL path only.** Java/Zeta validation goes through `ExpressionZetaValidator` in the test module; do not fold it in here.
7. **`osgi-itest` boots a real Karaf 4.4.7 container** — slower than unit tests and requires network access for feature resolution. Do not treat an `osgi-itest` failure as equivalent to a unit-test failure.
8. **CI-friendly versioning is tooling-owned.** `${revision}` is flattened at build time; never hand-edit child POM versions.
9. **Code style:** 4-space indentation, opening brace on the same line, JavaDoc on all public APIs.

## Code instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

<!-- dox-doctrine -->
## Documentation Update Protocol (WRITE discipline)

Per-directory `AGENTS.md` files form a tree. Each directory `AGENTS.md` is the
per-file record for the files in that directory. This module-root `AGENTS.md`
holds doctrine + architecture pointers only — never a per-file index.

**Keep the root lean.** This file loads into every agent turn — every byte costs
tokens on every turn. A verbose root file buries the rules the model must follow
(signal dilution) and measurably degrades adherence; a lean file keeps doctrine
salient. Default assumption: your update does NOT belong in the root — route it
by the table below.

**Route every doc update by kind:**

| Kind of update | Goes in |
|---|---|
| New file in a directory, or its per-file detail / change history | Nearest directory `AGENTS.md`. Add a `` | `<basename>` | <purpose> | `` row, path-alphabetical. |
| Data flow, protocol, architecture rationale | `docs/architecture.md` or a `docs/<topic>.md` |
| End-user / developer setup | `README.md` |
| Cross-cutting rule every agent needs every turn (rare) | this module-root `AGENTS.md` |

**Read before editing (chain walk).** Before editing a file, read the nearest
`AGENTS.md` chain root→leaf so you know the file's recorded purpose, contracts,
and change history. Do not edit blind.

**Update after editing (closeout pass).** After changing a file, update its row
in the nearest directory `AGENTS.md`: find the file's row, update its purpose in
place; if absent, add it in path-alphabetical order. New directory → scaffold
its `AGENTS.md`. One row per file. The purpose carries a one-line summary, key
exported symbols, contracts/invariants, and `See change: <id>` history.

**Row style (caveman).** Short declarative fragments. Drop articles. Subject →
verb → object, present tense. One fact per row. Prefer concrete tokens (paths,
symbols, env vars) over prose. Keep identifiers verbatim.

**Size rule — split an over-large directory `AGENTS.md` file-based.** pi
auto-injects a directory `AGENTS.md` on every turn when cwd sits at/below it, so
an over-large directory `AGENTS.md` is not supported. Split it file-based: a row
exceeding the length threshold promotes to a per-file `<File>.AGENTS.md`
sidecar carrying that file's full detail (including every `See change:`). The
sidecar is pull-only — its name is not `AGENTS.md`, so pi never auto-injects it
— yet it stays search-indexed (`agents` doc_type). The directory `AGENTS.md`
keeps a one-line summary plus a `→ see `<File>.AGENTS.md`` pointer. Rows within
the threshold stay verbatim (lossless).

## Finding docs (READ discipline)

`kb_*` tools are faster and cheaper than raw search — they return a one-line
purpose + key exports per file, not raw bytes. **This fires on the ACTION, not
the intent** — before you `grep`/`rg` for a symbol, `cat`/read a file to learn
what it does, or chase an import, the kb call goes first. It fires **even
mid-task when you already know the file**; knowing the file does not exempt you.
When your reflex is the left column, run the right column instead:

| You're about to… | Do this FIRST instead |
|---|---|
| `grep -rn "SymbolName" src/` — find where a fn / type / const lives | `kb_search --doc-type agents "SymbolName"` — tree indexes key exports per file |
| `grep -rn "feature\|topic" src/` — how does X work / where's X handled | `kb_search "feature topic"` |
| `cat` / read a file just to learn its purpose before editing | `kb agents <path>` — one-line purpose + exports + change history |
| chase imports / callers across files | `kb_neighbors <path\|heading>` |
| read one doc section in full | `kb_get <path> <section>` |

**Fall-through (explicit):** if the kb call returns nothing relevant, `rg` /
source read is allowed — then add the missing directory `AGENTS.md` row per the
WRITE discipline. kb does NOT replace grep; it goes first.
