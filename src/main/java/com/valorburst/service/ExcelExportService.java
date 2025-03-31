package com.valorburst.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public interface ExcelExportService {
    File exportRecords(LocalDate start, LocalDate end) throws IOException;
}
