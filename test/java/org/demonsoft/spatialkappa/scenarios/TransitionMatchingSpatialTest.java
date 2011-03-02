package org.demonsoft.spatialkappa.scenarios;

import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.tools.AbstractSimulation;
import org.demonsoft.spatialkappa.tools.TransitionMatchingSimulation;


public class TransitionMatchingSpatialTest extends AbstractSpatialTest {

    @Override
    protected AbstractSimulation createSimulation(KappaModel kappaModel) throws Exception {
        return new TransitionMatchingSimulation(kappaModel);
    }

}
