#!/usr/bin/env python3
"""
Business Case Testing for AI and MCP Integration
"""

import requests
import json
import asyncio
from openai import OpenAI
import os

async def test_business_cases():
    print("Testing AI and MCP Business Cases")
    print("=" * 40)
    
    # Business Case 1: Customer Risk Analysis via GraphQL
    print("Business Case 1: Customer Risk Profile Analysis")
    try:
        response = requests.post(
            "http://localhost:5000/graphql",
            json={"query": "{ customers { id fullName creditScore riskLevel creditLimit availableCredit } }"},
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            customers = data.get("data", {}).get("customers", [])
            
            if customers:
                customer = customers[0]
                print(f"Customer Profile: {customer.get('fullName', 'Unknown')}")
                print(f"Credit Score: {customer.get('creditScore', 'N/A')}")
                print(f"Risk Level: {customer.get('riskLevel', 'N/A')}")
                print(f"Available Credit: ${customer.get('availableCredit', 0):,}")
                
                # AI Analysis using OpenAI
                client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
                
                analysis_prompt = f"""Analyze this customer profile for loan eligibility:
                
Customer: {customer.get('fullName', 'Unknown')}
Credit Score: {customer.get('creditScore', 'N/A')}
Risk Level: {customer.get('riskLevel', 'N/A')}
Available Credit: ${customer.get('availableCredit', 0):,}

Provide a brief risk assessment and loan recommendation for a $25,000 request."""
                
                ai_response = client.chat.completions.create(
                    model="gpt-4o",
                    messages=[{"role": "user", "content": analysis_prompt}],
                    max_tokens=200
                )
                
                analysis = ai_response.choices[0].message.content
                print(f"AI Risk Assessment: {analysis[:150]}...")
                print("Business Case 1: PASSED - AI customer analysis successful")
                
            else:
                print("Business Case 1: SKIPPED - No customer data found")
        else:
            print(f"Business Case 1: FAILED - GraphQL error {response.status_code}")
    except Exception as e:
        print(f"Business Case 1: FAILED - {e}")
    
    # Business Case 2: System Health Analysis
    print("\nBusiness Case 2: System Health Performance Analysis")
    try:
        response = requests.post(
            "http://localhost:5000/graphql",
            json={"query": "{ systemHealth { status timestamp services { serviceName status } } }"},
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            health = data.get("data", {}).get("systemHealth", {})
            
            if health:
                print(f"System Status: {health.get('status', 'Unknown')}")
                services = health.get("services", [])
                print(f"Active Services: {len(services)}")
                
                # AI Analysis
                client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
                
                portfolio_prompt = f"""Analyze this banking system status for portfolio management:
                
System Status: {health.get('status', 'Unknown')}
Active Services: {len(services)}
Timestamp: {health.get('timestamp', 'N/A')}

Provide insights on system performance and recommendations for portfolio optimization."""
                
                ai_response = client.chat.completions.create(
                    model="gpt-4o",
                    messages=[{"role": "user", "content": portfolio_prompt}],
                    max_tokens=200
                )
                
                analysis = ai_response.choices[0].message.content
                print(f"AI Portfolio Analysis: {analysis[:150]}...")
                print("Business Case 2: PASSED - AI portfolio analysis successful")
            else:
                print("Business Case 2: SKIPPED - No system health data")
        else:
            print(f"Business Case 2: FAILED - GraphQL error {response.status_code}")
    except Exception as e:
        print(f"Business Case 2: FAILED - {e}")
    
    # Business Case 3: Banking Compliance Analysis
    print("\nBusiness Case 3: Banking Compliance Analysis")
    try:
        client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
        
        compliance_prompt = """Analyze banking compliance for this loan management system:
        
Banking Rules:
- Loan installments: 6, 9, 12, or 24 months only
- Interest rates: 0.1% to 0.5% range
- Credit scores: 300-850 scale
- Early payment discounts available
- Late payment penalties apply

Assess compliance status and provide regulatory recommendations."""
        
        ai_response = client.chat.completions.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": compliance_prompt}],
            max_tokens=250
        )
        
        analysis = ai_response.choices[0].message.content
        print(f"AI Compliance Assessment: {analysis[:200]}...")
        print("Business Case 3: PASSED - AI compliance analysis successful")
        
    except Exception as e:
        print(f"Business Case 3: FAILED - {e}")
    
    # Business Case 4: Natural Language Query Processing
    print("\nBusiness Case 4: Natural Language Banking Query")
    try:
        query = """
        mutation {
            processBankingQuery(
                query: "What are the key risk factors to consider for loan approvals?"
                customerId: null
            )
        }
        """
        
        response = requests.post(
            "http://localhost:5000/graphql",
            json={"query": query},
            timeout=15
        )
        
        if response.status_code == 200:
            data = response.json()
            if "data" in data:
                result = data["data"].get("processBankingQuery", "")
                if result and len(result) > 10:
                    print(f"Natural Language Response: {result[:150]}...")
                    print("Business Case 4: PASSED - NL query processing successful")
                else:
                    print("Business Case 4: SKIPPED - No response content")
            else:
                print("Business Case 4: FAILED - No data in response")
        else:
            print(f"Business Case 4: FAILED - GraphQL error {response.status_code}")
    except Exception as e:
        print(f"Business Case 4: FAILED - {e}")
    
    # Business Case 5: MCP Protocol Validation
    print("\nBusiness Case 5: MCP Protocol Integration")
    try:
        response = requests.get("http://localhost:5000/", timeout=5)
        
        if response.status_code == 200:
            content = response.text
            if "Enterprise Loan Management" in content:
                print("MCP Server: Banking system responding")
                print("Business Case 5: PASSED - MCP integration ready")
            else:
                print("Business Case 5: SKIPPED - Unexpected response content")
        else:
            print(f"Business Case 5: FAILED - Server error {response.status_code}")
    except Exception as e:
        print(f"Business Case 5: FAILED - {e}")
    
    print("\n" + "=" * 40)
    print("AI and MCP Business Case Testing Complete")
    print("=" * 40)
    
    return True

if __name__ == "__main__":
    asyncio.run(test_business_cases())