package finance_assistant;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

public class FinancialDataSummary {

    public String getBankingDetailsSummary(Map<String, Object> data) {
        StringBuilder summary = new StringBuilder("Here is a summary of your banking details:\n\n");

        if (data.containsKey("assets")) {
            summary.append("Bank Accounts (Cash Assets):\n");
            JSONArray assets = (JSONArray) data.get("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if ("cash".equals(asset.getString("type"))) {
                    summary.append(String.format("- %s: $%.2f\n",
                            asset.getString("name"),
                            asset.getDouble("amount")));
                }
            }
            summary.append("\n");
        }

        if (data.containsKey("liabilities")) {
            summary.append("Outstanding Loans & Credit Cards:\n");
            JSONArray liabilities = (JSONArray) data.get("liabilities");
            for (int i = 0; i < liabilities.length(); i++) {
                JSONObject liability = liabilities.getJSONObject(i);
                if ("loan".equals(liability.getString("type")) || "credit_card".equals(liability.getString("type"))) {
                    summary.append(String.format("- %s: $%.2f outstanding\n",
                            liability.getString("name"),
                            liability.getDouble("outstanding_balance")));
                }
            }
            summary.append("\n");
        }

        return summary.toString();
    }
}
