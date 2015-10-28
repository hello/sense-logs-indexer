package com.hello.suripu.logsindexer.models;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Map;


public class SenseDocument {
    private static final String ALARM_RINGING_REGEX = "(?s)^.*?(ALARM RINGING).*$";
    private static final String FIRMWARE_CRASH_REGEX = "(?s)^.*?(i2c recovery|boot completed|ASSERT|fail|fault|bounce|Bouncing|Bond Corruption).*$";
    private static final String WIFI_INFO_REGEX = "(?s)^.*?(SSID RSSI UNIQUE).*$";
    private static final String DUST_REGEX = "(?s)^.*?(dust).*$";
    public static final String DEFAULT_CATEGORY = "sense";

    public final String senseId;
    public final Long epochMillis;
    public final String timestamp;
    public final String text;
    public final String origin;
    public final Boolean hasAlarm;
    public final Boolean hasFirmwareCrash;
    public final Boolean hasWifiInfo;
    public final Boolean hasDust;
    public final String topFirmwareVersion;
    public final String middleFirmwareVersion;

    public SenseDocument(final String senseId, final Long epochMillis, final String text, final String origin, final String topFirmwareVersion, final String middleFirmwareVersion) {
        this.senseId = senseId;
        this.epochMillis = epochMillis;
        this.timestamp = new DateTime(epochMillis).toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z"));
        this.text = text;
        this.origin =  origin;
        this.hasAlarm = text.matches(ALARM_RINGING_REGEX);
        this.hasFirmwareCrash = text.matches(FIRMWARE_CRASH_REGEX);
        this.hasWifiInfo = text.matches(WIFI_INFO_REGEX);
        this.hasDust = text.matches(DUST_REGEX);
        this.topFirmwareVersion = topFirmwareVersion;
        this.middleFirmwareVersion = middleFirmwareVersion;
    }
    public Map<String, Object> toMap() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return objectMapper.convertValue(this, Map.class);
    }
}