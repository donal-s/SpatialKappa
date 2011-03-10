package org.demonsoft.spatialkappa.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;

public class RecordSimulation implements Simulation, ObservationListener {

    private Simulation simulation;
    private File recordFile;
    private ObjectOutputStream outputStream;
    
    public RecordSimulation(Simulation simulation, File recordFile) {
        this.simulation = simulation;
        this.recordFile = recordFile;
        simulation.addObservationListener(this);
    }
    
    public void reset() {
        simulation.reset();
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
        initialiseWriter();
        simulation.runByEvent(steps, stepSize);
    }

    public void runByTime(float steps, float stepSize) {
        initialiseWriter();
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

    private void initialiseWriter() {
        if (outputStream != null) {
            try {
                outputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(recordFile)));
            outputStream.writeObject(simulation.getCurrentObservation());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    private void stopWriter() {
        if (outputStream != null) {
            try {
                outputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
    }
    
    private synchronized void writeObservation(Observation observation) {
        try {
            outputStream.writeObject(observation);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (observation.finalObservation) {
            stopWriter();
        }
    }

}
