package org.demonsoft.spatialkappa.ui;
import java.io.File;

import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.tools.Simulation;
import org.demonsoft.spatialkappa.tools.TransitionMatchingSimulation;

public class TransitionMatchingSimulatorGui extends AbstractSimulatorGui {

    public TransitionMatchingSimulatorGui() throws Exception {
        super();
    }

    @Override
    protected Simulation createSimulation(IKappaModel kappaModel) throws Exception {
        return new TransitionMatchingSimulation(kappaModel);
    }

    @Override
    protected String getWindowTitle() {
        return WINDOW_TITLE;
    }


    public static void main(String[] args) throws Exception {
        TransitionMatchingSimulatorGui simulator = new TransitionMatchingSimulatorGui();
        if (args.length == 1) {
            File kappaFile = new File(args[0]);
            if (kappaFile.exists()) {
                simulator.openFile(kappaFile);
                simulator.runSimulation();
            }
        }
    }
}
