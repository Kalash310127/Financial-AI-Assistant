package finance_assistant;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiInsights {

    // IMPORTANT: Replace with your actual API key.
    private static final String API_KEY = "AIzaSyAM-LbR6JWM39Q_YxMRocDQhYxAY9XsVQ8";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-05-20:generateContent?key=" + API_KEY;

    public String getAIResponse(String prompt, Map<String, Object> data) {
        try {
            JSONObject payload = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            // Construct the user query with the financial data
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("Here is my financial data in JSON format:\n");
            queryBuilder.append(new JSONObject(data).toString(4));
            queryBuilder.append("\n\nBased on this data, please answer the following question:\n");
            queryBuilder.append(prompt);

            parts.put(new JSONObject().put("text", queryBuilder.toString()));
            content.put("parts", parts);
            contents.put(content);
            payload.put("contents", contents);

            // Optional: You can add system instructions to guide the model's persona
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("parts", new JSONArray().put(new JSONObject().put("text", "You are a helpful financial assistant. Provide concise and actionable insights.")));
            payload.put("systemInstruction", systemInstruction);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Error connecting to AI service: " + conn.getResponseMessage();
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                String responseBody = scanner.useDelimiter("\\A").next();
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
    }
}