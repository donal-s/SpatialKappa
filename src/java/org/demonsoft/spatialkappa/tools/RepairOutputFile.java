package org.demonsoft.spatialkappa.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.demonsoft.spatialkappa.model.Observation;

public class RepairOutputFile {

    private static final String[] FILEPATHS = {
        "test/data/input1.replay",
        "test/data/input2.replay",
    };

    
    
    public RepairOutputFile() throws Exception {
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
                                possibleFinalObservation.orderedObservables,
                                possibleFinalObservation.observables, false, possibleFinalObservation.elapsedTime, 
                                possibleFinalObservation.estimatedRemainingTime));
                        possibleFinalObservation = null;
                    }
                    
                    while (true) {
                        Observation observation = canonicaliseObservation((Observation) inputStream.readObject(), fileStartTime, fileStartElapsedTime);
                        
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
    
    
    private Observation canonicaliseObservation(Observation observation, float fileStartTime, long fileStartElapsedTime) {
        return new Observation(observation.time + fileStartTime, observation.orderedObservables, observation.observables, observation.finalObservation, 
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
