package org.demonsoft.spatialkappa.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;

public class DumpReplay {

    @SuppressWarnings("unchecked")
    public DumpReplay(File replayFile) throws Exception {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(replayFile));
        List<LocatedObservable> locatedObservables = (List<LocatedObservable>) inputStream.readObject();
        System.out.println(locatedObservables);
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
