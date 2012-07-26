package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.List;


public class Compartment {

    protected final String name;
    protected final int[] dimensions;
    
    public Compartment(String name, int... dimensions) {
        if (name == null) {
            throw new NullPointerException();
        }
        for (int dimension : dimensions) {
            if (dimension <= 0) {
                throw new IllegalArgumentException("" + dimension);
            }
        }
        this.name = name;
        this.dimensions = dimensions;
    }

    public String getName() {
        return name;
    }

    // TODO - remember - only limits when talking about spheres/cylinders/etc
    @Deprecated
    public int[] getDimensions() {
        return dimensions;
    }
    
    @Override
    public String toString() {
        if (dimensions == null || dimensions.length == 0) {
            return name;
        }
        StringBuilder builder = new StringBuilder(name);
        for (int dimension : dimensions) {
            builder.append("[").append(dimension).append("]");
        }
        return builder.toString();
    }

    private int getCellCount() {
        int result = 1;
        for (int dimension : dimensions) {
            result *= dimension;
        }
        return result;
    }

    public Location[] getDistributedCellReferences() {
        if (dimensions.length == 0) {
            return new Location[] { new Location(name) };
        }
        
        CellIndexExpression[][] indices = new CellIndexExpression[getCellCount()][dimensions.length];
        int cellRunLength = 1;
        int cycleLength = 1;
        for (int dimensionIndex = 0; dimensionIndex < dimensions.length; dimensionIndex++) {
            cycleLength *= dimensions[dimensionIndex];
            for (int cellIndex = 0; cellIndex < indices.length; cellIndex++) {
                indices[cellIndex][dimensionIndex] = new CellIndexExpression("" + ((cellIndex % cycleLength) / cellRunLength));
            }
            cellRunLength = cycleLength;
        }

        Location[] result = new Location[indices.length];
        for (int index = 0; index < indices.length; index++) {
            result[index] = new Location(name, indices[index]);
        }
        return result;
    }

    
    public static class OpenRectangle extends Compartment {

        public static final String NAME = "OpenRectangle";

        private static final int HEIGHT = 0;
        private static final int WIDTH = 1;
        private static final int THICKNESS = 2;
        
        private final int thickness;
        
