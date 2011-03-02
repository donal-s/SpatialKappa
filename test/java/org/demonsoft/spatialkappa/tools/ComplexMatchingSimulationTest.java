package org.demonsoft.spatialkappa.tools;

import org.demonsoft.spatialkappa.model.KappaModel;


public class ComplexMatchingSimulationTest extends AbstractSimulationTest {

    @Override
    protected AbstractSimulation createSimulation(KappaModel model) {
        return  new ComplexMatchingSimulation(model);
    }

    
}
