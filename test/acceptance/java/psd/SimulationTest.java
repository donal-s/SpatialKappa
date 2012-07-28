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
        checkEventSimulation("test-2-1-input.ka", new String[] {"dimer in [0]", "dimer in [1]"}, 400, 60, 
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
        checkEventSimulation("test-2-3-input.ka", new String[] {"dimer in membrane", "dimer in cytosol[0]", "dimer in cytosol[1]", "dimer in cytosol[2]"}, 1000, 80, 
                new float[][] {
                {1000, 0, 0, 0}, 
                {580, 280, 100, 40}, 
                {440, 300, 160, 100}, 
                {380, 300, 200, 120}, 
                {340, 260, 220, 180}, 
                {310, 270, 220, 200}, 
                });
    }
    
    @Test
    public void testTest2_4_membraneBoundScaffold1D() throws Exception {
        checkEventSimulation("test-2-4-input.ka", new String[] {"AB[1]", "AB[5]", "AB[10]", "AB[20]"}, 1000, 80, 
                new float[][] {
                {0, 0, 0, 0}, {280, 60, 30, 10}, {540, 110, 60, 30},
                });
    }

    @Test
    public void testTest2_5_membraneBoundScaffold2D() throws Exception {
        checkEventSimulation("test-2-5-input.ka", new String[] {"AB[1][1]", "AB[3][3]", "AB[5][5]", "AB[7][7]"}, 1000, 60, 
                new float[][] {
                {0, 0, 0, 0}, {280, 60, 30, 10}, {520, 80, 50, 20},
            });
    }
    
    @Test
    public void testTest2_6_membraneBoundScaffold3D() throws Exception {
         checkEventSimulation("test-2-6-input.ka", new String[] {"AB[1][1][1]", "AB[3][3][3]", "AB[5][5][5]", "AB[7][7][7]"}, 2000, 60, 
                new float[][] {
                {0, 0, 0, 0}, {430, 15, 4, 1}, {720, 30, 6, 2},
            });
    }
    
    @Test
    public void testTest2_7_lateralDiffusionOfMembraneProteins() throws Exception {
        checkTimeSimulation("test-2-7-input.ka", new String[] {"AB", "A2B2 centre", "A2B2 not centre"}, 10, 50, 
                new float[][] {
                    {0, 0, 0}, {50, 75, 25}, {40, 175, 80}, {25, 250, 125},
                });
    }

    @Test
    public void testTest2_8_lateralDiffusionOfTransmembraneProteins() throws Exception {
         checkTimeSimulation("test-2-8-input.ka", new String[] {"AB", "A2B2 centre", "A2B2 not centre"}, 10, 50, 
                new float[][] {
                    {0, 0, 0}, {50, 75, 25}, {40, 175, 80}, {25, 250, 125},
                });
    }

    @Test
    public void testTest3_1_creationOfGeometricPrimitives() throws Exception {
         checkTimeSimulation("test-3-1-input.ka", new String[] {}, 0, 0, 
                new float[][] {
                    {}, 
                });
    }
    
    @Test
    public void testTest3_2_locationWithinGeometricPrimitives() throws Exception {
         checkTimeSimulation("test-3-2-input.ka", new String[] {
                 "SolidRectangle A", "SolidRectangle B",
                 "OpenRectangle A", "OpenRectangle B",
                 "SolidCircle A", "SolidCircle B",
                 "OpenCircle A", "OpenCircle B",
                 "SolidCuboid A", "SolidCuboid B",
                 "OpenCuboid A", "OpenCuboid B",
                 "SolidSphere A", "SolidSphere B",
                 "OpenSphere A", "OpenSphere B",
                 "SolidCylinder A", "SolidCylinder B",
                 "OpenCylinder A", "OpenCylinder B",

                 }, 0, 0, 
                 new float[][] {
                     {5, 10, 5, 12, 5, 7, 5, 11, 5, 10, 5, 10, 5, 7, 5, 10, 5, 6, 5, 8},
                 });
    }
    
//    @Test
    public void testTest3_3_diffusionWithinGeometricPrimitives() throws Exception {
         checkTimeSimulation("test-3-3-input.ka", new String[] {"AB", "A2B2 centre", "A2B2 not centre"}, 10, 50, 
                new float[][] {
                    {0, 0, 0}, {50, 75, 25}, {40, 175, 80}, {25, 250, 125},
                });
    }
    
//    @Test
    public void testTest3_4_diffusionBetweenGeometricPrimitives() throws Exception {
         checkTimeSimulation("test-3-4-input.ka", new String[] {"AB", "A2B2 centre", "A2B2 not centre"}, 10, 50, 
                new float[][] {
                    {0, 0, 0}, {50, 75, 25}, {40, 175, 80}, {25, 250, 125},
                });
    }
    
//    @Test
    public void testTest3_5_lateralDiffusionOfTransmembraneProteins() throws Exception {
         checkTimeSimulation("test-3-5-input.ka", new String[] {"AB", "A2B2 centre", "A2B2 not centre"}, 10, 50, 
                new float[][] {
                    {0, 0, 0}, {50, 75, 25}, {40, 175, 80}, {25, 250, 125},
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
