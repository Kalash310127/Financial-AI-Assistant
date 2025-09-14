package finance_assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class AIInsights {

    private final GeminiInsights geminiInsights;

    public AIInsights() {
        this.geminiInsights = new GeminiInsights();
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
        } else if ("last_3_months".equals(timeframe)) {
            startDate = endDate.minus(3, ChronoUnit.MONTHS);
        } else if ("last_6_months".equals(timeframe)) {
            startDate = endDate.minus(6, ChronoUnit.MONTHS);
        } else {
            return "Invalid timeframe specified. Please use 'last_month', 'last_3_months', or 'last_6_months'.";
        }

        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        Map<String, Double> spendingByCategory = new HashMap<>();

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            LocalDate transactionDate = LocalDate.parse(transaction.getString("date"), formatter);
            if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                double amount = transaction.getDouble("amount");
                String type = transaction.getString("type");
                String category = transaction.optString("category", "Uncategorized");

                if ("income".equals(type)) {
                    totalIncome += amount;
                } else if ("expense".equals(type)) {
                    totalExpenses += amount;
                    spendingByCategory.put(category, spendingByCategory.getOrDefault(category, 0.0) + amount);
                }
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Spending Summary for %s:\n", timeframe));
        summary.append(String.format("- Total Income: $%.2f\n", totalIncome));
        summary.append(String.format("- Total Expenses: $%.2f\n", totalExpenses));
        summary.append(String.format("- Net Change: $%.2f\n\n", totalIncome - totalExpenses));

        if (!spendingByCategory.isEmpty()) {
            summary.append("Spending by Category:\n");
            spendingByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry ->
                            summary.append(String.format("- %s: $%.2f\n", entry.getKey(), entry.getValue()))
                    );
        }

        return summary.toString();
    }

    public String getRecurringBillsSummary(Map data) {
        Object o = data.get("recurring_bills");
        if (o == null) return "No recurring bills data available.";
        JSONArray arr = (JSONArray) o;
        if (arr.isEmpty()) return "No recurring bills found.";
        double monthlyTotal = 0.0;
        StringBuilder sb = new StringBuilder("Recurring Bills Summary:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject b = arr.getJSONObject(i);
            monthlyTotal += b.optDouble("amount", 0.0);
        }
        sb.append(String.format("- Estimated Monthly Total: $%.2f\n", monthlyTotal));
        return sb.toString();
    }

    public String getStocksPerformanceSummary(Map data) {
        Object o = data.get("stocks");
        if (o == null) return "No stocks data available.";
        JSONArray arr = (JSONArray) o;
        if (arr.isEmpty()) return "No stocks found.";
        double totalValue = 0.0;
        double totalCost = 0.0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject s = arr.getJSONObject(i);
            int shares = s.optInt("shares", 0);
            totalValue += s.optDouble("current_price", 0.0) * shares;
            totalCost  += s.optDouble("purchase_price", 0.0) * shares;
        }
        double pnl = totalValue - totalCost;
        double pnlPct = totalCost > 0 ? (pnl / totalCost) * 100.0 : 0.0;
        return String.format("Stocks Summary:\n- Market Value: $%.2f\n- Cost Basis: $%.2f\n- P/L: $%.2f (%.2f%%)\n", totalValue, totalCost, pnl, pnlPct);
    }

    public String getInsuranceCoverageSummary(Map data) {
        Object o = data.get("insurance_policies");
        if (o == null) return "No insurance policies data available.";
        JSONArray arr = (JSONArray) o;
        if (arr.isEmpty()) return "No insurance policies found.";
        double monthlyPremium = 0.0;
        double totalCoverage = 0.0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            monthlyPremium += p.optDouble("monthly_premium", 0.0);
            totalCoverage += p.optDouble("coverage_amount", 0.0);
        }
        return String.format("Insurance Summary:\n- Total Monthly Premium: $%.2f\n- Aggregate Coverage: $%.2f\n", monthlyPremium, totalCoverage);
    }

    public String getGoalsProgressSummary(Map data) {
        Object o = data.get("financial_goals");
        if (o == null) return "No financial goals data available.";
        JSONArray arr = (JSONArray) o;
        if (arr.isEmpty()) return "No financial goals found.";
        StringBuilder sb = new StringBuilder("Goals Progress:\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject g = arr.getJSONObject(i);
            double target = g.optDouble("target_amount", 0.0);
            double current = g.optDouble("current_amount_saved", 0.0);
            double pct = target > 0 ? (current / target) * 100.0 : 0.0;
            sb.append(String.format("- %s: $%.2f/$%.2f (%.1f%%) by %s\n",
                    g.optString("name", "Unnamed Goal"),
                    current, target, pct,
                    g.optString("target_date", "—")));
        }
        return sb.toString();
    }

    public String getComprehensiveSummary(Map<String, Object> data) {
        // Now, this method uses the AI to provide a comprehensive summary
        String prompt = "Please act as a helpful financial assistant. Analyze the provided financial data and provide a comprehensive summary of my financial health. Include a detailed overview of my assets, liabilities, investments, and credit score. Highlight any key trends or noteworthy points and provide actionable advice. Keep the response concise and easy to understand.";
        return this.geminiInsights.getAIResponse(prompt, data);
    }
}
