package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

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
        assertEquals("label: [[a -> b]]", channel.toString());

        channel.addChannelComponent(null, getList(reference1), getList(reference1));
        assertEquals("label: ([[a -> b]]) + ([[a -> a]])", channel.toString());
        
        // Multi compartment channels
        channel.addChannelComponent(null, getList(reference2, reference1), getList(reference1, reference2));
        assertEquals("label: ([[a -> b]]) + ([[a -> a]]) + ([[b -> a], [a -> b]])", channel.toString());
        
        // Channel types
        channel.addChannelComponent("Hexagonal", getList(reference1), getList(reference1));
        assertEquals("label: ([[a -> b]]) + ([[a -> a]]) + ([[b -> a], [a -> b]]) + ((Hexagonal) [[a -> a]])", channel.toString());
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
        assertEquals("label: [[a -> b]]", channel.toString());
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
        
        assertEquals(getList(new ChannelConstraint(new Location("a"), new Location("b"))), 
                channel.getCellReferencePairs(compartments));
        
        // Single cell-cell link
        compartments.clear();
        reference1 = new Location("a", INDEX_1);
        reference2 = new Location("a", INDEX_2);
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2))), 
                channel.getCellReferencePairs(compartments));

        VariableReference refX = new VariableReference("x");
        
        // Linear array
        compartments.clear();
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", INDEX_X_PLUS_1);
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(
                new ChannelConstraint(new Location("a", INDEX_0), new Location("a", INDEX_1)),
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2)),
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3")))),
                channel.getCellReferencePairs(compartments));

        // Linear array negative
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression(refX));
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        channel = new Channel("label", reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_0)),
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", INDEX_1)),
                new ChannelConstraint(new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_2))),
                channel.getCellReferencePairs(compartments));
        
        // Multiple components
        channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_PLUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", new CellIndexExpression("3"))), getList(new Location("a", INDEX_0)));
        
        assertEquals(getList(
                new ChannelConstraint(new Location("a", INDEX_0), new Location("a", INDEX_1)),
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2)),
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3"))),
                new ChannelConstraint(new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_0))),
                channel.getCellReferencePairs(compartments));
        
        // TODO channel type tests
    }


    @Test
    public void testApplyChannel_singleCompartment() {
        Location reference1 = new Location("a", INDEX_X);
        Location reference2 = new Location("a", INDEX_X_PLUS_1);
        Channel channel = new Channel("label", reference1, reference2);
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        
        try {
            channel.applyChannel(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), new Location("a"))), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("a", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1));
            channel2.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("c", INDEX_X), 
                    new Location("a", INDEX_X_PLUS_1));
            channel2.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        List<Location> expected = new ArrayList<Location>();

        assertEquals(expected, channel.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, channel.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", new CellIndexExpression("3")), NOT_LOCATED, compartments));
        
        expected.add(new Location("a", INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), new Location("a"), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), new Location("a", INDEX_2), compartments));
        expected.clear();
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), new Location("b"), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), new Location("a", INDEX_0), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        channel = new Channel("label", reference1, reference2);

        expected.clear();
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_0), NOT_LOCATED, compartments));

        expected.add(new Location("a", INDEX_0));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 5, 5, 5));
        
        reference1 = new Location("a", INDEX_X, INDEX_Y, INDEX_0);
        reference2 = new Location("a", INDEX_X_PLUS_1, new CellIndexExpression(INDEX_Y, Operator.MULTIPLY, INDEX_2), INDEX_2);
        channel = new Channel("label", reference1, reference2);

        expected.clear();
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_2, INDEX_1, INDEX_1), NOT_LOCATED, compartments));

        expected.add(new Location("a", new CellIndexExpression("3"), new CellIndexExpression("4"), INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_0), NOT_LOCATED, compartments));
        
        channel = new Channel("name");
        channel.addChannelComponent(null, getList(new Location("a", new CellIndexExpression("100"))), getList(new Location("a", new CellIndexExpression("101"))));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_MINUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X)), getList(new Location("a", INDEX_X_PLUS_1)));
        
        compartments.clear();
        compartments.add(new Compartment("a", 4));

        expected.clear();
        expected.add(new Location("a", INDEX_0));
        expected.add(new Location("a", INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));

        // Ensure each location pair are processed individually
        channel = new Channel("name");
        channel.addChannelComponent(null, getList(new Location("a")), getList(new Location("b")));
        channel.addChannelComponent(null, getList(new Location("b")), getList(new Location("a")));
        compartments.clear();
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));

        expected.clear();
        expected.add(new Location("b"));
        assertEquals(expected, channel.applyChannel(new Location("a"), NOT_LOCATED, compartments));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testApplyChannel_multipleComponents() {
        Channel channel = new Channel("label");
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_X_PLUS_1)));
        channel.addChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_MINUS_1), new Location("b", INDEX_X_MINUS_1)));

        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 3));
        List<List<Location>> results = channel.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_1), NOT_LOCATED)), compartments);
        List<List<Location>> expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)),
                getList(new Location("a", INDEX_0), new Location("b", INDEX_0)));
        assertEquals(expected, results);
        
        results = channel.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_0), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_0), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_1), new Location("b", INDEX_1)));
        assertEquals(expected, results);
    }
    
    
        // TODO check target locations are unique set
    
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
    
}
