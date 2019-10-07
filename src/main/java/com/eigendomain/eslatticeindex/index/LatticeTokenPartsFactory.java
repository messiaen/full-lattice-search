package com.eigendomain.eslatticeindex.index;

public interface LatticeTokenPartsFactory<T extends LatticeTokenParts<T>> {
    T create(char fieldDelimiter);
}
