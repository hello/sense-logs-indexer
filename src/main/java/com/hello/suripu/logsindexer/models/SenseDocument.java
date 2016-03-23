package com.hello.suripu.logsindexer.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.Map;
import java.util.Set;


public class SenseDocument {
    private static final String ALARM_RINGING_REGEX = "(?s)^.*?(ALARM RINGING).*$";
    private static final String FIRMWARE_CRASH_REGEX = "(?s)^.*?(i2c err|ASSERT|fault|Bond Corruption).*$";
    private static final String WIFI_INFO_REGEX = "(?s)^.*?(SSID).*$";
    private static final String DUST_REGEX = "(?s)^.*?(dust).*$";
    public static final String DEFAULT_CATEGORY = "sense";
    private static final Map<String, ImmutableSet<String>> FIRMWARE_VERSION_MAP = ImmutableMap.of(
            "0.7.6", ImmutableSet.of("50F4C5A7"),
            "0.8.7", ImmutableSet.of("74812427", "3DBC3140"),
            "0.9.1", ImmutableSet.of("74812427", "2CB67805", "441A665B"),
            "0.9.2", ImmutableSet.of("74812427"),
            "0.9.3", ImmutableSet.of("1CBD0136")
    );

    private static final Set<String> INTERNAL_TEST_SENSE_IDS = ImmutableSet.of("B0B5357C9CC1143E");


    public final String senseId;
    public final Long epochMillis;
    public final String text;
    public final String origin;
    public final String topFirmwareVersion;
    public final String middleFirmwareVersion;
    public final Set<String> certifiedCombinedFirmwareVersions;

    private final ObjectMapper objectMapper;

    private SenseDocument(final String senseId, final Long epochMillis, final String text, final String origin, final String topFirmwareVersion, final String middleFirmwareVersion, final Set<String> certifiedCombinedFirmwareVersions, final ObjectMapper objectMapper) {
        this.senseId = senseId;
        this.epochMillis = epochMillis;
        this.text = text;
        this.origin =  origin;
        this.topFirmwareVersion = topFirmwareVersion;
        this.middleFirmwareVersion = middleFirmwareVersion;
        this.certifiedCombinedFirmwareVersions = certifiedCombinedFirmwareVersions;
        this.objectMapper = objectMapper;
    }

    public static SenseDocument create(final String senseId, final Long epochMillis, final String text, final String origin, final String topFwVersion, final String middleFwVersion, final Set<String> certifiedCombinedFirmwareVersions) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return new SenseDocument(senseId, epochMillis, text, origin, topFwVersion, middleFwVersion, certifiedCombinedFirmwareVersions, objectMapper);
    }

    public static SenseDocument create(final String senseId, final Long epochMillis, final String text, final String origin, final String topFwVersion, final String middleFwVersion, final Set<String> certifiedCombinedFirmwareVersions, final ObjectMapper objectMapper) {
        return new SenseDocument(senseId, epochMillis, text, origin, topFwVersion, middleFwVersion, certifiedCombinedFirmwareVersions, objectMapper);
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
    public Boolean hasDust() { return text.matches(DUST_REGEX); }

    @JsonProperty("combined_firmware_versions")
    public String combinedFirmwareVersions() {
        return String.format("%s___%s", topFirmwareVersion, middleFirmwareVersion);
    }

    @JsonProperty("has_unexpected_firmware")
    public Boolean hasUnexpectedFirmware() {
        return !certifiedCombinedFirmwareVersions.contains(combinedFirmwareVersions());
    }

    // Anything document marked as immortal will never be purged
    public Boolean isInternalTestOnWifi() {
        return INTERNAL_TEST_SENSE_IDS.contains(senseId) && hasWifiInfo();
    }

    public Map<String, Object> toMap() {
        return objectMapper.convertValue(this, Map.class);
    }



}
