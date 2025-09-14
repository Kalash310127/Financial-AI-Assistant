package com.example.Financialassistantapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// The main class for the Spring Boot application.
// @SpringBootApplication automatically configures and runs the app.
@SpringBootApplication
class FinancialAssistantApplication {

	public static void main(String[] args) {
		// This runs the entire application.
		SpringApplication.run(FinancialAssistantApplication.class, args);
	}
}

// Data Transfer Object (DTO) for the incoming query request.
record QueryRequest(String query) {}

// Data Transfer Object (DTO) for the outgoing response.
record QueryResponse(String response) {}

// Main REST Controller to handle API requests.
@RestController
@CrossOrigin(origins = "*") // Allows requests from any origin (important for frontend testing)
class FinancialAssistantController {

	private final FinancialAssistantService assistantService;

	@Autowired
	public FinancialAssistantController(FinancialAssistantService assistantService) {
		this.assistantService = assistantService;
	}

	// Handles a POST request to the /query endpoint.
	@PostMapping("/query")
	public ResponseEntity<QueryResponse> handleQuery(@RequestBody QueryRequest request) {
		String assistantResponse = assistantService.handleQuery(request.query());
		return ResponseEntity.ok(new QueryResponse(assistantResponse));
	}
}

// Main service class containing the business logic.
// @Service marks this as a Spring service component.
@Service
class FinancialAssistantService {
	private final FinancialAssistant financialAssistant;

	@Autowired
	public FinancialAssistantService() {
		// Initialize the assistant with a mock data file path.
		// We'll ensure this file exists on startup.
		this.financialAssistant = new FinancialAssistant("mock_data.json");
	}

	// This method will be called automatically after the bean is created.
	// It ensures the mock data file exists.
	@PostConstruct
	public void setup() {
		File file = new File("mock_data.json");
		if (!file.exists()) {
			System.out.println("Mock data file not found. Creating a new one...");
			MockDataGenerator.createMockData("mock_data.json");
		} else {
			System.out.println("Using existing mock_data.json file.");
		}
	}

	// Exposes the core query handling method.
	public String handleQuery(String query) {
		return financialAssistant.handleQuery(query);
	}
}

// The following classes are directly ported from your original code.

class FinancialAssistant {
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
			return "Hello! I am your AI-powered financial assistant. How can I help you today?";
		}

		if (lowerCaseQuery.contains("help")) {
			return "I can answer questions about your financial data, provide insights, and manage data access.\n" +
					"Try asking about: 'assets', 'transactions', 'expenses', 'savings', 'credit score', or 'investments'.\n" +
					"You can also use 'grant access to [category]' or 'revoke access to [category]'.";
		}

		// Handle "my details" query with a specific, direct method
		if (lowerCaseQuery.contains("my details") || lowerCaseQuery.contains("my banking details")) {
			return new FinancialDataSummary().getBankingDetailsSummary(permittedData);
		}

		// 2. Handle queries based on available data
		if (permittedData.isEmpty()) {
			return "I don't have access to any of your financial data. Please grant me access to a category first (e.g., 'grant access to assets').";
		}

		// Handle "my assets" query
		if (lowerCaseQuery.contains("my assets") || lowerCaseQuery.contains("my savings") || lowerCaseQuery.contains("my accounts")) {
			this.lastQueryContext = "assets";
			if (permittedData.containsKey("assets")) {
				return new FinancialDataSummary().getBankingDetailsSummary(permittedData);
			} else {
				return "I cannot access your asset information. Please grant me access to 'assets'.";
			}
		}

		// Handle "transactions" query
		if (lowerCaseQuery.contains("my transactions") || lowerCaseQuery.contains("recent transactions")) {
			this.lastQueryContext = "transactions";
			return formatTransactions(permittedData);
		}

		// Handle "expenses" query
		Pattern spendingPattern = Pattern.compile("how much did i spend (in|last) (month|year|week|day|all time)");
		Matcher spendingMatcher = spendingPattern.matcher(lowerCaseQuery);
		if (spendingMatcher.find()) {
			String timeframe = spendingMatcher.group(2);
			return aiInsights.getSpendingSummary(permittedData, "last_" + timeframe);
		}
		if (lowerCaseQuery.contains("my expenses") || lowerCaseQuery.contains("spending")) {
			return aiInsights.getSpendingSummary(permittedData, "last_month");
		}

		// Handle "investments" query
		if (lowerCaseQuery.contains("my investments")) {
			this.lastQueryContext = "investments";
			return formatInvestments(permittedData);
		}

		// Handle "credit score" query
		if (lowerCaseQuery.contains("my credit score")) {
			this.lastQueryContext = "credit_score";
			if (permittedData.containsKey("credit_score")) {
				JSONObject creditScore = (JSONObject) permittedData.get("credit_score");
				return String.format("Your credit score is %d (%s).", creditScore.getInt("score"), creditScore.getString("rating"));
			} else {
				return "I cannot access your credit score. Please grant me access to 'credit_score'.";
			}
		}

		// Fallback to the AI model for any other query
		this.lastQueryContext = "gemini";
		return geminiInsights.getAIResponse(query, permittedData);
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

	private String formatInvestments(Map<String, Object> data) {
		if (!data.containsKey("investments")) {
			return "I cannot access your investment information. Please grant me access to 'investments'.";
		}
		StringBuilder sb = new StringBuilder("Here is a summary of your investments:\n\n");
		JSONArray investments = (JSONArray) data.get("investments");
		for (int i = 0; i < investments.length(); i++) {
			JSONObject investment = investments.getJSONObject(i);
			sb.append(String.format("- %s (%s): $%.2f\n",
					investment.getString("name"),
					investment.getString("type"),
					investment.getDouble("value")));
		}
		return sb.toString();
	}
}

