package finance_assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class AIInsights {

    public AIInsights() {
    }

    public String getSpendingSummary(Map<String, Object> data, String timeframe) {
        if (!data.containsKey("transactions")) {
            return "I cannot analyze spending. Please grant access to 'transactions'.";
        }

        JSONArray transactions = (JSONArray) data.get("transactions");
        if (transactions.isEmpty()) {
            return "You have no transactions to analyze.";
        }

        // Find the latest transaction date to use as the reference point
        LocalDate latestDate = LocalDate.MIN;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < transactions.length(); i++) {
            JSONObject t = transactions.getJSONObject(i);
            LocalDate transactionDate = LocalDate.parse(t.getString("date"), formatter);
            if (transactionDate.isAfter(latestDate)) {
                latestDate = transactionDate;
            }
        }

        LocalDate endDate = latestDate;
        LocalDate startDate;

        if ("last_month".equals(timeframe)) {
            startDate = endDate.minus(1, ChronoUnit.MONTHS);
        } else if ("last_quarter".equals(timeframe)) {
            startDate = endDate.minus(3, ChronoUnit.MONTHS);
        } else {
            return "I can only analyze spending for 'last_month' or 'last_quarter'.";
        }

        double totalExpenses = 0.0;
        Map<String, Double> spendingBreakdown = new HashMap<>();

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject t = transactions.getJSONObject(i);
            if ("expense".equals(t.getString("type"))) {
                LocalDate transactionDate = LocalDate.parse(t.getString("date"), formatter);
                if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                    double amount = t.getDouble("amount");
                    totalExpenses += amount;
                    String category = t.optString("category", "Uncategorized");
                    spendingBreakdown.put(category, spendingBreakdown.getOrDefault(category, 0.0) + amount);
                }
            }
        }

        if (totalExpenses > 0) {
            StringBuilder summary = new StringBuilder();
            summary.append(String.format("In the %s, your total expenses were $%.2f. Here's a breakdown by category:\n", timeframe.replace("_", " "), totalExpenses));
            for (Map.Entry<String, Double> entry : spendingBreakdown.entrySet()) {
                summary.append(String.format("- %s: $%.2f\n", entry.getKey(), entry.getValue()));
            }
            return summary.toString();
        }

        return "You had no expenses in the specified timeframe.";
    }

    public String forecastSavings(Map<String, Object> data) {
        if (!data.containsKey("transactions")) {
            return "I cannot forecast savings. Please grant access to 'transactions'.";
        }

        JSONArray transactions = (JSONArray) data.get("transactions");
        double totalIncome = 0.0;
        double totalExpenses = 0.0;

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject t = transactions.getJSONObject(i);
            if ("income".equals(t.getString("type"))) {
                totalIncome += t.getDouble("amount");
            } else if ("expense".equals(t.getString("type"))) {
                totalExpenses += t.getDouble("amount");
            }
        }

        double currentBalance = 0.0;
        if (data.containsKey("assets")) {
            JSONArray assets = (JSONArray) data.get("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if ("cash".equals(asset.getString("type"))) {
                    currentBalance += asset.getDouble("amount");
                }
            }
        }

        // Assuming mock data is for 3 months
        double netMonthlyFlow = (totalIncome - totalExpenses) / 3.0;

        if (netMonthlyFlow > 0) {
            double forecastInThreeMonths = currentBalance + (netMonthlyFlow * 3);
            return String.format(
                    "Based on your recent spending habits, you're projected to save approximately $%.2f per month. " +
                            "In three months, your cash balance could be around $%.2f.",
                    netMonthlyFlow, forecastInThreeMonths);
        } else {
            return "Based on your current spending, you are not saving. Consider creating a budget to improve your financial health.";
        }
    }

    public String analyzeLoanRepayment(Map<String, Object> data) {
        if (!data.containsKey("liabilities")) {
            return "I cannot analyze loans. Please grant access to 'liabilities'.";
        }

        JSONArray liabilities = (JSONArray) data.get("liabilities");
        JSONObject smallestLoan = null;
        double minBalance = Double.MAX_VALUE;

        for (int i = 0; i < liabilities.length(); i++) {
            JSONObject l = liabilities.getJSONObject(i);
            if ("loan".equals(l.getString("type"))) {
                double balance = l.getDouble("outstanding_balance");
                if (balance < minBalance) {
                    minBalance = balance;
                    smallestLoan = l;
                }
            }
        }

        if (smallestLoan != null) {
            return "Your best option for repaying your loans faster is to focus on the smallest one first. " +
                    "You can pay off your '" + smallestLoan.getString("name") + "' loan with any extra money you have. " +
                    "This method is often called the 'Debt Snowball' and can help you stay motivated.";
        }

        return "You have no outstanding loans to analyze.";
    }

    public String canAffordVacation(Map<String, Object> data, double cost) {
        if (!data.containsKey("transactions")) {
            return "I cannot check if you can afford this. Please grant access to 'transactions'.";
        }

        JSONArray transactions = (JSONArray) data.get("transactions");
        double totalIncome = 0.0;
        double totalExpenses = 0.0;

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject t = transactions.getJSONObject(i);
            if ("income".equals(t.getString("type"))) {
                totalIncome += t.getDouble("amount");
            } else if ("expense".equals(t.getString("type"))) {
                totalExpenses += t.getDouble("amount");
            }
        }

        // Assuming mock data is for 3 months
        double netMonthlyFlow = (totalIncome - totalExpenses) / 3.0;

        if (netMonthlyFlow >= cost) {
            return String.format(
                    "Yes, based on your current cash flow, you can comfortably afford a vacation costing $%.2f next month. " +
                            "I can even help you create a travel savings plan!", cost);
        } else if (netMonthlyFlow * 3 >= cost) {
            return String.format(
                    "You might be able to afford a vacation costing $%.2f but it would be a bit tight. You can start saving now and you will be ready in 3 months.", cost);
        } else {
            return String.format(
                    "No, based on your current cash flow, it would be difficult to afford a vacation costing $%.2f next month. " +
                            "You might want to consider saving for a few months before booking.", cost);
        }
    }

    public String getFinancialSummary(Map<String, Object> data) {
        StringBuilder summary = new StringBuilder("Here is a summary of your financial information:\n\n");

        if (data.containsKey("assets")) {
            summary.append("Assets:\n");
            JSONArray assets = (JSONArray) data.get("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                summary.append(String.format("- %s (%s): $%.2f\n",
                        asset.getString("name"),
                        asset.getString("type"),
                        asset.getDouble("amount")));
            }
            summary.append("\n");
        }

        if (data.containsKey("liabilities")) {
            summary.append("Liabilities:\n");
            JSONArray liabilities = (JSONArray) data.get("liabilities");
            for (int i = 0; i < liabilities.length(); i++) {
                JSONObject liability = liabilities.getJSONObject(i);
                summary.append(String.format("- %s (%s): $%.2f outstanding\n",
                        liability.getString("name"),
                        liability.getString("type"),
                        liability.getDouble("outstanding_balance")));
            }
            summary.append("\n");
        }

        if (data.containsKey("epf_balance")) {
            JSONObject epf = (JSONObject) data.get("epf_balance");
            summary.append(String.format("EPF Balance: $%.2f\n", epf.getDouble("current_balance")));
            summary.append(String.format("Last year's contributions: $%.2f\n\n", epf.getDouble("contributions_last_year")));
        }

        if (data.containsKey("credit_score")) {
            JSONObject credit = (JSONObject) data.get("credit_score");
            summary.append(String.format("Credit Score: %d (%s)\n\n",
                    credit.getInt("score"),
                    credit.getString("rating")));
        }

        if (data.containsKey("investments")) {
            summary.append("Investments:\n");
            JSONArray investments = (JSONArray) data.get("investments");
            for (int i = 0; i < investments.length(); i++) {
                JSONObject investment = investments.getJSONObject(i);
                summary.append(String.format("- %s (%s): $%.2f\n",
                        investment.getString("name"),
                        investment.getString("type"),
                        investment.getDouble("value")));
            }
            summary.append("\n");
        }

        if (data.containsKey("transactions")) {
            summary.append("Transactions: (Summary of recent activity)\n");
            // Placeholder for a detailed transaction summary
            summary.append("Please ask about your expenses or savings for a detailed report.\n\n");
        }

        return summary.toString();
    }
}
