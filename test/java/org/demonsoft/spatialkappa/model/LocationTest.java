package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y_PLUS_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
        Channel channelCytosolX = new Channel("cCytosol", 
                new Location("cytosol", INDEX_X, INDEX_Y), 
                new Location("cytosol", INDEX_X_PLUS_1, INDEX_Y), Direction.FORWARD);
        Channel channelCytosolY = new Channel("cCytosol", 
                new Location("cytosol", INDEX_X, INDEX_Y), 
                new Location("cytosol", INDEX_X, INDEX_Y_PLUS_1), Direction.FORWARD);
        Channel channelMembraneX = new Channel("cMembrane", 
                new Location("membrane", INDEX_X), 
                new Location("membrane", INDEX_X_PLUS_1), Direction.FORWARD);
        Channel channelMembraneTransport = new Channel("cMembraneTransport", 
                new Location("cytosol", INDEX_X, INDEX_0), 
                new Location("membrane", INDEX_X), 
                Direction.FORWARD);

        try {
            location.getLinkedLocations(null, channelCytosolX);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // Expected exception
        }
        
        try {
            location.getLinkedLocations(compartments, channelCytosolX);
            fail("Unknown location should have failed");
        }
        catch (IllegalArgumentException e) {
            // Expected exception
        }
        
        location = new Location("nucleus");

        // No matching channels
        List<Location> expected = new ArrayList<Location>();
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosolX, channelMembraneX));
        
        location = new Location("cytosol", INDEX_1, INDEX_1);
        
        expected.add(new Location("cytosol", INDEX_2, INDEX_1));
        expected.add(new Location("cytosol", INDEX_1, INDEX_2));
        
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosolX, channelCytosolY));
        
        location = new Location("cytosol", INDEX_1, INDEX_0);

        expected.clear();
        expected.add(new Location("cytosol", INDEX_2, INDEX_0));
        expected.add(new Location("cytosol", INDEX_1, INDEX_1));
        expected.add(new Location("membrane", INDEX_1));
        
        assertEquals(expected, location.getLinkedLocations(compartments, channelCytosolX, channelCytosolY, channelMembraneTransport));
    }
}