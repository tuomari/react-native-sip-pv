package com.carusto.ReactNativePjSip.dto;

import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ReadableMap;

import org.json.JSONObject;
import org.pjsip.pjsua2.StringVector;

import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Provider;

/**
 * See config variables: https://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm
 */
public class ServiceConfigurationDTO {

    private static final String TAG = "ServiceConfigurationDTO";
    public String ua = null;
    public ArrayList<String> stun;
    public Boolean noVad = false;
    private Long mediaClockRate = null;
    private Long mediaQuality = null;
    private Long ecOptions = null;
    private Long ecTailLen = null;
    private Long mediaThreadCount = null;
    private Long msgLogging = null;
    private Long logLevel = null;
    private Long consoleLogLevel = null;

    public String getUserAgent() {
        return ua;
    }

    public StringVector getStunServers() {
        StringVector serversVector = new StringVector();
        for (String server : stun) {
            serversVector.add(server);
        }
        return serversVector;
    }

    public boolean isUserAgentNotEmpty() {
        return ua != null && !ua.isEmpty();
    }

    public boolean isStunServersNotEmpty() {
        return stun != null && stun.size() > 0;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        try {
            json.put("ua", ua);

            return json;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceConfigurationDTO fromIntent(Intent intent) {
        ServiceConfigurationDTO c = new ServiceConfigurationDTO();

        if (intent.hasExtra("ua")) {
            c.ua = intent.getStringExtra("ua");
        }

        return c;
    }

    public static void setLong(Map<String, ?> conf, String key, Consumer<Long> foundValueCallback) {
        if (conf.containsKey(key)) {
            Object valueObj = conf.get(key);
            if (valueObj != null) {
                String valStr = valueObj.toString();
                try {
                    Long longValue = Long.getLong(valStr);
                    foundValueCallback.accept(longValue);
                } catch (NumberFormatException nex) {
                    Log.d(TAG, "Error parsing number key '" + key + "' from '" + valueObj + "'");
                }
            }

        }
    }

    public static ServiceConfigurationDTO fromMap(Map<String, ?> conf) {
        ServiceConfigurationDTO c = new ServiceConfigurationDTO();

        if (conf.containsKey("ua")) {
            c.ua = conf.get("ua").toString();
        }

        if (conf.containsKey("stun")) {
            c.stun = (ArrayList) conf.get("stun");
        }
        if (conf.containsKey("noVad")) {
            c.noVad = "true".equalsIgnoreCase(conf.get("noVad").toString());
        }
        setLong(conf, "mediaClockRate", (clockRate) -> c.mediaClockRate = clockRate );
        setLong(conf, "mediaQuality", quality -> c.mediaQuality = quality);
        setLong(conf, "ecOptions", ecOptions-> c.ecOptions = ecOptions);
        setLong(conf, "ecTailLen", ecTailLen -> c.ecTailLen = ecTailLen);
        setLong(conf, "mediaThreadCount", mediaThreadCount -> c.mediaThreadCount = mediaThreadCount);

        setLong(conf, "msgLogging", msgLogging -> c.msgLogging = msgLogging);
        setLong(conf, "logLevel", logLevel -> c.logLevel = logLevel);
        setLong(conf, "consoleLogLevel", consoleLogLevel -> c.consoleLogLevel = consoleLogLevel);
        return c;
    }

    public static ServiceConfigurationDTO fromConfiguration(ReadableMap data) {
        ServiceConfigurationDTO c = new ServiceConfigurationDTO();

        if (data.hasKey("ua")) {
            c.ua = data.getString("ua");
        }

        return c;
    }

    public String getUa() {
        return ua;
    }

    public ArrayList<String> getStun() {
        return stun;
    }

    public Boolean getNoVad() {
        return noVad;
    }

    public Long getMediaClockRate() {
        return mediaClockRate;
    }

    public Long getMediaQuality() {
        return mediaQuality;
    }

    public Long getEcOptions() {
        return ecOptions;
    }

    public Long getEcTailLen() {
        return ecTailLen;
    }

    public Long getMediaThreadCount() {
        return mediaThreadCount;
    }

    public Long getMsgLogging() {
        return msgLogging;
    }

    public Long getLogLevel() {
        return logLevel;
    }

    public Long getConsoleLogLevel() {
        return consoleLogLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceConfigurationDTO that = (ServiceConfigurationDTO) o;
        return Objects.equals(ua, that.ua)
                && Objects.equals(stun, that.stun)
                && Objects.equals(noVad, that.noVad)
                && Objects.equals(mediaClockRate, that.mediaClockRate)
                && Objects.equals(mediaQuality, that.mediaQuality)
                && Objects.equals(ecOptions, that.ecOptions)
                && Objects.equals(ecTailLen, that.ecTailLen)
                && Objects.equals(mediaThreadCount, that.mediaThreadCount)
                && Objects.equals(msgLogging, that.msgLogging)
                && Objects.equals(logLevel, that.logLevel)
                && Objects.equals(consoleLogLevel, that.consoleLogLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ua, stun, noVad, mediaClockRate, mediaQuality,
                ecOptions, ecTailLen, mediaThreadCount, msgLogging, logLevel, consoleLogLevel);
    }
}
