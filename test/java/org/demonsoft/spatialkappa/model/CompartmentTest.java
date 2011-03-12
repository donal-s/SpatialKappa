package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.junit.Test;

public class CompartmentTest {

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
    public void testGetDistributedCellCounts() {
        Compartment compartment = new Compartment("label");
        
        try {
            compartment.getDistributedCellCounts(-1);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertArrayEquals(new int[] {0}, compartment.getDistributedCellCounts(0));
        assertArrayEquals(new int[] {5}, compartment.getDistributedCellCounts(5));
        
        compartment = new Compartment("label", 3);
        assertArrayEquals(new int[] {0, 0, 0}, compartment.getDistributedCellCounts(0));
        assertArrayEquals(new int[] {2, 2, 1}, compartment.getDistributedCellCounts(5));
        assertArrayEquals(new int[] {2, 1, 1}, compartment.getDistributedCellCounts(4));
        
        compartment = new Compartment("label", 3, 2);
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0}, compartment.getDistributedCellCounts(0));
        assertArrayEquals(new int[] {2, 2, 2, 2, 1, 1}, compartment.getDistributedCellCounts(10));
        assertArrayEquals(new int[] {2, 2, 1, 1, 1, 1}, compartment.getDistributedCellCounts(8));
    }
    
    @Test
    public void testGetDistributedCellCounts_multipleCompartments() {
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("label"));
        
        try {
            Compartment.getDistributedCellCounts(5, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            Compartment.getDistributedCellCounts(5, new ArrayList<Compartment>());
            fail("empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            Compartment.getDistributedCellCounts(-1, compartments);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertArrayEquals(new int[] {0}, Compartment.getDistributedCellCounts(0, compartments));
        assertArrayEquals(new int[] {5}, Compartment.getDistributedCellCounts(5, compartments));
        
        compartments.add(new Compartment("label2", 3));
        compartments.add(new Compartment("label3", 3, 2));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, Compartment.getDistributedCellCounts(0, compartments));
        assertArrayEquals(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, Compartment.getDistributedCellCounts(10, compartments));
        assertArrayEquals(new int[] {2, 2, 2, 2, 2, 2, 2, 2, 1, 1}, Compartment.getDistributedCellCounts(18, compartments));
        assertArrayEquals(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 0, 0}, Compartment.getDistributedCellCounts(8, compartments));
    }
    
    @Test
    public void testGetCellStateSuffixes() {
        Compartment compartment = new Compartment("label");
        assertArrayEquals(new String[] {"loc~label"}, compartment.getCellStateSuffixes());
        
        compartment = new Compartment("label", 3);
        assertArrayEquals(new String[] {"loc~label,loc_index~0", "loc~label,loc_index~1", "loc~label,loc_index~2"}, compartment.getCellStateSuffixes());
        
        compartment = new Compartment("label", 3, 2);
        assertArrayEquals(new String[] {
                "loc~label,loc_index_1~0,loc_index_2~0", "loc~label,loc_index_1~1,loc_index_2~0", "loc~label,loc_index_1~2,loc_index_2~0", 
                "loc~label,loc_index_1~0,loc_index_2~1", "loc~label,loc_index_1~1,loc_index_2~1", "loc~label,loc_index_1~2,loc_index_2~1"}, 
                compartment.getCellStateSuffixes());
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

    @Test
    public void testGetDistributedCellReferences_multipleCompartments() {
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("label"));
        

        try {
            Compartment.getDistributedCellReferences(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            Compartment.getDistributedCellReferences(new ArrayList<Compartment>());
            fail("empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        
        checkCompartmentReferences(Compartment.getDistributedCellReferences(compartments), new Location("label"));
        
        compartments.add(new Compartment("label2", 3));
        compartments.add(new Compartment("label3", 3, 2));
        checkCompartmentReferences(Compartment.getDistributedCellReferences(compartments), 
                new Location("label"),
                new Location("label2", new CellIndexExpression("0")),
                new Location("label2", new CellIndexExpression("1")),
                new Location("label2", new CellIndexExpression("2")),
                new Location("label3", new CellIndexExpression("0"), new CellIndexExpression("0")),
                new Location("label3", new CellIndexExpression("1"), new CellIndexExpression("0")),
                new Location("label3", new CellIndexExpression("2"), new CellIndexExpression("0")),
                new Location("label3", new CellIndexExpression("0"), new CellIndexExpression("1")),
                new Location("label3", new CellIndexExpression("1"), new CellIndexExpression("1")),
                new Location("label3", new CellIndexExpression("2"), new CellIndexExpression("1"))
        );
    }

    private void checkCompartmentReferences(Location[] actual, Location... expected) {
        assertEquals(expected.length, actual.length);
        
        for (int index = 0 ; index < expected.length; index++) {
            assertEquals(expected[index], actual[index]);
        }
    }

}
