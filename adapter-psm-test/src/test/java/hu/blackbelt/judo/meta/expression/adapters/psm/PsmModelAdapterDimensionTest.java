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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hu.blackbelt.judo.meta.expression.NumericExpression;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.psm.data.EntityType;
import hu.blackbelt.judo.meta.psm.data.PrimitiveTypedElement;
import hu.blackbelt.judo.meta.psm.data.ReferenceTypedElement;
import hu.blackbelt.judo.meta.psm.data.Sequence;
import hu.blackbelt.judo.meta.psm.measure.DerivedMeasure;
import hu.blackbelt.judo.meta.psm.measure.Measure;
import hu.blackbelt.judo.meta.psm.measure.Unit;
import hu.blackbelt.judo.meta.psm.namespace.NamespaceElement;
import hu.blackbelt.judo.meta.psm.service.TransferAttribute;
import hu.blackbelt.judo.meta.psm.service.TransferObjectRelation;
import hu.blackbelt.judo.meta.psm.service.TransferObjectType;
import hu.blackbelt.judo.meta.psm.support.PsmModelResourceSupport;
import hu.blackbelt.judo.meta.psm.type.EnumerationType;
import hu.blackbelt.judo.meta.psm.type.Primitive;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.psm.measure.util.builder.MeasureBuilders.*;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;

public class PsmModelAdapterDimensionTest {

    private MeasureAdapter<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, Measure, Unit> measureAdapter;
    private MeasureProvider<Measure, Unit> measureProvider;
    private Resource resource;

    @BeforeEach
    public void setUp() {

        final ResourceSet resourceSet  = PsmModelResourceSupport.createPsmResourceSet();
        resource = resourceSet.createResource(URI.createURI("urn:psm.judo-meta-psm"));
        measureProvider = new PsmMeasureProvider(resourceSet);

        final ModelAdapter<NamespaceElement, Primitive, EnumerationType, EntityType, PrimitiveTypedElement, ReferenceTypedElement, TransferObjectType, TransferAttribute, TransferObjectRelation, Sequence, Measure, Unit> modelAdapter = Mockito.mock(ModelAdapter.class);

        Mockito.doAnswer(invocationOnMock -> {
            final Object[] args = invocationOnMock.getArguments();
            if (args[0] instanceof MeasuredDecimal) {
                final MeasuredDecimal measuredDecimal = (MeasuredDecimal) args[0];
                return measureAdapter.getUnit(measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace()) : Optional.empty(),
                        measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName()) : Optional.empty(),
                        measuredDecimal.getUnitName());
            } else {
                throw new IllegalStateException("Not supported by mock");
            }
        }).when(modelAdapter).getUnit(any(NumericExpression.class));

        measureAdapter = new MeasureAdapter<>(measureProvider, modelAdapter);
    }

    @AfterEach
    public void tearDown() {
        resource = null;
        measureAdapter = null;
    }

    @Test
    public void testBaseMeasuresChanged() {
        final Measure length = newMeasureBuilder()
                .withName("Length")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("metre")
                                .withRateDividend(1.0)
                                .withRateDivisor(1.0)
                                .withSymbol("m")
                                .build()
                ))
                .build();

        //(nonDerived)Measure added
        resource.getContents().add(length);
        try {
            assertThat(
                    measureAdapter.getDimension(
                    newMeasuredDecimalBuilder()
                            .withValue(BigDecimal.ONE)
                            .withUnitName("m")
                            .build()).get()
                    , is(Collections.singletonMap(measureIdFrom(length), 1))
                    );

        } catch (RuntimeException up) { //TODO: rename, 4th graders joke, not funny anymore
            up.printStackTrace();
            throw up;
        }
    }

    public void testDerivedMeasuresChanged() {
        final Unit metre = newUnitBuilder()
                .withName("metre")
                .withRateDividend(1.0)
                .withRateDivisor(1.0)
                .withSymbol("m")
                .build();

        final Measure length = newMeasureBuilder()
                .withName("Length")
                .withUnits(ImmutableList.of(metre)).build();

        final DerivedMeasure area = newDerivedMeasureBuilder()
                .withName("Area")
                .withTerms(
                        newMeasureDefinitionTermBuilder()
                                .withUnit(metre)
                                .withExponent(2)
                                .build()
                )
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("square metre")
                                .withRateDivisor(1.0)
                                .withRateDividend(1.0)
                                .withSymbol("m²")
                                .build()
                )).build();

        final Unit second = newUnitBuilder()
                .withName("second")
                .withRateDividend(1.0)
                .withRateDivisor(1.0)
                .withSymbol("s")
                .build();

        final Measure time = newMeasureBuilder()
                .withName("Time")
                .withUnits(ImmutableList.of(second)).build();

        final Measure velocity = newDerivedMeasureBuilder()
                .withName("Velocity")
                .withTerms(ImmutableList.of(
                        newMeasureDefinitionTermBuilder()
                                .withUnit(metre)
                                .withExponent(1)
                                .build(),
                        newMeasureDefinitionTermBuilder()
                                .withUnit(second)
                                .withExponent(-1)
                                .build()))
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("second")
                                .withRateDividend(1.0)
                                .withRateDivisor(1.0)
                                .withSymbol("m/s")
                                .build()
                )).build();

        resource.getContents().addAll(Arrays.asList(length, area, time));
        //(nonDerived)Measures added as collections


        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 1)));
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m²").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 2)));
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("s").build()).get(),
                is(Collections.singletonMap(measureIdFrom(time), 1)));

        resource.getContents().add(velocity);
        //DerivedMeasure added
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m/s").build()).get(),
                is(ImmutableMap.of(measureIdFrom(length), 1, measureIdFrom(time), 2)));

        final DerivedMeasure volume = newDerivedMeasureBuilder()
                .withName("Volume")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("cubic metre")
                                .withRateDividend(1.)
                                .withRateDivisor(1000.)
                                .withSymbol("dm³")
                                .build()
                )).build();
        resource.getContents().add(volume);

        //dimension of volume not defined yet
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).get(),
                is(Collections.emptyMap()));

        volume.getTerms().add(newMeasureDefinitionTermBuilder()
                .withUnit(metre)
                .withExponent(3)
                .build());

        //dimension of volume defined
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 3)));

        resource.getContents().removeAll(Arrays.asList(length, area, volume, time));
        resource.getContents().remove(velocity);

        // test cleanup
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m²").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("s").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m/s").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).isPresent());
    }

    private MeasureAdapter.MeasureId measureIdFrom(final Measure measure) {
        return MeasureAdapter.MeasureId.fromMeasure(measureProvider, measure);
    }
}
