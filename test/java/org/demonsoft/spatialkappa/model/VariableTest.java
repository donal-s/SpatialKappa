package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.List;

import org.demonsoft.spatialkappa.model.Variable.Type;
import org.easymock.EasyMock;
import org.junit.Test;

public class VariableTest {

    @SuppressWarnings("unused")
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
    }

    @SuppressWarnings("unused")
    @Test
    public void testVariable_kappaExpression() {
        Complex complex = new Complex(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        
        try {
            new Variable(null, location, "label", false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Variable(complex, location, null, false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Variable(complex, null, "label", false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Variable variable = new Variable(complex, location, "label", false);
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertSame(location, variable.location);
        assertSame(complex, variable.complex);
        assertEquals("'label' cytosol[2] ([agent1()])", variable.toString());
        assertSame(Type.KAPPA_EXPRESSION, variable.type);
 
        variable = new Variable(complex, location, "label", true);
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertSame(location, variable.location);
        assertSame(complex, variable.complex);
        assertEquals("'label' voxel cytosol[2] ([agent1()])", variable.toString());
        assertSame(Type.KAPPA_EXPRESSION, variable.type);
 
        variable = new Variable(complex, Location.NOT_LOCATED, "label", false);
        assertEquals("label", variable.label);
        assertNull(variable.expression);
        assertSame(Location.NOT_LOCATED, variable.location);
        assertSame(complex, variable.complex);
        assertEquals("'label' ([agent1()])", variable.toString());
        assertSame(Type.KAPPA_EXPRESSION, variable.type);
    }

    @SuppressWarnings("unused")
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

    @Test
    public void testEvaluate_simulationState() {
        SimulationState state = EasyMock.createMock(SimulationState.class);
        
        try {
            new Variable("label").evaluate((SimulationState) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        Variable variable = new Variable(new VariableExpression(2), "label");
        assertEquals(2f, variable.evaluate(state).value, 0.1f);

        Complex complex = new Complex(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        variable = new Variable(complex, location, "label", false);

        expect(state.getComplexQuantity(variable)).andReturn(new ObservationElement(3));
        
        replay(state);
        assertEquals(3f, variable.evaluate(state).value, 0.1f);
        verify(state);
        
        variable = new Variable("label");
        
        reset(state);
        expect(state.getTransitionFiredCount(variable)).andReturn(new ObservationElement(4));
        
        replay(state);
        assertEquals(4f, variable.evaluate(state).value, 0.1f);
        verify(state);
    }

    @Test
    public void testEvaluate_kappaModel() {
        IKappaModel model = EasyMock.createMock(IKappaModel.class);
        
        try {
            new Variable(new VariableExpression(2), "label").evaluate((KappaModel) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        Complex complex = new Complex(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        try {
            new Variable(complex, location, "label", false).evaluate(model);
            fail("null should have failed");
        }
        catch (IllegalStateException ex) {
            // expected exception
        }
        try {
            new Variable("label").evaluate(model);
            fail("null should have failed");
        }
        catch (IllegalStateException ex) {
            // expected exception
        }
        
        Variable variable = new Variable(new VariableExpression(2), "label");
        assertEquals(2, variable.evaluate(model));
    }
    
    @Test
    public void testValidate_kappaExpression_recordVoxels() {
        Compartment compartment = new Compartment("loc1", 4);
        List<Compartment> compartments = getList(compartment);
        Location loc1 = new Location("loc1");
        
        Variable variable = new Variable(new Complex(new Agent("agent1")), NOT_LOCATED, "label", true);
        checkValidate_failure(variable, compartments, 
                "Voxel based observation must refer to a voxel based compartment: label");
        
        variable = new Variable(new Complex(new Agent("agent1")), new Location("unknown"), "label", true);
        checkValidate_failure(variable, compartments, 
                "Compartment 'unknown' not found");
        
        variable = new Variable(new Complex(new Agent("agent1")), new Location("loc1", INDEX_0), "label", true);
        checkValidate_failure(variable, compartments, 
                "Voxel based observation must refer to a voxel based compartment: label");
        
        variable = new Variable(new Complex(new Agent("agent1")), new Location("noVoxels"), "label", true);
        checkValidate_failure(variable, getList(new Compartment("noVoxels")), 
                "Voxel based observation must refer to a voxel based compartment: label");
        
        variable = new Variable(new Complex(new Agent("agent1", new Location("loc1", INDEX_0))), loc1, "label", true);
        checkValidate_failure(variable, compartments, 
                "Agents of voxel based observation must have compatible location: label");
        
        variable = new Variable(new Complex(new Agent("agent1", new Location("loc2"))), loc1, "label", true);
        checkValidate_failure(variable, compartments, 
                "Agents of voxel based observation must have compatible location: label");
        
        variable = new Variable(new Complex(new Agent("agent1", new AgentSite("x", null, "1", "intra")),
                new Agent("agent1", new AgentSite("x", null, "1"))), loc1, "label", true);
        checkValidate_failure(variable, compartments, 
                "Agents of voxel based observation must be colocated in single voxel: label");
        
        
        // Valid cases
        
        variable = new Variable(new Complex(new Agent("agent1")), loc1, "label", true);
        variable.validate(compartments);
        
        variable = new Variable(new Complex(new Agent("agent1", NOT_LOCATED)), loc1, "label", true);
        variable.validate(compartments);
        
        variable = new Variable(new Complex(new Agent("agent1", loc1)), loc1, "label", true);
        variable.validate(compartments);
        
        variable = new Variable(new Complex(
                new Agent("agent1", new AgentSite("x", null, "1")),
                new Agent("agent1", new AgentSite("x", null, "1"))), loc1, "label", true);
        variable.validate(compartments);
        
        variable = new Variable(new Complex(
                new Agent("agent1", new AgentSite("x", null, "?")),
                new Agent("agent1", new AgentSite("x", null, "_"))), loc1, "label", true);
        variable.validate(compartments);
    }
    
    private void checkValidate_failure(Variable variable, List<Compartment> compartments, String expectedMessage) {
        try {
            variable.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals(expectedMessage, ex.getMessage());
        }

    }


}
