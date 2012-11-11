package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.SimulationToken;
import org.demonsoft.spatialkappa.model.VariableExpression.Type;
import org.demonsoft.spatialkappa.model.VariableExpression.UnaryOperator;
import org.easymock.EasyMock;
import org.junit.Test;


public class VariableExpressionTest {

    @SuppressWarnings("unused")
    @Test
    public void testConstructor_number() {
        try {
            new VariableExpression((String) null);
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
        
        VariableExpression expr = new VariableExpression("2");
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("2.0", expr.toString());
        
        expr = new VariableExpression("2.55e4");
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("25500.0", expr.toString());
        
        expr = new VariableExpression(2.5f);
        assertEquals(Type.NUMBER, expr.type);
        assertEquals("2.5", expr.toString());
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_constant() {
        try {
            new VariableExpression((Constant) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        VariableExpression expr = new VariableExpression(Constant.INFINITY);
        assertEquals(Type.CONSTANT, expr.type);
        assertEquals("[inf]", expr.toString());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_simulationToken() {
        try {
            new VariableExpression((SimulationToken) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        VariableExpression expr = new VariableExpression(SimulationToken.EVENTS);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[E]", expr.toString());
        
        expr = new VariableExpression(SimulationToken.TIME);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[T]", expr.toString());
        
        expr = new VariableExpression(SimulationToken.ELAPSED_TIME);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[Tsim]", expr.toString());
        
        expr = new VariableExpression(SimulationToken.MAX_EVENTS);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[Emax]", expr.toString());
        
        expr = new VariableExpression(SimulationToken.MAX_TIME);
        assertEquals(Type.SIMULATION_TOKEN, expr.type);
        assertEquals("[Tmax]", expr.toString());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_variableReference() {
        VariableReference reference = new VariableReference("x");
        
        try {
            new VariableExpression((VariableReference) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        VariableExpression expr = new VariableExpression(new VariableReference("x"));
        assertEquals(reference, expr.reference);
        assertEquals(Type.VARIABLE_REFERENCE, expr.type);
        assertEquals("'x'", expr.toString());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_unaryExpression() {
        VariableReference reference = new VariableReference("x");
        VariableExpression expr1 = new VariableExpression(reference);
        
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
        
        VariableExpression expr = new VariableExpression(UnaryOperator.LOG, expr1);
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
        
        expr = new VariableExpression(UnaryOperator.ABS, expr1);
        assertEquals(Type.UNARY_EXPRESSION, expr.type);
        assertEquals("[int] ('x')", expr.toString());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_binaryExpression() {
        VariableReference reference = new VariableReference("x");
        VariableExpression expr1 = new VariableExpression(reference);
        VariableExpression expr2 = new VariableExpression("2");
        VariableExpression expr3 = new VariableExpression(Constant.INFINITY);
        
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
        
        VariableExpression expr = new VariableExpression(expr1, Operator.PLUS, expr2);
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
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testConstructor_agentGroup() {
        try {
            new VariableExpression(null, NOT_LOCATED);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(getList(new Agent("A")), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new VariableExpression(new ArrayList<Agent>(), NOT_LOCATED);
            fail("missing agents should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        VariableExpression expr = new VariableExpression(getList(new Agent("A")), NOT_LOCATED);
        assertEquals(Type.AGENT_GROUP, expr.type);
        assertEquals("|[[A()]]|", expr.toString());
        
        expr = new VariableExpression(getList(new Agent("A"), new Agent("B", new Location("loc2"))), NOT_LOCATED);
        assertEquals(Type.AGENT_GROUP, expr.type);
        assertEquals("|[[A()], [B:loc2()]]|", expr.toString());
        
        expr = new VariableExpression(getList(new Agent("A"), new Agent("B", new Location("loc2"))), new Location("loc1"));
        assertEquals(Type.AGENT_GROUP, expr.type);
        assertEquals("|[[A:loc1()], [B:loc2()]]|", expr.toString());
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
        assertFalse(new VariableExpression(SimulationToken.TIME).isFixed(variables));
        assertFalse(new VariableExpression(SimulationToken.ELAPSED_TIME).isFixed(variables));
        assertFalse(new VariableExpression(SimulationToken.MAX_EVENTS).isFixed(variables));
        assertFalse(new VariableExpression(SimulationToken.MAX_TIME).isFixed(variables));
        
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
        
        expr = new VariableExpression(getList(new Agent("A")), NOT_LOCATED);
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
        assertFalse(new VariableExpression(SimulationToken.TIME).isInfinite(variables));
        assertFalse(new VariableExpression(SimulationToken.ELAPSED_TIME).isInfinite(variables));
        assertFalse(new VariableExpression(SimulationToken.MAX_EVENTS).isInfinite(variables));
        assertFalse(new VariableExpression(SimulationToken.MAX_TIME).isInfinite(variables));
        assertFalse(new VariableExpression(getList(new Agent("A")), NOT_LOCATED).isInfinite(variables));

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
    public void testEvaluate_simulationState() {
        VariableReference referenceX = new VariableReference("x");
        VariableReference referenceY = new VariableReference("y");
        VariableReference referenceZ = new VariableReference("z");
        VariableExpression expr = new VariableExpression(referenceX);
        Map<String, Variable> variables = new HashMap<String, Variable>();
        SimulationState state = new ModelOnlySimulationState(variables);
        
        try {
            expr.evaluate((SimulationState) null);
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
        assertEquals(100, new VariableExpression(UnaryOperator.ABS, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);
        variables.put("z", new Variable(new VariableExpression(3.5f), "z"));
        assertEquals(3, new VariableExpression(UnaryOperator.ABS, new VariableExpression(referenceZ)).evaluate(state).value, 0.01f);
        variables.put("z", new Variable(new VariableExpression(-3.5f), "z"));
        assertEquals(3, new VariableExpression(UnaryOperator.ABS, new VariableExpression(referenceZ)).evaluate(state).value, 0.01f);
        
        variables.put("y", new Variable(new VariableExpression(3), "y"));
        assertEquals(20.09f, new VariableExpression(UnaryOperator.EXP, new VariableExpression(referenceY)).evaluate(state).value, 0.01f);

        assertEquals(3.14f, new VariableExpression(Constant.PI).evaluate(state).value, 0.01f);

        // Agent based expressions should return 0
        assertEquals(0f, new VariableExpression(getList(new Agent("A")), NOT_LOCATED).evaluate(state).value, 0.01f);
    }
    
    @Test
    public void testEvaluate_simulationState_simulationTokens() {
        SimulationState state = EasyMock.createMock(SimulationState.class);

        reset(state);
        expect(state.getMaximumTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.MAX_TIME).evaluate(state).value);
        verify(state);

        reset(state);
        expect(state.getMaximumEventCount()).andReturn(5);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.MAX_EVENTS).evaluate(state).value);
        verify(state);

        reset(state);
        expect(state.getTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.TIME).evaluate(state).value);
        verify(state);

        reset(state);
        expect(state.getEventCount()).andReturn(5);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.EVENTS).evaluate(state).value);
        verify(state);

        reset(state);
        expect(state.getElapsedTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.ELAPSED_TIME).evaluate(state).value);
        verify(state);
    }
    
    
    @Test
    public void testEvaluate_kappaModel() {
        VariableReference referenceX = new VariableReference("x");
        VariableReference referenceY = new VariableReference("y");
        VariableExpression expr = new VariableExpression(referenceX);
        IKappaModel model = new KappaModel();
        Map<String, Variable> variables = model.getVariables();
        
        try {
            expr.evaluate((IKappaModel) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluate(model);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("y", new Variable(new VariableExpression(5), "y"));
        try {
            expr.evaluate(model);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new VariableExpression(Constant.INFINITY).evaluate(model);
            fail("evaluating infinity should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        variables.put("x", new Variable(new VariableExpression(12), "x"));
        variables.put("y", new Variable(new VariableExpression(3), "y"));

        assertEquals(2, new VariableExpression("2").evaluate(model));
        assertEquals(9, new VariableExpression(new VariableExpression("7"), Operator.PLUS, new VariableExpression("2")).evaluate(model));
        assertEquals(5, new VariableExpression(new VariableExpression("7"), Operator.MINUS, new VariableExpression("2")).evaluate(model));
        assertEquals(14, new VariableExpression(new VariableExpression("7"), Operator.MULTIPLY, new VariableExpression("2")).evaluate(model));
        assertEquals(3, new VariableExpression(new VariableExpression("7"), Operator.DIVIDE, new VariableExpression("2")).evaluate(model));
        assertEquals(0, new VariableExpression(new VariableExpression("0"), Operator.MODULUS, new VariableExpression("5")).evaluate(model));
        assertEquals(2, new VariableExpression(new VariableExpression("7"), Operator.MODULUS, new VariableExpression("5")).evaluate(model));
        assertEquals(8, new VariableExpression(new VariableExpression("2"), Operator.POWER, new VariableExpression("3")).evaluate(model));
        
        assertEquals(12, new VariableExpression(referenceX).evaluate(model));
        assertEquals(15, new VariableExpression(new VariableExpression(referenceX), Operator.PLUS, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(9, new VariableExpression(new VariableExpression(referenceX), Operator.MINUS, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(36, new VariableExpression(new VariableExpression(referenceX), Operator.MULTIPLY, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(4, new VariableExpression(new VariableExpression(referenceX), Operator.DIVIDE, new VariableExpression(referenceY)).evaluate(model));

        variables.put("y", new Variable(new VariableExpression(100), "y"));
        assertEquals(4, new VariableExpression(UnaryOperator.LOG, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(0, new VariableExpression(UnaryOperator.SIN, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(0, new VariableExpression(UnaryOperator.COS, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(0, new VariableExpression(UnaryOperator.TAN, new VariableExpression(referenceY)).evaluate(model));
        assertEquals(10, new VariableExpression(UnaryOperator.SQRT, new VariableExpression(referenceY)).evaluate(model));
        
        variables.put("y", new Variable(new VariableExpression(3), "y"));
        assertEquals(20, new VariableExpression(UnaryOperator.EXP, new VariableExpression(referenceY)).evaluate(model));

        assertEquals(3, new VariableExpression(Constant.PI).evaluate(model));

        // Agent based expressions should return 0
        assertEquals(0, new VariableExpression(getList(new Agent("A")), NOT_LOCATED).evaluate(model));
    }
    
    @Test
    public void testEvaluate_kappaModel_simulationTokens() {
        IKappaModel model = new KappaModel();
        
        try {
            new VariableExpression(SimulationToken.EVENTS).evaluate(model);
            fail("invalid should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        try {
            new VariableExpression(SimulationToken.TIME).evaluate(model);
            fail("invalid should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        try {
            new VariableExpression(SimulationToken.ELAPSED_TIME).evaluate(model);
            fail("invalid should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        try {
            new VariableExpression(SimulationToken.MAX_EVENTS).evaluate(model);
            fail("invalid should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        try {
            new VariableExpression(SimulationToken.MAX_TIME).evaluate(model);
            fail("invalid should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
    }
    
    @Test
    public void testEvaluate_transitionInstance() {
        VariableReference referenceX = new VariableReference("x");
        VariableReference referenceY = new VariableReference("y");
        VariableExpression expr = new VariableExpression(referenceX);
        Map<String, Variable> variables = new HashMap<String, Variable>();
        SimulationState state = new ModelOnlySimulationState(variables);
        Complex complex1 = new Complex(new Agent("agent1"));
        TransitionInstance instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 1);
        
        try {
            expr.evaluate(null, instance);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluate(state, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            expr.evaluate(state, instance);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("y", new Variable(new VariableExpression(5), "y"));
        try {
            expr.evaluate(state, instance);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new VariableExpression(Constant.INFINITY).evaluate(state, instance);
            fail("evaluating infinity should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        // No agent based rates - evaluate as normal

        variables.put("x", new Variable(new VariableExpression(12), "x"));
        variables.put("y", new Variable(new VariableExpression(3), "y"));

        assertEquals(2.0f, new VariableExpression("2").evaluate(state).value);
        assertEquals(9.0f, new VariableExpression(new VariableExpression("7"), Operator.PLUS, new VariableExpression("2")).evaluate(state, instance).value);
        assertEquals(5.0f, new VariableExpression(new VariableExpression("7"), Operator.MINUS, new VariableExpression("2")).evaluate(state, instance).value);
        assertEquals(14.0f, new VariableExpression(new VariableExpression("7"), Operator.MULTIPLY, new VariableExpression("2")).evaluate(state, instance).value);
        assertEquals(3.5f, new VariableExpression(new VariableExpression("7"), Operator.DIVIDE, new VariableExpression("2")).evaluate(state, instance).value);
        assertEquals(0.0f, new VariableExpression(new VariableExpression("0"), Operator.MODULUS, new VariableExpression("5")).evaluate(state, instance).value);
        assertEquals(2.0f, new VariableExpression(new VariableExpression("7"), Operator.MODULUS, new VariableExpression("5")).evaluate(state, instance).value);
        assertEquals(8.0f, new VariableExpression(new VariableExpression("2"), Operator.POWER, new VariableExpression("3")).evaluate(state, instance).value);
        
        assertEquals(12.0f, new VariableExpression(referenceX).evaluate(state, instance).value);
        assertEquals(15.0f, new VariableExpression(new VariableExpression(referenceX), Operator.PLUS, new VariableExpression(referenceY)).evaluate(state, instance).value);
        assertEquals(9.0f, new VariableExpression(new VariableExpression(referenceX), Operator.MINUS, new VariableExpression(referenceY)).evaluate(state, instance).value);
        assertEquals(36.0f, new VariableExpression(new VariableExpression(referenceX), Operator.MULTIPLY, new VariableExpression(referenceY)).evaluate(state, instance).value);
        assertEquals(4.0f, new VariableExpression(new VariableExpression(referenceX), Operator.DIVIDE, new VariableExpression(referenceY)).evaluate(state, instance).value);

        variables.put("y", new Variable(new VariableExpression(100), "y"));
        assertEquals(4.61f, new VariableExpression(UnaryOperator.LOG, new VariableExpression(referenceY)).evaluate(state, instance).value, 0.01f);
        assertEquals(-0.51f, new VariableExpression(UnaryOperator.SIN, new VariableExpression(referenceY)).evaluate(state, instance).value, 0.01f);
        assertEquals(0.86f, new VariableExpression(UnaryOperator.COS, new VariableExpression(referenceY)).evaluate(state, instance).value, 0.01f);
        assertEquals(-0.59f, new VariableExpression(UnaryOperator.TAN, new VariableExpression(referenceY)).evaluate(state, instance).value, 0.01f);
        assertEquals(10.0f, new VariableExpression(UnaryOperator.SQRT, new VariableExpression(referenceY)).evaluate(state, instance).value);
        
        variables.put("y", new Variable(new VariableExpression(3), "y"));
        assertEquals(20.09f, new VariableExpression(UnaryOperator.EXP, new VariableExpression(referenceY)).evaluate(state, instance).value, 0.01f);

        assertEquals(3.14f, new VariableExpression(Constant.PI).evaluate(state, instance).value, 0.01f);

        // Agent based rates - Single agent type 
        variables.put("y", new Variable(new VariableExpression(4), "y"));
        
        VariableExpression expression = new VariableExpression(getList(new Agent("A")), NOT_LOCATED);
        List<Complex> instanceComplexes = new ArrayList<Complex>();
        checkEvaluate_TransitionInstance(expression, state, instanceComplexes, 0f);
        
        expression = new VariableExpression(new VariableExpression(referenceY), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes = new ArrayList<Complex>();
        checkEvaluate_TransitionInstance(expression, state, instanceComplexes, Float.POSITIVE_INFINITY);
        
        expression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes.add(new Complex(new Agent("A")));
        checkEvaluate_TransitionInstance(expression, state, instanceComplexes, 4f);
        
        expression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes = getList(new Complex(new Agent("A")), new Complex(new Agent("A")));
        checkEvaluate_TransitionInstance(expression, state, instanceComplexes, 2f);
        
        expression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes = getList(new Complex(
                new Agent("A", new AgentSite("x", null, "1"), new AgentSite("y", null, "2")), 
                new Agent("A", new AgentSite("x", null, "1")), 
                new Agent("A", new AgentSite("x", null, "2"))));
        checkEvaluate_TransitionInstance(expression, state, instanceComplexes, 1.33f);
        
        // TODO multiple agent types and agent state

    }

    @Test
    public void testEvaluate_transitionInstance_simulationTokens() {
        Complex complex1 = new Complex(new Agent("agent1"));
        TransitionInstance instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 1);
        SimulationState state = EasyMock.createMock(SimulationState.class);

        reset(state);
        expect(state.getMaximumTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.MAX_TIME).evaluate(state, instance).value);
        verify(state);

        reset(state);
        expect(state.getMaximumEventCount()).andReturn(5);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.MAX_EVENTS).evaluate(state, instance).value);
        verify(state);

        reset(state);
        expect(state.getTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.TIME).evaluate(state, instance).value);
        verify(state);

        reset(state);
        expect(state.getEventCount()).andReturn(5);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.EVENTS).evaluate(state, instance).value);
        verify(state);

        reset(state);
        expect(state.getElapsedTime()).andReturn(5f);
        replay(state);
        assertEquals(5f, new VariableExpression(SimulationToken.ELAPSED_TIME).evaluate(state, instance).value);
        verify(state);
    }

    private void checkEvaluate_TransitionInstance(VariableExpression expression, SimulationState state, List<Complex> instanceComplexes, float expectedRate) {
        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        for (Complex complex : instanceComplexes) {
            complexMappings.add(new ComplexMapping(complex));
        }
        TransitionInstance instance = new TransitionInstance(complexMappings, 1);
        assertEquals(expectedRate, expression.evaluate(state, instance).value, 0.1f);
    }
    

}
