package org.demonsoft.spatialkappa.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class DumpReplay {

    public DumpReplay(File replayFile) throws Exception {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(replayFile));
        while (true) {
            Observation observation = null;
            observation = (Observation) inputStream.readObject();
            System.out.println(observation);
            if (observation == null || observation.finalObservation) {
                break;
            }
        }

        inputStream.close();
    }

    public static void main(String[] args) {
        try {
            new DumpReplay(new File("test/data/spatial-2D-diffusion.kareplay"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
