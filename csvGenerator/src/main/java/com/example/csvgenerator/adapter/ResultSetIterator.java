package com.example.csvgenerator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class ResultSetIterator<T> implements Iterator<T>, AutoCloseable {
    private final ResultSet resultSet;
    private final ResultSetRowMapper<T> mapper;
    private boolean hasNext;
    private boolean checked;

    public ResultSetIterator(ResultSet resultSet, ResultSetRowMapper<T> mapper) {
        this.resultSet = Objects.requireNonNull(resultSet, "resultSet");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public boolean hasNext() {
        if (!checked) {
            try {
                hasNext = resultSet.next();
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to advance ResultSet", ex);
            }
            checked = true;
        }
        return hasNext;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        checked = false;
        try {
            return mapper.map(resultSet);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map ResultSet row", ex);
        }
    }

    @Override
    public void close() throws Exception {
        resultSet.close();
    }
}
