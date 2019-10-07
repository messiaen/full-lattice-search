package com.eigendomain.eslatticeindex.index;

public class AudioLatticeTokenParts extends LatticeTokenParts<AudioLatticeTokenParts> {
    private final float positionIncrementSecs;

    private float startTime;
    private float stopTime;

    public AudioLatticeTokenParts(char fieldDelimiter, float positionIncrementSecs) {
        super(fieldDelimiter);
        this.positionIncrementSecs = positionIncrementSecs;
        this.reset();
    }

    public float startTime() {
        return startTime;
    }

    public float stopTime() {
        return stopTime;
    }

    @Override
    public void reset() {
        super.reset();
        startTime = 0;
        stopTime = 0;
    }

    @Override
    public int numFields() {
        return super.numFields() + 2;
    }

    @Override
    public int positionIncrement(AudioLatticeTokenParts lastTokenParts) {
        if (super.positionIncrement(lastTokenParts) == 0) {
           return 0;
        }

        // TODO first round of this will use the start of each word
        return (int)Math.ceil((this.startTime - lastTokenParts.startTime) / this.positionIncrementSecs);
    }

    @Override
    public boolean parseFields(char[] token, int len, int[] delimiterLocs) {
        super.parseFields(token, len, delimiterLocs);
        int fieldsOff = super.numFields() + 1;

        this.startTime = parseFloat(token, len, delimiterLocs, fieldsOff);
        this.stopTime = parseFloat(token, len, delimiterLocs, fieldsOff+1);
        return true;
    }

    public static class Factory implements LatticeTokenPartsFactory<AudioLatticeTokenParts> {
        private float incSecs;

        public Factory(float positionIncrementSecs) {
            super();
           incSecs = positionIncrementSecs;
        }

        @Override
        public AudioLatticeTokenParts create(char fieldDelimiter) {
            return new AudioLatticeTokenParts(fieldDelimiter, incSecs);
        }
    }
}
