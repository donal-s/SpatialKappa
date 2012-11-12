package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.*;

import java.util.HashMap;
import java.util.Map;


import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.Type;
import org.junit.Test;


public class CellIndexExpressionTest {

    @SuppressWarnings("unused")
    @Test
    public void testConstructor() {
        CellIndexExpression expr1 = new CellIndexExpression(new VariableReference("x"));
        CellIndexExpression expr2 = new CellIndexExpression("2");
        
        try {
            new CellIndexExpression((String) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression((VariableReference) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression("");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression("a");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression(null, Operator.PLUS, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression(expr1, null, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new CellIndexExpression(expr1, Operator.PLUS, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        CellIndexExpression expr = new CellIndexExpression(new VariableReference("x"));
        assertEquals(Type.VARIABLE_REFERENCE, expr.type);
        assertEquals("x", expr.toString());
        assertFalse(expr.isFixed());
        
        expr = new CellIndexExpression("2");
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("2", expr.toString());
        assertTrue(expr.isFixed());
        
        expr = new CellIndexExpression(expr1, Operator.PLUS, expr2);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("(x + 2)", expr.toString());
        assertFalse(expr.isFixed());
        
        expr = new CellIndexExpression(expr2, Operator.PLUS, expr2);
        assertEquals(Type.BINARY_EXPRESSION, expr.type);
        assertEquals("(2 + 2)", expr.toString());
        assertTrue(expr.isFixed());
    }
    
    @Test
    public void testWildcard() {
        assertEquals(Type.NUMBER, CellIndexExpression.WILDCARD.type);
        assertEquals("?", CellIndexExpression.WILDCARD.toString());
        assertTrue(CellIndexExpression.WILDCARD.isFixed());
    }
    
    @Test
    public void testGetDeltaIndex() {
        CellIndexExpression expr = new CellIndexExpression(new VariableReference("x"));
        assertEquals("(x + 1)", expr.getDeltaIndex(1).toString());
        assertSame(expr, expr.getDeltaIndex(0));
        assertEquals("(x - 2)", expr.getDeltaIndex(-2).toString());
        
        expr = new CellIndexExpression("3");
        assertEquals("4", expr.getDeltaIndex(1).toString());
        assertSame(expr, expr.getDeltaIndex(0));
        assertEquals("1", expr.getDeltaIndex(-2).toString());
    }
    
    @Test
    public void testEvaluateIndex() {
        VariableReference refX = new VariableReference("x");
        VariableReference refY = new VariableReference("y");
        CellIndexExpression expr = new CellIndexExpression(refX);
        Map<String, Integer> variables = new HashMap<String, Integer>();
        
        try {
            expr.evaluateIndex(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluateIndex(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("y", 5);
        try {
            expr.evaluateIndex(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        variables.put("x", 12);
        variables.put("y", 3);

        assertEquals(2, new CellIndexExpression("2").evaluateIndex(variables));
        assertEquals(9, new CellIndexExpression(new CellIndexExpression("7"), Operator.PLUS, new CellIndexExpression("2")).evaluateIndex(variables));
        assertEquals(5, new CellIndexExpression(new CellIndexExpression("7"), Operator.MINUS, new CellIndexExpression("2")).evaluateIndex(variables));
        assertEquals(14, new CellIndexExpression(new CellIndexExpression("7"), Operator.MULTIPLY, new CellIndexExpression("2")).evaluateIndex(variables));
        assertEquals(3, new CellIndexExpression(new CellIndexExpression("7"), Operator.DIVIDE, new CellIndexExpression("2")).evaluateIndex(variables));
        assertEquals(49, new CellIndexExpression(new CellIndexExpression("7"), Operator.POWER, new CellIndexExpression("2")).evaluateIndex(variables));
        assertEquals(0, new CellIndexExpression(new CellIndexExpression("0"), Operator.MODULUS, new CellIndexExpression("5")).evaluateIndex(variables));
        assertEquals(2, new CellIndexExpression(new CellIndexExpression("7"), Operator.MODULUS, new CellIndexExpression("5")).evaluateIndex(variables));
        
        assertEquals(12, new CellIndexExpression(refX).evaluateIndex(variables));
        assertEquals(15, new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression(refY)).evaluateIndex(variables));
        assertEquals(9, new CellIndexExpression(new CellIndexExpression(refX), Operator.MINUS, new CellIndexExpression(refY)).evaluateIndex(variables));
        assertEquals(36, new CellIndexExpression(new CellIndexExpression(refX), Operator.MULTIPLY, new CellIndexExpression(refY)).evaluateIndex(variables));
        assertEquals(4, new CellIndexExpression(new CellIndexExpression(refX), Operator.DIVIDE, new CellIndexExpression(refY)).evaluateIndex(variables));
    }
    
}