class MockDataGenerator {
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
		transactions.put(new JSONObject().put("id", "t1").put("date", "2024-09-10").put("type", "expense").put("amount", 50.50).put("description", "Coffee Shop").put("category", "Food"));
		transactions.put(new JSONObject().put("id", "t2").put("date", "2024-09-09").put("type", "expense").put("amount", 120.00).put("description", "Groceries").put("category", "Groceries"));
		transactions.put(new JSONObject().put("id", "t3").put("date", "2024-09-08").put("type", "income").put("amount", 150.00).put("description", "Freelance Work"));
		transactions.put(new JSONObject().put("id", "t4").put("date", "2024-09-07").put("type", "expense").put("amount", 35.75).put("description", "Gas Station").put("category", "Transportation"));
		transactions.put(new JSONObject().put("id", "t5").put("date", "2024-09-06").put("type", "expense").put("amount", 85.00).put("description", "Dinner with friends").put("category", "Food"));
		transactions.put(new JSONObject().put("id", "t6").put("date", "2024-08-25").put("type", "expense").put("amount", 750.00).put("description", "Rent").put("category", "Housing"));
		transactions.put(new JSONObject().put("id", "t7").put("date", "2024-08-22").put("type", "expense").put("amount", 80.00).put("description", "Electricity Bill").put("category", "Utilities"));
		transactions.put(new JSONObject().put("id", "t8").put("date", "2024-08-20").put("type", "expense").put("amount", 150.00).put("description", "Shopping").put("category", "Shopping"));
		transactions.put(new JSONObject().put("id", "t9").put("date", "2024-09-01").put("type", "income").put("amount", 4000.00).put("description", "Salary"));
		transactions.put(new JSONObject().put("id", "t10").put("date", "2024-09-05").put("type", "expense").put("amount", 120.00).put("description", "Gas").put("category", "Transportation"));
		mockData.put("transactions", transactions);

		mockData.put("epf_balance", new JSONObject().put("current_balance", 55000.00).put("contributions_last_year", 4800.00));
		mockData.put("credit_score", new JSONObject().put("score", 780).put("rating", "Excellent"));

