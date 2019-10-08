package com.eigendomain.eslatticeindex;

public enum LatticeFormat {
    LATTICE("lattice"),
    AUDIO("audio");

    private String name;
    private LatticeFormat(String name) {
        this.name = name;
    }


    public String toString() {
        return name;
    }

    public static LatticeFormat fromString(String name) {
        for (LatticeFormat f : LatticeFormat.values()) {
            if (f.toString().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }
}
