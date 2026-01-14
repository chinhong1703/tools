package com.example.csvgenerator;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetRowMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
}
