package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.junit.Test;

public class ChannelTest {

    @SuppressWarnings("unused")
    @Test
    public void testChannel() {
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

        try {
            new Channel("name", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Channel("name", locations);
            fail("empty locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Channel channel = new Channel("label", reference1, reference2);
        assertEquals("label", channel.getName());
        assertEquals("label: a -> b", channel.toString());

        locations.add(new Location[] {reference1, reference2});
        channel = new Channel("label", locations);
        assertEquals("label", channel.getName());
        assertEquals("label: a -> b", channel.toString());

        locations.add(new Location[] {reference1, reference1});
        channel = new Channel("label", locations);
        assertEquals("label", channel.getName());
        assertEquals("label: (a -> b) + (a -> a)", channel.toString());
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

        // TODO compartment link range out of bounds ?
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
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            channel.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
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
        List<Location[]> locations = new ArrayList<Location[]>();
        locations.add(new Location[] {new Location("a", INDEX_X), new Location("a", INDEX_X_PLUS_1)});
        locations.add(new Location[] {new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_0)});
        channel = new Channel("label", locations);
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_0), new Location("a", INDEX_1) },
                { new Location("a", INDEX_1), new Location("a", INDEX_2) },
                { new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3")) },
                { new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_0) },
                }, channel.getCellReferencePairs(compartments));
    }


    
    @Test
    public void testApplyChannel() {
        Location reference1 = new Location("a", INDEX_X);
        Location reference2 = new Location("a", INDEX_X_PLUS_1);
        Channel channel = new Channel("label", reference1, reference2);
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("a", 4));
        compartments.add(new Compartment("b", 2));
        
        try {
            channel.applyChannel(null, new Location("a"), compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.applyChannel(new Location("a", INDEX_1), new Location("a"), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("a", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1));
            channel2.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("c", INDEX_X), 
                    new Location("a", INDEX_X_PLUS_1));
            channel2.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
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
        
        List<Location[]> locations = new ArrayList<Location[]>();
        locations.add(new Location[] {new Location("a", new CellIndexExpression("100")), new Location("a", new CellIndexExpression("101"))});
        locations.add(new Location[] {new Location("a", INDEX_X), new Location("a", INDEX_X_MINUS_1)});
        locations.add(new Location[] {new Location("a", INDEX_X), new Location("a", INDEX_X_PLUS_1)});
        channel = new Channel("name", locations);
        
        compartments.clear();
        compartments.add(new Compartment("a", 4));

        expected.clear();
        expected.add(new Location("a", INDEX_0));
        expected.add(new Location("a", INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));

        // Ensure each location pair are processed individually
        locations.clear();
        locations.add(new Location[] {new Location("a"), new Location("b")});
        locations.add(new Location[] {new Location("b"), new Location("a")});
        channel = new Channel("name", locations);
        compartments.clear();
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));

        expected.clear();
        expected.add(new Location("b"));
        assertEquals(expected, channel.applyChannel(new Location("a"), NOT_LOCATED, compartments));
    }
}
