package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;

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

}
