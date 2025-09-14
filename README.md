Financial Assistant Application

This project is a Java-based command-line financial assistant designed to help users manage their personal finances. It uses a local mock data file to simulate a user's financial profile, a permission system for data privacy, and integrates with a large language model (LLM) to provide intelligent insights and summaries.

Features
Mock Data Generation: The application can automatically create a mock_data.json file with sample financial data, including assets, liabilities, transactions, and a credit score, if one doesn't already exist.

Data Privacy: A built-in permissions system allows users to grant or revoke access to different categories of their financial data, such as transactions or investments. This ensures that sensitive information is only used for analysis with explicit user permission.

Rule-Based Analysis: The assistant can perform specific, pre-defined financial calculations, such as determining your net worth or generating a spending summary over a specified period (e.g., "last 3 months").

AI-Powered Insights: For more complex or open-ended queries, the application integrates with an AI model to provide comprehensive, personalized financial advice and summaries based on the user's available data.

How to Use
Once the application is running, you can interact with it through the command line.

Grant/Revoke Access:

grant access to transactions

revoke access to assets

Financial Queries:

What is my net worth?

Show me my recent transactions

What is my spending summary for the last month?

Provide a comprehensive summary of my finances.

Exit:

exit
