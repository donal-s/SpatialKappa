package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.tools.ReplaySimulation.CompartmentElementDefinition;
import org.junit.Test;

public class ReplaySimulationTest {


    @Test
    public void testNoCompartmentSimulation() {
        ReplaySimulation simulation = new ReplaySimulation(new StringReader(NO_COMPARTMENT_INPUT), 0);
        checkObservationEquals(NO_COMPARTMENT_OUTPUT[0], simulation.getCurrentObservation());
        for (int index = 1; index < NO_COMPARTMENT_OUTPUT.length; index++) {
            checkObservationEquals(NO_COMPARTMENT_OUTPUT[index], simulation.readObservation());
        }
    }

    @Test
    public void testCompartmentSimulation() {
        ReplaySimulation simulation = new ReplaySimulation(new StringReader(COMPARTMENT_INPUT), 0);
        checkObservationEquals(COMPARTMENT_OUTPUT[0], simulation.getCurrentObservation());
        for (int index = 1; index < COMPARTMENT_OUTPUT.length; index++) {
            checkObservationEquals(COMPARTMENT_OUTPUT[index], simulation.readObservation());
        }
    }
    
    @Test
    public void testBadNumberSimulation() {
        ReplaySimulation simulation = new ReplaySimulation(new StringReader(BAD_NUMBER_INPUT), 0);
        checkObservationEquals(BAD_NUMBER_OUTPUT[0], simulation.getCurrentObservation());
        for (int index = 1; index < BAD_NUMBER_OUTPUT.length; index++) {
            checkObservationEquals(BAD_NUMBER_OUTPUT[index], simulation.readObservation());
        }
    }
    
    private void checkObservationEquals(Observation expected, Observation actual) {
        assertEquals(expected.time, actual.time, 0);
        assertEquals(expected.orderedObservables, actual.orderedObservables);
        assertEquals(expected.finalObservation, actual.finalObservation);
        assertEquals(expected.elapsedTime, actual.elapsedTime);
        assertEquals(expected.estimatedRemainingTime, actual.estimatedRemainingTime);
        
        assertEquals(expected.observables.size(), actual.observables.size());
        for (Map.Entry<String, ObservationElement> current : expected.observables.entrySet()) {
            ObservationElement actualElement = actual.observables.get(current.getKey());
            assertNotNull(actualElement);
            checkObservationElementEquals(current.getValue(), actualElement);
        }
    }

    private void checkObservationElementEquals(ObservationElement expected, ObservationElement actual) {
        assertEquals(expected.value, actual.value, 0);
        assertEquals(expected.isCompartment, actual.isCompartment);
        assertArrayEquals(expected.dimensions, actual.dimensions);

        assertTrue(Arrays.deepEquals((Object[]) expected.cellValues, (Object[]) actual.cellValues));
    }

    @Test
    public void testConstructCompartmentDefinitions_compartment() {
        ReplaySimulation simulation = new ReplaySimulation(new StringReader(COMPARTMENT_INPUT), 0);
        assertEquals(COMPARTMENT_DEFINITIONS, simulation.definitions);
    }

    @Test
    public void testConstructCompartmentDefinitions_noCompartment() {
        ReplaySimulation simulation = new ReplaySimulation(new StringReader(NO_COMPARTMENT_INPUT), 0);
        assertEquals(new ArrayList<CompartmentElementDefinition>(), simulation.definitions);
    }

    
    private static final String NO_COMPARTMENT_INPUT = 
        "# time E 'Red_cytosol' 'Green_cytosol'\n" + 
        " 0.000000E+00 0    0.000000E+00 0.000000E+00\n" + 
        " 2.335532E+00 1000 1.000000E+01 1.500000E+01\n" + 
        " 4.426644E+00 2000 5.000000E+01 2.500000E+01\n" + 
        " 6.025634E+00 3000 9.000000E+01 3.500000E+01\n" + 
        " 7.619085E+00 4000 13.000000E+01 4.500000E+01\n" + 
        "";
    
