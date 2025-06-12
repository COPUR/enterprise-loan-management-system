#!/usr/bin/env python3
"""
Enterprise Banking Assistant - OpenAI Integration
Provides intelligent banking operations through AI-powered conversations
"""

import json
import os
import asyncio
import websockets
import requests
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from openai import OpenAI

# the newest OpenAI model is "gpt-4o" which was released May 13, 2024.
# do not change this unless explicitly requested by the user
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
client = OpenAI(api_key=OPENAI_API_KEY)

class BankingAssistant:
    """Advanced Banking Assistant with OpenAI integration"""
    
    def __init__(self):
        self.graphql_url = "http://localhost:5000/graphql"
        self.mcp_url = "ws://localhost:5000/mcp"
        self.client = client
        self.assistant_id = None
        self.thread_id = None
        
    async def initialize_assistant(self):
        """Initialize OpenAI Assistant with banking expertise"""
        try:
            # Create specialized banking assistant
            assistant = self.client.beta.assistants.create(
                name="Enterprise Banking Assistant",
                instructions="""You are an expert banking assistant for an Enterprise Loan Management System. 
                
                Your capabilities include:
                - Customer account analysis and risk assessment
                - Loan application processing and approval recommendations
                - Payment calculations and optimization strategies
                - Financial reporting and analytics
                - Regulatory compliance guidance
                - Credit scoring and risk evaluation
                
                You have access to real banking data through GraphQL and MCP APIs. Always provide:
                - Accurate financial calculations
                - Risk-based recommendations
                - Compliance-aware advice
                - Clear explanations of banking terms
                - Actionable next steps
                
                Banking Rules:
                - Loan installments: 6, 9, 12, or 24 months only
                - Interest rates: 0.1% to 0.5% range
                - Early payment discounts available
                - Late payment penalties apply
                - Credit scores range 300-850
                
                Always verify data through the banking APIs before providing recommendations.""",
                model="gpt-4o",
                tools=[
                    {
                        "type": "function",
                        "function": {
                            "name": "get_customer_profile",
                            "description": "Retrieve comprehensive customer profile with loans and payment history",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "customer_id": {
                                        "type": "string",
                                        "description": "Customer ID to retrieve profile for"
                                    }
                                },
                                "required": ["customer_id"]
                            }
                        }
                    },
                    {
                        "type": "function",
                        "function": {
                            "name": "analyze_loan_eligibility",
                            "description": "Analyze customer eligibility for a new loan",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "customer_id": {
                                        "type": "string",
                                        "description": "Customer ID to analyze"
                                    },
                                    "loan_amount": {
                                        "type": "number",
                                        "description": "Requested loan amount"
                                    },
                                    "installment_count": {
                                        "type": "integer",
                                        "description": "Number of installments (6, 9, 12, or 24)"
                                    }
                                },
                                "required": ["customer_id", "loan_amount", "installment_count"]
                            }
                        }
                    },
                    {
                        "type": "function",
                        "function": {
                            "name": "calculate_payment_options",
                            "description": "Calculate optimal payment strategies for existing loans",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "loan_id": {
                                        "type": "string",
                                        "description": "Loan ID to calculate payments for"
                                    },
                                    "payment_amount": {
                                        "type": "number",
                                        "description": "Proposed payment amount"
                                    }
                                },
                                "required": ["loan_id", "payment_amount"]
                            }
                        }
                    },
                    {
                        "type": "function",
                        "function": {
                            "name": "generate_risk_report",
                            "description": "Generate comprehensive risk assessment report",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "customer_id": {
                                        "type": "string",
                                        "description": "Customer ID for risk assessment"
                                    }
                                },
                                "required": ["customer_id"]
                            }
                        }
                    },
                    {
                        "type": "function",
                        "function": {
                            "name": "get_banking_analytics",
                            "description": "Retrieve banking analytics and performance metrics",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "period": {
                                        "type": "string",
                                        "description": "Analysis period (LAST_30_DAYS, LAST_90_DAYS, LAST_YEAR)",
                                        "enum": ["LAST_30_DAYS", "LAST_90_DAYS", "LAST_YEAR"]
                                    }
                                },
                                "required": ["period"]
                            }
                        }
                    }
                ]
            )
            
            self.assistant_id = assistant.id
            print(f"Banking Assistant initialized: {self.assistant_id}")
            
            # Create conversation thread
            thread = self.client.beta.threads.create()
            self.thread_id = thread.id
            print(f"Conversation thread created: {self.thread_id}")
            
            return True
            
        except Exception as e:
            print(f"Failed to initialize assistant: {e}")
            return False
    
    def get_customer_profile(self, customer_id: str) -> Dict[str, Any]:
        """Retrieve comprehensive customer profile"""
        query = """
        query CustomerProfile($id: ID!) {
            customer(id: $id) {
                customerId
                customerNumber
                fullName
                email
                phone
                address {
                    street
                    city
                    state
                    zipCode
                    country
                }
                creditLimit
                availableCredit
                creditScore
                accountStatus
                riskLevel
                createdAt
                updatedAt
                loans {
                    loanId
                    loanNumber
                    loanAmount
                    outstandingAmount
                    interestRate
                    installmentCount
                    status
                    createdAt
                    dueDate
                    daysOverdue
                    installments {
                        installmentNumber
                        dueDate
                        principalAmount
                        interestAmount
                        totalAmount
                        status
                        paidDate
                        paidAmount
                    }
                }
                payments {
                    paymentId
                    paymentAmount
                    paymentDate
                    status
                    paymentMethod
                    processingFee
                }
                riskProfile {
                    overallRisk
                    creditRisk
                    incomeRisk
                    behavioralRisk
                    paymentHistory
                    creditUtilization
                    incomeStability
                    riskFactors {
                        factor
                        impact
                        description
                    }
                }
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": query, "variables": {"id": customer_id}},
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                if 'errors' in data:
                    return {"error": f"GraphQL errors: {data['errors']}"}
                return data.get('data', {}).get('customer', {})
            else:
                return {"error": f"HTTP {response.status_code}: {response.text}"}
                
        except Exception as e:
            return {"error": f"Failed to fetch customer profile: {e}"}
    
    def analyze_loan_eligibility(self, customer_id: str, loan_amount: float, installment_count: int) -> Dict[str, Any]:
        """Analyze customer eligibility for a new loan"""
        query = """
        query LoanEligibility($customerId: ID!, $loanAmount: Float!, $installmentCount: Int!) {
            loanEligibility(
                customerId: $customerId
                loanAmount: $loanAmount
                installmentCount: $installmentCount
            ) {
                eligible
                creditScore
                availableCredit
                maxLoanAmount
                recommendedAmount
                interestRate
                monthlyPayment
                reasons
                requirements
                riskLevel
                approvalProbability
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={
                    "query": query,
                    "variables": {
                        "customerId": customer_id,
                        "loanAmount": loan_amount,
                        "installmentCount": installment_count
                    }
                },
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                if 'errors' in data:
                    return {"error": f"GraphQL errors: {data['errors']}"}
                return data.get('data', {}).get('loanEligibility', {})
            else:
                return {"error": f"HTTP {response.status_code}: {response.text}"}
                
        except Exception as e:
            return {"error": f"Failed to analyze loan eligibility: {e}"}
    
    def calculate_payment_options(self, loan_id: str, payment_amount: float) -> Dict[str, Any]:
        """Calculate optimal payment strategies"""
        query = """
        query PaymentCalculation($input: PaymentCalculationInput!) {
            paymentCalculation(input: $input) {
                loanId
                paymentAmount
                baseAmount
                discountAmount
                penaltyAmount
                finalAmount
                earlyPaymentDays
                latePaymentDays
                savingsAmount
                installmentBreakdown {
                    installmentNumber
                    dueDate
                    originalAmount
                    discountApplied
                    penaltyApplied
                    amountToPay
                    status
                }
                recommendations {
                    strategy
                    description
                    potentialSavings
                    riskLevel
                }
            }
        }
        """
        
        try:
            current_date = datetime.now().isoformat() + "Z"
            response = requests.post(
                self.graphql_url,
                json={
                    "query": query,
                    "variables": {
                        "input": {
                            "loanId": loan_id,
                            "paymentAmount": payment_amount,
                            "paymentDate": current_date,
                            "simulateOnly": True
                        }
                    }
                },
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                if 'errors' in data:
                    return {"error": f"GraphQL errors: {data['errors']}"}
                return data.get('data', {}).get('paymentCalculation', {})
            else:
                return {"error": f"HTTP {response.status_code}: {response.text}"}
                
        except Exception as e:
            return {"error": f"Failed to calculate payment options: {e}"}
    
    def generate_risk_report(self, customer_id: str) -> Dict[str, Any]:
        """Generate comprehensive risk assessment report"""
        query = """
        query RiskAssessment($customerId: ID!) {
            customer(id: $customerId) {
                customerId
                fullName
                creditScore
                riskLevel
                riskProfile {
                    overallRisk
                    creditRisk
                    incomeRisk
                    behavioralRisk
                    paymentHistory
                    creditUtilization
                    incomeStability
                    riskFactors {
                        factor
                        impact
                        description
                    }
                }
            }
            
            riskAssessment(customerId: $customerId) {
                customerId
                assessmentDate
                overallRiskScore
                creditRisk
                incomeRisk
                behavioralRisk
                riskLevel
                riskFactors {
                    factor
                    impact
                    description
                    mitigation
                }
                recommendations {
                    action
                    description
                    urgency
                    expectedImpact
                }
                monitoring {
                    requiredActions
                    nextReviewDate
                    alertThresholds
                }
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": query, "variables": {"customerId": customer_id}},
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                if 'errors' in data:
                    return {"error": f"GraphQL errors: {data['errors']}"}
                return {
                    "customer": data.get('data', {}).get('customer', {}),
                    "riskAssessment": data.get('data', {}).get('riskAssessment', {})
                }
            else:
                return {"error": f"HTTP {response.status_code}: {response.text}"}
                
        except Exception as e:
            return {"error": f"Failed to generate risk report: {e}"}
    
    def get_banking_analytics(self, period: str = "LAST_30_DAYS") -> Dict[str, Any]:
        """Retrieve banking analytics and performance metrics"""
        query = """
        query BankingAnalytics($period: AnalyticsPeriod!) {
            loanAnalytics(period: $period) {
                totalLoansCreated
                totalLoanAmount
                averageLoanAmount
                approvalRate
                defaultRate
                collectionEfficiency
                loanTypeDistribution {
                    loanType
                    count
                    totalAmount
                    percentage
                }
                riskDistribution {
                    riskLevel
                    count
                    percentage
                }
            }
            
            paymentAnalytics(period: $period) {
                totalPayments
                totalPaymentAmount
                averagePaymentAmount
                onTimePaymentRate
                earlyPaymentRate
                latePaymentRate
                collectionRate
                paymentMethodDistribution {
                    method
                    count
                    totalAmount
                    percentage
                }
            }
            
            systemHealth {
                status
                timestamp
                uptime
                services {
                    serviceName
                    status
                    responseTime
                    errorRate
                    lastHealthCheck
                }
                metrics {
                    cpuUsage
                    memoryUsage
                    diskUsage
                    activeConnections
                    requestsPerSecond
                    averageResponseTime
                }
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": query, "variables": {"period": period}},
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                if 'errors' in data:
                    return {"error": f"GraphQL errors: {data['errors']}"}
                return data.get('data', {})
            else:
                return {"error": f"HTTP {response.status_code}: {response.text}"}
                
        except Exception as e:
            return {"error": f"Failed to get banking analytics: {e}"}
    
    async def handle_banking_conversation(self, user_message: str) -> str:
        """Handle banking conversation through OpenAI Assistant"""
        if not self.assistant_id or not self.thread_id:
            await self.initialize_assistant()
        
        try:
            # Add user message to thread
            self.client.beta.threads.messages.create(
                thread_id=self.thread_id,
                role="user",
                content=user_message
            )
            
            # Run the assistant
            run = self.client.beta.threads.runs.create(
                thread_id=self.thread_id,
                assistant_id=self.assistant_id
            )
            
            # Wait for completion and handle function calls
            while True:
                run_status = self.client.beta.threads.runs.retrieve(
                    thread_id=self.thread_id,
                    run_id=run.id
                )
                
                if run_status.status == 'completed':
                    break
                elif run_status.status == 'requires_action':
                    # Handle function calls
                    tool_outputs = []
                    
                    for tool_call in run_status.required_action.submit_tool_outputs.tool_calls:
                        function_name = tool_call.function.name
                        function_args = json.loads(tool_call.function.arguments)
                        
                        # Execute the appropriate function
                        if function_name == "get_customer_profile":
                            result = self.get_customer_profile(function_args["customer_id"])
                        elif function_name == "analyze_loan_eligibility":
                            result = self.analyze_loan_eligibility(
                                function_args["customer_id"],
                                function_args["loan_amount"],
                                function_args["installment_count"]
                            )
                        elif function_name == "calculate_payment_options":
                            result = self.calculate_payment_options(
                                function_args["loan_id"],
                                function_args["payment_amount"]
                            )
                        elif function_name == "generate_risk_report":
                            result = self.generate_risk_report(function_args["customer_id"])
                        elif function_name == "get_banking_analytics":
                            result = self.get_banking_analytics(function_args.get("period", "LAST_30_DAYS"))
                        else:
                            result = {"error": f"Unknown function: {function_name}"}
                        
                        tool_outputs.append({
                            "tool_call_id": tool_call.id,
                            "output": json.dumps(result)
                        })
                    
                    # Submit tool outputs
                    self.client.beta.threads.runs.submit_tool_outputs(
                        thread_id=self.thread_id,
                        run_id=run.id,
                        tool_outputs=tool_outputs
                    )
                
                elif run_status.status in ['failed', 'cancelled', 'expired']:
                    return f"Assistant run failed with status: {run_status.status}"
                
                # Small delay to avoid overwhelming the API
                await asyncio.sleep(1)
            
            # Get the assistant's response
            messages = self.client.beta.threads.messages.list(thread_id=self.thread_id)
            latest_message = messages.data[0]
            
            if latest_message.role == 'assistant':
                return latest_message.content[0].text.value
            else:
                return "No response from assistant"
                
        except Exception as e:
            return f"Error in banking conversation: {e}"
    
    async def start_interactive_session(self):
        """Start interactive banking assistant session"""
        print("🏦 Enterprise Banking Assistant - Interactive Session")
        print("=" * 60)
        print("Ask me anything about:")
        print("• Customer accounts and loan eligibility")
        print("• Risk assessment and credit analysis")
        print("• Payment calculations and optimization")
        print("• Banking analytics and performance metrics")
        print("• Regulatory compliance and best practices")
        print("\nType 'exit' to end the session")
        print("=" * 60)
        
        await self.initialize_assistant()
        
        while True:
            try:
                user_input = input("\n🤖 How can I help you today? ")
                
                if user_input.lower() in ['exit', 'quit', 'bye']:
                    print("\n👋 Thank you for using the Enterprise Banking Assistant!")
                    break
                
                if not user_input.strip():
                    continue
                
                print("\n🔄 Processing your request...")
                response = await self.handle_banking_conversation(user_input)
                print(f"\n💡 Banking Assistant:\n{response}")
                
            except KeyboardInterrupt:
                print("\n\n👋 Session ended by user")
                break
            except Exception as e:
                print(f"\n❌ Error: {e}")

# Main execution
async def main():
    """Main function to run the banking assistant"""
    assistant = BankingAssistant()
    
    # Test assistant initialization
    print("🚀 Initializing Enterprise Banking Assistant...")
    success = await assistant.initialize_assistant()
    
    if success:
        print("✅ Banking Assistant successfully initialized!")
        
        # Start interactive session
        await assistant.start_interactive_session()
    else:
        print("❌ Failed to initialize Banking Assistant")
        return 1
    
    return 0

if __name__ == "__main__":
    asyncio.run(main())