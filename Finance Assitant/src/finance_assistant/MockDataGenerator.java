package finance_assistant;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.IOException;

public class MockDataGenerator {

    public static void createMockData(String filePath) {
        JSONObject mockData = new JSONObject();

        JSONArray assets = new JSONArray();
        assets.put(new JSONObject().put("name", "Checking Account").put("type", "cash").put("amount", 2500.00));
        assets.put(new JSONObject().put("name", "Savings Account").put("type", "cash").put("amount", 12000.00));
        assets.put(new JSONObject().put("name", "Property 1").put("type", "property").put("amount", 350000.00));
        mockData.put("assets", assets);

        JSONArray liabilities = new JSONArray();
        liabilities.put(new JSONObject().put("name", "Student Loan").put("type", "loan").put("outstanding_balance", 15000.00).put("interest_rate", 0.05));
        liabilities.put(new JSONObject().put("name", "Credit Card A").put("type", "credit_card").put("outstanding_balance", 500.00).put("interest_rate", 0.18));
        mockData.put("liabilities", liabilities);

        JSONArray transactions = new JSONArray();
        transactions.put(new JSONObject().put("id", "t1").put("date", "2024-06-15").put("type", "income").put("amount", 4000.00).put("description", "Salary"));
        transactions.put(new JSONObject().put("id", "t2").put("date", "2024-06-20").put("type", "expense").put("amount", 75.50).put("description", "Groceries").put("category", "Food"));
        transactions.put(new JSONObject().put("id", "t3").put("date", "2024-07-05").put("type", "expense").put("amount", 1200.00).put("description", "Rent").put("category", "Housing"));
        transactions.put(new JSONObject().put("id", "t4").put("date", "2024-07-10").put("type", "expense").put("amount", 250.00).put("description", "Dining out").put("category", "Food"));
        transactions.put(new JSONObject().put("id", "t5").put("date", "2024-07-25").put("type", "expense").put("amount", 50.00).put("description", "Coffee").put("category", "Food"));
        transactions.put(new JSONObject().put("id", "t6").put("date", "2024-08-01").put("type", "income").put("amount", 4000.00).put("description", "Salary"));
        transactions.put(new JSONObject().put("id", "t7").put("date", "2024-08-15").put("type", "expense").put("amount", 300.00).put("description", "Utilities").put("category", "Utilities"));
        transactions.put(new JSONObject().put("id", "t8").put("date", "2024-08-20").put("type", "expense").put("amount", 150.00).put("description", "Shopping").put("category", "Shopping"));
        transactions.put(new JSONObject().put("id", "t9").put("date", "2024-09-01").put("type", "income").put("amount", 4000.00).put("description", "Salary"));
        transactions.put(new JSONObject().put("id", "t10").put("date", "2024-09-05").put("type", "expense").put("amount", 120.00).put("description", "Gas").put("category", "Transportation"));
        mockData.put("transactions", transactions);

        mockData.put("epf_balance", new JSONObject().put("current_balance", 55000.00).put("contributions_last_year", 4800.00));
        mockData.put("credit_score", new JSONObject().put("score", 780).put("rating", "Excellent"));

        JSONArray investments = new JSONArray();
        investments.put(new JSONObject().put("name", "Tech Stock Fund").put("type", "mutual_fund").put("value", 7500.00));
        investments.put(new JSONObject().put("name", "Bond Portfolio").put("type", "bond").put("value", 3000.00));
        mockData.put("investments", investments);

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(mockData.toString(4)); // toString(4) for pretty printing
            System.out.println("Successfully created mock_data.json.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
