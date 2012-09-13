package org.demonsoft.spatialkappa.tools;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.model.Utils;
import org.junit.Test;

public class RecordSimulationTest {


    @Test
    public void testNoCompartmentSimulation() {
        StringWriter writer = new StringWriter();
        RecordSimulation simulation = new RecordSimulation(new TestSimulation(NO_COMPARTMENT_INPUT), writer);
        simulation.runByEvent(0, 0);
        assertEquals(NO_COMPARTMENT_OUTPUT, writer.toString());
    }

    @Test
    public void testCompartmentSimulation() {
        StringWriter writer = new StringWriter();
        RecordSimulation simulation = new RecordSimulation(new TestSimulation(COMPARTMENT_INPUT), writer);
        simulation.runByEvent(0, 0);
        assertEquals(COMPARTMENT_OUTPUT, writer.toString());
    }

    private static final String NO_COMPARTMENT_OUTPUT = 
        "# time E 'Red_cytosol' 'Green_cytosol'\n" + 
        " 0.000000E00 0 0.000000E00 0.000000E00\n" + 
        " 2.335532E00 1000 1.000000E01 1.500000E01\n" + 
        " 4.426644E00 2000 5.000000E01 2.500000E01\n" + 
        " 6.025634E00 3000 9.000000E01 3.500000E01\n" + 
        " 7.619085E00 4000 1.300000E02 4.500000E01\n";
    
    private static final Observation[] NO_COMPARTMENT_INPUT = {
        new Observation(0, 0, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(0)},
                        {"Green_cytosol", new ObservationElement(0)}
                }), 
                false, 0, 0),
        new Observation(2.335532E+00f, 1000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(10)},
                        {"Green_cytosol", new ObservationElement(15)}
                }), 
                false, 0, 0),
        new Observation(4.426644E+00f, 2000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(50)},
                        {"Green_cytosol", new ObservationElement(25)}
                }), 
                false, 0, 0),
        new Observation(6.025634E+00f, 3000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(90)},
                        {"Green_cytosol", new ObservationElement(35)}
                }), 
                false, 0, 0),
        new Observation(7.619085E+00f, 4000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(130)},
                        {"Green_cytosol", new ObservationElement(45)}
                }), 
                true, 0, 0),
    };

    private static final String COMPARTMENT_OUTPUT = 
        "# time E 'Red_cytosol:cytosol[0][0]'" +
        " 'Red_cytosol:cytosol[0][1]'" +
        " 'Red_cytosol:cytosol[1][0]'" +
        " 'Red_cytosol:cytosol[1][1]'" +
        " 'Red_cytosol' 'Green_cytosol'\n" + 
        " 0.000000E00 0 0.000000E00 0.000000E00 0.000000E00 0.000000E00 0.000000E00 0.000000E00\n" + 
        " 2.335532E00 1000 1.000000E00 2.000000E00 3.000000E00 4.000000E00 1.000000E01 1.500000E01\n" + 
        " 4.426644E00 2000 1.100000E01 1.200000E01 1.300000E01 1.400000E01 5.000000E01 2.500000E01\n" + 
        " 6.025634E00 3000 2.100000E01 2.200000E01 2.300000E01 2.400000E01 9.000000E01 3.500000E01\n" + 
        " 7.619085E00 4000 3.100000E01 3.200000E01 3.300000E01 3.400000E01 1.300000E02 4.500000E01\n";
    
    private static final Observation[] COMPARTMENT_INPUT = {
        new Observation(0, 0, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(0, new int[]{2,2}, "cytosol", new Serializable[][] {{0,0}, {0,0}})},
                        {"Green_cytosol", new ObservationElement(0)}
                }), 
                false, 0, 0),
        new Observation(2.335532E+00f, 1000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(10, new int[]{2, 2}, "cytosol", new Serializable[][] {{1, 2}, {3, 4}})},
                        {"Green_cytosol", new ObservationElement(15)}
                }), 
                false, 0, 0),
        new Observation(4.426644E+00f, 2000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(50, new int[]{2, 2}, "cytosol", new Serializable[][] {{11, 12}, {13, 14}})},
                        {"Green_cytosol", new ObservationElement(25)}
                }), 
                false, 0, 0),
        new Observation(6.025634E+00f, 3000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(90, new int[]{2, 2}, "cytosol", new Serializable[][] {{21, 22}, {23, 24}})},
                        {"Green_cytosol", new ObservationElement(35)}
                }), 
                false, 0, 0),
        new Observation(7.619085E+00f, 4000, Utils.getList("Red_cytosol", "Green_cytosol"),
                getObservationElementMap(new Object[][] {
                        {"Red_cytosol", new ObservationElement(130, new int[]{2, 2}, "cytosol", new Serializable[][] {{31, 32}, {33, 34}})},
                        {"Green_cytosol", new ObservationElement(45)}
                }), 
                true, 0, 0),
    };

    private static Map<String, ObservationElement> getObservationElementMap(Object[][] objects) {
        Map<String, ObservationElement> result = new HashMap<String, ObservationElement>();
        for (Object[] pair : objects) {
            result.put((String) pair[0], (ObservationElement) pair[1]);
        }
        return result;
    }
    
    class TestSimulation implements Simulation {

        private final List<ObservationListener> listeners = new ArrayList<ObservationListener>();
        private Observation[] observations;
        
        public TestSimulation(Observation[] observations) {
            this.observations = observations;
        }
        
        public void reset() {
            // Do nothing
        }

        public Observation getCurrentObservation() {
            return observations[0];
        }

        public void removeObservationListener(ObservationListener listener) {
            listeners.remove(listener);
        }

        public void addObservationListener(ObservationListener listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        public void runByEvent(int steps, int stepSize) {
            for (Observation observation : observations) {
                for (ObservationListener listener : listeners) {
                    listener.observation(observation);
                }
            }
        }

        public void runByTime(float steps, float stepSize) {
            // Do nothing
        }

        public void stop() {
            // Do nothing
        }

        public String getDebugOutput() {
            return null;
        }
        
    }
    
}
