package finance_assistant;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class FinancialAssistant {
    public DataManager dataManager;
    private AIInsights aiInsights;
    private GeminiInsights geminiInsights;
    private String lastQueryContext;

    public FinancialAssistant(String mockDataFile) {
        this.dataManager = new DataManager(mockDataFile);
        this.aiInsights = new AIInsights();
        this.geminiInsights = new GeminiInsights();
        this.lastQueryContext = null;
    }

    public String handleQuery(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();
        Map<String, Object> permittedData = dataManager.getPermittedData();

        // 1. Handle specific, non-AI commands first
        if (lowerCaseQuery.contains("grant access to")) {
            String category = lowerCaseQuery.split("access to")[1].trim().split(" ")[0];
            return dataManager.grantAccess(category);
        }

        if (lowerCaseQuery.contains("revoke access to")) {
            String category = lowerCaseQuery.split("access to")[1].trim().split(" ")[0];
            return dataManager.revokeAccess(category);
        }

        if (lowerCaseQuery.contains("hello") || lowerCaseQuery.contains("hi")) {
            return "Hello! How can I help you with your finances today?";
        }

        // 2. Handle specific queries with predefined logic
        if (lowerCaseQuery.contains("spending summary")) {
            if (lowerCaseQuery.contains("last month")) {
                return aiInsights.getSpendingSummary(permittedData, "last_month");
            } else if (lowerCaseQuery.contains("last 3 months") || lowerCaseQuery.contains("last three months")) {
                return aiInsights.getSpendingSummary(permittedData, "last_3_months");
            }
        }

        if (lowerCaseQuery.contains("transactions")) {
            return formatTransactions(permittedData);
        }

        if (lowerCaseQuery.contains("net worth")) {
            return calculateNetWorth(permittedData);
        }

        if (lowerCaseQuery.contains("show all data") || lowerCaseQuery.contains("show my data")) {
            return formatAllData(permittedData);
        }

        // 3. Handle more complex queries using the AI model
        if (lastQueryContext != null && (lowerCaseQuery.equals("yes") || lowerCaseQuery.equals("no"))) {
            return geminiInsights.getAIResponse(lastQueryContext + " User response: " + query, permittedData);
        }

        String aiResponse = geminiInsights.getAIResponse(query, permittedData);

        // Check if the AI response requires a follow-up, and save the context
        Pattern p = Pattern.compile("Do you want to know more about (.*)?");
        Matcher m = p.matcher(aiResponse);
        if (m.find()) {
            lastQueryContext = m.group(1);
        } else {
            lastQueryContext = null;
        }

        return aiResponse;
    }

    // --- Helper methods to format data from a Map<String, Object> ---

    private String calculateNetWorth(Map<String, Object> data) {
        if (!data.containsKey("assets") || !data.containsKey("liabilities")) {
            return "I need access to both 'assets' and 'liabilities' to calculate your net worth. Please grant access to these categories.";
        }

        double totalAssets = 0;
        Object assetsObject = data.get("assets");
        // Check if assets is an object or a list to handle both data formats
        if (assetsObject instanceof JSONObject) {
            JSONObject assets = (JSONObject) assetsObject;
            for (String key : assets.keySet()) {
                totalAssets += assets.getDouble(key);
            }
        } else if (assetsObject instanceof JSONArray) {
            JSONArray assets = (JSONArray) assetsObject;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                totalAssets += asset.getDouble("amount");
            }
        }

        double totalLiabilities = 0;
        Object liabilitiesObject = data.get("liabilities");
        // Check if liabilities is an object or a list to handle both data formats
        if (liabilitiesObject instanceof JSONObject) {
            JSONObject liabilities = (JSONObject) liabilitiesObject;
            for (String key : liabilities.keySet()) {
                totalLiabilities += liabilities.getDouble(key);
            }
        } else if (liabilitiesObject instanceof JSONArray) {
            JSONArray liabilities = (JSONArray) liabilitiesObject;
            for (int i = 0; i < liabilities.length(); i++) {
                JSONObject liability = liabilities.getJSONObject(i);
                totalLiabilities += liability.getDouble("outstanding_balance");
            }
        }

        double netWorth = totalAssets - totalLiabilities;
        return String.format("Your estimated net worth is $%.2f.", netWorth);
    }

    private String formatAllData(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("Here is a summary of your available data:\n\n");

        if (data.containsKey("assets")) {
            sb.append("--- Assets ---\n");
            Object assetsObject = data.get("assets");
            if (assetsObject instanceof JSONObject) {
                JSONObject assets = (JSONObject) assetsObject;
                for (String key : assets.keySet()) {
                    sb.append(String.format("- %s: $%.2f\n", key, assets.getDouble(key)));
                }
            } else if (assetsObject instanceof JSONArray) {
                JSONArray assets = (JSONArray) assetsObject;
                assets.forEach(item -> {
                    JSONObject asset = (JSONObject) item;
                    sb.append(String.format("- %s (%s): $%.2f\n",
                            asset.getString("name"), asset.getString("type"), asset.getDouble("amount")));
                });
            }
            sb.append("\n");
        }

        if (data.containsKey("liabilities")) {
            sb.append("--- Liabilities ---\n");
            Object liabilitiesObject = data.get("liabilities");
            if (liabilitiesObject instanceof JSONObject) {
                JSONObject liabilities = (JSONObject) liabilitiesObject;
                for (String key : liabilities.keySet()) {
                    sb.append(String.format("- %s: $%.2f outstanding\n", key, liabilities.getDouble(key)));
                }
            } else if (liabilitiesObject instanceof JSONArray) {
                JSONArray liabilities = (JSONArray) liabilitiesObject;
                liabilities.forEach(item -> {
                    JSONObject liability = (JSONObject) item;
                    sb.append(String.format("- %s (%s): $%.2f outstanding\n",
                            liability.getString("name"), liability.getString("type"), liability.getDouble("outstanding_balance")));
                });
            }
            sb.append("\n");
        }

        if (data.containsKey("transactions")) {
            sb.append("--- Recent Transactions ---\n");
            JSONArray transactions = (JSONArray) data.get("transactions");
            for (int i = 0; i < transactions.length(); i++) {
                JSONObject transaction = transactions.getJSONObject(i);
                String type = transaction.getString("type");
                String description = transaction.getString("description");
                double amount = transaction.getDouble("amount");
                String date = transaction.getString("date");

                sb.append(String.format("- %s, %s: %s of $%.2f\n", date, type, description, amount));
            }
            sb.append("\n");
        }

        if (data.containsKey("investments")) {
            sb.append("--- Investments ---\n");
            Object investmentsObject = data.get("investments");
            if (investmentsObject instanceof JSONObject) {
                JSONObject investments = (JSONObject) investmentsObject;
                for (String key : investments.keySet()) {
                    sb.append(String.format("- %s: $%.2f\n", key, investments.getDouble(key)));
                }
            } else if (investmentsObject instanceof JSONArray) {
                JSONArray investments = (JSONArray) investmentsObject;
                investments.forEach(item -> {
                    JSONObject investment = (JSONObject) item;
                    sb.append(String.format("- %s (%s): $%.2f\n",
                            investment.getString("name"), investment.getString("type"), investment.getDouble("value")));
                });
            }
            sb.append("\n");
        }

        if (data.containsKey("epf_balance")) {
            sb.append("--- EPF Balance ---\n");
            JSONObject epf = (JSONObject) data.get("epf_balance");
            sb.append(String.format("Current Balance: $%.2f\n", epf.getDouble("current_balance")));
            sb.append(String.format("Last Year's Contributions: $%.2f\n", epf.getDouble("contributions_last_year")));
            sb.append("\n");
        }

        if (data.containsKey("credit_score")) {
            sb.append("--- Credit Score ---\n");
            JSONObject creditScore = (JSONObject) data.get("credit_score");
            sb.append(String.format("Score: %d (%s)\n", creditScore.getInt("score"), creditScore.getString("rating")));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatTransactions(Map<String, Object> data) {
        if (!data.containsKey("transactions")) {
            return "I cannot access your transactions. Please grant access to 'transactions' to see this data.";
        }

        StringBuilder sb = new StringBuilder("Here is a list of your recent transactions:\n\n");
        JSONArray transactions = (JSONArray) data.get("transactions");
        for (int i = 0; i < transactions.length(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            String type = transaction.getString("type");
            double amount = transaction.getDouble("amount");
            String date = transaction.getString("date");
            String description = transaction.optString("description", "N/A");
            String category = transaction.optString("category", "N/A");

            sb.append(String.format("- Date: %s | Type: %s | Amount: $%.2f | Description: %s | Category: %s\n",
                    date, type, amount, description, category));
        }
        return sb.toString();
    }
}
