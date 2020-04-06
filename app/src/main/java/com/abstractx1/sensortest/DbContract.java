package com.abstractx1.sensortest;

import android.provider.BaseColumns;

public final class DbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DbContract() {}

    /* Inner class that defines the table contents */
    public static class EventEntry {
        public static final String TABLE_NAME = "Event";
        public static final String COLUMN_NAME_EVENT_ID = "EventId";
        public static final String COLUMN_NAME_MEASUREMENT = "Measurement";
        public static final String COLUMN_NAME_VALUE = "Value";
        public static final String COLUMN_NAME_RECORDED_AT = "RecordedAt";
        public static final String COLUMN_NAME_CREATED_AT = "CreatedAt";
    }
}