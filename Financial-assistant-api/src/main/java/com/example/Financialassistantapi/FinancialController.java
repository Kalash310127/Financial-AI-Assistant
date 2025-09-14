package com.example.Financialassistantapi;

import finance_assistant.FinancialAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/api/financial-assistant")
@CrossOrigin(origins = "*") // Add this line
public class FinancialController {

    private final FinancialAssistant assistant;

    // Use a mock data file path relative to the project root
    private static final String MOCK_DATA_FILE_PATH = "mock_data.json";

    public FinancialController() {
        this.assistant = new FinancialAssistant(MOCK_DATA_FILE_PATH);
    }

    @GetMapping("/handle-query")
    public String handleQuery(@RequestParam("query") String query) {
        return assistant.handleQuery(query);
    }

    @GetMapping("/spending-summary")
    public String getSpendingSummary(@RequestParam("timeframe") String timeframe) {
        Map<String, Object> data = assistant.dataManager.getPermittedData();
        return assistant.aiInsights.getSpendingSummary(data, timeframe);
    }

    @GetMapping("/comprehensive-summary")
    public String getComprehensiveSummary() {
        Map<String, Object> data = assistant.dataManager.getPermittedData();
        return assistant.aiInsights.getComprehensiveSummary(data);
    }
}
