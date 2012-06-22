package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class InitialValueTest {

    @SuppressWarnings("unused")
    @Test
    public void testInitialValue_value() {
        List<Complex> complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new AgentSite("x", null, "1")), 
                new Agent("C", new AgentSite("y", null, "1"))));
        Location location = new Location("cytosol", new CellIndexExpression("2"));

        try {
            new InitialValue(null, 5, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(new ArrayList<Complex>(), 5, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, 0, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        InitialValue initialValue = new InitialValue(complexes, 5, location);
        assertEquals("[[A], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertNull(initialValue.reference);
        assertEquals(location, initialValue.location);

        initialValue = new InitialValue(complexes, 5, null);
        assertEquals("[[A], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertNull(initialValue.reference);
        assertEquals(null, initialValue.location);
    }

    @SuppressWarnings("unused")
    @Test
    public void testInitialValue_reference() {
        List<Complex> complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new AgentSite("x", null, "1")), 
                new Agent("C", new AgentSite("y", null, "1"))));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        VariableReference reference = new VariableReference("variable");

        try {
            new InitialValue(null, reference, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(new ArrayList<Complex>(), reference, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        InitialValue initialValue = new InitialValue(complexes, reference, location);
        assertEquals("[[A], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(0, initialValue.quantity);
        assertSame(reference, initialValue.reference);
        assertEquals(location, initialValue.location);

        initialValue = new InitialValue(complexes, reference, null);
        assertEquals("[[A], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(0, initialValue.quantity);
        assertSame(reference, initialValue.reference);
        assertEquals(null, initialValue.location);
    }


}
