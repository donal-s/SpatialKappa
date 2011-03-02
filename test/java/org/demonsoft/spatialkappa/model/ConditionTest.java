package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.model.Perturbation.Condition;
import org.demonsoft.spatialkappa.model.Perturbation.Inequality;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;
import org.junit.Test;

public class ConditionTest {

    @Test
    public void testCondition_invalid() {
        ConcentrationExpression expression = new ConcentrationExpression(2);
        
        try {
            new Condition(null, Inequality.GREATER_THAN, expression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Condition(expression, null, expression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Condition(expression, Inequality.GREATER_THAN, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
    }

    @Test
    public void testIsConditionMet() {
        
        Condition condition = new Condition(new ConcentrationExpression(1), Inequality.GREATER_THAN, new ConcentrationExpression(0.1f));
        
        try {
            condition.isConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        
        assertTrue(new Condition(new ConcentrationExpression(1), Inequality.GREATER_THAN, new ConcentrationExpression(0.1f)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression(1), Inequality.GREATER_THAN, new ConcentrationExpression(1)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression(1), Inequality.GREATER_THAN, new ConcentrationExpression(1.1f)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression(1), Inequality.LESS_THAN, new ConcentrationExpression(0.1f)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression(1), Inequality.LESS_THAN, new ConcentrationExpression(1)).isConditionMet(simulation));
        assertTrue(new Condition(new ConcentrationExpression(1), Inequality.LESS_THAN, new ConcentrationExpression(1.1f)).isConditionMet(simulation));
        
        kappaModel.addTransform(new LocatedTransform(new Transform("label", TransformTest.getComplexes(new Agent("A")), null, "0.5"), null));
        simulation.initialise();
        assertTrue(new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(0.1f)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(0.5f)).isConditionMet(simulation));
        assertFalse(new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(1.1f)).isConditionMet(simulation));
        

        // TODO conditions making use of simulation - needs understanding of semantics
    }

}
