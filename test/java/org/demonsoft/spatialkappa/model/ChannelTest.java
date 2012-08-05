package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_PLUS_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.Channel.ChannelComponent;
import org.demonsoft.spatialkappa.model.Channel.EdgeNeighbourComponent;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.junit.Test;

public class ChannelTest {

    public static CellIndexExpression INDEX_3 = new CellIndexExpression("3");

    @SuppressWarnings({ "unused" })
    @Test
    public void testChannel() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        
        List<List<Location>[]> locations = new ArrayList<List<Location>[]>();
        
        try {
            new Channel(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Channel channel = new Channel("label");
        assertEquals("label", channel.getName());
        assertEquals("label: ", channel.toString());
    }
    
    @Test
    public void testAddChannelComponent() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        
        Channel channel = new Channel("label");

        channel.addChannelComponent(null, getList(reference1), getList(reference2));
        assertEquals("label: [a] -> [b]", channel.toString());

        channel.addChannelComponent(null, getList(reference1), getList(reference1));
        assertEquals("label: ([a] -> [b]) + ([a] -> [a])", channel.toString());
        
        // Multi compartment channels
        channel.addChannelComponent(null, getList(reference2, reference1), getList(reference1, reference2));
        assertEquals("label: ([a] -> [b]) + ([a] -> [a]) + ([b, a] -> [a, b])", channel.toString());
        
        // Channel types
        channel.addChannelComponent("Hexagonal", getList(reference1), getList(reference1));
        assertEquals("label: ([a] -> [b]) + ([a] -> [a]) + ([b, a] -> [a, b]) + ((Hexagonal) [a] -> [a])", channel.toString());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testChannel_testConstructor() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        
        List<Location[]> locations = new ArrayList<Location[]>();
        
        try {
            new Channel(null, reference1, reference2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Channel("name", null, reference2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Channel("name", reference1, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Channel channel = new Channel("label", reference1, reference2);
        assertEquals("label", channel.getName());
        assertEquals("label: [a] -> [b]", channel.toString());
    }
    
    @Test
    public void testValidate() {
        List<Compartment> compartments = Utils.getList(new Compartment("known"));
        
        Channel channel = new Channel("name", new Location("known"), new Location("unknown"));

        try {
            channel.validate(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        
        // Missing compartment link reference
        try {
            channel.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Compartment 'unknown' not found", ex.getMessage());
        }
        
        channel = new Channel("name", new Location("known"), new Location("unknown"));
        try {
            channel.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Compartment 'unknown' not found", ex.getMessage());
        }
        
        channel = new Channel("name", new Location("known"), new Location("known"));
        channel.validate(compartments);

        try {
            channel = new Channel("name");
            channel.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("No location pairs defined", ex.getMessage());
        }

        channel.addChannelComponent(null, getList(new Location("known")), getList(new Location("known")));
        channel.validate(compartments);

        // TODO compartment link range out of bounds ?
        
        // TODO channel type validation
    }

    @Test
    public void testGetCellReferencePairs() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        Channel channel = new Channel("label", reference1, reference2);
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            channel.getCellReferencePairs(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        compartments.add(new Compartment("a"));
        
        try {
            channel.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            channel.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        compartments.add(new Compartment("b"));

        try {
            channel.getCellReferencePairs(compartments);
            fail("dimension mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Single cell compartments
        compartments.clear();
        reference1 = new Location("a");
        reference2 = new Location("b");
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertArrayEquals(new Location[][] {
                { new Location("a"), new Location("b") },
                }, channel.getCellReferencePairs(compartments));
        
        // Single cell-cell link
        compartments.clear();
        reference1 = new Location("a", INDEX_1);
        reference2 = new Location("a", INDEX_2);
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_1), new Location("a", INDEX_2) }
                }, channel.getCellReferencePairs(compartments));

        VariableReference refX = new VariableReference("x");
        
        // Linear array
        compartments.clear();
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", INDEX_X_PLUS_1);
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_0), new Location("a", INDEX_1) },
                { new Location("a", INDEX_1), new Location("a", INDEX_2) },
                { new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3")) },
                }, channel.getCellReferencePairs(compartments));

        // Linear array negative
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression(refX));
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_1), new Location("a", INDEX_0) },
                { new Location("a", INDEX_2), new Location("a", INDEX_1) },
                { new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_2) },
                }, channel.getCellReferencePairs(compartments));
        
        // Multiple components
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_PLUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", new CellIndexExpression("3"))), getList(new Location("a", INDEX_0)));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_0), new Location("a", INDEX_1) },
                { new Location("a", INDEX_1), new Location("a", INDEX_2) },
                { new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3")) },
                { new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_0) },
                }, channel.getCellReferencePairs(compartments));
        
        // TODO channel type tests
    }


    @Test
    public void testApplyChannel_singleCompartment() {
        Location reference1 = new Location("a", INDEX_X);
        Location reference2 = new Location("a", INDEX_X_PLUS_1);
        Channel channel = new Channel("label", reference1, reference2);
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        
        try {
            channel.applyChannel(null, getList(new Location("a")), compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.applyChannel(getList(new Location("a", INDEX_1)), getList(new Location("a")), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("a", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1));
            channel2.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("c", INDEX_X), 
                    new Location("a", INDEX_X_PLUS_1));
            channel2.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        List<List<Location>> expected = new ArrayList<List<Location>>();

        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", new CellIndexExpression("3"))), getList(NOT_LOCATED), compartments));
        
        expected.add(getList(new Location("a", INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(new Location("a")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(new Location("a", INDEX_2)), compartments));
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(new Location("a", INDEX_0)), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        channel = new Channel("label", reference1, reference2);

        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0)), getList(NOT_LOCATED), compartments));

        expected.add(getList(new Location("a", INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 5, 5, 5));
        
        reference1 = new Location("a", INDEX_X, INDEX_Y, INDEX_0);
        reference2 = new Location("a", INDEX_X_PLUS_1, new CellIndexExpression(INDEX_Y, Operator.MULTIPLY, INDEX_2), INDEX_2);
        channel = new Channel("label", reference1, reference2);

        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));

        expected.add(getList(new Location("a", new CellIndexExpression("3"), new CellIndexExpression("4"), INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        channel = new Channel("name");
        channel.addChannelComponent(null, getList(new Location("a", new CellIndexExpression("100"))), getList(new Location("a", new CellIndexExpression("101"))));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_MINUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_PLUS_1)));
        
        compartments.clear();
        compartments.add(new Compartment("a", 4));

        expected.clear();
        expected.add(getList(new Location("a", INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));

        // Ensure each location pair are processed individually
        channel = new Channel("name");
        channel.addChannelComponent(null, getList(new Location("a")), getList(new Location("b")));
        channel.addChannelComponent(null, getList(new Location("b")), getList(new Location("a")));
        compartments.clear();
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));

        expected.clear();
        expected.add(getList(new Location("b")));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
    }
    

    @Test
    public void testApplyChannel_edgeNeighbour_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("EdgeNeighbour", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("4"))), getList(NOT_LOCATED), compartments));
        
        // Corner voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_1)), compartments));

        // Inner voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
    }
    


    @Test
    public void testApplyChannel_faceNeighbour_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("FaceNeighbour", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4, 4), new Compartment("b", 4, 4, 4));
        

        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_1, new CellIndexExpression("4"))), getList(NOT_LOCATED), compartments));
        
        // Corner voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)), compartments));

        // Inner voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
    }
    
    @Test
    public void testApplyChannel_neighbour3d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Neighbour", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4, 4), new Compartment("b", 4, 4, 4));
        

        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_1, new CellIndexExpression("4"))), getList(NOT_LOCATED), compartments));
        
        // Corner voxel
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)), compartments));

        // Inner voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
    }
    

    @Test
    public void testApplyChannel_hexagonal_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Hexagonal", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("4"))), getList(NOT_LOCATED), compartments));
        
        // Corner voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_1)), compartments));

        // Inner voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
    }
    

    @Test
    public void testApplyChannel_neighbour2d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Neighbour", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("4"))), getList(NOT_LOCATED), compartments));
        
        // Corner voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_1)), compartments));

        // Inner voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
    }

    @Test
    public void testApplyChannel_radial2d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Radial", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("54"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a", INDEX_1, INDEX_1)), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
    }

    @Test
    public void testApplyChannel_radialOut2d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("RadialOut", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("54"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(new Location("a", INDEX_1, INDEX_1)), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
    }


    @Test
    public void testApplyChannel_lateral2d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Lateral", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("54"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_1)), compartments));
    }

    @Test
    public void testApplyChannel_lateral3d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Lateral", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)), compartments));
    }

    @Test
    public void testApplyChannel_radialIn3d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("RadialIn", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), compartments));
    }


    @Test
    public void testApplyChannel_radialOut3d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("RadialOut", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_3)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
    }

    @Test
    public void testApplyChannel_radial3d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("Radial", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_3, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_3, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_1, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_2, INDEX_3)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_2)));
        expected.add(getList(new Location("a", INDEX_3, INDEX_3, INDEX_3)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)), getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_0, INDEX_1, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_0, INDEX_1)));
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)));
        expected.add(getList(new Location("a", INDEX_2, INDEX_2, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
    }

    @Test
    public void testApplyChannel_radialIn2d_intracompartment() {
        Location location = new Location("a");
        Channel channel = new Channel("label");
        channel.addChannelComponent("RadialIn", getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<List<Location>> expected = new ArrayList<List<Location>>();

        // Invalid input locations
        assertEquals(expected, channel.applyChannel(getList(new Location("b", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a")), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_X)), getList(NOT_LOCATED), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_0, new CellIndexExpression("54"))), getList(NOT_LOCATED), compartments));
        
        // Central voxel
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_2, INDEX_2)), getList(NOT_LOCATED), compartments));

        // Midway voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_2, INDEX_2)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_1)), getList(NOT_LOCATED), compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(NOT_LOCATED), compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a")), compartments));
        
        expected.clear();
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("b")), compartments));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a", INDEX_0, INDEX_0)), compartments));
        
        expected.add(getList(new Location("a", INDEX_1, INDEX_1)));
        assertEquals(expected, channel.applyChannel(getList(new Location("a", INDEX_1, INDEX_0)), getList(new Location("a", INDEX_1, INDEX_1)), compartments));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplyChannel_multipleCompartments() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 3));
        Channel channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_X_PLUS_1)));
        
        // Source & constraints mismatch