		JSONArray investments = new JSONArray();
		investments.put(new JSONObject().put("name", "Tech Stock Fund").put("type", "mutual_fund").put("value", 7500.00));
		investments.put(new JSONObject().put("name", "Bond Portfolio").put("type", "bond").put("value", 3000.00));
		investments.put(new JSONObject().put("name", "Crypto Portfolio").put("type", "crypto").put("value", 1200.00));
		mockData.put("investments", investments);

		try (FileWriter file = new FileWriter(filePath)) {
			file.write(mockData.toString(4));
			System.out.println("Successfully created mock_data.json");
		} catch (IOException e) {
			System.err.println("Failed to write mock data file: " + e.getMessage());
		}
	}
}

class DataManager {
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
		// Default to no access for privacy
		initialPermissions.put("assets", false);
		initialPermissions.put("liabilities", false);
		initialPermissions.put("transactions", false);
		initialPermissions.put("epf_balance", false);
		initialPermissions.put("credit_score", false);
		initialPermissions.put("investments", false);
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

class AIInsights {

	public String getSpendingSummary(Map<String, Object> data, String timeframe) {
		if (!data.containsKey("transactions")) {
			return "I cannot analyze spending. Please grant access to 'transactions'.";
		}

		JSONArray transactions = (JSONArray) data.get("transactions");
		if (transactions.isEmpty()) {
			return "You have no transactions to analyze.";
		}

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
		} else if ("last_week".equals(timeframe)) {
			startDate = endDate.minus(1, ChronoUnit.WEEKS);
		} else if ("last_year".equals(timeframe)) {
			startDate = endDate.minus(1, ChronoUnit.YEARS);
		} else if ("all_time".equals(timeframe)) {
			startDate = LocalDate.MIN;
		} else {
			return "I'm not sure how to analyze that timeframe. Please try 'last month', 'last week', or 'last year'.";
		}

		double totalSpending = 0.0;
		Map<String, Double> categorySpending = new HashMap<>();

		for (int i = 0; i < transactions.length(); i++) {
			JSONObject t = transactions.getJSONObject(i);
			if ("expense".equals(t.getString("type"))) {
				LocalDate transactionDate = LocalDate.parse(t.getString("date"), formatter);
				if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
					double amount = t.getDouble("amount");
					totalSpending += amount;
					String category = t.optString("category", "Uncategorized");
					categorySpending.put(category, categorySpending.getOrDefault(category, 0.0) + amount);
				}
			}
		}

		if (totalSpending == 0.0) {
			return String.format("You had no spending during the %s period.", timeframe.replace("_", " "));
		}

		StringBuilder summary = new StringBuilder(String.format("In the %s, you spent a total of $%.2f.\n\n",
				timeframe.replace("_", " "), totalSpending));
		summary.append("Spending by category:\n");
		double finalTotalSpending = totalSpending;
		categorySpending.entrySet().stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.forEach(entry -> summary.append(String.format("- %s: $%.2f (%.1f%%)\n",
						entry.getKey(),
						entry.getValue(),
						(entry.getValue() / finalTotalSpending) * 100)));

		return summary.toString();
	}
}

class GeminiInsights {

	// IMPORTANT: You MUST replace this with your actual API key.
	// The placeholder key will not work.
	private static final String API_KEY = "YOUR_API_KEY_HERE";
	private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-05-20:generateContent?key=" + API_KEY;

	public String getAIResponse(String prompt, Map<String, Object> data) {
		if ("YOUR_API_KEY_HERE".equals(API_KEY)) {
			return "Please update the API_KEY in GeminiInsights.java with a valid key to use this feature.";
		}

		try {
			JSONObject payload = new JSONObject();
			JSONArray contents = new JSONArray();
			JSONObject content = new JSONObject();
			JSONArray parts = new JSONArray();

			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("Here is my financial data in JSON format:\n");
			queryBuilder.append(new JSONObject(data).toString(4));
			queryBuilder.append("\n\nBased on this data, please answer the following question:\n");
			queryBuilder.append(prompt);

			parts.put(new JSONObject().put("text", queryBuilder.toString()));

			content.put("parts", parts);
			contents.put(content);
			payload.put("contents", contents);

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

class FinancialDataSummary {

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
