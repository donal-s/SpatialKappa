package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.junit.Test;

public class CompartmentLinkTest {

    @SuppressWarnings("unused")
    @Test
    public void testCompartmentLink() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        
        try {
            new CompartmentLink(null, reference1, reference2, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new CompartmentLink("name", null, reference2, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new CompartmentLink("name", reference1, null, Direction.FORWARD);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new CompartmentLink("name", reference1, reference2, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        CompartmentLink compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        assertEquals("label", compartmentLink.getName());
        assertSame(reference1, compartmentLink.getSourceReference());
        assertSame(reference2, compartmentLink.getTargetReference());
        assertEquals(Direction.FORWARD, compartmentLink.getDirection());
        assertEquals("label: a -> b", compartmentLink.toString());

        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.BACKWARD);
        assertEquals("label", compartmentLink.getName());
        assertSame(reference1, compartmentLink.getSourceReference());
        assertSame(reference2, compartmentLink.getTargetReference());
        assertEquals(Direction.BACKWARD, compartmentLink.getDirection());
        assertEquals("label: a <- b", compartmentLink.toString());

        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.BIDIRECTIONAL);
        assertEquals("label", compartmentLink.getName());
        assertSame(reference1, compartmentLink.getSourceReference());
        assertSame(reference2, compartmentLink.getTargetReference());
        assertEquals(Direction.BIDIRECTIONAL, compartmentLink.getDirection());
        assertEquals("label: a <-> b", compartmentLink.toString());
    }
    
    @Test
    public void testGetCellReferencePairs() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        CompartmentLink compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            compartmentLink.getCellReferencePairs(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        compartments.add(new Compartment("a"));
        
        try {
            compartmentLink.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            compartmentLink.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        compartments.add(new Compartment("b"));

        try {
            compartmentLink.getCellReferencePairs(compartments);
            fail("dimension mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Single cell compartments
        compartments.clear();
        reference1 = new Location("a");
        reference2 = new Location("b");
        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertArrayEquals(new Location[][] {
                { new Location("a"), new Location("b") },
                }, compartmentLink.getCellReferencePairs(compartments));
        
        // Single cell-cell link
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression("1"));
        reference2 = new Location("a", new CellIndexExpression("2"));
        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", new CellIndexExpression("1")), new Location("a", new CellIndexExpression("2")) }
                }, compartmentLink.getCellReferencePairs(compartments));

        VariableReference refX = new VariableReference("x");
        
        // Linear array
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression(refX));
        reference2 = new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression("1")));
        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", new CellIndexExpression("0")), new Location("a", new CellIndexExpression("1")) },
                { new Location("a", new CellIndexExpression("1")), new Location("a", new CellIndexExpression("2")) },
                { new Location("a", new CellIndexExpression("2")), new Location("a", new CellIndexExpression("3")) },
                }, compartmentLink.getCellReferencePairs(compartments));

        // Linear array negative
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression(refX));
        reference2 = new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.MINUS, new CellIndexExpression("1")));
        compartmentLink = new CompartmentLink("label", reference1, reference2, Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new Location[][] {
                { new Location("a", new CellIndexExpression("1")), new Location("a", new CellIndexExpression("0")) },
                { new Location("a", new CellIndexExpression("2")), new Location("a", new CellIndexExpression("1")) },
                { new Location("a", new CellIndexExpression("3")), new Location("a", new CellIndexExpression("2")) },
                }, compartmentLink.getCellReferencePairs(compartments));
        
    }


}
