package org.demonsoft.spatialkappa.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;

public class RepairOutputFile {

    private static final String[] FILEPATHS = {
        "test/data/input1.replay",
        "test/data/input2.replay",
    };

    
    
    public RepairOutputFile() throws Exception {
        Map<String, LocatedObservable> observables = new HashMap<String, LocatedObservable>();
        
        List<LocatedObservable> locatedObservables = null;
        float fileStartTime = 0f;
        long fileStartElapsedTime = 0;
        
        File outputFile = new File("test/data/output.kareplay");
        ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        Observation possibleFinalObservation = null;
        try {
            for (String currentFilepath : FILEPATHS) {
                File inputFile = new File(currentFilepath);
                ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
                try {
                    if (possibleFinalObservation != null) {
                        fileStartTime = possibleFinalObservation.time;
                        fileStartElapsedTime = possibleFinalObservation.elapsedTime;
                        outputStream.writeObject(new Observation(possibleFinalObservation.time, 
                                possibleFinalObservation.observables, false, possibleFinalObservation.elapsedTime, 
                                possibleFinalObservation.estimatedRemainingTime));
                        possibleFinalObservation = null;
                    }
                    
                    
                    @SuppressWarnings("unchecked")
                    List<LocatedObservable> newObservables = (List<LocatedObservable>) inputStream.readObject();
                    if (locatedObservables == null) {
                        locatedObservables = newObservables;
                        for (LocatedObservable observable : locatedObservables) {
                            observables.put(observable.label, observable);
                        }
                        outputStream.writeObject(locatedObservables);
                    }
                    
                    while (true) {
                        Observation observation = canonicaliseObservation((Observation) inputStream.readObject(), observables, fileStartTime, fileStartElapsedTime);
                        
                        if (observation.finalObservation) {
                            possibleFinalObservation = observation;
                            break;
                        }
                           
                        outputStream.writeObject(observation);
                    }
                    
                    
                    
                }
                finally {
                    inputStream.close();
                }
            }
            if (possibleFinalObservation != null) {
                outputStream.writeObject(possibleFinalObservation);
            }
        }
        finally {
            outputStream.close();
        }
        
    }
    
    
    private Observation canonicaliseObservation(Observation observation, Map<String, LocatedObservable> observables, float fileStartTime, long fileStartElapsedTime) {
        Map<LocatedObservable, ObservationElement> elementMap = new HashMap<LocatedObservable, ObservationElement>();
        
        for (Map.Entry<LocatedObservable, ObservationElement> entry : observation.observables.entrySet()) {
            elementMap.put(observables.get(entry.getKey().label), entry.getValue());
        }
        return new Observation(observation.time + fileStartTime, elementMap, observation.finalObservation, 
                observation.elapsedTime + fileStartElapsedTime, observation.estimatedRemainingTime);
    }
    
    public static void main(String[] args) {
        try {
            new RepairOutputFile();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    
}
