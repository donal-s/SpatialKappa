package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.*;

import java.util.HashMap;
import java.util.Map;


import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.MathExpression.Operator;
import org.junit.Test;


public class MathExpressionTest {

    @Test
    public void testConstructor() {
        MathExpression expr1 = new MathExpression("x");
        MathExpression expr2 = new MathExpression("2");
        
        try {
            new MathExpression(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new MathExpression("");
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        try {
            new MathExpression(null, Operator.PLUS, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new MathExpression(expr1, null, expr2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new MathExpression(expr1, Operator.PLUS, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        MathExpression expr = new MathExpression("x");
        assertEquals(null, expr.getOperator());
        assertEquals("x", expr.getVariable());
        assertEquals(0, expr.getValue());
        assertEquals(null, expr.getLhsExpression());
        assertEquals(null, expr.getRhsExpression());
        assertEquals("'x'", expr.toString());
        assertFalse(expr.isConcrete());
        
        expr = new MathExpression("2");
        assertEquals(null, expr.getOperator());
        assertEquals(null, expr.getVariable());
        assertEquals(2, expr.getValue());
        assertEquals(null, expr.getLhsExpression());
        assertEquals(null, expr.getRhsExpression());
        assertEquals("2", expr.toString());
        assertTrue(expr.isConcrete());
        
        expr = new MathExpression(expr1, Operator.PLUS, expr2);
        assertEquals(Operator.PLUS, expr.getOperator());
        assertEquals(null, expr.getVariable());
        assertEquals(0, expr.getValue());
        assertEquals(expr1, expr.getLhsExpression());
        assertEquals(expr2, expr.getRhsExpression());
        assertEquals("('x' + 2)", expr.toString());
        assertFalse(expr.isConcrete());
        
        expr = new MathExpression(expr2, Operator.PLUS, expr2);
        assertEquals(Operator.PLUS, expr.getOperator());
        assertEquals(null, expr.getVariable());
        assertEquals(0, expr.getValue());
        assertEquals(expr2, expr.getLhsExpression());
        assertEquals(expr2, expr.getRhsExpression());
        assertEquals("(2 + 2)", expr.toString());
        assertTrue(expr.isConcrete());
        
    }
    
    @Test
    public void testEvaluate() {
        MathExpression expr = new MathExpression("x");
        Map<String, Integer> variables = new HashMap<String, Integer>();
        
        try {
            expr.evaluate(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluate(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("y", 5);
        try {
            expr.evaluate(variables);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        variables.put("x", 12);
        variables.put("y", 3);

        assertEquals(2, new MathExpression("2").evaluate(variables));
        assertEquals(9, new MathExpression(new MathExpression("7"), Operator.PLUS, new MathExpression("2")).evaluate(variables));
        assertEquals(5, new MathExpression(new MathExpression("7"), Operator.MINUS, new MathExpression("2")).evaluate(variables));
        assertEquals(14, new MathExpression(new MathExpression("7"), Operator.MULTIPLY, new MathExpression("2")).evaluate(variables));
        assertEquals(3, new MathExpression(new MathExpression("7"), Operator.DIVIDE, new MathExpression("2")).evaluate(variables));
        assertEquals(49, new MathExpression(new MathExpression("7"), Operator.EXPONENT, new MathExpression("2")).evaluate(variables));
        assertEquals(0, new MathExpression(new MathExpression("0"), Operator.MODULUS, new MathExpression("5")).evaluate(variables));
        assertEquals(2, new MathExpression(new MathExpression("7"), Operator.MODULUS, new MathExpression("5")).evaluate(variables));
        
        assertEquals(12, new MathExpression("x").evaluate(variables));
        assertEquals(15, new MathExpression(new MathExpression("x"), Operator.PLUS, new MathExpression("y")).evaluate(variables));
        assertEquals(9, new MathExpression(new MathExpression("x"), Operator.MINUS, new MathExpression("y")).evaluate(variables));
        assertEquals(36, new MathExpression(new MathExpression("x"), Operator.MULTIPLY, new MathExpression("y")).evaluate(variables));
        assertEquals(4, new MathExpression(new MathExpression("x"), Operator.DIVIDE, new MathExpression("y")).evaluate(variables));
    }
    
}
