package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;
import static org.demonsoft.spatialkappa.model.Utils.propogateLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Complex.MappingInstance;


public class InitialValue {

    public final List<Complex> complexes = new ArrayList<Complex>();
    public int quantity;
    public final VariableReference reference;

    public InitialValue(List<Complex> complexes, int quantity, Location location) {
        if (complexes == null || location == null) {
            throw new NullPointerException();
        }
        if (complexes.size() == 0 || quantity <= 0) {
            throw new IllegalArgumentException();
        }

        this.quantity = quantity;
        this.complexes.addAll(complexes);
        for (Complex complex : complexes) {
            propogateLocation(complex.agents, location);
        }
        this.reference = null;
    }

    public InitialValue(List<Complex> complexes, VariableReference reference, Location location) {
        if (complexes == null || reference == null || location == null) {
            throw new NullPointerException();
        }
        if (complexes.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.quantity = 0;
        this.complexes.addAll(complexes);
        for (Complex complex : complexes) {
            propogateLocation(complex.agents, location);
        }
        this.reference = reference;
    }
    

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(quantity).append("  ").append(getFlatString(complexes));
        return result.toString();
    }

    public Map<Complex, Integer> getFixedLocatedComplexMap(List<Compartment> compartments, List<Channel> channels) {
        Map<Complex, Integer> result = new HashMap<Complex, Integer>();
        
        
        for (Complex complex : complexes) {
            List<Complex> resultComplexes = new ArrayList<Complex>();

            List<MappingInstance> mappings = complex.getMappingInstances(compartments, channels);
            for (MappingInstance mapping : mappings) {
                Complex locatedComplex = new Complex(mapping.locatedAgents);
                resultComplexes.add(locatedComplex);
            }
            
            int totalComplexes = resultComplexes.size();
            if (totalComplexes > 0) {
                int quantityEach = quantity / totalComplexes;
                int remainder = quantity % totalComplexes;
                int count = 0;
                
                for (Complex resultComplex : resultComplexes) {
                    result.put(resultComplex, quantityEach + ((count++) < remainder ? 1 : 0));
                }
            }        
        }
        
        return result;
    }
}