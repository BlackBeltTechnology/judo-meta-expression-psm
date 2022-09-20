package hu.blackbelt.judo.meta.expression;

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

import com.google.common.collect.ImmutableList;
import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.psm.PsmEpsilonValidator;
import hu.blackbelt.judo.meta.psm.PsmUtils;
import hu.blackbelt.judo.meta.psm.data.*;
import hu.blackbelt.judo.meta.psm.measure.*;
import hu.blackbelt.judo.meta.psm.namespace.Model;
import hu.blackbelt.judo.meta.psm.namespace.Package;
import hu.blackbelt.judo.meta.psm.runtime.PsmModel;
import hu.blackbelt.judo.meta.psm.type.EnumerationType;
import hu.blackbelt.judo.meta.psm.type.Primitive;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.notify.Notifier;

import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static hu.blackbelt.judo.meta.psm.data.util.builder.DataBuilders.*;
import static hu.blackbelt.judo.meta.psm.measure.util.builder.MeasureBuilders.*;
import static hu.blackbelt.judo.meta.psm.namespace.util.builder.NamespaceBuilders.*;
import static hu.blackbelt.judo.meta.psm.runtime.PsmModel.buildPsmModel;
import static hu.blackbelt.judo.meta.psm.type.util.builder.TypeBuilders.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class ExecutionContextOnPsmTest {

    public PsmModel psmModel;
    public ExpressionModel expressionModel;
    public PsmUtils psmUtils;

    public void setUp() throws Exception {

        psmModel = buildPsmModel().name("test").build();
        populatePsmModel();
        
        log.info(psmModel.getDiagnosticsAsString());
    	assertTrue(psmModel.isValid());
    	runEpsilon();
    	
    	psmUtils = new PsmUtils();
    }
    
    private void populateMeasureModel(Package measures) {  	  
    	
        Measure time = newMeasureBuilder().withName("Time").withUnits(
        		newDurationUnitBuilder().withName("nanosecond").withSymbol("ns").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0E+9)).withUnitType(DurationType.NANOSECOND).build(),
        		newDurationUnitBuilder().withName("microsecond").withSymbol("μs").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1000000.0)).withUnitType(DurationType.MICROSECOND).build(),
    			newDurationUnitBuilder().withName("millisecond").withSymbol("ms").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.MILLISECOND).build(),
    			newDurationUnitBuilder().withName("second").withSymbol("s").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.SECOND).build(),
    			newDurationUnitBuilder().withName("minute").withSymbol("min").withRateDividend(new Double(60.0)).withRateDivisor(new Double(1.0)).build(),
    			newDurationUnitBuilder().withName("hour").withSymbol("h").withRateDividend(new Double(3600.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.HOUR).build(),
    			newDurationUnitBuilder().withName("day").withSymbol("").withRateDividend(new Double(86400.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.DAY).build(),
    			newDurationUnitBuilder().withName("week").withSymbol("").withRateDividend(new Double(604800.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.WEEK).build(),
    			newDurationUnitBuilder().withName("halfDay").withSymbol("").withRateDividend(new Double(43200.0)).withRateDivisor(new Double(1.0)).build())
        	.build();
        
        Measure monthBasedTime = newMeasureBuilder().withName("MonthBasedTime").withUnits(
    			newDurationUnitBuilder().withName("month").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.MONTH).build(),
    			newDurationUnitBuilder().withName("year").withRateDividend(new Double(12.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.YEAR).build())
        	.build();
        
        Measure mass = newMeasureBuilder().withName("Mass").withUnits(
        		newUnitBuilder().withName("milligram").withSymbol("mg").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("gram").withSymbol("g").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("dekagram").withSymbol("dkg").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("kilogram").withSymbol("kg").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("quintal").withSymbol("q").withRateDividend(new Double(100.0)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("tonne").withSymbol("t").withRateDividend(new Double(1000.0)).withRateDivisor(new Double(1.0)).build())
    		.build();
        
        Measure length = newMeasureBuilder().withName("Length").withUnits(
        		newUnitBuilder().withName("nanometre").withSymbol("nm").withRateDividend(new Double(1.0E-9)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("micrometre").withSymbol("μm").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("millimetre").withSymbol("mm").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("centimetre").withSymbol("cm").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("decimetre").withSymbol("dm").withRateDividend(new Double(0.1)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("metre").withSymbol("m").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("kilometre").withSymbol("km").withRateDividend(new Double(1000.0)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("inch").withSymbol("&quot;").withRateDividend(new Double(0.0254)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("foot").withSymbol("ft").withRateDividend(new Double(0.3048)).withRateDivisor(new Double(1.0)).build(),
        		newUnitBuilder().withName("mile").withSymbol("mi").withRateDividend(new Double(1609.344)).withRateDivisor(new Double(1.0)).build())
    		.build();
        
        DerivedMeasure velocity = newDerivedMeasureBuilder().withName("Velocity").withUnits(
        		newUnitBuilder().withName("kilometrePerHour").withSymbol("km/h").withRateDividend(new Double(1.0)).withRateDivisor(new Double(3.6)).build(),
        		newUnitBuilder().withName("metrePerSecond").withSymbol("m/s").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build())
        		.withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(length.getUnits().get(6)).build())
        		.withTerms(newMeasureDefinitionTermBuilder().withExponent(-1).withUnit(time.getUnits().get(5)).build())
    		.build();
        
    	DerivedMeasure area = newDerivedMeasureBuilder().withName("Area").withUnits(
      			newUnitBuilder().withName("squareMillimetre").withSymbol("mm²").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
      			newUnitBuilder().withName("squareCentimetre").withSymbol("cm²").withRateDividend(new Double(0.00010)).withRateDivisor(new Double(1.0)).build(),
      			newUnitBuilder().withName("squareDecimetre").withSymbol("dm²").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
      			newUnitBuilder().withName("squareMetre").withSymbol("m²").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
      			newUnitBuilder().withName("hectare").withSymbol("ha").withRateDividend(new Double(10000.0)).withRateDivisor(new Double(1.0)).build(),
      			newUnitBuilder().withName("squareKilometre").withSymbol("km²").withRateDividend(new Double(1000000.0)).withRateDivisor(new Double(1.0)).build())
    			.withTerms(newMeasureDefinitionTermBuilder().withExponent(2).withUnit(length.getUnits().get(0)).build())
    	.build();
    	
        DerivedMeasure force = newDerivedMeasureBuilder().withName("Force").withUnits(
        		newUnitBuilder().withName("newton").withSymbol("N").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build())
        		.withTerms(newMeasureDefinitionTermBuilder().withExponent(-2).withUnit(time.getUnits().get(0)).build())
        		.withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(mass.getUnits().get(0)).build())
        		.withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(length.getUnits().get(0)).build())
    		.build();
        
        usePackage(measures).withElements(time,mass,length,velocity,area,force,monthBasedTime).build();
    }
    
    
    private void populatePsmModel() {
    	
    	Model model = newModelBuilder().withName("demo").build();
        
        psmModel.addContent(model);
    	
        Package measures = newPackageBuilder().withName("measures").build();
        useModel(model).withPackages(measures).build();
        
        populateMeasureModel(measures);
        
        Primitive timestampType = newTimestampTypeBuilder()
                .withName("Timestamp")
                .build();
        
        Primitive phoneType = newStringTypeBuilder()
                .withName("Phone")
                .withMaxLength(256)
                .build();
        
        Primitive booleanType = newBooleanTypeBuilder()
                .withName("Boolean")
                .build();
        
        Primitive integerType = newNumericTypeBuilder()
                .withName("Integer")
                .withPrecision(8)
                .build();
        
        Primitive stringType = newStringTypeBuilder()
        		.withMaxLength(256)
                .withName("String")
                .build();
        
        Primitive binaryType = newCustomTypeBuilder()
                .withName("Binary")
                .build();

        Primitive doubleType = newNumericTypeBuilder()
                .withName("Double")
                .withScale(7)
                .withPrecision(18)
                .build();

        Primitive kgType = newMeasuredTypeBuilder()
                .withName("kg")
                .withPrecision(18)
                .withScale(7)
                .withStoreUnit(getMeasureElement(Unit.class).filter(u -> "kg".equals(u.getSymbol())).findAny().get())
                .build();

        Primitive cmType = newMeasuredTypeBuilder()
                .withName("cm")
                .withPrecision(18)
                .withScale(7)
                .withStoreUnit(getMeasureElement(Unit.class).filter(u -> "cm".equals(u.getSymbol())).findAny().get())
                .build();
        
        EnumerationType countries = newEnumerationTypeBuilder().withName("Countries").withMembers(
        		newEnumerationMemberBuilder().withName("HU").withOrdinal(0).build(),
        		newEnumerationMemberBuilder().withName("AT").withOrdinal(1).build(),
        		newEnumerationMemberBuilder().withName("RO").withOrdinal(2).build(),
        		newEnumerationMemberBuilder().withName("SK").withOrdinal(3).build()
        		).build();
        EnumerationType titles = newEnumerationTypeBuilder().withName("Titles").withMembers(
        		newEnumerationMemberBuilder().withName("MS").withOrdinal(0).build(),
        		newEnumerationMemberBuilder().withName("MRS").withOrdinal(1).build(),
        		newEnumerationMemberBuilder().withName("MR").withOrdinal(2).build(),
        		newEnumerationMemberBuilder().withName("DR").withOrdinal(3).build()
        		).build();

        EntityType product = newEntityTypeBuilder()
                .withName("Product")
                .withAttributes(ImmutableList.of(
                        newAttributeBuilder().withName("discount").withDataType(doubleType).build(),
                        newAttributeBuilder().withName("discounted").withDataType(booleanType).build(),
                        newAttributeBuilder().withName("url").withDataType(stringType).build(),
                        newAttributeBuilder().withName("productName").withDataType(stringType).build(),
                        newAttributeBuilder().withName("unitPrice").withDataType(doubleType).build(),
                        newAttributeBuilder().withName("weight").withDataType(kgType).build(),
                        newAttributeBuilder().withName("height").withDataType(cmType).build()
                )).build();
        
        EntityType customer = newEntityTypeBuilder()
        		.withName("Customer")
        		.build();
        
        EntityType company = newEntityTypeBuilder()
        		.withName("Company")
        		.withSuperEntityTypes(customer)
        		.build();
        
        EntityType order = newEntityTypeBuilder()
        		.withName("Order")
        		.withAttributes(newAttributeBuilder().withName("orderDate").withDataType(timestampType).build(),
        				newAttributeBuilder().withName("shippedDate").withDataType(timestampType).build(),
        				newAttributeBuilder().withName("exciseTax").withDataType(doubleType).build())
        		.build();
        
        EntityType internationalOrder = newEntityTypeBuilder()
        		.withName("InternationalOrder")
        		.withSuperEntityTypes(order)
        		.build();
        
        EntityType orderDetail = newEntityTypeBuilder()
        		.withName("OrderDetail")
        		.withAttributes(newAttributeBuilder().withName("unitPrice").withDataType(doubleType).build(),
        				newAttributeBuilder().withName("discount").withDataType(doubleType).build(),
        				newAttributeBuilder().withName("quantity").withDataType(integerType).build())
        		.build();
        
        EntityType shipper = newEntityTypeBuilder()
        		.withName("Shipper")
        		.withAttributes(newAttributeBuilder().withName("companyName").withDataType(stringType).build())
        		.build();
        
        EntityType category = newEntityTypeBuilder()
        		.withName("Category")
        		.withAttributes(newAttributeBuilder().withName("categoryName").withDataType(stringType).build(),
        				newAttributeBuilder().withName("picture").withDataType(binaryType).build())
        		.build();
        
        EntityType address = newEntityTypeBuilder()
        		.withName("Address")
        		.withAttributes(newAttributeBuilder().withName("postalCode").withDataType(phoneType).build())
        		.build();
        
        EntityType employee = newEntityTypeBuilder()
        		.withName("Employee")
        		.withAttributes(newAttributeBuilder().withName("firstName").withDataType(stringType).build(),
        				newAttributeBuilder().withName("lastName").withDataType(stringType).build())
        		.build();
        
        EntityType intAddress = newEntityTypeBuilder().withName("InternationalAddress").withSuperEntityTypes(address)
        		.withAttributes(newAttributeBuilder().withName("country").withDataType(countries).build())
        		.build();
        
        AssociationEnd products = newAssociationEndBuilder().withName("products").withTarget(product)
				.withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
        AssociationEnd customerForOrder = newAssociationEndBuilder().withName("customer").withTarget(customer)
				.withCardinality(newCardinalityBuilder().withLower(0).withUpper(1).build()).build();
        AssociationEnd productOrderDetail = newAssociationEndBuilder().withName("product").withTarget(product)
				.withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
        AssociationEnd categoryRef = newAssociationEndBuilder().withName("category").withTarget(category)
				.withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
        Containment orderDetails = newContainmentBuilder().withName("orderDetails").withTarget(orderDetail)
				.withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
        Containment addresses = newContainmentBuilder().withName("addresses").withTarget(address)
				.withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
        AssociationEnd shipperRef = newAssociationEndBuilder().withName("shipper").withTarget(shipper)
				.withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
        AssociationEnd emplRef = newAssociationEndBuilder().withName("employee").withTarget(employee)
				.withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
        AssociationEnd orders = newAssociationEndBuilder().withName("orders").withTarget(order)
				.withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
		
        useAssociationEnd(products).withPartner(categoryRef).build();
        useAssociationEnd(categoryRef).withPartner(products).build();
        useAssociationEnd(orders).withPartner(emplRef).build();
        useAssociationEnd(emplRef).withPartner(orders).build();
        
        useEntityType(order).withRelations(orderDetails,shipperRef,emplRef,customerForOrder).build();
        useEntityType(category).withRelations(products).build();
        useEntityType(product).withRelations(categoryRef).build();
        useEntityType(customer).withRelations(addresses).build();
        useEntityType(employee).withRelations(orders).build();
        useEntityType(orderDetail).withRelations(productOrderDetail).build();
        
        Package entities = newPackageBuilder().withName("entities").withElements(category,order,customer,company,product,orderDetail,shipper,address,intAddress,internationalOrder,employee).build();
        Package types = newPackageBuilder().withName("types").withElements(stringType,doubleType,kgType,cmType,countries,titles,timestampType,phoneType,booleanType,integerType,binaryType).build();
        
        useModel(model).withPackages(entities,types).build();
    }
    
    private void runEpsilon() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            PsmEpsilonValidator.validatePsm(bufferedLog,
            		psmModel,
            		PsmEpsilonValidator.calculatePsmValidationScriptURI(),
            		Collections.emptyList(),
            		Collections.emptyList());
        } catch (EvlScriptExecutionException ex) {
            log.error("EVL failed", ex);
            log.error("\u001B[31m - unexpected errors: {}\u001B[0m", ex.getUnexpectedErrors());
            log.error("\u001B[33m - unexpected warnings: {}\u001B[0m", ex.getUnexpectedWarnings());
            throw ex;
        }
    }
    
    <T> Stream<T> getMeasureElement(final Class<T> clazz) {
        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }
}
