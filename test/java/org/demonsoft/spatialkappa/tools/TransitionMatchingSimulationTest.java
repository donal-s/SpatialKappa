package org.demonsoft.spatialkappa.tools;

import org.demonsoft.spatialkappa.model.KappaModel;


public class TransitionMatchingSimulationTest extends AbstractSimulationTest {

    @Override
    protected AbstractSimulation createSimulation(KappaModel model) {
        return new TransitionMatchingSimulation(model);
    }

    
}
