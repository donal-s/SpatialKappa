package org.demonsoft.spatialkappa.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;

public class ReplaySimulation implements Simulation {


    private static final String TOKEN_HASH = "#";
    private static final String TOKEN_TIME = "time";
    private static final String TOKEN_EVENTS = "E";
    private static final String COMPARTMENT_ELEMENT_PREFIX = ":";
    
    List<CompartmentElementDefinition> definitions = new ArrayList<CompartmentElementDefinition>();
    final Set<ObservationListener> listeners = new HashSet<ObservationListener>();
    int interval;
    Observation currentObservation;
    boolean stopped;
    private BufferedReader reader;
    List<String> observableNames = new ArrayList<String>();
    List<String> outputObservableNames = new ArrayList<String>();
    String nextLine;
    
    private boolean readTime = false;
    private boolean readEvents = false;
    
    
    public ReplaySimulation(Reader reader, int interval) {
        this.reader = new BufferedReader(reader);
        this.interval = interval;
        readObservationHeader();
        currentObservation = readObservation();

    }

    public void reset() {
//        stopReader();
    }

    public Observation getCurrentObservation() {
        return currentObservation;
    }

    public void removeObservationListener(ObservationListener listener) {
        listeners.remove(listener);
    }

    public void addObservationListener(ObservationListener listener) {
        listeners.add(listener);
    }

