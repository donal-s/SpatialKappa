package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.junit.Test;


public class CompartmentReferenceTest {

    @Test
    public void testConstructor() {
        CellIndexExpression expr1 = new CellIndexExpression("2");
        CellIndexExpression expr2 = new CellIndexExpression(new VariableReference("x"));

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
        assertArrayEquals(new CellIndexExpression[0], compartmentReference.getIndices());
        assertEquals("label", compartmentReference.toString());
        assertTrue(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", expr1, expr2);
        assertEquals("label", compartmentReference.getName());
        assertArrayEquals(new CellIndexExpression[] {expr1, expr2}, compartmentReference.getIndices());
        assertEquals("label[2]['x']", compartmentReference.toString());
        assertFalse(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", 
                new CellIndexExpression(new CellIndexExpression("2"), Operator.PLUS, new CellIndexExpression("2")), 
                new CellIndexExpression("5"));
        assertEquals("label[(2 + 2)][5]", compartmentReference.toString());
        assertTrue(compartmentReference.isConcreteLocation());

        compartmentReference = new Location("label", new CellIndexExpression(new VariableReference("x")));
        assertEquals("label['x']", compartmentReference.toString());
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

        compartmentReference = new Location("label", new CellIndexExpression("2"));
        assertEquals(compartmentReference, compartmentReference.getConcreteLocation(variables));


        compartmentReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new CellIndexExpression("3"), Operator.PLUS, new CellIndexExpression("1")));
        Location expectedReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression("4"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));

        compartmentReference = new Location("label", new CellIndexExpression(new VariableReference("x")));
        try {
            compartmentReference.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new CellIndexExpression("7"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));

        compartmentReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new VariableReference("x")));
        variables.clear();
        try {
            compartmentReference.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression("7"));
        assertEquals(expectedReference, compartmentReference.getConcreteLocation(variables));
    }
    
}
