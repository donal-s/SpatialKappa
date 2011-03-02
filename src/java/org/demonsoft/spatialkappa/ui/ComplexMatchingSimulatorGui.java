package org.demonsoft.spatialkappa.ui;
import java.io.File;

import org.demonsoft.spatialkappa.tools.AbstractSimulation;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;

public class ComplexMatchingSimulatorGui extends AbstractSimulatorGui {

    public ComplexMatchingSimulatorGui() throws Exception {
        super();
    }

    @Override
    protected AbstractSimulation createSimulation(File inputFile) throws Exception {
        return ComplexMatchingSimulation.createSimulation(inputFile);
    }

    @Override
    protected String getWindowTitle() {
        return WINDOW_TITLE;
    }


    public static void main(String[] args) throws Exception {
        ComplexMatchingSimulatorGui simulator = new ComplexMatchingSimulatorGui();
        if (args.length == 1) {
            File kappaFile = new File(args[0]);
            if (kappaFile.exists()) {
                simulator.openKappaFile(kappaFile);
                simulator.runSimulation();
            }
        }
    }


}