    private static final Observation[] NO_COMPARTMENT_OUTPUT = {
        new Observation(0, 0, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(0)},
                        {"Green_cytosol", new ObservationElement(0)}
                }), 
                false, 0, 0),
        new Observation(2.335532E+00f, 1000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(10)},
                        {"Green_cytosol", new ObservationElement(15)}
                }), 
                false, 0, 0),
        new Observation(4.426644E+00f, 2000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(50)},
                        {"Green_cytosol", new ObservationElement(25)}
                }), 
                false, 0, 0),
        new Observation(6.025634E+00f, 3000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(90)},
                        {"Green_cytosol", new ObservationElement(35)}
                }), 
                false, 0, 0),
        new Observation(7.619085E+00f, 4000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(130)},
                        {"Green_cytosol", new ObservationElement(45)}
                }), 
                true, 0, 0),
    };

    private static final String COMPARTMENT_INPUT = 
        "# time E 'Red_cytosol_:loc~cytosol,loc_index_1~0,loc_index_2~0'" +
        " 'Red_cytosol_:loc~cytosol,loc_index_1~0,loc_index_2~1'" +
        " 'Red_cytosol_:loc~cytosol,loc_index_1~1,loc_index_2~0'" +
        " 'Red_cytosol_:loc~cytosol,loc_index_1~1,loc_index_2~1'" +
        " 'Red_cytosol' 'Green_cytosol'\n" + 
        " 0.000000E+00 0    0.000000E+00 0.000000E+00 0.000000E+00 0.000000E+00 0.000000E+00 0.000000E+00\n" + 
        " 2.335532E+00 1000 0.100000E+01 0.200000E+01 0.300000E+01 0.400000E+01 1.000000E+01 1.500000E+01\n" + 
        " 4.426644E+00 2000 1.100000E+01 1.200000E+01 1.300000E+01 1.400000E+01 5.000000E+01 2.500000E+01\n" + 
        " 6.025634E+00 3000 2.100000E+01 2.200000E+01 2.300000E+01 2.400000E+01 9.000000E+01 3.500000E+01\n" + 
        " 7.619085E+00 4000 3.100000E+01 3.200000E+01 3.300000E+01 3.400000E+01 13.000000E+01 4.500000E+01\n" + 
        "";
    
    private static final Observation[] COMPARTMENT_OUTPUT = {
        new Observation(0, 0, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(0, new int[]{2,2}, "cytosol", new float[][] {{0,0}, {0,0}})},
                        {"Green_cytosol", new ObservationElement(0)}
                }), 
                false, 0, 0),
        new Observation(2.335532E+00f, 1000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(10, new int[]{2, 2}, "cytosol", new float[][] {{1, 2}, {3, 4}})},
                        {"Green_cytosol", new ObservationElement(15)}
                }), 
                false, 0, 0),
        new Observation(4.426644E+00f, 2000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(50, new int[]{2, 2}, "cytosol", new float[][] {{11, 12}, {13, 14}})},
                        {"Green_cytosol", new ObservationElement(25)}
                }), 
                false, 0, 0),
        new Observation(6.025634E+00f, 3000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(90, new int[]{2, 2}, "cytosol", new float[][] {{21, 22}, {23, 24}})},
                        {"Green_cytosol", new ObservationElement(35)}
                }), 
                false, 0, 0),
        new Observation(7.619085E+00f, 4000, getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(130, new int[]{2, 2}, "cytosol", new float[][] {{31, 32}, {33, 34}})},
                        {"Green_cytosol", new ObservationElement(45)}
                }), 
                true, 0, 0),
    };

    private static final List<CompartmentElementDefinition> COMPARTMENT_DEFINITIONS = getList(
            new CompartmentElementDefinition("Red_cytosol", new int[]{2,2}, new Object[] {
                    new String[] { "Red_cytosol_:loc~cytosol,loc_index_1~0,loc_index_2~0",
                    "Red_cytosol_:loc~cytosol,loc_index_1~0,loc_index_2~1",},
                    new String[] {"Red_cytosol_:loc~cytosol,loc_index_1~1,loc_index_2~0",
                    "Red_cytosol_:loc~cytosol,loc_index_1~1,loc_index_2~1",}
            })
    );

    private static Map<String, ObservationElement> getObservationElementMap(Object[][] objects) {
        Map<String, ObservationElement> result = new HashMap<String, ObservationElement>();
        for (Object[] pair : objects) {
            result.put((String) pair[0], (ObservationElement) pair[1]);
        }
        return result;
    }
    
    
    private static final String BAD_NUMBER_INPUT = 
            "    # time E 'AB' 'BC' 'ABC' 'B' 'ABC_fraction'\n" + 
            "    0.000000E+00 0 0.000000E+00 0.000000E+00 0.000000E+00 0.000000E+00 NAN\n" + 
            "    6.000000E+01 0 0.000000E+00 0.000000E+00 0.000000E+00 0.000000E+00 NAN\n" + 
            "    1.800000E+02 83 1.000000E+00 2.000000E+00 1.000000E+00 0.000000E+00 INF\n" + 
            "    2.400000E+02 153 2.000000E+00 2.000000E+00 2.000000E+00 0.000000E+00 INF\n";
        
    private static final Observation[] BAD_NUMBER_OUTPUT = {
        new Observation(0, 0, getList("AB", "BC", "ABC", "B", "ABC_fraction"),
                getObservationElementMap(new Object[][] {
                        {"AB", new ObservationElement(0)},
                        {"BC", new ObservationElement(0)},
                        {"ABC", new ObservationElement(0)},
                        {"B", new ObservationElement(0)},
                        {"ABC_fraction", new ObservationElement(Float.NaN)}
                }), 
                false, 0, 0),
        new Observation(60f, 0, getList("AB", "BC", "ABC", "B", "ABC_fraction"),
                getObservationElementMap(new Object[][] {
                        {"AB", new ObservationElement(0)},
                        {"BC", new ObservationElement(0)},
                        {"ABC", new ObservationElement(0)},
                        {"B", new ObservationElement(0)},
                        {"ABC_fraction", new ObservationElement(Float.NaN)}
                }), 
                false, 0, 0),
        new Observation(180f, 83, getList("AB", "BC", "ABC", "B", "ABC_fraction"),
                getObservationElementMap(new Object[][] {
                        {"AB", new ObservationElement(1)},
                        {"BC", new ObservationElement(2)},
                        {"ABC", new ObservationElement(1)},
                        {"B", new ObservationElement(0)},
                        {"ABC_fraction", new ObservationElement(Float.POSITIVE_INFINITY)}
                }), 
                false, 0, 0),
        new Observation(240f, 153, getList("AB", "BC", "ABC", "B", "ABC_fraction"),
                getObservationElementMap(new Object[][] {
                        {"AB", new ObservationElement(2)},
                        {"BC", new ObservationElement(2)},
                        {"ABC", new ObservationElement(2)},
                        {"B", new ObservationElement(0)},
                        {"ABC_fraction", new ObservationElement(Float.POSITIVE_INFINITY)}
                }), 
                true, 0, 0),
    };

}
