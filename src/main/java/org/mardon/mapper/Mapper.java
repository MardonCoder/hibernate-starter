package org.mardon.mapper;

public interface Mapper<F, T> {
    T mapFrom(F object);
}