        public OpenRectangle(String name, int[] dimensions) {
            super(name, new int[] {dimensions[HEIGHT], dimensions[WIDTH]});
            thickness = dimensions[THICKNESS];
            if (dimensions.length != 3) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
            if (dimensions[HEIGHT] < thickness * 2 || dimensions[WIDTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenRectangle) [").append(dimensions[HEIGHT]).append("][");
            builder.append(dimensions[WIDTH]).append("] [").append(thickness).append("]");
            return builder.toString();
        }

        @Override
        public Location[] getDistributedCellReferences() {
            int centreStartX = thickness;
            int centreEndX = dimensions[WIDTH] - thickness - 1;
            int centreStartY = thickness;
            int centreEndY = dimensions[HEIGHT] - thickness - 1;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[WIDTH]; x++) {
                for (int y=0; y<dimensions[HEIGHT]; y++) {
                    if (!(x >= centreStartX && x <= centreEndX && y >= centreStartY && y <= centreEndY)) {
                        result.add(new Location(name, new CellIndexExpression("" + y), new CellIndexExpression("" + x)));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
    }
    
    public static class SolidCircle extends Compartment {

        public static final String NAME = "SolidCircle";

        private static final int DIAMETER = 0;
        
        public SolidCircle(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER]});
            if (dimensions.length != 1) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (SolidCircle) [").append(dimensions[DIAMETER]).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            float rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    float centreX = Math.abs(centre - x);
                    float centreY = Math.abs(centre - y);
                    if (centreX*centreX + centreY*centreY <= rSquared) {
                        result.add(new Location(name, new CellIndexExpression("" + y), new CellIndexExpression("" + x)));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

    }
    
    public static class OpenCircle extends Compartment {

        public static final String NAME = "OpenCircle";

        private static final int DIAMETER = 0;
        private static final int THICKNESS = 1;
        
        private final int thickness;
        
        public OpenCircle(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER]});
            thickness = dimensions[THICKNESS];
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
            if (dimensions[DIAMETER] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenCircle) [").append(dimensions[DIAMETER]).append("] [");
            builder.append(thickness).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            float rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    float centreX = Math.abs(centre - x);
                    float centreY = Math.abs(centre - y);
                    float voxelSquared = centreX*centreX + centreY*centreY;
                    if (voxelSquared <= rSquaredOuter && voxelSquared >= rSquaredInner) {
                        result.add(new Location(name, new CellIndexExpression("" + y), new CellIndexExpression("" + x)));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

    }
    
    public static class OpenCuboid extends Compartment {

        public static final String NAME = "OpenCuboid";

        private static final int HEIGHT = 0;
        private static final int WIDTH = 1;
        private static final int DEPTH = 2;
        private static final int THICKNESS = 3;

        private final int thickness;
        
        public OpenCuboid(String name, int[] dimensions) {
            super(name, new int[] {dimensions[HEIGHT], dimensions[WIDTH], dimensions[DEPTH]});
            thickness = dimensions[THICKNESS];
            if (dimensions.length != 4) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
            if (dimensions[HEIGHT] < thickness * 2 || dimensions[WIDTH] < thickness * 2
                    || dimensions[DEPTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenCuboid) [").append(dimensions[HEIGHT]);
            builder.append("][").append(dimensions[WIDTH]).append("][");
            builder.append(dimensions[DEPTH]).append("] [").append(thickness).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            int centreStartX = thickness;
            int centreEndX = dimensions[WIDTH] - thickness - 1;
            int centreStartY = thickness;
            int centreEndY = dimensions[HEIGHT] - thickness - 1;
            int centreStartZ = thickness;
            int centreEndZ = dimensions[DEPTH] - thickness - 1;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[WIDTH]; x++) {
                for (int y=0; y<dimensions[HEIGHT]; y++) {
                    for (int z=0; z<dimensions[DEPTH]; z++) {
                        if (!(x >= centreStartX && x <= centreEndX && y >= centreStartY && y <= centreEndY
                                && z >= centreStartZ && z <= centreEndZ)) {
                            result.add(new Location(name, new CellIndexExpression("" + y), 
                                    new CellIndexExpression("" + x), new CellIndexExpression("" + z)));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
    }
    
    public static class SolidSphere extends Compartment {

        public static final String NAME = "SolidSphere";

        private static final int DIAMETER = 0;
        
        public SolidSphere(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[DIAMETER]});
            if (dimensions.length != 1) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (SolidSphere) [").append(dimensions[DIAMETER]).append("]");
            return builder.toString();
        }

        @Override
        public Location[] getDistributedCellReferences() {
            float rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        float centreX = Math.abs(centre - x);
                        float centreY = Math.abs(centre - y);
                        float centreZ = Math.abs(centre - z);
                        if (centreX*centreX + centreY*centreY + centreZ*centreZ <= rSquared) {
                            result.add(new Location(name, new CellIndexExpression("" + y), 
                                    new CellIndexExpression("" + x), new CellIndexExpression("" + z)));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
    }
    
    public static class OpenSphere extends Compartment {

        public static final String NAME = "OpenSphere";

        private static final int DIAMETER = 0;
        private static final int THICKNESS = 1;
        
        private final int thickness;
        
        public OpenSphere(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[DIAMETER]});
            thickness = dimensions[THICKNESS];
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
            if (dimensions[DIAMETER] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenSphere) [").append(dimensions[DIAMETER]).append("] [");
            builder.append(thickness).append("]");
            return builder.toString();
        }

        @Override
        public Location[] getDistributedCellReferences() {
            float rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        float centreX = Math.abs(centre - x);
                        float centreY = Math.abs(centre - y);
                        float centreZ = Math.abs(centre - z);
                        float voxelSquared = centreX*centreX + centreY*centreY + centreZ*centreZ;
                        if (voxelSquared <= rSquaredOuter && voxelSquared >= rSquaredInner) {
                            result.add(new Location(name, new CellIndexExpression("" + y), 
                                    new CellIndexExpression("" + x), new CellIndexExpression("" + z)));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
    }
    
    public static class SolidCylinder extends Compartment {

        public static final String NAME = "SolidCylinder";

        private static final int DIAMETER = 0;
        private static final int LENGTH = 1;
        
        public SolidCylinder(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[LENGTH]});
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (SolidCylinder) [").append(dimensions[DIAMETER]);
            builder.append("][").append(dimensions[2]).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            float rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    float centreX = Math.abs(centre - x);
                    float centreY = Math.abs(centre - y);
                    if (centreX*centreX + centreY*centreY <= rSquared) {
                        for (int z=0; z<dimensions[2]; z++) {
                            result.add(new Location(name, new CellIndexExpression("" + y), 
                                    new CellIndexExpression("" + x), new CellIndexExpression("" + z)));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

    }
    
    public static class OpenCylinder extends Compartment {

        public static final String NAME = "OpenCylinder";

        private static final int DIAMETER = 0;
        private static final int LENGTH = 1;
        private static final int THICKNESS = 2;
        
        private final int thickness;
        
        public OpenCylinder(String name, int[] dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[LENGTH]});
            thickness = dimensions[THICKNESS];
            if (dimensions.length != 3) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions);
            }
            if (dimensions[DIAMETER] < thickness * 2 || dimensions[LENGTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + dimensions);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenCylinder) [").append(dimensions[DIAMETER]).append("][");
            builder.append(dimensions[2]).append("] [");
            builder.append(thickness).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            float rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            float rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            float centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            int centreStartZ = thickness;
            int centreEndZ = dimensions[2] - thickness - 1;

            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    float centreX = Math.abs(centre - x);
                    float centreY = Math.abs(centre - y);
                    float voxelSquared = centreX*centreX + centreY*centreY;
                    for (int z=0; z<dimensions[2]; z++) {
                        if (voxelSquared <= rSquaredOuter && voxelSquared >= rSquaredInner ||
                            voxelSquared <= rSquaredOuter && !(z >= centreStartZ && z <= centreEndZ)) {
                            result.add(new Location(name, new CellIndexExpression("" + y),
                                    new CellIndexExpression("" + x), new CellIndexExpression("" + z)));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

    }
}
