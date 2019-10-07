package com.eigendomain.eslatticeindex.index;

public class BaseLatticeTokenParts extends LatticeTokenParts<BaseLatticeTokenParts> {
    public BaseLatticeTokenParts(char fieldDelimiter) {
        super(fieldDelimiter);
    }

    public static class Factory implements LatticeTokenPartsFactory<BaseLatticeTokenParts> {
        @Override
        public BaseLatticeTokenParts create(char fieldDelimiter) {
            return new BaseLatticeTokenParts(fieldDelimiter);
        }
    }
}
