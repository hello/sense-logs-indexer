package com.hello.suripu.logsindexer.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.Map;


public class SenseDocument {
    private static final String ALARM_RINGING_REGEX = "(?s)^.*?(ALARM RINGING).*$";
    private static final String FIRMWARE_CRASH_REGEX = "(?s)^.*?(i2c recovery|ASSERT|fault|Bond Corruption).*$";
    private static final String WIFI_INFO_REGEX = "(?s)^.*?(SSID RSSI UNIQUE).*$";
    private static final String DUST_REGEX = "(?s)^.*?(dust).*$";
    public static final String DEFAULT_CATEGORY = "sense";

    public final String senseId;
    public final Long epochMillis;
    public final String text;
    public final String origin;
    public final String topFirmwareVersion;
    public final String middleFirmwareVersion;
    private final ObjectMapper objectMapper;

    private SenseDocument(final String senseId, final Long epochMillis, final String text, final String origin, final String topFirmwareVersion, final String middleFirmwareVersion, final ObjectMapper objectMapper) {
        this.senseId = senseId;
        this.epochMillis = epochMillis;
        this.text = text;
        this.origin =  origin;
        this.topFirmwareVersion = topFirmwareVersion;
        this.middleFirmwareVersion = middleFirmwareVersion;
        this.objectMapper = objectMapper;
    }

    public static SenseDocument create(final String senseId, final Long epochMillis, final String text, final String origin, final String topFwVersion, final String middleFwVersion) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return new SenseDocument(senseId, epochMillis, text, origin, topFwVersion, middleFwVersion, objectMapper);
    }

    public static SenseDocument create(final String senseId, final Long epochMillis, final String text, final String origin, final String topFwVersion, final String middleFwVersion, final ObjectMapper objectMapper) {
        return new SenseDocument(senseId, epochMillis, text, origin, topFwVersion, middleFwVersion, objectMapper);
    }

    @JsonProperty("timestamp")
    public String getISODateTime() {
        return new DateTime(epochMillis, DateTimeZone.UTC).toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z"));
    }

    @JsonProperty("has_alarm")
    public Boolean hasAlarm() {
        return text.matches(ALARM_RINGING_REGEX);
    }

    @JsonProperty("has_firmware_crash")
    public Boolean hasFirmwareCrash() {
        return text.matches(FIRMWARE_CRASH_REGEX);
    }

    @JsonProperty("has_wifi_info")
    public Boolean hasWifiInfo() {
        return text.matches(WIFI_INFO_REGEX);
    }

    @JsonProperty("has_dust")
    public Boolean hasDust() {
        return text.matches(DUST_REGEX);
    }

    public Map<String, Object> toMap() {
        return objectMapper.convertValue(this, Map.class);
    }
}
