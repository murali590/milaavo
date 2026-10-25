package com.milaavo.exception;

import java.util.UUID;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(UUID id) {
        super("The report with id " + id + " could not be found.");
    }

}
