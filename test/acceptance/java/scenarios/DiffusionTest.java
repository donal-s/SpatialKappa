package scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.model.TestUtils;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.tools.TransitionMatchingSimulation;
import org.junit.Test;


public class DiffusionTest {

    IKappaModel kappaModel;
    TransitionMatchingSimulation simulation;
    Observation currentObservation;
    
//    @BeforeClass
    public static void profilerDelay() throws InterruptedException {
        Thread.sleep(10000);
    }
    
    
    @Test
    public void testSolidSpineCase() throws Exception {
        simulation = createSimulation(SOLID_SPINE_INPUT);
        simulation.runByEvent(1, 100000);
        checkEvenDistribution("cytosol", new String[] {"A()", "B()"}, 2000);
    }
    
    @Test
    public void testOpenSpineCase() throws Exception {
        simulation = createSimulation(OPEN_SPINE_INPUT);
        simulation.runByEvent(1, 200000);
        checkNonZeroDistribution("membrane", new String[] {"A()", "B()"});
    }
    
    @Test
    public void testNestedSpineCase() throws Exception {
        simulation = createSimulation(NESTED_SPINE_INPUT);
        simulation.runByEvent(1, 50000);
        checkNonZeroDistribution("membrane", new String[] {"A()"});
    }
    
    private final void checkEvenDistribution(String compartmentName, String[] observableNames, int totalAgents) {
        Compartment compartment = Utils.getCompartment(kappaModel.getCompartments(), compartmentName);
        
        Location[] expectedVoxels = compartment.getDistributedCellReferences();
        int expectedAgentsPerVoxel = totalAgents / expectedVoxels.length;
        int accuracy = expectedAgentsPerVoxel / 2;
        
        for (int index = 0; index < observableNames.length; index++) {
            ObservationElement element = currentObservation.observables.get(observableNames[index]);
            assertNotNull("Observable not found: " + observableNames[index] + " in " + currentObservation.observables.keySet(),
                    element);

            assertTrue(element.isCompartment);
            
            for (int x=0; x<element.dimensions[1]; x++) {
                for (int y=0; y<element.dimensions[0]; y++) {
                    for (int z=0; z<element.dimensions[2]; z++) {
                        if (compartment.isValidVoxel(y, x, z)) {
                            assertTrue("Incorrect count at voxel: [" + y + "][" + x + "][" + z + "] = " + element.getCellValue(x, y, z), 
                                    Math.abs(expectedAgentsPerVoxel - element.getCellValue(x, y, z)) < accuracy);
                        }
                        else {
                            assertEquals("Incorrect count at voxel: [" + y + "][" + x + "][" + z + "]", 
                                    0, element.getCellValue(x, y, z));
                        }
                    }
                }
            }
        }
    }

    private final void checkNonZeroDistribution(String compartmentName, String[] observableNames) {
        Compartment compartment = Utils.getCompartment(kappaModel.getCompartments(), compartmentName);
        
        for (int index = 0; index < observableNames.length; index++) {
            ObservationElement element = currentObservation.observables.get(observableNames[index]);
            assertNotNull("Observable not found: " + observableNames[index] + " in " + currentObservation.observables.keySet(),
                    element);

            assertTrue(element.isCompartment);
            
            for (int x=0; x<element.dimensions[1]; x++) {
                for (int y=0; y<element.dimensions[0]; y++) {
                    for (int z=0; z<element.dimensions[2]; z++) {
                        if (compartment.isValidVoxel(y, x, z)) {
                            assertTrue("Incorrect count at voxel: [" + y + "][" + x + "][" + z + "] = " + element.getCellValue(x, y, z), 
                                    0 != element.getCellValue(x, y, z));
                        }
                        else {
                            assertEquals("Incorrect count at voxel: [" + y + "][" + x + "][" + z + "]", 
                                    0, element.getCellValue(x, y, z));
                        }
                    }
                }
            }
        }
    }

    private static final String SOLID_SPINE_INPUT = 
            "%agent: A()\n" +
            "%agent: B()\n" +
            "%compartment: cytosol SolidSpine [3][3][1]\n" +
            "%channel: diffusion Neighbour :cytosol -> :cytosol\n" + 
            "" + 
            "'diffusion A' A() ->:diffusion A() @ 1.0\n" + 
            "'diffusion B' B() ->:diffusion B() @ 1.0\n" + 
            "%init: 2000 :cytosol [1][1][0] A() \n" + // Top
            "%init: 2000 :cytosol [1][1][3] B() \n" + // Bottom
            "%obs: voxel 'A()' :cytosol A() \n" + 
            "%obs: voxel 'B()' :cytosol B() \n" + 
            "";

    private static final String OPEN_SPINE_INPUT = 
            "%agent: A()\n" +
            "%agent: B()\n" +
            "%compartment: membrane OpenSpine [7][5][2][1]\n" +
            "%channel: diffusion Neighbour :membrane -> :membrane\n" + 
            "" + 
            "'diffusion A' A() ->:diffusion A() @ 1.0\n" + 
            "'diffusion B' B() ->:diffusion B() @ 1.0\n" + 
            "%init: 2000 :membrane [3][3][0] A() \n" + // Top
            "%init: 2000 :membrane [3][3][8] B() \n" + // Bottom
            "%obs: voxel 'A()' :membrane A() \n" + 
            "%obs: voxel 'B()' :membrane B() \n" + 
            "";

    private static final String NESTED_SPINE_INPUT = 
            "%agent: A_Membrane(d)\n" + 
            "%agent: A_Cytosol(d)\n" + 
            "%compartment: cytosol SolidSpine [3][3][1]\n" +
            "%compartment: membrane OpenSpine [5][5][1][1]\n" +
            "%channel: diffusion Neighbour :cytosol, :membrane -> :cytosol, :membrane\n" + 
            "%channel: domainLink Neighbour :membrane -> :cytosol\n" + 
            "" + 
            "'diffusion A' A_Membrane(d!1:domainLink),A_Cytosol(d!1) ->:diffusion A_Membrane(d!1:domainLink),A_Cytosol(d!1) @ 1.0\n" + 
            "%init: 2000 A_Membrane:membrane[2][2][0](d!1:domainLink),A_Cytosol(d!1)\n" + 
            "%obs: voxel 'A()' :membrane A_Membrane()\n" + 
            "";

    private final TransitionMatchingSimulation createSimulation(String inputText) throws Exception {
        kappaModel = TestUtils.createKappaModel(inputText);
        simulation = new TransitionMatchingSimulation(kappaModel);
        currentObservation = simulation.getCurrentObservation();
        
        simulation.addObservationListener(new ObservationListener() {
            
            public void observation(Observation observation) {
                currentObservation = observation;
            }
        });
        return simulation;
    }
    
}