    public void runByEvent(int steps, int stepSize) {
        stopped = false;
        
        Runnable runnable = new Runnable() {
            
            public void run() {
                while (true) {
                    Observation observation = readObservation();
                    if (observation == null) {
                        break;
                    }
                    
                    currentObservation = observation;
                    if (observation.finalObservation || stopped) {
                        currentObservation = new Observation(currentObservation.time, currentObservation.event, 
                                currentObservation.orderedObservables, currentObservation.observables, true, currentObservation.elapsedTime, 0);
                    }
                    for (ObservationListener listener : listeners) {
                        listener.observation(currentObservation);
                    }
                    if (currentObservation.finalObservation) {
                        break;
                    }
                    if (interval > 0) {
                        try {
                            Thread.sleep(interval);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                stopReader();
            }
        };
        new Thread(runnable).start();
    }

    public void runByTime(float steps, float stepSize) {
        runByEvent(0, 0);
    }

    public void stop() {
        stopped = true;
    }

    public String getDebugOutput() {
        return "Replay";
    }

    void stopReader() {
        if (reader != null) {
            try {
                reader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            reader = null;
        }
    }
    
    Observation readObservation() {
        try {
            String line = nextLine;
            nextLine = reader.readLine();
            StringTokenizer tokens = new StringTokenizer(line);
            
            float time = readTime ? Float.parseFloat(tokens.nextToken()) : 0;
            int event = readEvents ? Integer.parseInt(tokens.nextToken()) : 0;
            
            Map<String, ObservationElement> elements = new HashMap<String, ObservationElement>();
            for (String observableName : observableNames) {
                String token = tokens.nextToken();
                float value;
                if ("INF".equalsIgnoreCase(token)) {
                    value = Float.POSITIVE_INFINITY;
                }
                else if ("-INF".equalsIgnoreCase(token)) {
                    value = Float.NEGATIVE_INFINITY;
                }
                else if ("NAN".equalsIgnoreCase(token) || "-NAN".equalsIgnoreCase(token)) {
                    value = Float.NaN;
                }
                else {
                    value = Float.parseFloat(token);
                }
                elements.put(observableName, new ObservationElement(value));
            }
            constructCompartmentObservations(elements);
            Observation result = new Observation(time, event, outputObservableNames, elements, nextLine == null, 0, 0);
            return result;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void constructCompartmentObservations(Map<String, ObservationElement> elements) {
        for (CompartmentElementDefinition definition : definitions) {
            Serializable[] voxelValues = constructSlice(definition.elementNames, elements);
            int totalValue = (int) elements.get(definition.compartmentName).value;
            elements.put(definition.compartmentName, new ObservationElement(totalValue, definition.dimensions, definition.compartmentName, voxelValues));
        }
    }
    
    private Serializable[] constructSlice(Object[] elementNames, Map<String, ObservationElement> elements) {
        if (elementNames[0] instanceof String) {
            Serializable[] result = new Serializable[elementNames.length];
            for (int index = 0; index < elementNames.length; index++) {
                result[index] = (int) elements.get(elementNames[index]).value;
                elements.remove(elementNames[index]);
            }
            return result;
        }
        Serializable[] result = new Serializable[elementNames.length];
        for (int index = 0; index < elementNames.length; index++) {
            result[index] = constructSlice((Object[]) elementNames[index], elements);
        }
        return result;
    }

    public static class CompartmentElementDefinition {
        
        public final String compartmentName;
        public final int[] dimensions;
        public final Object[] elementNames;
        
        public CompartmentElementDefinition(String compartmentName, int[] dimensions, Object[] elementNames) {
            this.compartmentName = compartmentName;
            this.dimensions = dimensions;
            this.elementNames = elementNames;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((compartmentName == null) ? 0 : compartmentName.hashCode());
            result = prime * result + Arrays.hashCode(dimensions);
            result = prime * result + Arrays.deepHashCode(elementNames);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CompartmentElementDefinition other = (CompartmentElementDefinition) obj;
            if (compartmentName == null) {
                if (other.compartmentName != null)
                    return false;
            }
            else if (!compartmentName.equals(other.compartmentName))
                return false;
            if (!Arrays.equals(dimensions, other.dimensions))
                return false;
            if (!Arrays.deepEquals(elementNames, other.elementNames))
                return false;
            return true;
        }
        
        @Override
        public String toString() {
            return compartmentName + ", " + Arrays.toString(dimensions) + ", " + Arrays.deepToString(elementNames);
        }
    }
    

    void readObservationHeader() {
        try {
            String line = reader.readLine();
            StringTokenizer tokens = new StringTokenizer(line);
            
            if (!tokens.hasMoreTokens()) {
                throw new IllegalArgumentException("Event or time column missing");
            }
            
            String token = tokens.nextToken();
            if (TOKEN_HASH.equals(token)) {
                if (!tokens.hasMoreTokens()) {
                    throw new IllegalArgumentException("Event or time column missing");
                }
                token = tokens.nextToken();
            }
            if (TOKEN_TIME.equals(token)) {
                readTime = true;
                token = tokens.hasMoreTokens() ? tokens.nextToken() : null;
            }
            if (TOKEN_EVENTS.equals(token)) {
                readEvents = true;
                token = tokens.hasMoreTokens() ? tokens.nextToken() : null;
            }
            
            observableNames.clear();
            while (token != null) {
                token = token.substring(1, token.length() - 1);
                observableNames.add(token);
                token = tokens.hasMoreTokens() ? tokens.nextToken() : null;
            }
            
            constructCompartmentDefinitions();
            
            nextLine = reader.readLine();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void constructCompartmentDefinitions() {
        outputObservableNames = new ArrayList<String>(observableNames);
        List<String> compartmentNames = getCompartmentNames();
        for (String compartmentName : compartmentNames) {
            List<String> elementNames = getElementNames(compartmentName);
            outputObservableNames.removeAll(elementNames);
            int[] dimensions = getDimensions(elementNames);
            Object[] orderedElementNames = getOrderedElementNames(elementNames, "", 0, dimensions);
            definitions.add(new CompartmentElementDefinition(compartmentName, dimensions, orderedElementNames));
        }
    }

    private Object[] getOrderedElementNames(List<String> elementNames, String indexPrefix, int dimension, int[] dimensions) {
        if (dimension == dimensions.length - 1) {
            String[] result = new String[dimensions[dimension]];
            for (int index = 0; index < result.length; index++) {
                String indexString = indexPrefix + "[" + index +  "]";
                for (String current : elementNames) {
                    if (current.contains(indexString)) {
                        result[index] = current;
                        break;
                    }
                }
            }
            return result;
        }
        
        Object[] result = new Object[dimensions[dimension]];
        for (int index = 0; index < result.length; index++) {
            String indexString = indexPrefix + "[" + index +  "]";
            result[index] = getOrderedElementNames(elementNames, indexString, dimension + 1, dimensions);
        }
        return result;
    }

    private int[] getDimensions(List<String> elementNames) {
        String elementName = elementNames.get(0);
        int highestDimension = 0;
        int currentDimensions = count(elementName, '[');
        if (currentDimensions > highestDimension) {
            highestDimension = currentDimensions;
        }
        int[] result = new int[highestDimension];
        for (String current : elementNames) {
            int[] dimensions = getDimensions(current);
            for (int index = 0; index < dimensions.length; index++) {
                if (dimensions[index] > result[index]) {
                    result[index] = dimensions[index];
                }
            }
        }
        return result;
    }

    private int[] getDimensions(String elementName) {
        int[] result = new int[count(elementName, '[')];
        int startIndex = 0;
        int dimensionIndex = 0;
        while (dimensionIndex < result.length) {
            startIndex = elementName.indexOf('[', startIndex) + 1;
            int endIndex = elementName.indexOf(']', startIndex);
            result[dimensionIndex++] = Integer.parseInt(elementName.substring(startIndex, endIndex)) + 1;
            startIndex = endIndex;
        }
        return result;
    }

    private int count(String input, char matchChar) {
        int result = 0;
        for (char c : input.toCharArray()) {
            if (c == matchChar) {
                result++;
            }
        }
        return result;
    }

    private List<String> getElementNames(String compartmentName) {
        List<String> result = new ArrayList<String>();
        for (String observableName : observableNames) {
            if (observableName.startsWith(compartmentName + COMPARTMENT_ELEMENT_PREFIX)) {
                result.add(observableName);
            }
        }
        return result;
    }

    private List<String> getCompartmentNames() {
        List<String> result = new ArrayList<String>();
        for (String observableName : observableNames) {
            if (observableName.contains(COMPARTMENT_ELEMENT_PREFIX)) {
                String compartmentName = observableName.substring(0, observableName.indexOf(COMPARTMENT_ELEMENT_PREFIX));
                if (!result.contains(compartmentName)) {
                    result.add(compartmentName);
                }
            }
        }
        return result;
    }
}
