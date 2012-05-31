package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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
        
        try {
            new Channel(null, reference1, reference2, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Channel("name", null, reference2, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Channel("name", reference1, null, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Channel("name", reference1, reference2, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Channel channel = new Channel("label", reference1, reference2, Direction.FORWARD);
        assertEquals("label", channel.getName());
        assertSame(reference1, channel.getSourceReference());
        assertSame(reference2, channel.getTargetReference());
        assertEquals(Direction.FORWARD, channel.getDirection());
        assertEquals("label: a -> b", channel.toString());

        channel = new Channel("label", reference1, reference2, Direction.BACKWARD);
        assertEquals("label", channel.getName());
        assertSame(reference1, channel.getSourceReference());
        assertSame(reference2, channel.getTargetReference());
        assertEquals(Direction.BACKWARD, channel.getDirection());
        assertEquals("label: a <- b", channel.toString());

        channel = new Channel("label", reference1, reference2, Direction.BIDIRECTIONAL);
        assertEquals("label", channel.getName());
        assertSame(reference1, channel.getSourceReference());
        assertSame(reference2, channel.getTargetReference());
        assertEquals(Direction.BIDIRECTIONAL, channel.getDirection());
        assertEquals("label: a <-> b", channel.toString());
    }
    
    @Test
    public void testGetCellReferencePairs() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        Channel channel = new Channel("label", reference1, reference2, Direction.FORWARD);
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
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertArrayEquals(new Location[][] {
                { new Location("a"), new Location("b") },
                }, channel.getCellReferencePairs(compartments));
        
        // Single cell-cell link
        compartments.clear();
        reference1 = new Location("a", INDEX_1);
        reference2 = new Location("a", INDEX_2);
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_1), new Location("a", INDEX_2) }
                }, channel.getCellReferencePairs(compartments));

        VariableReference refX = new VariableReference("x");
        
        // Linear array
        compartments.clear();
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", INDEX_X_PLUS_1);
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);
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
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", INDEX_1), new Location("a", INDEX_0) },
                { new Location("a", INDEX_2), new Location("a", INDEX_1) },
                { new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_2) },
                }, channel.getCellReferencePairs(compartments));
        
    }


    
    @Test
    public void testCanUseChannel() {
        // TODO add complex channels here
        Channel channel = new Channel("label", new Location("a", INDEX_X), 
                new Location("a", INDEX_X_PLUS_1), Direction.FORWARD);
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("a", 4));
        compartments.add(new Compartment("b", 2));
        
        
        try {
            channel.canUseChannel(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.canUseChannel(new Location("a", INDEX_1), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("c", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1), Direction.FORWARD);
            channel2.canUseChannel(new Location("a", INDEX_1), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        assertFalse(channel.canUseChannel(new Location("b", INDEX_1), compartments));
        assertFalse(channel.canUseChannel(new Location("a"), compartments));
        assertFalse(channel.canUseChannel(new Location("a", INDEX_1, INDEX_1), compartments));
        assertFalse(channel.canUseChannel(new Location("a", INDEX_X), compartments));
        assertTrue(channel.canUseChannel(new Location("a", new CellIndexExpression("3000")), compartments));
        assertTrue(channel.canUseChannel(new Location("a", INDEX_1), compartments));
        
        channel = new Channel("label", new Location("a", INDEX_1), new Location("a", INDEX_0), Direction.FORWARD);
        assertFalse(channel.canUseChannel(new Location("a", INDEX_0), compartments));
        assertTrue(channel.canUseChannel(new Location("a", INDEX_1), compartments));
    }
    
    @Test
    public void testApplyChannel() {
        // TODO add complex channels here
        Location reference1 = new Location("a", INDEX_X);
        Location reference2 = new Location("a", INDEX_X_PLUS_1);
        Channel channel = new Channel("label", reference1, reference2, Direction.FORWARD);
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("a", 4));
        compartments.add(new Compartment("b", 2));
        
        try {
            channel.applyChannel(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            channel.applyChannel(new Location("a", INDEX_1), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            Channel channel2 = new Channel("label", new Location("c", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1), Direction.FORWARD);
            channel2.applyChannel(new Location("a", INDEX_1), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        List<Location> expected = new ArrayList<Location>();
        
        assertEquals(expected, channel.applyChannel(new Location("b", INDEX_1), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a"), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1, INDEX_1), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_X), compartments));
        assertEquals(expected, channel.applyChannel(new Location("a", new CellIndexExpression("3")), compartments));
        
        expected.add(new Location("a", INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);

        expected.clear();
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_0), compartments));

        expected.add(new Location("a", INDEX_0));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_1), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 5, 5, 5));
        
        reference1 = new Location("a", INDEX_X, INDEX_Y, INDEX_0);
        reference2 = new Location("a", INDEX_X_PLUS_1, new CellIndexExpression(INDEX_Y, Operator.MULTIPLY, INDEX_2), INDEX_2);
        channel = new Channel("label", reference1, reference2, Direction.FORWARD);

        expected.clear();
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_2, INDEX_1, INDEX_1), compartments));

        expected.add(new Location("a", new CellIndexExpression("3"), new CellIndexExpression("4"), INDEX_2));
        assertEquals(expected, channel.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_0), compartments));
    }
    
}
