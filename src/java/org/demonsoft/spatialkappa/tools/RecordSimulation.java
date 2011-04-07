package org.demonsoft.spatialkappa.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;

public class RecordSimulation implements Simulation, ObservationListener {

    private Simulation simulation;
    private Writer writer;
    private boolean firstObservation;
    
    public RecordSimulation(Simulation simulation, File recordFile) throws IOException {
        this(simulation, new FileWriter(recordFile));
    }
    
    public RecordSimulation(Simulation simulation, Writer writer) {
        this.simulation = simulation;
        this.writer = writer;
        firstObservation = true;
        simulation.addObservationListener(this);
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
                writer.write(observation.toKaSimHeaderString());
                firstObservation = false;
            }
            writer.write(observation.toKaSimString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (observation.finalObservation) {
            stopWriter();
        }
    }

    
    
}
