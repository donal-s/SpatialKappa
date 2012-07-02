package psd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;
import org.demonsoft.spatialkappa.tools.TransitionMatchingSimulation;
import org.junit.Test;


public class SimulationTest {


    private static final String TEST_DATA_DIRECTORY = "test/acceptance/data/";

    private TransitionMatchingSimulation simulation;
    Observation currentObservation;
    
    @Test
    public void testTest2_1_diffusionAcrossFullyPermeableMembrane() throws Exception {
        checkEventSimulation("test-2-1-input.ka", new String[] {"dimer in [0]", "dimer in [1]"}, 400, 50, 
                new float[][] {
                    {1000, 0}, {700, 300}, {600, 400}, {550, 450}, {500, 500}, {500, 500}, {500, 500}
                });
    }
    
    @Test
    public void testTest2_2_diffusionAcrossPartiallyPermeableMembrane() throws Exception {
        checkEventSimulation("test-2-2-input.ka", 
                new String[] {"dimer in [0]", "dimer in [1]", "dimer embedded", "A in [0]", "A in [1]", "B in [0]", "B in [1]"}, 800, 100, 
                new float[][] {
                {1000, 0, 0, 2000, 0, 2000, 0}, 
                {730, 0, 270, 1460, 540, 2000, 0}, 
                {570, 0, 430, 1140, 860, 2000, 0}, 
                {530, 0, 470, 1060, 940, 2000, 0}, 
                {500, 0, 500, 1000, 1000, 2000, 0}, 
                });
    }
    
    @Test
    public void testTest2_3_diffusionBetweenCompartmentsOfDifferentDimensions() throws Exception {
        checkEventSimulation("test-2-3-input.ka", new String[] {"dimer in membrane", "dimer in cytosol[0]", "dimer in cytosol[1]", "dimer in cytosol[2]"}, 400, 80, 
                new float[][] {
                {1000, 0, 0, 0}, 
                {700, 270, 30, 0}, 
                {530, 380, 70, 20}, 
                {425, 425, 100, 50}, 
                {350, 450, 150, 50}, 
                {300, 400, 250, 50}, 
                });
    }
    
    @Test
    public void testTest2_4_membraneBoundScaffold1D() throws Exception {
        checkTimeSimulation("test-2-4-input.ka", new String[] {"AB"}, 0.002f, 60, 
                new float[][] {
                    {0}, {150}, {300}, {400, 600}, {480}, {530}, {590}
                });
    }
    
    @Test
    public void testTest2_5_membraneBoundScaffold2D() throws Exception {
        checkTimeSimulation("test-2-5-input.ka", new String[] {"AB"}, 0.002f, 60, 
                new float[][] {
                {0}, {150}, {300}, {400, 600}, {480}, {530}, {590}
            });
    }
    
//     @Test //TODO - work in progress
    public void testTest2_6_membraneBoundScaffold3D() throws Exception {
        checkTimeSimulation("test-2-6-input.ka", new String[] {"AB"}, 0.002f, 60, 
                new float[][] {
                {0}, {150}, {300}, {400, 600}, {480}, {530}, {590}
            });
    }
    
    // @Test TODO - work in progress
    public void testTest2_7_lateralDiffusionOfMembraneProteins() throws Exception {
        checkEventSimulation("test-2-7-input.ka", new String[] {"A2B2"}, 200, 50, 
                new float[][] {
                    {0}, {200}, {400}, {600}, {800}, {1000}, {1000}
                });
    }
    
    // @Test TODO - work in progress
    public void testTest2_8_lateralDiffusionOfTransmembraneProteins() throws Exception {
        checkEventSimulation("test-2-8-input.ka", new String[] {"AB", "A2B2"}, 200, 50, 
                new float[][] {
                {0, 0}, {90, 0}, {140, 0}, {0, 600}, {0, 800}, {0, 1000}, {0, 1000}
            });
    }
    
    // @Test TODO - work in progress
    public void testTest2_9_extensionOfEGFRModel() throws Exception {
        checkEventSimulation("test-2-9-input.ka", new String[] {"[A(x~s)]", "[A(x~t)]"}, 200, 50, 
                new float[][] {
                    {1000, 0}, {800, 200}, {600, 400}, {400, 600}, {200, 800}, {0, 1000}, {0, 1000}
                });
    }

    
    private void checkEventSimulation(String inputModelFilename, String[] observableNames, int eventsPerStep, float accuracy, float[][] expectedObservableValues) throws Exception {
        simulation = createSimulation(inputModelFilename);
        
        // Check time 0
        checkObservations(0, observableNames, accuracy, expectedObservableValues[0]);
   
        // Check remaining steps
        for (int index = 1; index < expectedObservableValues.length; index++) {
            simulation.runByEvent(1, eventsPerStep);
            checkObservations(index, observableNames, accuracy, expectedObservableValues[index]);
        }
    }
    private void checkTimeSimulation(String inputModelFilename, String[] observableNames, float timePerStep, float accuracy, float[][] expectedObservableValues) throws Exception {
        simulation = createSimulation(inputModelFilename);
        
        // Check time 0
        checkObservations(0, observableNames, accuracy, expectedObservableValues[0]);
   
        // Check remaining steps
        for (int index = 1; index < expectedObservableValues.length; index++) {
            simulation.runByTime(timePerStep, timePerStep);
            checkObservations(index, observableNames, accuracy, expectedObservableValues[index]);
        }
    }

    private final void checkObservations(int observation, String[] observableNames, float accuracy, float... values) {
        for (int index = 0; index < observableNames.length; index++) {
            assertNotNull("Observable not found: " + observableNames[index] + " in " + currentObservation.observables.keySet(),
                    currentObservation.observables.get(observableNames[index]));

            assertEquals("Observation " + observation + ": " + observableNames[index] + " " + currentObservation, 
                    values[index], currentObservation.observables.get(observableNames[index]).value, accuracy);
        }
    }

    private final TransitionMatchingSimulation createSimulation(String inputModelFilename) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(
                new File(TEST_DATA_DIRECTORY, inputModelFilename)));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        SpatialKappaParser.prog_return r = new SpatialKappaParser(tokens).prog();

        CommonTree t = (CommonTree) r.getTree();
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        simulation = new TransitionMatchingSimulation(walker.prog());
        currentObservation = simulation.getCurrentObservation();
        
        simulation.addObservationListener(new ObservationListener() {
            
            public void observation(Observation observation) {
                currentObservation = observation;
            }
        });
        return simulation;
    }

}
