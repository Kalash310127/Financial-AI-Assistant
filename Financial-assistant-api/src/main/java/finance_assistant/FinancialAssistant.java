package finance_assistant;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class FinancialAssistant {
    public DataManager dataManager;
    public AIInsights aiInsights;
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

        if (lowerCaseQuery.contains("stocks")) {
            return formatStocks(permittedData);
        }
        if (lowerCaseQuery.contains("insurance") || lowerCaseQuery.contains("insurance policies")) {
            return formatInsurancePolicies(permittedData);
        }
        if (lowerCaseQuery.contains("recurring bills") || lowerCaseQuery.contains("subscriptions")) {
            return formatRecurringBills(permittedData);
        }
        if (lowerCaseQuery.contains("goals") || lowerCaseQuery.contains("financial goals")) {
            return formatFinancialGoals(permittedData);
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

        if (data.containsKey("stocks") && data.get("stocks") != null) {
            sb.append("--- Stocks ---\n");
            sb.append(formatStocks(data)).append("\n");
        }
        if (data.containsKey("insurance_policies") && data.get("insurance_policies") != null) {
            sb.append("--- Insurance Policies ---\n");
            sb.append(formatInsurancePolicies(data)).append("\n");
        }
        if (data.containsKey("recurring_bills") && data.get("recurring_bills") != null) {
            sb.append("--- Recurring Bills ---\n");
            sb.append(formatRecurringBills(data)).append("\n");
        }
        if (data.containsKey("financial_goals") && data.get("financial_goals") != null) {
            sb.append("--- Financial Goals ---\n");
            sb.append(formatFinancialGoals(data)).append("\n");
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

    private String formatStocks(Map data) {
        if (!data.containsKey("stocks") || data.get("stocks") == null) {
            return "No 'stocks' data available or access not granted.";
        }
        JSONArray arr = (JSONArray) data.get("stocks");
        if (arr.isEmpty()) return "No stocks found.";
        StringBuilder sb = new StringBuilder("Stocks:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject s = arr.getJSONObject(i);
            sb.append(String.format("- %s (%s): %d shares | Buy: $%.2f | Now: $%.2f | Value: $%.2f\n",
                    s.optString("company_name", s.optString("ticker", "Unknown")),
                    s.optString("ticker", "—"),
                    s.optInt("shares", 0),
                    s.optDouble("purchase_price", 0.0),
                    s.optDouble("current_price", 0.0),
                    s.optDouble("total_value", s.optDouble("current_price", 0.0) * s.optInt("shares", 0))));
        }
        return sb.toString();
    }

    private String formatInsurancePolicies(Map data) {
        if (!data.containsKey("insurance_policies") || data.get("insurance_policies") == null) {
            return "No 'insurance_policies' data available or access not granted.";
        }
        JSONArray arr = (JSONArray) data.get("insurance_policies");
        if (arr.isEmpty()) return "No insurance policies found.";
        StringBuilder sb = new StringBuilder("Insurance Policies:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            sb.append(String.format("- %s | Provider: %s | Policy: %s | Premium: $%.2f/mo | Coverage: $%.2f\n",
                    p.optString("type", "Unknown"),
                    p.optString("provider", "Unknown"),
                    p.optString("policy_number", "—"),
                    p.optDouble("monthly_premium", 0.0),
                    p.optDouble("coverage_amount", 0.0)));
        }
        return sb.toString();
    }

    private String formatRecurringBills(Map data) {
        if (!data.containsKey("recurring_bills") || data.get("recurring_bills") == null) {
            return "No 'recurring_bills' data available or access not granted.";
        }
        JSONArray arr = (JSONArray) data.get("recurring_bills");
        if (arr.isEmpty()) return "No recurring bills found.";
        StringBuilder sb = new StringBuilder("Recurring Bills:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject b = arr.getJSONObject(i);
            sb.append(String.format("- %s (%s): $%.2f due on day %d\n",
                    b.optString("service", "Unknown"),
                    b.optString("provider", "Unknown"),
                    b.optDouble("amount", 0.0),
                    b.optInt("due_day", 1)));
        }
        return sb.toString();
    }

    private String formatFinancialGoals(Map data) {
        if (!data.containsKey("financial_goals") || data.get("financial_goals") == null) {
            return "No 'financial_goals' data available or access not granted.";
        }
        JSONArray arr = (JSONArray) data.get("financial_goals");
        if (arr.isEmpty()) return "No financial goals found.";
        StringBuilder sb = new StringBuilder("Financial Goals:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject g = arr.getJSONObject(i);
            double target = g.optDouble("target_amount", 0.0);
            double current = g.optDouble("current_amount_saved", 0.0);
            double pct = target > 0 ? (current / target) * 100.0 : 0.0;
            sb.append(String.format("- %s: $%.2f/$%.2f (%.1f%%) | Target Date: %s\n",
                    g.optString("name", "Unnamed Goal"),
                    current, target, pct,
                    g.optString("target_date", "—")));
        }
        return sb.toString();
    }
}
