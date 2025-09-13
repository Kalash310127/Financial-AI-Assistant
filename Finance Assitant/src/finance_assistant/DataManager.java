package finance_assistant;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

public class DataManager {
    private String dataSource;
    private JSONObject rawData;
    private Map<String, Object> parsedData;
    private Map<String, Boolean> permissions;

    public DataManager(String dataSource) {
        this.dataSource = dataSource;
        this.rawData = loadData();
        this.parsedData = parseData();
        this.permissions = getInitialPermissions();
    }

    private JSONObject loadData() {
        try (FileReader reader = new FileReader(dataSource)) {
            return new JSONObject(new JSONTokener(reader));
        } catch (IOException e) {
            System.err.println("Error: Mock data file '" + dataSource + "' not found.");
            return new JSONObject();
        }
    }

    private Map<String, Object> parseData() {
        Map<String, Object> data = new HashMap<>();
        if (rawData.isEmpty()) {
            return data;
        }

        data.put("assets", rawData.optJSONArray("assets"));
        data.put("liabilities", rawData.optJSONArray("liabilities"));
        data.put("transactions", rawData.optJSONArray("transactions"));
        data.put("epf_balance", rawData.optJSONObject("epf_balance"));
        data.put("credit_score", rawData.optJSONObject("credit_score"));
        data.put("investments", rawData.optJSONArray("investments"));

        return data;
    }

    private Map<String, Boolean> getInitialPermissions() {
        Map<String, Boolean> initialPermissions = new HashMap<>();
        initialPermissions.put("assets", true);
        initialPermissions.put("liabilities", true);
        initialPermissions.put("transactions", true);
        initialPermissions.put("epf_balance", true);
        initialPermissions.put("credit_score", true);
        initialPermissions.put("investments", true);
        return initialPermissions;
    }

    public String grantAccess(String category) {
        if (permissions.containsKey(category)) {
            permissions.put(category, true);
            return "Access granted for '" + category + "'.";
        }
        return "Warning: Invalid data category '" + category + "'.";
    }

    public String revokeAccess(String category) {
        if (permissions.containsKey(category)) {
            permissions.put(category, false);
            return "Access revoked for '" + category + "'.";
        }
        return "Warning: Invalid data category '" + category + "'.";
    }

    public Map<String, Object> getPermittedData() {
        Map<String, Object> permittedData = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String category = entry.getKey();
            boolean hasAccess = entry.getValue();

            if (hasAccess) {
                Object data = parsedData.get(category);
                if (data != null) {
                    permittedData.put(category, data);
                }
            }
        }
        return permittedData;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }
}