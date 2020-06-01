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

        String requestPath = String.format("/api/Measurements");
        HttpResponse httpResponse = sendPutRequest(this.baseUrl + requestPath, eventsArrayJson.toString());

        return httpResponse;
    }

    private HttpResponse sendPutRequest(String rawUrl, String payload) throws IOException {
        URL url = new URL(rawUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("PUT");
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
