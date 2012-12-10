package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
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
        
        int[][] indices = new int[getCellCount()][dimensions.length];
        int cellRunLength = 1;
        int cycleLength = 1;
        for (int dimensionIndex = 0; dimensionIndex < dimensions.length; dimensionIndex++) {
            cycleLength *= dimensions[dimensionIndex];
            for (int cellIndex = 0; cellIndex < indices.length; cellIndex++) {
                indices[cellIndex][dimensionIndex] = (cellIndex % cycleLength) / cellRunLength;
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
        private final int centreStartX;
        private final int centreEndX;
        private final int centreStartY;
        private final int centreEndY;

        
        public OpenRectangle(String name, int... dimensions) {
            super(name, new int[] {dimensions[HEIGHT], dimensions[WIDTH]});
            thickness = dimensions[THICKNESS];
            centreStartX = thickness;
            centreEndX = dimensions[WIDTH] - thickness - 1;
            centreStartY = thickness;
            centreEndY = dimensions[HEIGHT] - thickness - 1;

            if (dimensions.length != 3) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (dimensions[HEIGHT] < thickness * 2 || dimensions[WIDTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[WIDTH]; x++) {
                for (int y=0; y<dimensions[HEIGHT]; y++) {
                    if (isValidVoxel(y, x)) {
                        result.add(new Location(name, y, x));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
        
        @Override
        public boolean isValidVoxel(int... indices) {
            return super.isValidVoxel(indices) && 
                    !(indices[WIDTH] >= centreStartX && indices[WIDTH] <= centreEndX &&
                    indices[HEIGHT] >= centreStartY && indices[HEIGHT] <= centreEndY);
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }
    
    public static class SolidCircle extends Compartment {

        public static final String NAME = "SolidCircle";

        private static final int DIAMETER = 0;
        
        private final float rSquared;
        private final float centre;
        

        
        public SolidCircle(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER]});
            if (dimensions.length != 1) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (SolidCircle) [").append(dimensions[DIAMETER]).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    if (isValidVoxel(y, x)) {
                        result.add(new Location(name, y, x));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            return centreX*centreX + centreY*centreY <= rSquared;
        }
    }
    
    public static class OpenCircle extends Compartment {

        public static final String NAME = "OpenCircle";

        private static final int DIAMETER = 0;
        private static final int THICKNESS = 1;
        
        private final int thickness;
        private final float rSquaredOuter;
        private final float rSquaredInner;
        private final float centre;
        
        public OpenCircle(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER]});
            thickness = dimensions[THICKNESS];
            rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;

            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (dimensions[DIAMETER] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    if (isValidVoxel(y, x)) {
                        result.add(new Location(name, y, x));
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            float voxelSquared = centreX*centreX + centreY*centreY;
            return voxelSquared <= rSquaredOuter && voxelSquared >= rSquaredInner;
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }
    
    public static class OpenCuboid extends Compartment {

        public static final String NAME = "OpenCuboid";

        private static final int HEIGHT = 0;
        private static final int WIDTH = 1;
        private static final int DEPTH = 2;
        private static final int THICKNESS = 3;

        private final int thickness;
        private final int centreStartX;
        private final int centreEndX;
        private final int centreStartY;
        private final int centreEndY;
        private final int centreStartZ;
        private final int centreEndZ;

        
        public OpenCuboid(String name, int... dimensions) {
            super(name, new int[] {dimensions[HEIGHT], dimensions[WIDTH], dimensions[DEPTH]});
            thickness = dimensions[THICKNESS];
            centreStartX = thickness;
            centreEndX = dimensions[WIDTH] - thickness - 1;
            centreStartY = thickness;
            centreEndY = dimensions[HEIGHT] - thickness - 1;
            centreStartZ = thickness;
            centreEndZ = dimensions[DEPTH] - thickness - 1;

            if (dimensions.length != 4) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (dimensions[HEIGHT] < thickness * 2 || dimensions[WIDTH] < thickness * 2
                    || dimensions[DEPTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[WIDTH]; x++) {
                for (int y=0; y<dimensions[HEIGHT]; y++) {
                    for (int z=0; z<dimensions[DEPTH]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }
        
        @Override
        public boolean isValidVoxel(int... indices) {
            return super.isValidVoxel(indices) && 
                    !(indices[WIDTH] >= centreStartX && indices[WIDTH] <= centreEndX && 
                            indices[HEIGHT] >= centreStartY && indices[HEIGHT] <= centreEndY && 
                                    indices[DEPTH] >= centreStartZ && indices[DEPTH] <= centreEndZ);
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }
    
    public static class SolidSphere extends Compartment {

        public static final String NAME = "SolidSphere";

        private static final int DIAMETER = 0;

        private final float rSquared;
        private final float centre;

        public SolidSphere(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[DIAMETER]});
            rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;
            if (dimensions.length != 1) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            float centreZ = Math.abs(centre - indices[2]);
            return centreX*centreX + centreY*centreY + centreZ*centreZ <= rSquared;
        }
    }
    
    public static class OpenSphere extends Compartment {

        public static final String NAME = "OpenSphere";

        private static final int DIAMETER = 0;
        private static final int THICKNESS = 1;
        
        private final int thickness;
        private final float rSquaredOuter;
        private final float rSquaredInner;
        private final float centre;
        
        public OpenSphere(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[DIAMETER]});
            thickness = dimensions[THICKNESS];
            rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (dimensions[DIAMETER] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            float centreZ = Math.abs(centre - indices[2]);
            float voxelSquared = centreX*centreX + centreY*centreY + centreZ*centreZ;
            return voxelSquared <= rSquaredOuter && voxelSquared >= rSquaredInner;
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }
    
    public static class SolidCylinder extends Compartment {

        public static final String NAME = "SolidCylinder";

        private static final int DIAMETER = 0;
        private static final int LENGTH = 1;
        
        private final float rSquared;
        private final float centre;

        public SolidCylinder(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[LENGTH]});
            rSquared = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    if (isValidVoxel(y, x)) {
                        for (int z=0; z<dimensions[2]; z++) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            return centreX*centreX + centreY*centreY <= rSquared;
        }
    }
    
    public static class OpenCylinder extends Compartment {

        public static final String NAME = "OpenCylinder";

        private static final int DIAMETER = 0;
        private static final int LENGTH = 1;
        private static final int THICKNESS = 2;
        
        private final int thickness;
        private final float rSquaredOuter;
        private final float rSquaredInner;
        private final float centre;
        
        private final int centreStartZ;
        private final int centreEndZ;

        
        public OpenCylinder(String name, int... dimensions) {
            super(name, new int[] {dimensions[DIAMETER], dimensions[DIAMETER], dimensions[LENGTH]});
            thickness = dimensions[THICKNESS];
            rSquaredOuter = dimensions[DIAMETER] * dimensions[DIAMETER] / 4f;
            rSquaredInner = (dimensions[DIAMETER] - thickness * 2) * (dimensions[DIAMETER] - thickness * 2) / 4f;
            centre = dimensions[DIAMETER] / 2f - 0.5f;
            
            centreStartZ = thickness;
            centreEndZ = dimensions[LENGTH] - thickness - 1;
            
            if (dimensions.length != 3) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (dimensions[DIAMETER] < thickness * 2 || dimensions[LENGTH] < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
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
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = Math.abs(centre - indices[1]);
            float centreY = Math.abs(centre - indices[0]);
            float voxelSquared = centreX*centreX + centreY*centreY;
            return super.isValidVoxel(indices) && 
                    voxelSquared <= rSquaredOuter && (
                            voxelSquared >= rSquaredInner || 
                            !(indices[2] >= centreStartZ && indices[2] <= centreEndZ));
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }

    public static interface Spine {
        public int getCylinderDiameter();
    }
    
    public static class SolidSpine extends Compartment implements Spine {

        public static final String NAME = "SolidSpine";

        private static final int SPHERE_DIAMETER = 0;
        private static final int CYLINDER_DIAMETER = 1;
        private static final int CYLINDER_LENGTH = 2;
        
        private final float rSquaredSphere;
        private final float rSquaredCylinder;
        private final float centre;
        
        private final int sphereDiameter;
        private final int cylinderDiameter;
        private final int cylinderLength;

        public SolidSpine(String name, int... dimensions) {
            super(name, new int[] {dimensions[SPHERE_DIAMETER], dimensions[SPHERE_DIAMETER], 
                    dimensions[SPHERE_DIAMETER] + dimensions[CYLINDER_LENGTH]});
            sphereDiameter = dimensions[SPHERE_DIAMETER];
            cylinderDiameter = dimensions[CYLINDER_DIAMETER];
            cylinderLength = dimensions[CYLINDER_LENGTH];
            
            rSquaredSphere = sphereDiameter * sphereDiameter / 4f;
            rSquaredCylinder = cylinderDiameter * cylinderDiameter / 4f;
            centre = sphereDiameter / 2f - 0.5f;
            if (dimensions.length != 3) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (sphereDiameter % 2 != cylinderDiameter % 2) {
                throw new IllegalArgumentException("Sphere and cylinder diameters must both be either even or odd");
            }
            if (sphereDiameter < cylinderDiameter) {
                throw new IllegalArgumentException("Sphere diameter must not be less than cylinder diameter");
            }
        }
        
        public int getCylinderDiameter() {
            return cylinderDiameter;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (SolidSpine) [").append(sphereDiameter);
            builder.append("][").append(cylinderDiameter);
            builder.append("][").append(cylinderLength).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = centre - indices[1];
            float centreY = centre - indices[0];
            float centreZ = centre - indices[2];
            
            boolean insideSphere = centreX*centreX + centreY*centreY + centreZ*centreZ <= rSquaredSphere;
            boolean insideCylinder = centreX*centreX + centreY*centreY  <= rSquaredCylinder && 
                    indices[2] < sphereDiameter + cylinderLength;

            if (centreZ > 0) {
                return insideSphere;
            }
            return insideSphere || insideCylinder;
        }

    }
    
    public static class OpenSpine extends Compartment implements Spine {

        public static final String NAME = "OpenSpine";

        private static final int SPHERE_DIAMETER = 0;
        private static final int CYLINDER_DIAMETER = 1;
        private static final int CYLINDER_LENGTH = 2;
        private static final int THICKNESS = 3;
        
        private final int sphereDiameter;
        private final int cylinderDiameter;
        private final int cylinderLength;
        private final int thickness;
        
        private final float rSquaredOuterSphere;
        private final float rSquaredInnerSphere;
        private final float rSquaredOuterCylinder;
        private final float rSquaredInnerCylinder;
        private final float centre;
        
        public OpenSpine(String name, int... dimensions) {
            super(name, new int[] {dimensions[SPHERE_DIAMETER], dimensions[SPHERE_DIAMETER], 
                    dimensions[SPHERE_DIAMETER] + dimensions[CYLINDER_LENGTH]});
            sphereDiameter = dimensions[SPHERE_DIAMETER];
            cylinderDiameter = dimensions[CYLINDER_DIAMETER];
            cylinderLength = dimensions[CYLINDER_LENGTH];
            thickness = dimensions[THICKNESS];
            
            rSquaredOuterSphere = sphereDiameter * sphereDiameter / 4f;
            rSquaredInnerSphere = (sphereDiameter - thickness * 2) * (sphereDiameter - thickness * 2) / 4f;
            rSquaredOuterCylinder = cylinderDiameter * cylinderDiameter / 4f;
            rSquaredInnerCylinder = (cylinderDiameter - thickness * 2) * (cylinderDiameter - thickness * 2) / 4f;
            centre = sphereDiameter / 2f - 0.5f;
            
            if (dimensions.length != 4) {
                throw new IllegalArgumentException("Wrong number of dimensions: " + dimensions.length);
            }
            if (sphereDiameter % 2 != cylinderDiameter % 2) {
                throw new IllegalArgumentException("Sphere and cylinder diameters must both be either even or odd");
            }
            if (sphereDiameter < cylinderDiameter) {
                throw new IllegalArgumentException("Sphere diameter must not be less than cylinder diameter");
            }
            if (cylinderDiameter < thickness * 2) {
                throw new IllegalArgumentException("Too thick: " + thickness);
            }
        }
        
        public int getCylinderDiameter() {
            return cylinderDiameter;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(" (OpenSpine) [").append(sphereDiameter);
            builder.append("][").append(cylinderDiameter);
            builder.append("][").append(cylinderLength);
            builder.append("] [").append(thickness).append("]");
            return builder.toString();
        }
        
        @Override
        public Location[] getDistributedCellReferences() {
            List<Location> result = new ArrayList<Location>();
            for (int x=0; x<dimensions[1]; x++) {
                for (int y=0; y<dimensions[0]; y++) {
                    for (int z=0; z<dimensions[2]; z++) {
                        if (isValidVoxel(y, x, z)) {
                            result.add(new Location(name, y, x, z));
                        }
                    }
                }
            }
            return result.toArray(new Location[result.size()]);
        }

        @Override
        public boolean isValidVoxel(int... indices) {
            float centreX = centre - indices[1];
            float centreY = centre - indices[0];
            float centreZ = centre - indices[2];
            
            boolean insideOuterSphere = centreX*centreX + centreY*centreY + centreZ*centreZ <= rSquaredOuterSphere;
            boolean insideOuterCylinder = centreX*centreX + centreY*centreY  <= rSquaredOuterCylinder && 
                    indices[2] < sphereDiameter + cylinderLength;

            boolean insideInnerSphere = centreX*centreX + centreY*centreY + centreZ*centreZ <= rSquaredInnerSphere;
            boolean insideInnerCylinder = (centreX*centreX + centreY*centreY  <= rSquaredInnerCylinder) && 
                    dimensions[2] > indices[2];

            if (centreZ > 0) {
                return insideOuterSphere && !insideInnerSphere;
            }
            return (insideOuterSphere || insideOuterCylinder) && !(insideInnerSphere || insideInnerCylinder);
        }
        
        @Override
        public int getThickness() {
            return thickness;
        }
    }

    public boolean isValidVoxel(Location location) {
        if (!location.getName().equals(name) || location.getDimensionCount() != dimensions.length) {
            return false;
        }
        if (!location.isConcreteLocation()) {
            return true;
        }
        return isValidVoxel(location.getFixedIndices());
    }

    public boolean isValidVoxel(int... indices) {
        if (indices.length != dimensions.length) {
            return false;
        }
        for (int i=0; i<indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= dimensions[i]) {
                return false;
            }
        }
        return true;
    }
    
    
    public static Compartment createCompartment(String name, String type, List<Integer> dimensions) {
        if (name == null || dimensions == null) {
            throw new NullPointerException();
        }
        int[] dimArray = new int[dimensions.size()];
        for (int index = 0; index < dimensions.size(); index++) {
            dimArray[index] = dimensions.get(index);
        }
        try {
            if (null == type) {
                return new Compartment(name, dimArray);
            }
            else if (OpenRectangle.NAME.equals(type)) {
                return new OpenRectangle(name, dimArray);
            }
            else if (SolidCircle.NAME.equals(type)) {
                return new SolidCircle(name, dimArray);
            }
            else if (OpenCircle.NAME.equals(type)) {
                return new OpenCircle(name, dimArray);
            }
            else if (OpenCuboid.NAME.equals(type)) {
                return new OpenCuboid(name, dimArray);
            }
            else if (SolidSphere.NAME.equals(type)) {
                return new SolidSphere(name, dimArray);
            }
            else if (OpenSphere.NAME.equals(type)) {
                return new OpenSphere(name, dimArray);
            }
            else if (SolidCylinder.NAME.equals(type)) {
                return new SolidCylinder(name, dimArray);
            }
            else if (OpenCylinder.NAME.equals(type)) {
                return new OpenCylinder(name, dimArray);
            }
            else if (SolidSpine.NAME.equals(type)) {
                return new SolidSpine(name, dimArray);
            }
            else if (OpenSpine.NAME.equals(type)) {
                return new OpenSpine(name, dimArray);
            }
            else {
                throw new IllegalArgumentException("Unknown shape: " + type);
            }
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Wrong number of dimensions: " + dimArray, ex);
        }
    }

    public int getThickness() {
        return 0;
    }

    public static void translate(Compartment sourceCompartment, Compartment targetCompartment, int[] voxelIndices) {
        if (sourceCompartment.dimensions.length != targetCompartment.dimensions.length ||
                sourceCompartment.dimensions.length != voxelIndices.length) {
            throw new IllegalArgumentException("Compartment dimension mismatch");
        }
        if (sourceCompartment instanceof OpenSpine || targetCompartment instanceof OpenSpine) {
            voxelIndices[0] +=  (targetCompartment.dimensions[0] - sourceCompartment.dimensions[0]) / 2;
            voxelIndices[1] +=  (targetCompartment.dimensions[1] - sourceCompartment.dimensions[1]) / 2;
            // Open ended spine changes the calculations
            voxelIndices[2] +=  (targetCompartment.dimensions[2] - sourceCompartment.dimensions[2]);
            return;
        }
        for (int index=0; index < voxelIndices.length; index++) {
            voxelIndices[index] += (targetCompartment.dimensions[index] - sourceCompartment.dimensions[index]) / 2;
        }
    }
    
    public static void translate(Compartment sourceCompartment, Compartment targetCompartment, List<int[]> voxelIndicesList) {
        for (int[] voxelIndices : voxelIndicesList) {
            translate(sourceCompartment, targetCompartment, voxelIndices);
        }
    }
    
    public Serializable[] createVoxelArray() {
        if (dimensions.length == 0) {
            return null;
        }
        return createVoxelArray(0);
    }

    private Serializable[] createVoxelArray(int dimensionIndex) {
        int currentDimension = dimensions[dimensionIndex];
        Serializable[] result = new Serializable[currentDimension];
        for (int index=0; index < result.length; index++) {
            if (dimensionIndex == dimensions.length - 1) {
                result[index] = 0;
            }
            else {
                result[index] = createVoxelArray(dimensionIndex + 1);
            }
        }
        return result;
    }

    public void validate() {
        if (Location.FIXED_LOCATION.getName().equals(name)) {
            throw new IllegalStateException("Compartment '" + name + "' uses a reserved compartment name");
        }
    }
}
