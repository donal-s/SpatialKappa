package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.demonsoft.spatialkappa.model.Perturbation.Assignment;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;
import org.junit.Test;

public class AssignmentTest {

    @Test
    public void testAssignment() {
        
        ConcentrationExpression expression = new ConcentrationExpression(2f);
        
        try {
            new Assignment(null, expression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Assignment("transform", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Assignment assignment = new Assignment("transform", expression);
        assertEquals("transform", assignment.getTargetTransform());
        assertSame(expression, assignment.getExpression());
    }

    @Test
    public void testApply() {
        
        ConcentrationExpression expression = new ConcentrationExpression(2f);
        
        try {
            new Assignment("transform", expression).apply(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        KappaModel kappaModel = new KappaModel();
        kappaModel.addTransform(new LocatedTransform(new Transform("transform", TransformTest.getComplexes(new Agent("A")), null, 0.4f), null));
        kappaModel.addTransform(new LocatedTransform(new Transform("otherTransform", TransformTest.getComplexes(new Agent("B")), null, 0.5f), null));
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        simulation.initialise();
        
        new Assignment("unknown", expression).apply(simulation);
        // Should have no side effects and throw no exceptions

        Map<String, Variable> variables = new HashMap<String, Variable>();
        SimulationState state = new ModelOnlySimulationState(variables);

        new Assignment("transform", expression).apply(simulation);
        assertEquals(2f, simulation.getTransition("transform").getRate().evaluate(state).value, 0.01f);
        assertEquals(0.5f, simulation.getTransition("otherTransform").getRate().evaluate(state).value, 0.01f);
        
        new Assignment("transform", new ConcentrationExpression("otherTransform")).apply(simulation);
        assertEquals(0.5f, simulation.getTransition("transform").getRate().evaluate(state).value, 0.01f);
        assertEquals(0.5f, simulation.getTransition("otherTransform").getRate().evaluate(state).value, 0.01f);

        // TODO assignments from %var & %obs
        // TODO assignments to reversible reactions
    }

}
