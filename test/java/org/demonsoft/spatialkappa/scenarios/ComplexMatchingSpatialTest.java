package org.demonsoft.spatialkappa.scenarios;

import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.tools.AbstractSimulation;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;


public class ComplexMatchingSpatialTest extends AbstractSpatialTest {

    @Override
    protected AbstractSimulation createSimulation(IKappaModel kappaModel) throws Exception {
        return new ComplexMatchingSimulation(kappaModel);
    }

}
