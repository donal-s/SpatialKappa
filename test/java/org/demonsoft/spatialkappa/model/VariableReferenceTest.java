package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.junit.Test;


public class VariableReferenceTest {

    @SuppressWarnings("unused")
    @Test
    public void testConstructor() {
        try {
            new VariableReference(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableReference("");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        VariableReference reference = new VariableReference("x");
        assertEquals("x", reference.variableName);
        assertEquals("'x'", reference.toString());
    }
}
