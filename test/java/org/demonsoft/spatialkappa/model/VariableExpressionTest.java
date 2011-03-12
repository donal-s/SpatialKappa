package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.SimulationToken;
import org.demonsoft.spatialkappa.model.VariableExpression.Type;
import org.demonsoft.spatialkappa.model.VariableExpression.UnaryOperator;
import org.junit.Test;


public class VariableExpressionTest {

    @Test
    public void testConstructor() {
        VariableReference reference = new VariableReference("x");
        VariableExpression expr1 = new VariableExpression(reference);
        VariableExpression expr2 = new VariableExpression("2");
        VariableExpression expr3 = new VariableExpression(Constant.INFINITY);
        
        try {
            new VariableExpression((String) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression((Constant) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression((SimulationToken) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression((VariableReference) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(null, expr1);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(UnaryOperator.LOG, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression("");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        try {
            new VariableExpression("x");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        try {
            new VariableExpression(null, Operator.PLUS, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(expr1, null, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(expr1, Operator.PLUS, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        VariableExpression expr = new VariableExpression(new VariableReference("x"));
        assertEquals(reference, expr.reference);
        assertEquals(Type.VARIABLE_REFERENCE, expr.type);
        assertEquals("'x'", expr.toString());
        
        expr = new VariableExpression("2");
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("2.0", expr.toString());
        
        expr = new VariableExpression(Constant.INFINITY);
        assertEquals(Type.CONSTANT, expr.type);
        assertEquals("[inf]", expr.toString());

        expr = new VariableExpression(SimulationToken.EVENTS);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[E]", expr.toString());

        expr = new VariableExpression("2.55e4");
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("25500.0", expr.toString());
        
        expr = new VariableExpression(expr1, Operator.PLUS, expr2);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("('x' + 2.0)", expr.toString());
        
        expr = new VariableExpression(expr2, Operator.PLUS, expr2);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("(2.0 + 2.0)", expr.toString());
        
        expr = new VariableExpression(expr1, Operator.PLUS, expr3);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("('x' + [inf])", expr.toString());
        
        expr = new VariableExpression(expr2, Operator.PLUS, expr3);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("(2.0 + [inf])", expr.toString());
        
        expr = new VariableExpression(UnaryOperator.LOG, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[log] ('x')", expr.toString());
        
        expr = new VariableExpression(UnaryOperator.SIN, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[sin] ('x')", expr.toString());
        
        expr = new VariableExpression(UnaryOperator.COS, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[cos] ('x')", expr.toString());
        
        expr = new VariableExpression(UnaryOperator.TAN, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[tan] ('x')", expr.toString());
        
        expr = new VariableExpression(UnaryOperator.SQRT, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[sqrt] ('x')", expr.toString());
    }
    
    @Test
    public void testIsFixed() {
        Map<String, Variable> variables = new HashMap<String, Variable>();
        
        VariableExpression expr = new VariableExpression("2");
        try {
            expr.isFixed(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        assertTrue(expr.isFixed(variables));

        assertTrue(new VariableExpression("2.55e4").isFixed(variables));
        assertTrue(new VariableExpression(Constant.INFINITY).isFixed(variables));
        assertFalse(new VariableExpression(SimulationToken.EVENTS).isFixed(variables));
        
        expr = new VariableExpression(new VariableExpression("2"), Operator.PLUS, new VariableExpression("3"));
        assertTrue(expr.isFixed(variables));

        expr = new VariableExpression(new VariableExpression("2"), Operator.MULTIPLY, new VariableExpression(SimulationToken.TIME));
        assertFalse(expr.isFixed(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression("3"));
        assertTrue(expr.isFixed(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression(SimulationToken.TIME));
        assertFalse(expr.isFixed(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression(Constant.INFINITY));
        assertTrue(expr.isFixed(variables));
        
        
        expr = new VariableExpression(new VariableReference("x"));
        try {
            expr.isFixed(variables);
            fail("missing reference should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        variables.put("x", new Variable(new VariableExpression("2"), "x"));
        assertTrue(expr.isFixed(variables));
        
        variables.put("x", new Variable(new VariableExpression(Constant.INFINITY), "x"));
        assertTrue(expr.isFixed(variables));
        
        variables.put("x", new Variable(new VariableExpression(SimulationToken.TIME), "x"));
        assertFalse(expr.isFixed(variables));
        
        variables.put("x", new Variable(new VariableExpression(new VariableReference("y")), "x"));
        try {
            expr.isFixed(variables);
            fail("missing reference should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        variables.put("y", new Variable(new VariableExpression("2"), "y"));
        assertTrue(expr.isFixed(variables));
        
        variables.put("y", new Variable(new VariableExpression(Constant.INFINITY), "y"));
        assertTrue(expr.isFixed(variables));
        
        variables.put("y", new Variable(new VariableExpression(SimulationToken.TIME), "y"));
        assertFalse(expr.isFixed(variables));
    }
    
    @Test
    public void testIsInfinite() {
        Map<String, Variable> variables = new HashMap<String, Variable>();
        
        VariableExpression expr = new VariableExpression("2");
        try {
            expr.isInfinite(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        assertFalse(expr.isInfinite(variables));

        assertFalse(new VariableExpression("2.55e4").isInfinite(variables));
        assertTrue(new VariableExpression(Constant.INFINITY).isInfinite(variables));
        assertFalse(new VariableExpression(Constant.PI).isInfinite(variables));
        assertFalse(new VariableExpression(SimulationToken.EVENTS).isInfinite(variables));
        
        expr = new VariableExpression(new VariableExpression("2"), Operator.PLUS, new VariableExpression("3"));
        assertFalse(expr.isInfinite(variables));

        expr = new VariableExpression(new VariableExpression("2"), Operator.MULTIPLY, new VariableExpression(SimulationToken.TIME));
        assertFalse(expr.isInfinite(variables));

        expr = new VariableExpression(new VariableExpression("2"), Operator.MULTIPLY, new VariableExpression(Constant.INFINITY));
        assertTrue(expr.isInfinite(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression("3"));
        assertFalse(expr.isInfinite(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression(SimulationToken.TIME));
        assertFalse(expr.isInfinite(variables));

        expr = new VariableExpression(UnaryOperator.LOG, new VariableExpression(Constant.INFINITY));
        assertTrue(expr.isInfinite(variables));
        
        
        expr = new VariableExpression(new VariableReference("x"));
        try {
            expr.isInfinite(variables);
            fail("missing reference should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        variables.put("x", new Variable(new VariableExpression("2"), "x"));
        assertFalse(expr.isInfinite(variables));
        
        variables.put("x", new Variable(new VariableExpression(Constant.INFINITY), "x"));
        assertTrue(expr.isInfinite(variables));
        
        variables.put("x", new Variable(new VariableExpression(SimulationToken.TIME), "x"));
        assertFalse(expr.isInfinite(variables));
        
        variables.put("x", new Variable(new VariableExpression(new VariableReference("y")), "x"));
        try {
            expr.isInfinite(variables);
            fail("missing reference should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        variables.put("y", new Variable(new VariableExpression("2"), "y"));
        assertFalse(expr.isInfinite(variables));
        
        variables.put("y", new Variable(new VariableExpression(Constant.INFINITY), "y"));
        assertTrue(expr.isInfinite(variables));
        
        variables.put("y", new Variable(new VariableExpression(SimulationToken.TIME), "y"));
        assertFalse(expr.isInfinite(variables));
    }
    
    @Test
    public void testEvaluate() {
        VariableReference referenceX = new VariableReference("x");
        VariableReference referenceY = new VariableReference("y");
        VariableExpression expr = new VariableExpression(referenceX);
        Map<String, Variable> variables = new HashMap<String, Variable>();
        SimulationState state = new ModelOnlySimulationState(variables);
        
        try {
            expr.evaluate(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluate(state);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("y", new Variable(new VariableExpression(5), "y"));
        try {
            expr.evaluate(state);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new VariableExpression(Constant.INFINITY).evaluate(state);
            fail("evaluating infinity should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        variables.put("x", new Variable(new VariableExpression(12), "x"));
        variables.put("y", new Variable(new VariableExpression(3), "y"));

        assertEquals(2.0f, new VariableExpression("2").evaluate(state).value);
        assertEquals(9.0f, new VariableExpression(new VariableExpression("7"), Operator.PLUS, new VariableExpression("2")).evaluate(state).value);
        assertEquals(5.0f, new VariableExpression(new VariableExpression("7"), Operator.MINUS, new VariableExpression("2")).evaluate(state).value);
        assertEquals(14.0f, new VariableExpression(new VariableExpression("7"), Operator.MULTIPLY, new VariableExpression("2")).evaluate(state).value);
        assertEquals(3.5f, new VariableExpression(new VariableExpression("7"), Operator.DIVIDE, new VariableExpression("2")).evaluate(state).value);
        assertEquals(0.0f, new VariableExpression(new VariableExpression("0"), Operator.MODULUS, new VariableExpression("5")).evaluate(state).value);
        assertEquals(2.0f, new VariableExpression(new VariableExpression("7"), Operator.MODULUS, new VariableExpression("5")).evaluate(state).value);
        assertEquals(8.0f, new VariableExpression(new VariableExpression("2"), Operator.POWER, new VariableExpression("3")).evaluate(state).value);
        
        assertEquals(12.0f, new VariableExpression(referenceX).evaluate(state).value);
        assertEquals(15.0f, new VariableExpression(new VariableExpression(referenceX), Operator.PLUS, new VariableExpression(referenceY)).evaluate(state).value);
        assertEquals(9.0f, new VariableExpression(new VariableExpression(referenceX), Operator.MINUS, new VariableExpression(referenceY)).evaluate(state).value);
        assertEquals(36.0f, new VariableExpression(new VariableExpression(referenceX), Operator.MULTIPLY, new VariableExpression(referenceY)).evaluate(state).value);
        assertEquals(4.0f, new VariableExpression(new VariableExpression(referenceX), Operator.DIVIDE, new VariableExpression(referenceY)).evaluate(state).value);

        variables.put("y", new Variable(new VariableExpression(100), "y"));
        assertEquals(4.61f, new VariableExpression(UnaryOperator.LOG, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);
        assertEquals(-0.51f, new VariableExpression(UnaryOperator.SIN, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);
        assertEquals(0.86f, new VariableExpression(UnaryOperator.COS, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);
        assertEquals(-0.59f, new VariableExpression(UnaryOperator.TAN, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);
        assertEquals(10.0f, new VariableExpression(UnaryOperator.SQRT, new VariableExpression(referenceY)).evaluate(state).value);
        
        variables.put("y", new Variable(new VariableExpression(3), "y"));
        assertEquals(20.09f, new VariableExpression(UnaryOperator.EXP, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);

        // TODO handle simulation tokens
    }
    
}