//        try {
//            // Unknown compartment
//            List<Location> sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
//            List<Location> targetConstraints = getList(NOT_LOCATED);
//            channel.applyChannel(sourceLocations, targetConstraints, compartments);
//            fail("constraints mismatch should have failed");
//        }
//        catch (IllegalArgumentException ex) {
//            // Expected exception
//        }
        
        
        // No match
        List<Location> sourceLocations = getList(new Location("a", INDEX_1));
        List<Location> targetConstraints = getList(NOT_LOCATED);
        List<List<Location>> results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        List<List<Location>> expected = new ArrayList<List<Location>>();
        assertEquals(expected, results);
        
        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_2));
        targetConstraints = getList(NOT_LOCATED, NOT_LOCATED);
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        assertEquals(expected, results);
        
        sourceLocations = getList(new Location("a", INDEX_1), new Location("a", INDEX_1));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        assertEquals(expected, results);
        
        // No match - geometry mismatch
        sourceLocations = getList(new Location("a", INDEX_2), new Location("b", INDEX_2));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        assertEquals(expected, results);
        
        // Variable linked
        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        
        // Variable independent
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_Y_PLUS_1)));
        
        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_0));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_1)));
        assertEquals(expected, results);

        
        // Swap locations - allow source reordering
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("b", INDEX_X), new Location("a", INDEX_Y)));
        
        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_0));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("b", INDEX_1), new Location("a", INDEX_0)));
        assertEquals(expected, results);

        sourceLocations = getList(new Location("b", INDEX_1), new Location("a", INDEX_0));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_1), new Location("b", INDEX_0)));
        assertEquals(expected, results);
        
        // Target invalid - no results
        sourceLocations = getList(new Location("a", new CellIndexExpression("3")), new Location("b", INDEX_0));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected.clear();
        assertEquals(expected, results);
        
        // Multiple components
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_X_PLUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_MINUS_1), new Location("b", INDEX_X_MINUS_1)));

        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)),
                getList(new Location("a", INDEX_0), new Location("b", INDEX_0)));
        assertEquals(expected, results);
        
        sourceLocations = getList(new Location("a", INDEX_0), new Location("b", INDEX_0));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_1), new Location("b", INDEX_1)));
        assertEquals(expected, results);
        
        
        // Target constraints
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_Y_PLUS_1)));

        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
        targetConstraints = getList(new Location("a"), new Location("b", INDEX_2));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_2));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected.clear();
        assertEquals(expected, results);

        sourceLocations = getList(new Location("a", INDEX_1), new Location("b", INDEX_1));
        targetConstraints = getList(new Location("a", INDEX_2), NOT_LOCATED);
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        sourceLocations = getList(new Location("a", INDEX_2), new Location("b", INDEX_2));
        results = channel.applyChannel(sourceLocations, targetConstraints, compartments);
        expected.clear();
        assertEquals(expected, results);

        // TODO channel type tests
    }
    
    // TODO check target locations are unique set
    
    @Test
    public void testIsValidSourceLocations() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        ChannelComponent component = new ChannelComponent(getList(new Location("a", INDEX_X)),
                getList(new Location("c", INDEX_X_PLUS_1)));

        assertTrue(component.isValidSourceLocations(getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_1)), compartments));
    }
    
    @Test
    public void testIsValidSourceLocation() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        ChannelComponent component = new ChannelComponent(getList(new Location("a", INDEX_X)), getList(new Location("c", INDEX_X_PLUS_1)));

        assertTrue(component.isValidSourceLocation(new Location("a", INDEX_X), new Location("a", INDEX_1), compartments));
    }
    
    @Test
    public void testIsValidLinkChannel() {
        // Single cell compartments
        Channel channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a")), getList(new Location("b")));
        assertTrue(channel.isValidLinkChannel());
        
        // Single cell-cell link
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_1)), getList(new Location("a", INDEX_2)));
        assertTrue(channel.isValidLinkChannel());

        // Multiple components
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a")), getList(new Location("b")));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_1)), getList(new Location("a", INDEX_2)));
        assertTrue(channel.isValidLinkChannel());
        
        // Multi agent channel
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a"), new Location("c")), getList(new Location("b"), new Location("d")));
        assertFalse(channel.isValidLinkChannel());
        
        // Predefined channel type
        channel = new Channel("label");
        channel.addChannelComponent("Hexagonal", getList(new Location("a")), getList(new Location("b")));
        assertTrue(channel.isValidLinkChannel());
    }
    
    @Test
    public void testPermuteLocations() {
        ChannelComponent component = new ChannelComponent(
                getList(new Location("a", INDEX_X)), getList(new Location("c", INDEX_X_PLUS_1)));

        List<List<Location>> locationPermutations = new ArrayList<List<Location>>();
        List<List<Location>> targetConstraintPermutations = new ArrayList<List<Location>>();

        // Single element
        List<Location> locations = getList(new Location("a"));
        List<Location> targetConstraints = getList(new Location("b"));
        component.permuteLocations(locations, targetConstraints, locationPermutations, targetConstraintPermutations);
        
        List<List<Location>> expectedLocations = new ArrayList<List<Location>>();
        List<List<Location>> expectedTargetConstraints = new ArrayList<List<Location>>();
        expectedLocations.add(getList(new Location("a")));
        expectedTargetConstraints.add(getList(new Location("b")));
        
        assertEquals(expectedLocations, locationPermutations);
        assertEquals(expectedTargetConstraints, targetConstraintPermutations);
        

        // 3 elements
        locations = getList(new Location("a"), new Location("b"), new Location("c"));
        targetConstraints = getList(new Location("d"), new Location("e"), new Location("f"));
        locationPermutations.clear();
        targetConstraintPermutations.clear();
        component.permuteLocations(locations, targetConstraints, locationPermutations, targetConstraintPermutations);
        
        expectedLocations.clear();
        expectedTargetConstraints.clear();
        expectedLocations.add(getList(new Location("a"), new Location("b"), new Location("c")));
        expectedLocations.add(getList(new Location("a"), new Location("c"), new Location("b")));
        expectedLocations.add(getList(new Location("b"), new Location("a"), new Location("c")));
        expectedLocations.add(getList(new Location("b"), new Location("c"), new Location("a")));
        expectedLocations.add(getList(new Location("c"), new Location("a"), new Location("b")));
        expectedLocations.add(getList(new Location("c"), new Location("b"), new Location("a")));
        expectedTargetConstraints.add(getList(new Location("d"), new Location("e"), new Location("f")));
        expectedTargetConstraints.add(getList(new Location("d"), new Location("f"), new Location("e")));
        expectedTargetConstraints.add(getList(new Location("e"), new Location("d"), new Location("f")));
        expectedTargetConstraints.add(getList(new Location("e"), new Location("f"), new Location("d")));
        expectedTargetConstraints.add(getList(new Location("f"), new Location("d"), new Location("e")));
        expectedTargetConstraints.add(getList(new Location("f"), new Location("e"), new Location("d")));
        
        assertEquals(expectedLocations, locationPermutations);
        assertEquals(expectedTargetConstraints, targetConstraintPermutations);
        
    }
    
    @Test
    public void testCreateChannelComponent() {
        Location location1 = new Location("a");
        Location location2 = new Location("b");
        
        Channel channel = new Channel("label");
        
        try {
            channel.createChannelComponent(null, getList(location1), null);
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent(null, null, getList(location1));
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent(null, getList(location1, location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent("Unknown", getList(location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        ChannelComponent component = channel.createChannelComponent(null, getList(location1), getList(location2));
        assertEquals("[a] -> [b]", component.toString());

        component = channel.createChannelComponent(null, getList(location1), getList(location1));
        assertEquals("[a] -> [a]", component.toString());
        
        // Multi compartment channels
        component = channel.createChannelComponent(null, getList(location2, location1), getList(location1, location2));
        assertEquals("[b, a] -> [a, b]", component.toString());
    }
    
    @Test
    public void testCreateChannelComponent_channelTypes() {
        checkCreateChannelComponent("Neighbour");
        checkCreateChannelComponent("EdgeNeighbour");
        checkCreateChannelComponent("FaceNeighbour");
        checkCreateChannelComponent("Hexagonal");
        checkCreateChannelComponent("Radial");
        checkCreateChannelComponent("RadialOut");
        checkCreateChannelComponent("RadialIn");
        checkCreateChannelComponent("Lateral");
    }
    
    private void checkCreateChannelComponent(String channelType) {
        Location location1 = new Location("a");
        Location location2 = new Location("b");
        Location location3 = new Location("a", INDEX_0);
        
        Channel channel = new Channel("label");
        
        try {
            channel.createChannelComponent(channelType, getList(location1), null);
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent(channelType, null, getList(location1));
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent(channelType, getList(location1, location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            channel.createChannelComponent(channelType, getList(location3), getList(location2));
            fail("non compartment locations should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        ChannelComponent component = channel.createChannelComponent(channelType, getList(location1), getList(location2));
        assertEquals("(" + channelType + ") [a] -> [b]", component.toString());

        component = channel.createChannelComponent(channelType, getList(location1), getList(location1));
        assertEquals("(" + channelType + ") [a] -> [a]", component.toString());
        
        // Multi compartment channels
        component = channel.createChannelComponent(channelType, getList(location2, location1), getList(location1, location2));
        assertEquals("(" + channelType + ") [b, a] -> [a, b]", component.toString());
    }
    
    @Test
    public void testGetChannelSubcomponents_edgeNeighbour() {
        List<Compartment> compartments = getList(new Compartment("a", 4, 4));
        
        Location location = new Location("a");

        EdgeNeighbourComponent component = new EdgeNeighbourComponent(getList(location), getList(location));
        
        List<ChannelComponent> expected = getList(
                new ChannelComponent(getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X_MINUS_1, INDEX_Y))),
                new ChannelComponent(getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X_PLUS_1, INDEX_Y))),
                new ChannelComponent(getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X, INDEX_Y_MINUS_1))),
                new ChannelComponent(getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X, INDEX_Y_PLUS_1))));

        assertEquals(expected, component.getChannelSubcomponents(compartments));
    }
}
