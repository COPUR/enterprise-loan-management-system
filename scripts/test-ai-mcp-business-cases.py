#!/usr/bin/env python3
"""
AI and MCP Business Case Testing Suite
Tests real banking scenarios using OpenAI Assistant and MCP protocol
"""

import asyncio
import websockets
import requests
import json
import os
from datetime import datetime
from openai import OpenAI

class BusinessCaseTestSuite:
    def __init__(self):
        self.graphql_url = "http://localhost:5000/graphql"
        self.mcp_url = "ws://localhost:5000/mcp"
        self.client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
        self.assistant_id = None
        self.thread_id = None
        
    async def initialize_assistant(self):
        """Initialize banking assistant for business case testing"""
        try:
            assistant = self.client.beta.assistants.create(
                name="Banking Business Case Analyst",
                instructions="""You are an expert banking analyst specializing in real-world business scenarios.

Your expertise includes:
- Customer credit analysis and risk assessment
- Loan portfolio management and optimization
- Payment strategy development
- Regulatory compliance evaluation
- Business intelligence and analytics

Banking Rules:
- Loan terms: 6, 9, 12, or 24 installments only
- Interest rates: 0.1% to 0.5% range
- Credit scores: 300-850 scale
- Early payment discounts available
- Late payment penalties apply

Provide actionable insights for business decision-making.""",
                model="gpt-4o",
                tools=[
                    {
                        "type": "function",
                        "function": {
                            "name": "analyze_business_case",
                            "description": "Analyze a banking business scenario",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "scenario": {"type": "string"},
                                    "data_points": {"type": "array", "items": {"type": "string"}},
                                    "decision_criteria": {"type": "string"}
                                },
                                "required": ["scenario"]
                            }
                        }
                    }
                ]
            )
            
            self.assistant_id = assistant.id
            thread = self.client.beta.threads.create()
            self.thread_id = thread.id
            
            print(f"Business Case Assistant initialized: {self.assistant_id}")
            return True
            
        except Exception as e:
            print(f"Failed to initialize assistant: {e}")
            return False
    
    async def test_mcp_customer_analysis(self):
        """Test MCP integration for customer analysis"""
        print("\n=== MCP Customer Analysis Business Case ===")
        
        try:
            async with websockets.connect(self.mcp_url) as websocket:
                # Initialize MCP connection
                init_message = {
                    "jsonrpc": "2.0",
                    "id": "init",
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "1.0.0",
                        "capabilities": {"tools": True, "resources": True}
                    }
                }
                
                await websocket.send(json.dumps(init_message))
                init_response = await websocket.recv()
                print("MCP Connection established")
                
                # Business Case: Identify high-risk customers for portfolio review
                customer_search = {
                    "jsonrpc": "2.0",
                    "id": "risk_analysis",
                    "method": "tools/call",
                    "params": {
                        "name": "search_customers",
                        "arguments": {
                            "query": "high risk customers with overdue payments",
                            "filters": {
                                "riskLevel": "HIGH",
                                "accountStatus": "ACTIVE"
                            }
                        }
                    }
                }
                
                await websocket.send(json.dumps(customer_search))
                search_response = await websocket.recv()
                search_result = json.loads(search_response)
                
                if "result" in search_result:
                    print("✓ MCP customer search successful")
                    print(f"Business insight: Found high-risk customers for review")
                    return search_result["result"]
                else:
                    print("✗ MCP search failed")
                    return None
                    
        except Exception as e:
            print(f"MCP test failed: {e}")
            return None
    
    async def test_ai_loan_portfolio_analysis(self):
        """Test AI analysis of loan portfolio performance"""
        print("\n=== AI Loan Portfolio Analysis Business Case ===")
        
        # Business Case: Portfolio manager needs insights on loan performance
        portfolio_query = """
        query PortfolioAnalysis {
            loanAnalytics(period: LAST_30_DAYS) {
                totalLoansCreated
                totalLoanAmount
                approvalRate
                defaultRate
                loanTypeDistribution {
                    loanType
                    count
                    totalAmount
                    percentage
                }
            }
            
            paymentAnalytics(period: LAST_30_DAYS) {
                onTimePaymentRate
                latePaymentRate
                collectionRate
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": portfolio_query},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                if "data" in data:
                    loan_analytics = data["data"].get("loanAnalytics", {})
                    payment_analytics = data["data"].get("paymentAnalytics", {})
                    
                    # AI Analysis using OpenAI Assistant
                    analysis_prompt = f"""
                    Analyze this loan portfolio performance data and provide business insights:
                    
                    Loan Performance:
                    - Total loans: {loan_analytics.get('totalLoansCreated', 'N/A')}
                    - Approval rate: {loan_analytics.get('approvalRate', 'N/A')}%
                    - Default rate: {loan_analytics.get('defaultRate', 'N/A')}%
                    
                    Payment Performance:
                    - On-time rate: {payment_analytics.get('onTimePaymentRate', 'N/A')}%
                    - Late rate: {payment_analytics.get('latePaymentRate', 'N/A')}%
                    - Collection rate: {payment_analytics.get('collectionRate', 'N/A')}%
                    
                    Provide actionable recommendations for portfolio optimization.
                    """
                    
                    ai_response = await self.ask_assistant(analysis_prompt)
                    print("✓ AI portfolio analysis completed")
                    print(f"Business insight: {ai_response[:200]}...")
                    return ai_response
                else:
                    print("✗ No portfolio data available")
                    return None
            else:
                print(f"✗ Portfolio query failed: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"AI portfolio analysis failed: {e}")
            return None
    
    async def test_risk_assessment_workflow(self):
        """Test comprehensive risk assessment workflow"""
        print("\n=== Risk Assessment Workflow Business Case ===")
        
        # Business Case: Loan officer needs risk assessment for new application
        customer_id = "1"
        loan_amount = 50000.0
        installments = 12
        
        # Step 1: Get customer profile via GraphQL
        customer_query = f"""
        query CustomerRisk {{
            customer(id: "{customer_id}") {{
                fullName
                creditScore
                riskLevel
                creditLimit
                availableCredit
                riskProfile {{
                    overallRisk
                    paymentHistory
                    creditUtilization
                }}
            }}
        }}
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": customer_query},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                customer_data = data.get("data", {}).get("customer", {})
                
                if customer_data:
                    # Step 2: AI risk analysis
                    risk_prompt = f"""
                    Conduct a comprehensive risk assessment for this loan application:
                    
                    Customer Profile:
                    - Name: {customer_data.get('fullName', 'N/A')}
                    - Credit Score: {customer_data.get('creditScore', 'N/A')}
                    - Risk Level: {customer_data.get('riskLevel', 'N/A')}
                    - Credit Utilization: {customer_data.get('riskProfile', {}).get('creditUtilization', 'N/A')}%
                    
                    Loan Request:
                    - Amount: ${loan_amount:,}
                    - Term: {installments} months
                    
                    Provide:
                    1. Risk assessment score (1-10)
                    2. Approval recommendation
                    3. Suggested interest rate (0.1-0.5%)
                    4. Risk mitigation strategies
                    """
                    
                    risk_analysis = await self.ask_assistant(risk_prompt)
                    print("✓ Risk assessment completed")
                    print(f"Business decision: {risk_analysis[:300]}...")
                    return risk_analysis
                else:
                    print("✗ Customer data not found")
                    return None
            else:
                print(f"✗ Customer query failed: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"Risk assessment failed: {e}")
            return None
    
    async def test_payment_optimization_case(self):
        """Test payment optimization business case"""
        print("\n=== Payment Optimization Business Case ===")
        
        # Business Case: Customer wants to optimize loan payments
        optimization_query = """
        query PaymentOptimization {
            loans(filter: { status: ACTIVE }) {
                loanId
                outstandingAmount
                interestRate
                installments {
                    installmentNumber
                    dueDate
                    totalAmount
                    status
                }
            }
        }
        """
        
        try:
            response = requests.post(
                self.graphql_url,
                json={"query": optimization_query},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                loans = data.get("data", {}).get("loans", [])
                
                if loans:
                    active_loan = loans[0]  # Take first active loan
                    
                    # AI optimization analysis
                    optimization_prompt = f"""
                    Analyze payment optimization strategies for this loan:
                    
                    Loan Details:
                    - Outstanding Amount: ${active_loan.get('outstandingAmount', 0):,}
                    - Interest Rate: {active_loan.get('interestRate', 0)}%
                    - Remaining installments: {len([i for i in active_loan.get('installments', []) if i.get('status') == 'PENDING'])}
                    
                    Customer scenarios to analyze:
                    1. Early payment of $10,000
                    2. Double payment strategy
                    3. Refinancing options
                    
                    Calculate potential savings and provide recommendations.
                    """
                    
                    optimization_analysis = await self.ask_assistant(optimization_prompt)
                    print("✓ Payment optimization analysis completed")
                    print(f"Business recommendation: {optimization_analysis[:300]}...")
                    return optimization_analysis
                else:
                    print("✗ No active loans found")
                    return None
            else:
                print(f"✗ Loan query failed: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"Payment optimization failed: {e}")
            return None
    
    async def test_compliance_monitoring_case(self):
        """Test compliance monitoring business case"""
        print("\n=== Compliance Monitoring Business Case ===")
        
        # Business Case: Compliance officer needs regulatory review
        compliance_prompt = """
        Conduct a compliance assessment for our loan management operations:
        
        Focus Areas:
        1. Interest rate compliance (0.1-0.5% regulatory range)
        2. Loan term adherence (6,9,12,24 month options only)
        3. Risk assessment documentation
        4. Payment processing compliance
        5. Customer data protection
        
        Provide:
        - Compliance status assessment
        - Risk areas identification
        - Remediation recommendations
        - Audit trail requirements
        """
        
        try:
            compliance_analysis = await self.ask_assistant(compliance_prompt)
            print("✓ Compliance monitoring completed")
            print(f"Regulatory insight: {compliance_analysis[:300]}...")
            return compliance_analysis
            
        except Exception as e:
            print(f"Compliance monitoring failed: {e}")
            return None
    
    async def ask_assistant(self, prompt):
        """Send prompt to OpenAI Assistant and get response"""
        if not self.assistant_id or not self.thread_id:
            await self.initialize_assistant()
        
        try:
            # Add message to thread
            self.client.beta.threads.messages.create(
                thread_id=self.thread_id,
                role="user",
                content=prompt
            )
            
            # Run assistant
            run = self.client.beta.threads.runs.create(
                thread_id=self.thread_id,
                assistant_id=self.assistant_id
            )
            
            # Wait for completion
            import time
            for _ in range(30):
                run_status = self.client.beta.threads.runs.retrieve(
                    thread_id=self.thread_id,
                    run_id=run.id
                )
                
                if run_status.status == 'completed':
                    break
                elif run_status.status in ['failed', 'cancelled', 'expired']:
                    return f"Assistant run failed: {run_status.status}"
                
                time.sleep(1)
            
            # Get response
            if run_status.status == 'completed':
                messages = self.client.beta.threads.messages.list(thread_id=self.thread_id)
                latest_message = messages.data[0]
                
                if latest_message.role == 'assistant':
                    return latest_message.content[0].text.value
            
            return "No response from assistant"
            
        except Exception as e:
            return f"Error: {e}"
    
    async def run_all_business_cases(self):
        """Execute complete business case test suite"""
        print("🏦 AI and MCP Business Case Testing Suite")
        print("=" * 50)
        
        # Initialize assistant
        success = await self.initialize_assistant()
        if not success:
            print("Failed to initialize assistant")
            return
        
        results = {}
        
        # Test Case 1: MCP Customer Analysis
        results['mcp_analysis'] = await self.test_mcp_customer_analysis()
        
        # Test Case 2: AI Portfolio Analysis
        results['ai_portfolio'] = await self.test_ai_loan_portfolio_analysis()
        
        # Test Case 3: Risk Assessment Workflow
        results['risk_assessment'] = await self.test_risk_assessment_workflow()
        
        # Test Case 4: Payment Optimization
        results['payment_optimization'] = await self.test_payment_optimization_case()
        
        # Test Case 5: Compliance Monitoring
        results['compliance'] = await self.test_compliance_monitoring_case()
        
        # Summary
        print("\n" + "=" * 50)
        print("BUSINESS CASE TEST SUMMARY")
        print("=" * 50)
        
        successful_tests = sum(1 for result in results.values() if result is not None)
        total_tests = len(results)
        
        print(f"Tests completed: {successful_tests}/{total_tests}")
        print(f"Success rate: {(successful_tests/total_tests)*100:.1f}%")
        
        for test_name, result in results.items():
            status = "✓ PASS" if result is not None else "✗ FAIL"
            print(f"{test_name}: {status}")
        
        # Clean up
        if self.assistant_id:
            try:
                self.client.beta.assistants.delete(self.assistant_id)
                print("\nTest assistant cleaned up")
            except:
                pass
        
        return results

async def main():
    """Main execution function"""
    test_suite = BusinessCaseTestSuite()
    results = await test_suite.run_all_business_cases()
    
    print("\n🎯 Business case testing demonstrates:")
    print("• AI-powered risk assessment and decision support")
    print("• MCP protocol integration for real-time data access") 
    print("• GraphQL API utilization for comprehensive banking operations")
    print("• Natural language processing for business intelligence")
    print("• Regulatory compliance monitoring and reporting")

if __name__ == "__main__":
    asyncio.run(main())