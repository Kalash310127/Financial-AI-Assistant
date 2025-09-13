package finance_assistant;

import java.io.File;
import java.util.Scanner;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        String mockDataFile = "mock_data.json";

        // Check if mock data file exists, if not, create it
        File file = new File(mockDataFile);
        if (!file.exists()) {
            MockDataGenerator.createMockData(mockDataFile);
        } else {
            System.out.println("Using existing mock_data.json file.");
        }

        System.out.println("Welcome to your AI-powered Financial Assistant!");
        System.out.println("--------------------------------------------------");

        FinancialAssistant assistant = new FinancialAssistant(mockDataFile);
        System.out.println("Initial Permissions:");
        System.out.println(new JSONObject(assistant.dataManager.getPermissions()).toString(4));
        System.out.println("--------------------------------------------------");

        Scanner scanner = new Scanner(System.in);
        while (true)
        {
            System.out.print("You: ");
            String query = scanner.nextLine();
            if ("exit".equalsIgnoreCase(query)) {
                break;
            }

            String response = assistant.handleQuery(query);
            System.out.println("Assistant: " + response);
            System.out.println("---");
        }
        scanner.close();
        System.out.println("Exiting assistant. Goodbye!");
    }
}
//310127