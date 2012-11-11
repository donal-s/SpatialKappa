package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.demonsoft.spatialkappa.model.BooleanExpression.Operator;
import org.demonsoft.spatialkappa.model.BooleanExpression.RelationalOperator;
import org.demonsoft.spatialkappa.model.BooleanExpression.Type;
import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.junit.Test;

public class BooleanExpressionTest {

    @SuppressWarnings("unused")
    @Test
    public void testBooleanExpression() {
        VariableExpression expr1 = new VariableExpression(2);
        VariableExpression expr2 = new VariableExpression(3);
        BooleanExpression boolExpr1 = new BooleanExpression(true);
        BooleanExpression boolExpr2 = new BooleanExpression(false);
        
        try {
            new BooleanExpression(null, expr1, expr2);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(RelationalOperator.GREATER, null, expr2);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(RelationalOperator.GREATER, expr1, null);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(null, boolExpr1);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(Operator.NOT, null);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(Operator.AND, boolExpr1);
            fail("Invalid operator should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(null, boolExpr1, boolExpr2);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(Operator.AND, null, boolExpr2);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(Operator.AND, boolExpr1, null);
            fail("Null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new BooleanExpression(Operator.NOT, boolExpr1, boolExpr2);
            fail("Invalid operator should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        

        BooleanExpression expression = new BooleanExpression(true);
        assertEquals(Type.VALUE, expression.type);
        assertEquals("[true]", expression.toString());

        expression = new BooleanExpression(RelationalOperator.GREATER, expr1, expr2);
        assertEquals(Type.VARIABLE_RELATION, expression.type);
        assertEquals("(2.0 > 3.0)", expression.toString());

        expression = new BooleanExpression(RelationalOperator.LESS, expr1, expr2);
        assertEquals(Type.VARIABLE_RELATION, expression.type);
        assertEquals("(2.0 < 3.0)", expression.toString());

        expression = new BooleanExpression(RelationalOperator.EQUAL, expr1, expr2);
        assertEquals(Type.VARIABLE_RELATION, expression.type);
        assertEquals("(2.0 = 3.0)", expression.toString());

        expression = new BooleanExpression(RelationalOperator.NOT_EQUAL, expr1, expr2);
        assertEquals(Type.VARIABLE_RELATION, expression.type);
        assertEquals("(2.0 <> 3.0)", expression.toString());

        expression = new BooleanExpression(Operator.AND, boolExpr1, boolExpr2);
        assertEquals(Type.BOOLEAN_RELATION, expression.type);
        assertEquals("([true] && [false])", expression.toString());

        expression = new BooleanExpression(Operator.NOT, boolExpr1);
        assertEquals(Type.NEGATION, expression.type);
        assertEquals("[not] [true]", expression.toString());
    }

    @Test
    public void testEvaluate() {
        VariableReference referenceX = new VariableReference("x");
        VariableExpression expr1 = new VariableExpression(referenceX);
        VariableExpression expr2 = new VariableExpression(3);

        BooleanExpression expr = new BooleanExpression(RelationalOperator.EQUAL, expr1, expr2);
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
        
        BooleanExpression exprTrue = new BooleanExpression(true);
        BooleanExpression exprFalse = new BooleanExpression(false);
        
        assertTrue(exprTrue.evaluate(state));
        assertFalse(exprFalse.evaluate(state));
        
        assertTrue(new BooleanExpression(Operator.NOT, exprFalse).evaluate(state));
        assertFalse(new BooleanExpression(Operator.NOT, exprTrue).evaluate(state));

        assertFalse(new BooleanExpression(Operator.AND, exprFalse, exprFalse).evaluate(state));
        assertFalse(new BooleanExpression(Operator.AND, exprFalse, exprTrue).evaluate(state));
        assertFalse(new BooleanExpression(Operator.AND, exprTrue, exprFalse).evaluate(state));
        assertTrue(new BooleanExpression(Operator.AND, exprTrue, exprTrue).evaluate(state));
        
        assertFalse(new BooleanExpression(Operator.OR, exprFalse, exprFalse).evaluate(state));
        assertTrue(new BooleanExpression(Operator.OR, exprFalse, exprTrue).evaluate(state));
        assertTrue(new BooleanExpression(Operator.OR, exprTrue, exprFalse).evaluate(state));
        assertTrue(new BooleanExpression(Operator.OR, exprTrue, exprTrue).evaluate(state));
        
        variables.put("x", new Variable(new VariableExpression(5), "x"));
        assertTrue(new BooleanExpression(RelationalOperator.GREATER, expr1, new VariableExpression(4)).evaluate(state));
        assertFalse(new BooleanExpression(RelationalOperator.GREATER, expr1, new VariableExpression(5)).evaluate(state));
        assertFalse(new BooleanExpression(RelationalOperator.GREATER, expr1, new VariableExpression(6)).evaluate(state));

        assertFalse(new BooleanExpression(RelationalOperator.EQUAL, expr1, new VariableExpression(4)).evaluate(state));
        assertTrue(new BooleanExpression(RelationalOperator.EQUAL, expr1, new VariableExpression(5)).evaluate(state));
        assertFalse(new BooleanExpression(RelationalOperator.EQUAL, expr1, new VariableExpression(6)).evaluate(state));

        assertTrue(new BooleanExpression(RelationalOperator.NOT_EQUAL, expr1, new VariableExpression(4)).evaluate(state));
        assertFalse(new BooleanExpression(RelationalOperator.NOT_EQUAL, expr1, new VariableExpression(5)).evaluate(state));
        assertTrue(new BooleanExpression(RelationalOperator.NOT_EQUAL, expr1, new VariableExpression(6)).evaluate(state));

        assertFalse(new BooleanExpression(RelationalOperator.LESS, expr1, new VariableExpression(4)).evaluate(state));
        assertFalse(new BooleanExpression(RelationalOperator.LESS, expr1, new VariableExpression(5)).evaluate(state));
        assertTrue(new BooleanExpression(RelationalOperator.LESS, expr1, new VariableExpression(6)).evaluate(state));
    }

}
