package com.abstractx1.sensortest.entities;

import java.util.Date;

public class Event {
    public static final String MEASUREMENT_LINEAR_ACCELERATION = "linear_acceleration";
    public static final String MEASUREMENT_HEART_RATE = "heart_rate";

    private int eventId;
    private String measurement;
    private double value;
    private String recordedAt;

    public Event() {

    }

    public Event(int _eventId, String _measurement, double _value, String _recordedAt) {
        this.eventId = _eventId;
        this.measurement = _measurement;
        this.value = _value;
        this.recordedAt = _recordedAt;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }
}
