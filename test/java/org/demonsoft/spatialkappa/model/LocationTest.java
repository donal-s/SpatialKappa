package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.WILDCARD;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getCompartment;
import static org.demonsoft.spatialkappa.model.Utils.getList;
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
        assertNull(location.getIndices());
        assertEquals(0, location.getFixedIndices().length);
        assertEquals("name", location.toString());
        assertTrue(location.isConcreteLocation());
        assertFalse(location.isWildcard());
        
        location = new Location("name", INDEX_2, new CellIndexExpression("3"));
        assertEquals("name", location.getName());
        assertNull(location.getIndices());
        assertArrayEquals(new int[] {2, 3}, location.getFixedIndices());
        assertEquals("name[2][3]", location.toString());
        assertTrue(location.isConcreteLocation());
        assertFalse(location.isWildcard());
        
        location = new Location("name", new CellIndexExpression[] {INDEX_2, new CellIndexExpression("3")});
        assertEquals("name", location.getName());
        assertNull(location.getIndices());
        assertArrayEquals(new int[] {2, 3}, location.getFixedIndices());
        assertEquals("name[2][3]", location.toString());
        assertTrue(location.isConcreteLocation());
        assertFalse(location.isWildcard());

        location = new Location("label", INDEX_2, INDEX_X);
        assertEquals("label", location.getName());
        assertNull(location.getFixedIndices());
        assertArrayEquals(new CellIndexExpression[] {INDEX_2, INDEX_X}, location.getIndices());
        assertEquals("label[2][x]", location.toString());
        assertFalse(location.isConcreteLocation());
        assertFalse(location.isWildcard());

        location = new Location("label", 
                new CellIndexExpression(INDEX_2, Operator.PLUS, INDEX_2), 
                new CellIndexExpression("5"));
        assertNull(location.getIndices());
        assertArrayEquals(new int[] {4, 5}, location.getFixedIndices());
        assertEquals("label[4][5]", location.toString());
        assertTrue(location.isConcreteLocation());
        assertFalse(location.isWildcard());
        
        // Wildcard locations
        location = new Location("label", INDEX_2, WILDCARD);
        assertEquals("label", location.getName());
        assertNull(location.getFixedIndices());
        assertArrayEquals(new CellIndexExpression[] {INDEX_2, WILDCARD}, location.getIndices());
        assertEquals("label[2][?]", location.toString());
        assertFalse(location.isConcreteLocation());
        assertTrue(location.isWildcard());

        try {
            new Location("label", INDEX_X, WILDCARD);
            fail("mixing variables and wildcard should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        
	}

    @Test
    public void testGetLinkedLocations() {

        Location location = new Location("unknown", new CellIndexExpression("2"), new CellIndexExpression("3"));

        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("nucleus"));
        compartments.add(new Compartment("cytosol", 3, 3));
        compartments.add(new Compartment("membrane", 3));
        Channel channelCytosol = new Channel("cCytosol");
        channelCytosol.addChannelComponent(null, 
                getList(new Location("cytosol", INDEX_X, INDEX_Y)), 
                getList(new Location("cytosol", INDEX_X_PLUS_1, INDEX_Y))
        );
        channelCytosol.addChannelComponent(null,
                getList(new Location("cytosol", INDEX_X, INDEX_Y)),
                getList(new Location("cytosol", INDEX_X, INDEX_Y_PLUS_1))
        );
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
        
        Channel complexChannel = new Channel("horiz");
        // Ensure more than the first pair get processed
        complexChannel.addChannelComponent(null, getList(new Location("cytosol", new CellIndexExpression("100"), INDEX_0)), 
                getList(new Location("cytosol", new CellIndexExpression("101"), INDEX_0)));
        complexChannel.addChannelComponent(null, getList(new Location("cytosol", INDEX_X, INDEX_Y)), 
                getList(new Location("cytosol", INDEX_X_MINUS_1, INDEX_Y)));
        complexChannel.addChannelComponent(null, getList(new Location("cytosol", INDEX_X, INDEX_Y)), 
                getList(new Location("cytosol", INDEX_X_PLUS_1, INDEX_Y)));
        
        location = new Location("cytosol", INDEX_0, INDEX_0);

        expected.clear();
        expected.add(new Location("cytosol", INDEX_1, INDEX_0));

        assertEquals(expected, location.getLinkedLocations(compartments, complexChannel));

        // TODO test predefined channel types

    }
    
    @Test
    public void testIsRefinement_nonWildcard() {
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
    
    @Test
    public void testIsRefinement_wildcard() {
        Location wildcard = new Location("cytosol", INDEX_0, WILDCARD, WILDCARD);
        
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_X, INDEX_Y)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, INDEX_Y)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, WILDCARD)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_1, INDEX_1, INDEX_1)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1)));
        assertTrue(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, INDEX_1)));
        assertFalse(wildcard.isRefinement(new Location("cytosol")));
        assertFalse(wildcard.isRefinement(new Location("nucleus", INDEX_0, INDEX_1, INDEX_1)));
        
        wildcard = new Location("cytosol", WILDCARD, INDEX_1, INDEX_2);
        
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, WILDCARD)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, INDEX_Y)));
        assertFalse(wildcard.isRefinement(new Location("cytosol", INDEX_1, INDEX_1, INDEX_1)));
        assertTrue(wildcard.isRefinement(new Location("cytosol", INDEX_0, INDEX_1, INDEX_2)));
    }
    
    @Test
    public void testIsWildcard() {
        assertTrue(new Location("cytosol", INDEX_0, WILDCARD, WILDCARD).isWildcard());
        assertTrue(new Location("cytosol", WILDCARD).isWildcard());
        assertFalse(new Location("cytosol", INDEX_0, INDEX_X, INDEX_Y).isWildcard());
        assertFalse(new Location("cytosol", INDEX_Y).isWildcard());
        assertFalse(new Location("cytosol", INDEX_0, INDEX_1).isWildcard());
        assertFalse(new Location("cytosol").isWildcard());
    }
    
    @Test
    public void testIsVoxel() {
        Compartment compartment1 = new Compartment("cytosol", 5, 5);
        Compartment compartment2 = new Compartment("nucleus");
        Location compartmentLocation1 = new Location("cytosol");
        Location compartmentLocation2 = new Location("nucleus");
        Location voxel1 = new Location("cytosol", INDEX_0, INDEX_1);
        Location invalidVoxel = new Location("nucleus", INDEX_0);
        
        try {
            compartmentLocation1.isVoxel(null);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // Expected exception
        }
        
        try {
            NOT_LOCATED.isVoxel(compartment1);
            fail("Not located should have failed");
        }
        catch (IllegalStateException e) {
            // Expected exception
        }
        
        try {
            compartmentLocation2.isVoxel(compartment1);
            fail("Name mismatch located should have failed");
        }
        catch (IllegalArgumentException e) {
            // Expected exception
        }
        
        assertFalse(compartmentLocation1.isVoxel(compartment1));
        assertTrue(compartmentLocation2.isVoxel(compartment2));
        assertTrue(voxel1.isVoxel(compartment1));
        assertFalse(invalidVoxel.isVoxel(compartment2));
    }
    
    @Test
    public void testGetReferencedCompartment() {
        Location location = new Location("label");
        
        try {
            getCompartment(null, location.getName());
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("label2"));
        try {
            getCompartment(compartments, location.getName());
            fail("Missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        Compartment match = new Compartment("label");
        compartments.add(match);
        assertEquals(match, getCompartment(compartments, location.getName()));
    }
    
    
    @Test
    public void testGetConcreteReference() {
        Location location = new Location("label");
        Map<String, Integer> variables = new HashMap<String, Integer>();
        
        try {
            location.getConcreteLocation(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertEquals(location, location.getConcreteLocation(variables));

        location = new Location("label", new CellIndexExpression("2"));
        assertEquals(location, location.getConcreteLocation(variables));


        location = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new CellIndexExpression("3"), Operator.PLUS, new CellIndexExpression("1")));
        Location expectedReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression("4"));
        assertEquals(expectedReference, location.getConcreteLocation(variables));

        location = new Location("label", new CellIndexExpression(new VariableReference("x")));
        try {
            location.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new CellIndexExpression("7"));
        assertEquals(expectedReference, location.getConcreteLocation(variables));

        location = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new VariableReference("x")));
        variables.clear();
        try {
            location.getConcreteLocation(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        expectedReference = new Location("label", new CellIndexExpression("2"), new CellIndexExpression("7"));
        assertEquals(expectedReference, location.getConcreteLocation(variables));
    }
}
