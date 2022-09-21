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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.eclipse.emf.common.notify.Notifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.psm.measure.Measure;
import hu.blackbelt.judo.meta.psm.measure.Unit;
import hu.blackbelt.judo.meta.psm.measure.util.builder.MeasureBuilders;

public class PsmMeasureProviderTest extends ExecutionContextOnPsmTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PsmMeasureProviderTest.class);
    private MeasureProvider<Measure, Unit> measureProvider;

    //private MeasureAdapter<Measure, Unit, EClass> measureAdapter;

    @BeforeEach
    public void setUp() throws Exception {
    	super.setUp();
        measureProvider = new PsmMeasureProvider(psmModel.getResourceSet());
        //TODO: uncomment after implementing PsmModelAdapter
        //measureAdapter = new MeasureAdapter<>(measureProvider, Mockito.mock(ModelAdapter.class));
    }
    
    @AfterEach
    public void tearDown() {
        psmModel = null;
        measureProvider = null;
    }

    @Test
    void testGetMeasureNamespace() {
        log.info("Testing: getMeasureNamespace...");
        final Optional<Measure> mass = getMeasureByName("Mass");
        final Measure negtestMeasure = MeasureBuilders.newMeasureBuilder().withName("NegtestMeasure").build();

        assertTrue(mass.isPresent());
        assertThat(measureProvider.getMeasureNamespace(mass.get()), is("demo::measures"));
        assertTrue(measureProvider.getMeasureNamespace(negtestMeasure) == null);
    }

    @Test
    void testGetMeasureName() {
        log.info("Testing: getMeasureName...");
        final Optional<Measure> mass = measureProvider.getMeasures()
                .filter(m -> m.getUnits().stream().anyMatch(u -> "kilogram".equals(u.getName())))
                .findAny();

        assertTrue(mass.isPresent());
        assertThat(measureProvider.getMeasureName(mass.get()), is("Mass"));

    }

    @Test
    void testGetMeasure() {
        log.info("Testing: getMeasure...");
        final Optional<Measure> length = measureProvider.getMeasure("demo::measures", "Length");

        final Optional<Measure> invalidName = measureProvider.getMeasure("demo::measures", "Price");
        final Optional<Measure> invalidNamespace = measureProvider.getMeasure("northwind::measures", "Length");
        final Optional<Measure> invalidNameAndNamespace = measureProvider.getMeasure("northwind::measures", "Price");

        final Optional<Measure> expectedLength = getMeasureByName("Length");

        assertTrue(length.isPresent());
        assertThat(length, is(expectedLength));
        assertFalse(invalidName.isPresent());
        assertFalse(invalidNamespace.isPresent());
        assertFalse(invalidNameAndNamespace.isPresent());
    }

    @Test
    public void testBaseMeasuresOfBaseMeasure() {
        final Measure length = getMeasureByName("Length").get();

        assertThat(measureProvider.getBaseMeasures(length).map(), is(Collections.singletonMap(length, 1)));
    }

    @Test
    public void testBaseMeasuresOfDerivedMeasure() {
        final Measure length = getMeasureByName("Length").get();
        final Measure area = getMeasureByName("Area").get();
        final Measure mass = getMeasureByName("Mass").get();
        final Measure time = getMeasureByName("Time").get();
        final Measure force = getMeasureByName("Force").get();

        assertThat(measureProvider.getBaseMeasures(area).map(), is(Collections.singletonMap(length, 2)));
        assertThat(measureProvider.getBaseMeasures(force).map(), is(ImmutableMap.of(
                mass, 1,
                length, 1,
                time, -2
        )));
    }

    @Test
    public void testGetUnits() {
        log.info("Testing: getUnits...");
        final Measure length = getMeasureByName("Length").get();

        assertThat(new HashSet<>(measureProvider.getUnits(length)), is(new HashSet<>(length.getUnits())));
    }

    @Test
    public void testIsDurationSupportingAddition() {
        log.info("Testing: isDurationSupportingAddition...");
        final Unit second = getUnitByName("millisecond").get();
        final Unit metre = getUnitByName("metre").get();
        final Unit microsecond = getUnitByName("microsecond").get();
        final Unit month = getUnitByName("month").get();

        assertTrue(measureProvider.isDurationSupportingAddition(second));
        assertFalse(measureProvider.isDurationSupportingAddition(metre));
        assertFalse(measureProvider.isDurationSupportingAddition(microsecond));
        assertFalse(measureProvider.isDurationSupportingAddition(month));
    }

    @Test
    public void testGetUnitByNameOrSymbol() {
        log.info("Testing: getUnitByNameOrSymbol...");
        final Optional<Measure> length = getMeasureByName("Length");
        final Optional<Unit> metre = getUnitByName("metre");
        final Optional<Measure> time = getMeasureByName("Time");
        final Optional<Unit> halfDay = getUnitByName("halfDay");

        assertThat(measureProvider.getUnitByNameOrSymbol(length, "metre"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "metre"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(length, "m"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "m"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(time, "halfDay"), is(halfDay));
        assertFalse(measureProvider.getUnitByNameOrSymbol(time, null).isPresent()); // units are not compared by symbol if is it not defined
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "halfDay"), is(halfDay));
        assertFalse(measureProvider.getUnitByNameOrSymbol(Optional.empty(), null).isPresent()); // nothing is defined
    }

    @Test
    public void testGetMeasures() {
        log.info("Testing: getMeasures...");
        assertThat(measureProvider.getMeasures().count(), is(7L));

    }

    private Optional<Measure> getMeasureByName(final String measureName) {
        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> Measure.class.isAssignableFrom(e.getClass())).map(e -> (Measure) e)
                .filter(m -> measureName.equals(m.getName()))
                .findAny();
    }

    private Optional<Unit> getUnitByName(final String unitName) {
        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> Measure.class.isAssignableFrom(e.getClass())).map(e -> (Measure) e)
                .map(m -> m.getUnits().stream().filter(u -> unitName.equals(u.getName())).findAny())
                .filter(u -> u.isPresent()).map(u -> u.get())
                .findAny();
    }
}
