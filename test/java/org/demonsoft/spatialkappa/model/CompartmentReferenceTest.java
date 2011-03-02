package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.MathExpression.Operator;
import org.junit.Test;


public class CompartmentReferenceTest {

    @Test
    public void testConstructor() {
        MathExpression expr1 = new MathExpression("2");
        MathExpression expr2 = new MathExpression("x");

        try {
            new Location(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Location(null, expr1, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Location compartmentReference = new Location("label");
        assertEquals("label", compartmentReference.getName());
        assertArrayEquals(new MathExpression[0], compartmentReference.getIndices());
        assertEquals("label", compartmentReference.toString());
        assertTrue(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", expr1, expr2);
        assertEquals("label", compartmentReference.getName());
        assertArrayEquals(new MathExpression[] {expr1, expr2}, compartmentReference.getIndices());
        assertEquals("label[2][x]", compartmentReference.toString());
        assertFalse(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", 
                new MathExpression(new MathExpression("2"), Operator.PLUS, new MathExpression("2")), 
                new MathExpression("5"));
        assertEquals("label[(2 + 2)][5]", compartmentReference.toString());
        assertTrue(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", new MathExpression("x"));
        assertEquals("label[x]", compartmentReference.toString());
        assertFalse(compartmentReference.isConcreteLocation());
    }
    
    @Test
    public void testGetReferencedCompartment() {
        Location reference = new Location("label");
        
        try {
            reference.getReferencedCompartment(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("label2"));
        assertNull(reference.getReferencedCompartment(compartments));
        
        Compartment match = new Compartment("label");
        compartments.add(match);
        assertEquals(match, reference.getReferencedCompartment(compartments));
    }
    
    
    @Test
    public void testGetConcreteReference() {
        Location compartmentReference = new Location("label");
        Map<String, Integer> variables = new HashMap<String, Integer>();
        
        try {
            compartmentReference.getConcreteLocation(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertEquals(compartmentReference, compartmentReference.getConcreteLocation(variables));

        compartmentReference = new Location("label", new MathExpression("2"));
        assertEquals(compartmentReference, compartmentReference.getConcreteLocation(variables));


        compartmentReference = new Location("label", new MathExpression("2"), new MathExpression(new MathExpression("3"), Operator.PLUS, new MathExpression("1")));
        Location expectedReference = new Location("label", new MathExpression("2"), new MathExpression("4"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));

        compartmentReference = new Location("label", new MathExpression("x"));
        try {
            compartmentReference.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new MathExpression("7"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));

        compartmentReference = new Location("label", new MathExpression("2"), new MathExpression("x"));
        variables.clear();
        try {
            compartmentReference.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new MathExpression("2"), new MathExpression("7"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));
    }
    
}
