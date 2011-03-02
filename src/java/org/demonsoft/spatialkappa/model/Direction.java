package org.demonsoft.spatialkappa.model;

public enum Direction {
    FORWARD {
        @Override
        public String toString() {
            return "->";
        }
    },
    BACKWARD {
        @Override
        public String toString() {
            return "<-";
        }
    },
    BIDIRECTIONAL {
        @Override
        public String toString() {
            return "<->";
        }
    },
}