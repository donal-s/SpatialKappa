package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class CompartmentTest {

    @SuppressWarnings("unused")
    @Test
    public void testCompartment() {
        try {
            new Compartment(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Compartment(null, 2, 3);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Compartment("label", 0);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new Compartment("label", -1);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        Compartment compartment = new Compartment("label");
        assertEquals("label", compartment.getName());
        assertArrayEquals(new int[0], compartment.getDimensions());
        assertEquals("label", compartment.toString());

        compartment = new Compartment("label", 2, 3, 5);
        assertEquals("label", compartment.getName());
        assertArrayEquals(new int[] {2, 3, 5}, compartment.getDimensions());
        assertEquals("label[2][3][5]", compartment.toString());
    }
    
    @Test
    public void testGetDistributedCellReferences() {
        Compartment compartment = new Compartment("label");
        checkCompartmentReferences(compartment.getDistributedCellReferences(), new Location("label"));
        
        compartment = new Compartment("label", 3);
        checkCompartmentReferences(compartment.getDistributedCellReferences(), 
                new Location("label", new CellIndexExpression("0")),
                new Location("label", new CellIndexExpression("1")),
                new Location("label", new CellIndexExpression("2"))
        );
        
        compartment = new Compartment("label", 3, 2);
        checkCompartmentReferences(compartment.getDistributedCellReferences(), 
                new Location("label", new CellIndexExpression("0"), new CellIndexExpression("0")),
                new Location("label", new CellIndexExpression("1"), new CellIndexExpression("0")),
                new Location("label", new CellIndexExpression("2"), new CellIndexExpression("0")),
                new Location("label", new CellIndexExpression("0"), new CellIndexExpression("1")),
                new Location("label", new CellIndexExpression("1"), new CellIndexExpression("1")),
                new Location("label", new CellIndexExpression("2"), new CellIndexExpression("1"))
        );
    }

    private void checkCompartmentReferences(Location[] actual, Location... expected) {
        assertEquals(expected.length, actual.length);
        
        for (int index = 0 ; index < expected.length; index++) {
            assertEquals(expected[index], actual[index]);
        }
    }

}
