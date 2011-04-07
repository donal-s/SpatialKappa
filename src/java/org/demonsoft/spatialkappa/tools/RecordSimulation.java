package org.demonsoft.spatialkappa.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;

public class RecordSimulation implements Simulation, ObservationListener {

    private final DecimalFormat valueFormat;
    private Simulation simulation;
    private Writer writer;
    private boolean firstObservation;
    private File recordFile;
    
    public RecordSimulation(Simulation simulation, File recordFile) throws IOException {
        this(simulation, new FileWriter(recordFile));
        this.recordFile = recordFile;
    }
    
    public RecordSimulation(Simulation simulation, Writer writer) {
        this.simulation = simulation;
        this.writer = writer;
        firstObservation = true;
        simulation.addObservationListener(this);
        
        valueFormat = new DecimalFormat("0.000000E00");
        DecimalFormatSymbols symbols = valueFormat.getDecimalFormatSymbols();
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");
        valueFormat.setDecimalFormatSymbols(symbols);
    }

    public void reset() {
        if (recordFile == null) {
            throw new IllegalStateException("Reset of writer based record not allowed");
        }
        simulation.reset();
        try {
            writer = new FileWriter(recordFile);
        }
        catch (IOException ex) {
            throw new RuntimeException("Problem reopening output file", ex);
        }
    }

    public Observation getCurrentObservation() {
        return simulation.getCurrentObservation();
    }

    public void removeObservationListener(ObservationListener listener) {
        simulation.removeObservationListener(listener);
    }

    public void addObservationListener(ObservationListener listener) {
        simulation.addObservationListener(listener);

    }

    public void runByEvent(int steps, int stepSize) {
        simulation.runByEvent(steps, stepSize);
    }

    public void runByTime(float steps, float stepSize) {
        simulation.runByTime(steps, stepSize);
    }

    public void stop() {
        simulation.stop();
    }

    public String getDebugOutput() {
        return simulation.getDebugOutput();
    }

    @Override
    public String toString() {
        return simulation.toString();
    }
    
    public void observation(Observation observation) {
        writeObservation(observation);
    }

    private void stopWriter() {
        if (writer != null) {
            try {
                writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            writer = null;
        }
    }
    
    private synchronized void writeObservation(Observation observation) {
        
        try {
            if (firstObservation) {
                writer.write(toKaSimHeaderString(observation));
                firstObservation = false;
            }
            writer.write(toKaSimString(observation));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (observation.finalObservation) {
            stopWriter();
        }
    }

    private String toKaSimString(Observation observation) {
        StringBuilder result = new StringBuilder();
        result.append(" ").append(valueFormat.format(observation.time));
        result.append(" ").append(observation.event);
        
        for (String observableName : observation.orderedObservables) {
            ObservationElement element = observation.observables.get(observableName);
            
            if (element.isCompartment) {
                result.append(toKaSimString(element.cellValues));
            }
            result.append(" ").append(valueFormat.format(element.value));
        }
        
        result.append("\n");
        return result.toString();
    }

    
    private String toKaSimString(Object cells) {
        StringBuilder result = new StringBuilder();
        if (cells instanceof float[]) {
            for (float cell : (float[]) cells) {
                result.append(" ").append(valueFormat.format(cell));
            }
        }
        else {
            for (Object slice : (Object[]) cells) {
                result.append(toKaSimString(slice));
            }
        }
        return result.toString();
    }
    

    
    private String toKaSimHeaderString(Observation observation) {
        StringBuilder result = new StringBuilder();
        result.append("# time E");
        
        for (String observableName : observation.orderedObservables) {
            ObservationElement element = observation.observables.get(observableName);
            
            if (element.isCompartment) {
                String[] cellIndices = getCellIndexStrings(element.dimensions, 0);
                for (String cellIndex : cellIndices) {
                    String name = observableName + "_:loc~" + element.compartmentName + cellIndex;
                    result.append(" '").append(name.replace(' ', '_')).append("'");
                }
            }
            result.append(" '").append(observableName.replace(' ', '_')).append("'");
            
        }
        
        result.append("\n");
        return result.toString();
    }

    
    String[] getCellIndexStrings(int[] dimensions, int index) {
        String prefix = ",loc_index_" + (index + 1) + "~";
        String[] suffixes = new String[] {""};
        
        if (index + 1 < dimensions.length) {
            suffixes = getCellIndexStrings(dimensions, index + 1);
        }
        
        String[] result = new String[dimensions[index] * suffixes.length];
        int resultIndex = 0;
        for (int outer = 0; outer < dimensions[index]; outer++) {
            for (int inner = 0; inner < suffixes.length; inner++) {
                result[resultIndex++] = prefix + outer + suffixes[inner];
            }
        }
        return result;
    }

    
    
}
