package com.example.csvparser.spreadsheet.adapter.poi;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;

public class PoiWorkbookLoader {
    public Workbook load(byte[] content) {
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load spreadsheet", exception);
        }
    }
}
