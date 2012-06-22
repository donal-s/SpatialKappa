package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y_PLUS_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class LocationTest {

    @SuppressWarnings("unused")
	@Test
	public void testConstructor() {
        try {
            new Location(null, new CellIndexExpression[] {});
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
		
        try {
            new Location(null, new ArrayList<CellIndexExpression>());
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
		
        Location location = new Location("name");
        assertEquals("name", location.getName());
        assertEquals(0, location.getIndices().length);
        assertEquals("name", location.toString());
        
        location = new Location("name", new CellIndexExpression("2"), new CellIndexExpression("3"));
        assertEquals("name", location.getName());
        assertArrayEquals(new CellIndexExpression[] {new CellIndexExpression("2"), new CellIndexExpression("3")}, 
        		location.getIndices());
        assertEquals("name[2][3]", location.toString());
        
        location = new Location("name", new CellIndexExpression[] {new CellIndexExpression("2"), new CellIndexExpression("3")});
        assertEquals("name", location.getName());
        assertArrayEquals(new CellIndexExpression[] {new CellIndexExpression("2"), new CellIndexExpression("3")}, 
        		location.getIndices());
        assertEquals("name[2][3]", location.toString());
	}

	//TODO @Test
	public void testGetReferencedCompartment() {
		fail("Not yet implemented");
	}

	//TODO @Test
	public void testIsConcreteLocation() {
		fail("Not yet implemented");
	}

	//TODO @Test
	public void testEqualsHashCode() {
		fail("Not yet implemented");
	}
	
	//TODO @Test
	public void testGetConcreteLocation() {
		fail("Not yet implemented");
	}

	//TODO @Test
	public void testMatches() {
		fail("Not yet implemented");
	}

	//TODO @Test
	public void testDoLocationsMatch() {
		fail("Not yet implemented");
	}

	@Test
    public void testGetLinkedLocations() {

        Location location = new Location("unknown", new CellIndexExpression("2"), new CellIndexExpression("3"));

        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("nucleus"));
        compartments.add(new Compartment("cytosol", 3, 3));
        compartments.add(new Compartment("membrane", 3));
        Channel channelCytosol = new Channel("cCytosol", Utils.getList(
                new Location[] {
                        new Location("cytosol", INDEX_X, INDEX_Y), 
                        new Location("cytosol", INDEX_X_PLUS_1, INDEX_Y), 
                },
                new Location[] { 
                        new Location("cytosol", INDEX_X, INDEX_Y),
                        new Location("cytosol", INDEX_X, INDEX_Y_PLUS_1)
                }));
        Channel channelMembraneX = new Channel("cMembrane", 
                new Location("membrane", INDEX_X), 
                new Location("membrane", INDEX_X_PLUS_1));
        Channel channelMembraneTransport = new Channel("cMembraneTransport", 
                new Location("cytosol", INDEX_X, INDEX_0), 
                new Location("membrane", INDEX_X));

        try {
            location.getLinkedLocations(null, channelCytosol);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // Expected exception
        }
        
        try {
            location.getLinkedLocations(compartments, channelCytosol);
            fail("Unknown location should have failed");
        }
        catch (IllegalArgumentException e) {
            // Expected exception
        }
        
        location = new Location("nucleus");

        // No matching channels
        List<Location> expected = new ArrayList<Location>();
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosol, channelMembraneX));
        
        location = new Location("cytosol", INDEX_1, INDEX_1);
        
        expected.add(new Location("cytosol", INDEX_2, INDEX_1));
        expected.add(new Location("cytosol", INDEX_1, INDEX_2));
        
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosol));
        
        location = new Location("cytosol", INDEX_1, INDEX_0);

        expected.clear();
        expected.add(new Location("cytosol", INDEX_2, INDEX_0));
        expected.add(new Location("cytosol", INDEX_1, INDEX_1));
        expected.add(new Location("membrane", INDEX_1));
        
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosol, channelMembraneTransport));
        
        // Complex channels
        
        List<Location[]> locations = new ArrayList<Location[]>();
        // Ensure more than the first pair get processed
        locations.add(new Location[] {new Location("cytosol", new CellIndexExpression("100"), INDEX_0), new Location("cytosol", new CellIndexExpression("101"), INDEX_0)});
        locations.add(new Location[] {new Location("cytosol", INDEX_X, INDEX_Y), new Location("cytosol", INDEX_X_MINUS_1, INDEX_Y)});
        locations.add(new Location[] {new Location("cytosol", INDEX_X, INDEX_Y), new Location("cytosol", INDEX_X_PLUS_1, INDEX_Y)});
        Channel complexChannel = new Channel("horiz", locations);
        
        location = new Location("cytosol", INDEX_0, INDEX_0);

        expected.clear();
        expected.add(new Location("cytosol", INDEX_1, INDEX_0));

        assertEquals(expected, location.getLinkedLocations(compartments, complexChannel));

    }
	
	@Test
	public void testIsRefinement() {
        Location compartment1 = new Location("cytosol");
        Location compartment2 = new Location("nucleus");
        Location voxel1 = new Location("cytosol", INDEX_0);
        Location voxel2 = new Location("nucleus", INDEX_1);
        
        try {
            compartment1.isRefinement(null);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // Expected exception
        }
        
        assertTrue(NOT_LOCATED.isRefinement(compartment1));
        assertTrue(NOT_LOCATED.isRefinement(voxel1));
        assertFalse(NOT_LOCATED.isRefinement(NOT_LOCATED));
        
        assertFalse(compartment1.isRefinement(compartment1));
        assertFalse(compartment1.isRefinement(compartment2));
        assertTrue(compartment1.isRefinement(voxel1));
        assertFalse(compartment1.isRefinement(voxel2));
        assertFalse(compartment1.isRefinement(NOT_LOCATED));
        
        assertFalse(voxel1.isRefinement(compartment1));
        assertFalse(voxel1.isRefinement(voxel1));
        assertFalse(voxel1.isRefinement(voxel2));
        assertFalse(voxel1.isRefinement(NOT_LOCATED));
	}
	
}
