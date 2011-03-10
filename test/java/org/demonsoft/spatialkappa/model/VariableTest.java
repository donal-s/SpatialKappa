package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import org.demonsoft.spatialkappa.model.Variable.Type;
import org.junit.Test;

public class VariableTest {

    @Test
    public void testVariable_variableExpression() {
        VariableExpression expression = new VariableExpression(new VariableReference("x"));
        
        try {
            new Variable(null, "label");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Variable(expression, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Variable variable = new Variable(expression, "label");
        assertEquals("label", variable.label);
        assertSame(expression, variable.expression);
        assertNull(variable.location);
        assertNull(variable.complex);
        assertEquals("'label' ('x')", variable.toString());
        assertSame(Type.VARIABLE_EXPRESSION, variable.type);
 
        variable = new Variable(expression, "label");
        assertEquals("label", variable.label);
        assertSame(expression, variable.expression);
        assertNull(variable.location);
        assertNull(variable.complex);
        assertEquals("'label' ('x')", variable.toString());
        assertSame(Type.VARIABLE_EXPRESSION, variable.type);
    }

    @Test
    public void testVariable_kappaExpression() {
        Complex complex = new Complex(new Agent("agent1"));
        Location location = new Location("cytosol", new MathExpression("2"));
        
        try {
            new Variable(null, location, "label");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Variable(complex, location, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Variable variable = new Variable(complex, location, "label");
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertSame(location, variable.location);
        assertSame(complex, variable.complex);
        assertEquals("'label' cytosol[2] ([agent1()])", variable.toString());
        assertSame(Type.KAPPA_EXPRESSION, variable.type);
 
        variable = new Variable(complex, null, "label");
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertNull(variable.location);
        assertSame(complex, variable.complex);
        assertEquals("'label' ([agent1()])", variable.toString());
        assertSame(Type.KAPPA_EXPRESSION, variable.type);
    }

    @Test
    public void testVariable_transition() {
        try {
            new Variable(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Variable variable = new Variable("label");
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertNull(variable.location);
        assertNull(variable.complex);
        assertEquals("'label'", variable.toString());
        assertSame(Type.TRANSITION_LABEL, variable.type);
    }

}
