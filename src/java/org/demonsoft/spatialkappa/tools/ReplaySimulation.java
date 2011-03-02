package org.demonsoft.spatialkappa.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;

public class ReplaySimulation implements Simulation {


    private final Map<String, LocatedObservable> observables = new HashMap<String, LocatedObservable>();

    final Set<ObservationListener> listeners = new HashSet<ObservationListener>();
    private File recordFile;
    int interval;
    private ObjectInputStream inputStream;
    private List<LocatedObservable> locatedObservables;
    Observation currentObservation;
    boolean stopped;
    
    
    public ReplaySimulation(File recordFile, int interval) {
        this.recordFile = recordFile;
        this.interval = interval;
        initialiseReader();
    }
    
    public void reset() {
        stopReader();
        initialiseReader();
    }

    public Observation getCurrentObservation() {
        return currentObservation;
    }

    public void removeObservationListener(ObservationListener listener) {
        listeners.remove(listener);
    }

    public List<LocatedObservable> getLocatedObservables() {
        return locatedObservables;
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
                        currentObservation = new Observation(currentObservation.time, currentObservation.observables, true, currentObservation.elapsedTime, 0);
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

    @Override
    public String toString() {
        return "Replay of file: " + recordFile.getPath();
    }
    
    @SuppressWarnings("unchecked")
    private void initialiseReader() {
        if (inputStream != null) {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            inputStream = new ObjectInputStream(new FileInputStream(recordFile));
            List<LocatedObservable> newObservables = (List<LocatedObservable>) inputStream.readObject();
            if (locatedObservables == null) {
                locatedObservables = newObservables;
                for (LocatedObservable observable : locatedObservables) {
                    observables.put(observable.label, observable);
                }
            }
            currentObservation = readObservation();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
    }
    
    void stopReader() {
        if (inputStream != null) {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
    }
    
    Observation readObservation() {
        Observation observation = null;
        try {
            observation = canonicaliseObservation((Observation) inputStream.readObject());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (observation == null || observation.finalObservation) {
            stopReader();
        }
        return observation;
    }

    private Observation canonicaliseObservation(Observation observation) {
        Map<LocatedObservable, ObservationElement> elementMap = new HashMap<LocatedObservable, ObservationElement>();
        
        for (Map.Entry<LocatedObservable, ObservationElement> entry : observation.observables.entrySet()) {
            elementMap.put(observables.get(entry.getKey().label), entry.getValue());
        }
        
        observation.observables.clear();
        observation.observables.putAll(elementMap);
        return observation;
    }


    
}
