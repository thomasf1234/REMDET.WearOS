package com.abstractx1.sensortest;

import android.util.Log;

import com.abstractx1.sensortest.entities.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerClient {
    private String baseUrl;

    public ServerClient(String _baseUrl) {
        this.baseUrl = _baseUrl;
    }

    public HttpResponse healthy() throws IOException {
        String requestPath = "/-/healthy";
        HttpResponse httpResponse = sendGetRequest(this.baseUrl + requestPath);

        return httpResponse;
    }

    public HttpResponse ready() throws IOException {
        String requestPath = "/-/ready";
        HttpResponse httpResponse = sendGetRequest(this.baseUrl + requestPath);

        return httpResponse;
    }

    public HttpResponse sendBatchUpdate(Event[] events) throws IOException, JSONException {
        int eventCount = events.length;
        JSONArray eventsArrayJson = new JSONArray();

        for (int i=0; i<eventCount; ++i) {
            Event event = events[i];
            JSONObject eventParams = new JSONObject();

            eventParams.put("measurement", event.getMeasurement());
            eventParams.put("value", event.getValue());
            eventParams.put("recorded_at", event.getRecordedAt());
            eventsArrayJson.put(eventParams);
        }

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("events", eventsArrayJson);

        String requestPath = String.format("/api/v1/event");

        HttpResponse httpResponse = sendPostRequest(this.baseUrl + requestPath, jsonParam.toString());

        return httpResponse;
    }

    private HttpResponse sendGetRequest(String rawUrl) throws IOException {
        URL url = new URL(rawUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoInput(true);

            if (Constants.IN_DEBUG) {
                Log.d(Constants.TAG, String.format("Issuing GET request to %s", url.toString()));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = conn.getResponseMessage();

            HttpResponse httpResponse = new HttpResponse(responseCode, responseBody);

            return httpResponse;
        } finally {
            conn.disconnect();
        }
    }

    private HttpResponse sendPostRequest(String rawUrl, String payload) throws IOException {
        URL url = new URL(rawUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            if (Constants.IN_DEBUG) {
                Log.d(Constants.TAG, String.format("Issuing POST request to %s with payload %s", url.toString(), payload));
            }

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(payload);

            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            String responseBody = conn.getResponseMessage();

            HttpResponse httpResponse = new HttpResponse(responseCode, responseBody);

            return httpResponse;
        } finally {
            conn.disconnect();
        }
    }
}
