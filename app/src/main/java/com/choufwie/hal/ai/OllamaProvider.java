package com.choufwie.hal.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Talks to a local Ollama server (https://ollama.com). Private: no cloud, nothing leaves your network. */
public final class OllamaProvider {
    private OllamaProvider() {}

    private static String base(String host) {
        String h = host == null ? "" : host.trim();
        if (!h.startsWith("http://") && !h.startsWith("https://")) h = "http://" + h;
        while (h.endsWith("/")) h = h.substring(0, h.length() - 1);
        return h;
    }

    /** Throws if the server can't be reached. */
    public static void ping(String host) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(base(host) + "/api/tags").openConnection();
        c.setConnectTimeout(3000);
        c.setReadTimeout(3000);
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        } finally { c.disconnect(); }
    }

    /** One non-streaming chat call. Returns the assistant text. */
    public static String chat(String host, String model, String system, String user, boolean json, int timeoutMs) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", false);
        body.put("keep_alive", "30m");
        if (json) body.put("format", "json");
        body.put("options", new JSONObject().put("temperature", json ? 0.1 : 0.6).put("num_predict", json ? 120 : 380));
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "system").put("content", system));
        msgs.put(new JSONObject().put("role", "user").put("content", user));
        body.put("messages", msgs);

        HttpURLConnection c = (HttpURLConnection) new URL(base(host) + "/api/chat").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(4000);
        c.setReadTimeout(timeoutMs);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try {
            try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            StringBuilder sb = new StringBuilder();
            try (InputStream in = c.getInputStream(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString()).getJSONObject("message").getString("content").trim();
        } finally { c.disconnect(); }
    }
}
