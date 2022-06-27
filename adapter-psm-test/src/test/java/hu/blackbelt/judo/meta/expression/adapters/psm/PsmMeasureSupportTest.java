package hu.blackbelt.judo.meta.expression.adapters.psm;

import static hu.blackbelt.judo.meta.psm.data.util.builder.DataBuilders.newAttributeBuilder;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.emf.common.notify.Notifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import hu.blackbelt.judo.meta.expression.ExecutionContextOnPsmTest;
import hu.blackbelt.judo.meta.psm.data.EntityType;
import hu.blackbelt.judo.meta.psm.type.Primitive;

public class PsmMeasureSupportTest extends ExecutionContextOnPsmTest {

    private PsmModelAdapter modelAdapter;
    private EntityType product;
    private Primitive doubleType;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        modelAdapter = new PsmModelAdapter(psmModel.getResourceSet(), psmModel.getResourceSet());
        product = psmUtils.all(psmModel.getResourceSet(), EntityType.class).filter(e -> e.getName().equals("Product")).findAny().get();
        doubleType = psmUtils.all(psmModel.getResourceSet(), Primitive.class).filter(e -> e.getName().equals("Double")).findAny().get();
    }

    @AfterEach
    public void tearDown() {
        product = null;
        modelAdapter = null;
    }

    @Test
    public void testGetUnitOfNonMeasuredAttribute() {
        assertFalse(modelAdapter.getUnit(product, "discount").isPresent());
    }

    @Test
    public void testGetUnitOfMeasuredAttribute() {
        assertTrue(modelAdapter.getUnit(product, "weight").isPresent());
        assertTrue(modelAdapter.getUnit(product, "height").isPresent());
    }

    @Test
    public void testGetUnitOfAttributeWithUnknownUnit() {
        product.getAttributes().addAll(ImmutableList.of(
           newAttributeBuilder().withName("vat").withDataType(doubleType).build(),
           newAttributeBuilder().withName("netWeight").withDataType(doubleType).build(),
           newAttributeBuilder().withName("grossWeight").withDataType(doubleType).build(),
           newAttributeBuilder().withName("width").withDataType(doubleType).build()
        ));

        //TODO

        assertFalse(modelAdapter.getUnit(product, "vat").isPresent());           // EUR not defined as unit
        assertFalse(modelAdapter.getUnit(product, "netWeight").isPresent());     // unit belongs to another measure
        assertFalse(modelAdapter.getUnit(product, "grossWeight").isPresent());   // measure name not matching expected pattern
        assertFalse(modelAdapter.getUnit(product, "width").isPresent());         // measure name invalid
    }

    @Test
    public void testGetUnitOfNonNumericAttribute() {
        assertFalse(modelAdapter.getUnit(product, "url").isPresent());           //NOT NUMERIC >:[
    }

    @Test
    public void testGetUnitOfNonExistingAttribute() {
        assertFalse(modelAdapter.getUnit(product, "width").isPresent());         // attribute is not defined
        assertFalse(modelAdapter.getUnit(product, "unitPrice").isPresent());     // annotation is added without 'unit' key
    }

    <T> Stream<T> getMeasureElement(final Class<T> clazz) {
        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }
}
