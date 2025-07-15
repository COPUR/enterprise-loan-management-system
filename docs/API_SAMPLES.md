# API Implementation Samples

## Overview

This document provides comprehensive code samples for implementing the Enterprise Banking Platform APIs across different programming languages and frameworks. All samples include error handling, authentication, and best practices.

---

## Authentication Setup

### JWT Token Management

#### Node.js Implementation
```javascript
const jwt = require('jsonwebtoken');
const axios = require('axios');

class AmanahFiAuthManager {
    constructor(apiKey, clientId, clientSecret) {
        this.apiKey = apiKey;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = 'https://api.amanahfi.ae';
        this.token = null;
        this.tokenExpiry = null;
    }

    async authenticate() {
        try {
            const response = await axios.post(`${this.baseUrl}/auth/token`, {
                grant_type: 'client_credentials',
                client_id: this.clientId,
                client_secret: this.clientSecret,
                scope: 'banking:read banking:write islamic:read islamic:write'
            }, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-API-Key': this.apiKey
                }
            });

            this.token = response.data.access_token;
            this.tokenExpiry = Date.now() + (response.data.expires_in * 1000);
            
            return this.token;
        } catch (error) {
            throw new Error(`Authentication failed: ${error.response?.data?.error || error.message}`);
        }
    }

    async getValidToken() {
        if (!this.token || Date.now() >= this.tokenExpiry) {
            await this.authenticate();
        }
        return this.token;
    }

    async makeAuthenticatedRequest(method, endpoint, data = null) {
        const token = await this.getValidToken();
        
        const config = {
            method,
            url: `${this.baseUrl}${endpoint}`,
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-API-Key': this.apiKey
            }
        };

        if (data) {
            config.data = data;
        }

        try {
            const response = await axios(config);
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                // Token expired, retry with new token
                await this.authenticate();
                config.headers['Authorization'] = `Bearer ${this.token}`;
                const retryResponse = await axios(config);
                return retryResponse.data;
            }
            throw error;
        }
    }
}
```

#### Python Implementation
```python
import requests
import time
from datetime import datetime, timedelta
from typing import Optional, Dict, Any

class AmanahFiAuthManager:
    def __init__(self, api_key: str, client_id: str, client_secret: str):
        self.api_key = api_key
        self.client_id = client_id
        self.client_secret = client_secret
        self.base_url = 'https://api.amanahfi.ae'
        self.token: Optional[str] = None
        self.token_expiry: Optional[datetime] = None
    
    def authenticate(self) -> str:
        """Authenticate and get access token"""
        try:
            response = requests.post(
                f'{self.base_url}/auth/token',
                data={
                    'grant_type': 'client_credentials',
                    'client_id': self.client_id,
                    'client_secret': self.client_secret,
                    'scope': 'banking:read banking:write islamic:read islamic:write'
                },
                headers={
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-API-Key': self.api_key
                }
            )
            response.raise_for_status()
            
            data = response.json()
            self.token = data['access_token']
            self.token_expiry = datetime.now() + timedelta(seconds=data['expires_in'])
            
            return self.token
        except requests.exceptions.RequestException as e:
            raise Exception(f'Authentication failed: {str(e)}')
    
    def get_valid_token(self) -> str:
        """Get valid token, refreshing if necessary"""
        if not self.token or datetime.now() >= self.token_expiry:
            self.authenticate()
        return self.token
    
    def make_authenticated_request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Dict[Any, Any]:
        """Make authenticated API request"""
        token = self.get_valid_token()
        
        headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json',
            'X-API-Key': self.api_key
        }
        
        try:
            response = requests.request(
                method=method,
                url=f'{self.base_url}{endpoint}',
                headers=headers,
                json=data
            )
            
            if response.status_code == 401:
                # Token expired, retry with new token
                self.authenticate()
                headers['Authorization'] = f'Bearer {self.token}'
                response = requests.request(
                    method=method,
                    url=f'{self.base_url}{endpoint}',
                    headers=headers,
                    json=data
                )
            
            response.raise_for_status()
            return response.json()
            
        except requests.exceptions.RequestException as e:
            raise Exception(f'API request failed: {str(e)}')
```

---

## Traditional Banking Implementation

### Customer Management

#### Node.js Customer Service
```javascript
class CustomerService {
    constructor(authManager) {
        this.auth = authManager;
    }

    async createCustomer(customerData) {
        try {
            // Validate required fields
            this.validateCustomerData(customerData);

            const response = await this.auth.makeAuthenticatedRequest(
                'POST',
                '/api/v1/customers',
                customerData
            );

            console.log(`✅ Customer created successfully: ${response.customerId}`);
            return response;
        } catch (error) {
            console.error('❌ Customer creation failed:', error.message);
            throw error;
        }
    }

    async getCustomerCreditProfile(customerId) {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'GET',
                `/api/v1/customers/${customerId}/credit-profile`
            );

            return response;
        } catch (error) {
            console.error('❌ Failed to fetch credit profile:', error.message);
            throw error;
        }
    }

    async updateCustomerIncome(customerId, newIncome, currency = 'AED') {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'PATCH',
                `/api/v1/customers/${customerId}`,
                {
                    annualIncome: newIncome,
                    currency: currency
                }
            );

            console.log(`✅ Customer income updated: ${customerId}`);
            return response;
        } catch (error) {
            console.error('❌ Failed to update customer income:', error.message);
            throw error;
        }
    }

    validateCustomerData(data) {
        const required = ['firstName', 'lastName', 'email', 'phone', 'annualIncome'];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Missing required fields: ${missing.join(', ')}`);
        }

        if (data.annualIncome <= 0) {
            throw new Error('Annual income must be greater than 0');
        }

        if (!this.isValidEmail(data.email)) {
            throw new Error('Invalid email format');
        }
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
}

// Usage Example
const authManager = new AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret');
const customerService = new CustomerService(authManager);

async function createNewCustomer() {
    try {
        const customer = await customerService.createCustomer({
            firstName: 'Ahmed',
            lastName: 'Al-Mahmoud',
            email: 'ahmed.mahmoud@example.com',
            phone: '+971-50-123-4567',
            annualIncome: 150000.00,
            currency: 'AED',
            nationalId: '784-1234-5678901-2',
            address: {
                street: 'Sheikh Zayed Road',
                city: 'Dubai',
                emirate: 'Dubai',
                postalCode: '12345',
                country: 'UAE'
            }
        });

        console.log('Customer created:', customer);
        
        // Fetch credit profile
        const creditProfile = await customerService.getCustomerCreditProfile(customer.customerId);
        console.log('Credit profile:', creditProfile);
        
    } catch (error) {
        console.error('Error:', error.message);
    }
}
```

#### Python Customer Service
```python
from typing import Dict, Any, Optional
import re

class CustomerService:
    def __init__(self, auth_manager: AmanahFiAuthManager):
        self.auth = auth_manager
    
    def create_customer(self, customer_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new customer"""
        try:
            # Validate required fields
            self._validate_customer_data(customer_data)
            
            response = self.auth.make_authenticated_request(
                'POST',
                '/api/v1/customers',
                customer_data
            )
            
            print(f"✅ Customer created successfully: {response['customerId']}")
            return response
            
        except Exception as error:
            print(f"❌ Customer creation failed: {str(error)}")
            raise error
    
    def get_customer_credit_profile(self, customer_id: str) -> Dict[str, Any]:
        """Get customer credit profile"""
        try:
            response = self.auth.make_authenticated_request(
                'GET',
                f'/api/v1/customers/{customer_id}/credit-profile'
            )
            
            return response
            
        except Exception as error:
            print(f"❌ Failed to fetch credit profile: {str(error)}")
            raise error
    
    def update_customer_income(self, customer_id: str, new_income: float, currency: str = 'AED') -> Dict[str, Any]:
        """Update customer annual income"""
        try:
            response = self.auth.make_authenticated_request(
                'PATCH',
                f'/api/v1/customers/{customer_id}',
                {
                    'annualIncome': new_income,
                    'currency': currency
                }
            )
            
            print(f"✅ Customer income updated: {customer_id}")
            return response
            
        except Exception as error:
            print(f"❌ Failed to update customer income: {str(error)}")
            raise error
    
    def _validate_customer_data(self, data: Dict[str, Any]) -> None:
        """Validate customer data"""
        required_fields = ['firstName', 'lastName', 'email', 'phone', 'annualIncome']
        missing_fields = [field for field in required_fields if field not in data or not data[field]]
        
        if missing_fields:
            raise ValueError(f"Missing required fields: {', '.join(missing_fields)}")
        
        if data['annualIncome'] <= 0:
            raise ValueError('Annual income must be greater than 0')
        
        if not self._is_valid_email(data['email']):
            raise ValueError('Invalid email format')
    
    def _is_valid_email(self, email: str) -> bool:
        """Validate email format"""
        email_pattern = r'^[^\s@]+@[^\s@]+\.[^\s@]+$'
        return re.match(email_pattern, email) is not None

# Usage Example
def main():
    auth_manager = AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret')
    customer_service = CustomerService(auth_manager)
    
    try:
        # Create new customer
        customer = customer_service.create_customer({
            'firstName': 'Fatima',
            'lastName': 'Al-Zahra',
            'email': 'fatima.alzahra@example.com',
            'phone': '+971-55-987-6543',
            'annualIncome': 120000.00,
            'currency': 'AED',
            'nationalId': '784-9876-5432109-8',
            'address': {
                'street': 'Al Wasl Road',
                'city': 'Dubai',
                'emirate': 'Dubai',
                'postalCode': '54321',
                'country': 'UAE'
            }
        })
        
        print('Customer created:', customer)
        
        # Fetch credit profile
        credit_profile = customer_service.get_customer_credit_profile(customer['customerId'])
        print('Credit profile:', credit_profile)
        
    except Exception as error:
        print(f'Error: {str(error)}')
```

### Loan Management

#### Node.js Loan Service
```javascript
class LoanService {
    constructor(authManager) {
        this.auth = authManager;
    }

    async createLoan(loanData) {
        try {
            // Validate loan data
            this.validateLoanData(loanData);

            const response = await this.auth.makeAuthenticatedRequest(
                'POST',
                '/api/v1/loans',
                loanData
            );

            console.log(`✅ Loan application created: ${response.loanId}`);
            return response;
        } catch (error) {
            console.error('❌ Loan creation failed:', error.message);
            throw error;
        }
    }

    async getLoanDetails(loanId) {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'GET',
                `/api/v1/loans/${loanId}`
            );

            return response;
        } catch (error) {
            console.error('❌ Failed to fetch loan details:', error.message);
            throw error;
        }
    }

    async makePayment(loanId, paymentData) {
        try {
            this.validatePaymentData(paymentData);

            const response = await this.auth.makeAuthenticatedRequest(
                'POST',
                `/api/v1/loans/${loanId}/payments`,
                paymentData
            );

            console.log(`✅ Payment processed: ${response.paymentId}`);
            return response;
        } catch (error) {
            console.error('❌ Payment processing failed:', error.message);
            throw error;
        }
    }

    async getPaymentSchedule(loanId) {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'GET',
                `/api/v1/loans/${loanId}/schedule`
            );

            return response;
        } catch (error) {
            console.error('❌ Failed to fetch payment schedule:', error.message);
            throw error;
        }
    }

    validateLoanData(data) {
        const required = ['customerId', 'principalAmount', 'interestRate', 'termMonths'];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Missing required fields: ${missing.join(', ')}`);
        }

        if (data.principalAmount <= 0) {
            throw new Error('Principal amount must be greater than 0');
        }

        if (data.interestRate < 0) {
            throw new Error('Interest rate cannot be negative');
        }

        if (data.termMonths <= 0) {
            throw new Error('Term months must be greater than 0');
        }
    }

    validatePaymentData(data) {
        const required = ['paymentAmount', 'paymentMethod', 'fromAccount'];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Missing required fields: ${missing.join(', ')}`);
        }

        if (data.paymentAmount <= 0) {
            throw new Error('Payment amount must be greater than 0');
        }
    }
}

// Usage Example
async function processLoanWorkflow() {
    const authManager = new AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret');
    const loanService = new LoanService(authManager);

    try {
        // Create loan application
        const loan = await loanService.createLoan({
            customerId: 'CUST-12345678',
            loanType: 'PERSONAL',
            principalAmount: 50000.00,
            currency: 'AED',
            interestRate: 5.25,
            termMonths: 24,
            purpose: 'HOME_IMPROVEMENT',
            collateral: {
                type: 'PROPERTY',
                value: 75000.00,
                description: 'Apartment in Dubai Marina'
            }
        });

        console.log('Loan created:', loan);

        // Get loan details
        const loanDetails = await loanService.getLoanDetails(loan.loanId);
        console.log('Loan details:', loanDetails);

        // Get payment schedule
        const schedule = await loanService.getPaymentSchedule(loan.loanId);
        console.log('Payment schedule:', schedule);

        // Make a payment (if loan is approved and active)
        if (loanDetails.status === 'ACTIVE') {
            const payment = await loanService.makePayment(loan.loanId, {
                paymentAmount: loanDetails.monthlyPayment,
                paymentMethod: 'BANK_TRANSFER',
                fromAccount: 'ACC-11111111',
                paymentDate: new Date().toISOString(),
                description: 'Monthly loan payment'
            });

            console.log('Payment processed:', payment);
        }

    } catch (error) {
        console.error('Error in loan workflow:', error.message);
    }
}
```

---

## Islamic Finance Implementation

### Murabaha Contract Service

#### Node.js Murabaha Service
```javascript
class MurabahaService {
    constructor(authManager) {
        this.auth = authManager;
    }

    async createMurabahaContract(contractData) {
        try {
            // Validate Murabaha contract data
            this.validateMurabahaData(contractData);

            const response = await this.auth.makeAuthenticatedRequest(
                'POST',
                '/api/v1/amanahfi/murabaha',
                contractData,
                {
                    'X-Sharia-Compliance': 'required'
                }
            );

            console.log(`✅ Murabaha contract created: ${response.contractId}`);
            return response;
        } catch (error) {
            console.error('❌ Murabaha contract creation failed:', error.message);
            throw error;
        }
    }

    async getMurabahaDetails(contractId) {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'GET',
                `/api/v1/amanahfi/murabaha/${contractId}`
            );

            return response;
        } catch (error) {
            console.error('❌ Failed to fetch Murabaha details:', error.message);
            throw error;
        }
    }

    async getShariaCompliance(contractId) {
        try {
            const response = await this.auth.makeAuthenticatedRequest(
                'GET',
                `/api/v1/amanahfi/murabaha/${contractId}/sharia-certificate`
            );

            return response;
        } catch (error) {
            console.error('❌ Failed to fetch Sharia compliance:', error.message);
            throw error;
        }
    }

    async makeMurabahaPayment(contractId, paymentData) {
        try {
            this.validatePaymentData(paymentData);

            const response = await this.auth.makeAuthenticatedRequest(
                'POST',
                `/api/v1/amanahfi/murabaha/${contractId}/payments`,
                paymentData,
                {
                    'X-Sharia-Compliance': 'required'
                }
            );

            console.log(`✅ Murabaha payment processed: ${response.paymentId}`);
            return response;
        } catch (error) {
            console.error('❌ Murabaha payment processing failed:', error.message);
            throw error;
        }
    }

    validateMurabahaData(data) {
        const required = ['customerId', 'assetType', 'assetValue', 'profitRate', 'termMonths'];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Missing required fields: ${missing.join(', ')}`);
        }

        if (data.assetValue <= 0) {
            throw new Error('Asset value must be greater than 0');
        }

        if (data.profitRate < 0) {
            throw new Error('Profit rate cannot be negative');
        }

        if (data.termMonths <= 0) {
            throw new Error('Term months must be greater than 0');
        }

        // Validate Sharia compliance requirements
        if (data.shariaCompliance && !data.shariaCompliance.certificateRequired) {
            throw new Error('Sharia compliance certificate is required for Murabaha contracts');
        }
    }

    validatePaymentData(data) {
        const required = ['paymentAmount', 'paymentMethod', 'fromAccount'];
        const missing = required.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Missing required fields: ${missing.join(', ')}`);
        }

        if (data.paymentAmount <= 0) {
            throw new Error('Payment amount must be greater than 0');
        }
    }
}

// Usage Example
async function processMurabahaWorkflow() {
    const authManager = new AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret');
    const murabahaService = new MurabahaService(authManager);

    try {
        // Create Murabaha contract
        const contract = await murabahaService.createMurabahaContract({
            customerId: 'CUST-12345678',
            assetType: 'PROPERTY',
            assetValue: 800000.00,
            currency: 'AED',
            profitRate: 4.5,
            termMonths: 60,
            downPayment: 160000.00,
            assetDescription: '3-bedroom villa in Dubai Hills',
            shariaCompliance: {
                certificateRequired: true,
                boardApproval: true,
                assetOwnership: 'BANK_OWNED'
            }
        });

        console.log('Murabaha contract created:', contract);

        // Get contract details
        const contractDetails = await murabahaService.getMurabahaDetails(contract.contractId);
        console.log('Contract details:', contractDetails);

        // Get Sharia compliance certificate
        const shariaCertificate = await murabahaService.getShariaCompliance(contract.contractId);
        console.log('Sharia compliance:', shariaCertificate);

        // Make payment (if contract is approved)
        if (contractDetails.status === 'ACTIVE') {
            const payment = await murabahaService.makeMurabahaPayment(contract.contractId, {
                paymentAmount: contractDetails.monthlyPayment,
                paymentMethod: 'BANK_TRANSFER',
                fromAccount: 'ACC-11111111',
                paymentDate: new Date().toISOString(),
                description: 'Monthly Murabaha payment'
            });

            console.log('Payment processed:', payment);
        }

    } catch (error) {
        console.error('Error in Murabaha workflow:', error.message);
    }
}
```

### Sukuk Investment Service

#### Python Sukuk Service
```python
from typing import Dict, Any, Optional
from datetime import datetime

class SukukService:
    def __init__(self, auth_manager: AmanahFiAuthManager):
        self.auth = auth_manager
    
    def create_sukuk_investment(self, investment_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new Sukuk investment"""
        try:
            # Validate investment data
            self._validate_sukuk_data(investment_data)
            
            response = self.auth.make_authenticated_request(
                'POST',
                '/api/v1/amanahfi/sukuk',
                investment_data
            )
            
            print(f"✅ Sukuk investment created: {response['sukukId']}")
            return response
            
        except Exception as error:
            print(f"❌ Sukuk investment creation failed: {str(error)}")
            raise error
    
    def get_sukuk_details(self, sukuk_id: str) -> Dict[str, Any]:
        """Get Sukuk investment details"""
        try:
            response = self.auth.make_authenticated_request(
                'GET',
                f'/api/v1/amanahfi/sukuk/{sukuk_id}'
            )
            
            return response
            
        except Exception as error:
            print(f"❌ Failed to fetch Sukuk details: {str(error)}")
            raise error
    
    def get_sukuk_distributions(self, sukuk_id: str) -> Dict[str, Any]:
        """Get Sukuk distribution history"""
        try:
            response = self.auth.make_authenticated_request(
                'GET',
                f'/api/v1/amanahfi/sukuk/{sukuk_id}/distributions'
            )
            
            return response
            
        except Exception as error:
            print(f"❌ Failed to fetch Sukuk distributions: {str(error)}")
            raise error
    
    def redeem_sukuk(self, sukuk_id: str, redemption_data: Dict[str, Any]) -> Dict[str, Any]:
        """Redeem Sukuk investment"""
        try:
            self._validate_redemption_data(redemption_data)
            
            response = self.auth.make_authenticated_request(
                'POST',
                f'/api/v1/amanahfi/sukuk/{sukuk_id}/redeem',
                redemption_data
            )
            
            print(f"✅ Sukuk redemption processed: {response['redemptionId']}")
            return response
            
        except Exception as error:
            print(f"❌ Sukuk redemption failed: {str(error)}")
            raise error
    
    def _validate_sukuk_data(self, data: Dict[str, Any]) -> None:
        """Validate Sukuk investment data"""
        required_fields = ['customerId', 'sukukType', 'investmentAmount', 'expectedReturn', 'maturityPeriod']
        missing_fields = [field for field in required_fields if field not in data or not data[field]]
        
        if missing_fields:
            raise ValueError(f"Missing required fields: {', '.join(missing_fields)}")
        
        if data['investmentAmount'] <= 0:
            raise ValueError('Investment amount must be greater than 0')
        
        if data['expectedReturn'] < 0:
            raise ValueError('Expected return cannot be negative')
        
        if data['maturityPeriod'] <= 0:
            raise ValueError('Maturity period must be greater than 0')
        
        # Validate minimum investment
        if 'minimumInvestment' in data and data['investmentAmount'] < data['minimumInvestment']:
            raise ValueError(f"Investment amount must be at least {data['minimumInvestment']}")
    
    def _validate_redemption_data(self, data: Dict[str, Any]) -> None:
        """Validate redemption data"""
        required_fields = ['redemptionAmount', 'redemptionReason']
        missing_fields = [field for field in required_fields if field not in data or not data[field]]
        
        if missing_fields:
            raise ValueError(f"Missing required fields: {', '.join(missing_fields)}")
        
        if data['redemptionAmount'] <= 0:
            raise ValueError('Redemption amount must be greater than 0')

# Usage Example
def main():
    auth_manager = AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret')
    sukuk_service = SukukService(auth_manager)
    
    try:
        # Create Sukuk investment
        sukuk = sukuk_service.create_sukuk_investment({
            'customerId': 'CUST-12345678',
            'sukukType': 'IJARAH',
            'investmentAmount': 250000.00,
            'currency': 'AED',
            'expectedReturn': 6.5,
            'maturityPeriod': 36,
            'underlyingAsset': 'Commercial real estate portfolio',
            'minimumInvestment': 50000.00,
            'distributionFrequency': 'QUARTERLY'
        })
        
        print('Sukuk investment created:', sukuk)
        
        # Get investment details
        sukuk_details = sukuk_service.get_sukuk_details(sukuk['sukukId'])
        print('Sukuk details:', sukuk_details)
        
        # Get distribution history
        distributions = sukuk_service.get_sukuk_distributions(sukuk['sukukId'])
        print('Distribution history:', distributions)
        
    except Exception as error:
        print(f'Error: {str(error)}')
```

---

## Error Handling and Retry Logic

### Advanced Error Handling

#### Node.js Error Handler
```javascript
class APIErrorHandler {
    constructor(maxRetries = 3, retryDelay = 1000) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    async executeWithRetry(operation, context = '') {
        let lastError;
        
        for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
            try {
                return await operation();
            } catch (error) {
                lastError = error;
                
                if (this.isRetryableError(error) && attempt < this.maxRetries) {
                    console.log(`⚠️ Attempt ${attempt} failed for ${context}. Retrying in ${this.retryDelay}ms...`);
                    await this.sleep(this.retryDelay * attempt); // Exponential backoff
                    continue;
                }
                
                // Log the error details
                this.logError(error, context, attempt);
                throw this.createUserFriendlyError(error);
            }
        }
        
        throw lastError;
    }

    isRetryableError(error) {
        if (error.response) {
            const status = error.response.status;
            // Retry on server errors and rate limiting
            return status >= 500 || status === 429 || status === 408;
        }
        
        // Retry on network errors
        return error.code === 'ECONNRESET' || error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT';
    }

    createUserFriendlyError(error) {
        if (error.response) {
            const status = error.response.status;
            const errorData = error.response.data;
            
            switch (status) {
                case 400:
                    return new Error(`Invalid request: ${errorData.error?.message || 'Please check your input'}`);
                case 401:
                    return new Error('Authentication failed. Please check your credentials.');
                case 403:
                    return new Error('Access denied. You don\'t have permission to perform this action.');
                case 404:
                    return new Error('Resource not found. Please check the ID and try again.');
                case 409:
                    return new Error('Conflict: The resource already exists or is in an invalid state.');
                case 422:
                    return new Error(`Business rule violation: ${errorData.error?.message || 'Operation cannot be completed'}`);
                case 429:
                    return new Error('Rate limit exceeded. Please try again later.');
                case 500:
                    return new Error('Internal server error. Please try again later.');
                case 503:
                    return new Error('Service temporarily unavailable. Please try again later.');
                default:
                    return new Error(`API error (${status}): ${errorData.error?.message || 'Unknown error'}`);
            }
        }
        
        return new Error(`Network error: ${error.message}`);
    }

    logError(error, context, attempt) {
        const errorInfo = {
            timestamp: new Date().toISOString(),
            context,
            attempt,
            error: {
                message: error.message,
                status: error.response?.status,
                code: error.code,
                response: error.response?.data
            }
        };
        
        console.error('🚨 API Error:', JSON.stringify(errorInfo, null, 2));
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Usage Example
const errorHandler = new APIErrorHandler(3, 1000);

async function robustLoanCreation(loanData) {
    const authManager = new AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret');
    const loanService = new LoanService(authManager);
    
    try {
        const loan = await errorHandler.executeWithRetry(
            () => loanService.createLoan(loanData),
            'loan creation'
        );
        
        console.log('✅ Loan created successfully:', loan);
        return loan;
    } catch (error) {
        console.error('❌ Final error after retries:', error.message);
        throw error;
    }
}
```

---

## WebSocket Real-time Updates

### Real-time Event Streaming

#### Node.js WebSocket Client
```javascript
const WebSocket = require('ws');

class AmanahFiWebSocketClient {
    constructor(authManager) {
        this.auth = authManager;
        this.ws = null;
        this.isConnected = false;
        this.eventHandlers = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }

    async connect() {
        try {
            const token = await this.auth.getValidToken();
            const wsUrl = `wss://api.amanahfi.ae/ws?token=${token}`;
            
            this.ws = new WebSocket(wsUrl);
            
            this.ws.on('open', () => {
                console.log('✅ WebSocket connected');
                this.isConnected = true;
                this.reconnectAttempts = 0;
            });
            
            this.ws.on('message', (data) => {
                try {
                    const event = JSON.parse(data);
                    this.handleEvent(event);
                } catch (error) {
                    console.error('❌ Error parsing WebSocket message:', error);
                }
            });
            
            this.ws.on('close', (code, reason) => {
                console.log(`🔌 WebSocket disconnected: ${code} - ${reason}`);
                this.isConnected = false;
                this.attemptReconnect();
            });
            
            this.ws.on('error', (error) => {
                console.error('❌ WebSocket error:', error);
            });
            
        } catch (error) {
            console.error('❌ WebSocket connection failed:', error);
            throw error;
        }
    }

    handleEvent(event) {
        const handler = this.eventHandlers.get(event.eventType);
        if (handler) {
            try {
                handler(event);
            } catch (error) {
                console.error(`❌ Error handling event ${event.eventType}:`, error);
            }
        } else {
            console.log(`📢 Unhandled event: ${event.eventType}`, event);
        }
    }

    subscribe(eventType, handler) {
        this.eventHandlers.set(eventType, handler);
        
        if (this.isConnected) {
            this.ws.send(JSON.stringify({
                action: 'subscribe',
                eventType: eventType
            }));
        }
    }

    unsubscribe(eventType) {
        this.eventHandlers.delete(eventType);
        
        if (this.isConnected) {
            this.ws.send(JSON.stringify({
                action: 'unsubscribe',
                eventType: eventType
            }));
        }
    }

    async attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = Math.pow(2, this.reconnectAttempts) * 1000; // Exponential backoff
            
            console.log(`🔄 Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
            
            setTimeout(() => {
                this.connect();
            }, delay);
        } else {
            console.error('❌ Max reconnection attempts reached');
        }
    }

    disconnect() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
            this.isConnected = false;
        }
    }
}

// Usage Example
async function setupRealTimeMonitoring() {
    const authManager = new AmanahFiAuthManager('your-api-key', 'client-id', 'client-secret');
    const wsClient = new AmanahFiWebSocketClient(authManager);
    
    // Set up event handlers
    wsClient.subscribe('loan.approved', (event) => {
        console.log('🎉 Loan approved:', event.data);
        // Update UI, send notification, etc.
    });
    
    wsClient.subscribe('loan.payment.completed', (event) => {
        console.log('💰 Payment completed:', event.data);
        // Update loan balance, send receipt, etc.
    });
    
    wsClient.subscribe('murabaha.sharia.approved', (event) => {
        console.log('✅ Murabaha Sharia approved:', event.data);
        // Proceed with contract execution
    });
    
    wsClient.subscribe('sukuk.distribution.paid', (event) => {
        console.log('📈 Sukuk distribution paid:', event.data);
        // Update investment portfolio
    });
    
    // Connect to WebSocket
    await wsClient.connect();
    
    // Keep the connection alive
    process.on('SIGINT', () => {
        console.log('🔌 Disconnecting WebSocket...');
        wsClient.disconnect();
        process.exit(0);
    });
}
```

---

## Testing and Validation

### Unit Testing with Jest

#### Node.js Test Suite
```javascript
const { describe, it, expect, beforeEach, jest } = require('@jest/globals');
const axios = require('axios');
const AmanahFiAuthManager = require('./AmanahFiAuthManager');
const CustomerService = require('./CustomerService');

// Mock axios
jest.mock('axios');
const mockedAxios = axios;

describe('CustomerService', () => {
    let authManager;
    let customerService;
    
    beforeEach(() => {
        jest.clearAllMocks();
        authManager = new AmanahFiAuthManager('test-api-key', 'test-client-id', 'test-secret');
        customerService = new CustomerService(authManager);
    });
    
    describe('createCustomer', () => {
        it('should create customer successfully', async () => {
            // Arrange
            const customerData = {
                firstName: 'Ahmed',
                lastName: 'Al-Mahmoud',
                email: 'ahmed@example.com',
                phone: '+971-50-123-4567',
                annualIncome: 120000.00,
                currency: 'AED'
            };
            
            const expectedResponse = {
                customerId: 'CUST-12345678',
                status: 'ACTIVE',
                creditScore: 750,
                createdAt: '2024-01-15T10:30:00Z'
            };
            
            mockedAxios.post.mockResolvedValueOnce({
                data: { access_token: 'test-token', expires_in: 3600 }
            });
            
            mockedAxios.mockResolvedValueOnce({
                data: expectedResponse
            });
            
            // Act
            const result = await customerService.createCustomer(customerData);
            
            // Assert
            expect(result).toEqual(expectedResponse);
            expect(mockedAxios).toHaveBeenCalledWith({
                method: 'POST',
                url: 'https://api.amanahfi.ae/api/v1/customers',
                headers: {
                    'Authorization': 'Bearer test-token',
                    'Content-Type': 'application/json',
                    'X-API-Key': 'test-api-key'
                },
                data: customerData
            });
        });
        
        it('should throw error for invalid email', async () => {
            // Arrange
            const customerData = {
                firstName: 'Ahmed',
                lastName: 'Al-Mahmoud',
                email: 'invalid-email',
                phone: '+971-50-123-4567',
                annualIncome: 120000.00,
                currency: 'AED'
            };
            
            // Act & Assert
            await expect(customerService.createCustomer(customerData))
                .rejects
                .toThrow('Invalid email format');
        });
        
        it('should throw error for negative income', async () => {
            // Arrange
            const customerData = {
                firstName: 'Ahmed',
                lastName: 'Al-Mahmoud',
                email: 'ahmed@example.com',
                phone: '+971-50-123-4567',
                annualIncome: -1000.00,
                currency: 'AED'
            };
            
            // Act & Assert
            await expect(customerService.createCustomer(customerData))
                .rejects
                .toThrow('Annual income must be greater than 0');
        });
    });
});
```

### Integration Testing

#### Python Integration Tests
```python
import unittest
from unittest.mock import Mock, patch
import requests
from your_module import AmanahFiAuthManager, MurabahaService

class TestMurabahaService(unittest.TestCase):
    def setUp(self):
        self.auth_manager = AmanahFiAuthManager('test-api-key', 'test-client-id', 'test-secret')
        self.murabaha_service = MurabahaService(self.auth_manager)
    
    @patch('requests.post')
    @patch('requests.request')
    def test_create_murabaha_contract_success(self, mock_request, mock_post):
        # Arrange
        mock_post.return_value.json.return_value = {
            'access_token': 'test-token',
            'expires_in': 3600
        }
        
        mock_request.return_value.json.return_value = {
            'contractId': 'MUR-12345678',
            'status': 'SHARIA_REVIEW',
            'customerId': 'CUST-12345678',
            'assetValue': 1000000.00,
            'profitRate': 4.5,
            'termMonths': 48
        }
        
        contract_data = {
            'customerId': 'CUST-12345678',
            'assetType': 'PROPERTY',
            'assetValue': 1000000.00,
            'currency': 'AED',
            'profitRate': 4.5,
            'termMonths': 48,
            'downPayment': 200000.00,
            'assetDescription': 'Villa in Dubai Hills',
            'shariaCompliance': {
                'certificateRequired': True,
                'boardApproval': True,
                'assetOwnership': 'BANK_OWNED'
            }
        }
        
        # Act
        result = self.murabaha_service.create_murabaha_contract(contract_data)
        
        # Assert
        self.assertEqual(result['contractId'], 'MUR-12345678')
        self.assertEqual(result['status'], 'SHARIA_REVIEW')
        mock_request.assert_called_once()
    
    def test_validate_murabaha_data_missing_fields(self):
        # Arrange
        invalid_data = {
            'customerId': 'CUST-12345678',
            'assetType': 'PROPERTY'
            # Missing required fields
        }
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.murabaha_service._validate_murabaha_data(invalid_data)
        
        self.assertIn('Missing required fields', str(context.exception))
    
    def test_validate_murabaha_data_invalid_values(self):
        # Arrange
        invalid_data = {
            'customerId': 'CUST-12345678',
            'assetType': 'PROPERTY',
            'assetValue': -1000.00,  # Invalid negative value
            'profitRate': 4.5,
            'termMonths': 48
        }
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.murabaha_service._validate_murabaha_data(invalid_data)
        
        self.assertIn('Asset value must be greater than 0', str(context.exception))

if __name__ == '__main__':
    unittest.main()
```

---

## Performance Optimization

### Caching Strategy

#### Redis Caching Implementation
```javascript
const redis = require('redis');

class CacheManager {
    constructor() {
        this.client = redis.createClient({
            host: 'redis-server',
            port: 6379,
            retry_strategy: (times) => {
                const delay = Math.min(times * 50, 2000);
                return delay;
            }
        });
        
        this.client.on('error', (error) => {
            console.error('Redis error:', error);
        });
    }

    async get(key) {
        try {
            const result = await this.client.get(key);
            return result ? JSON.parse(result) : null;
        } catch (error) {
            console.error('Cache get error:', error);
            return null;
        }
    }

    async set(key, value, ttl = 3600) {
        try {
            await this.client.setex(key, ttl, JSON.stringify(value));
        } catch (error) {
            console.error('Cache set error:', error);
        }
    }

    async del(key) {
        try {
            await this.client.del(key);
        } catch (error) {
            console.error('Cache delete error:', error);
        }
    }

    generateKey(prefix, ...parts) {
        return `${prefix}:${parts.join(':')}`;
    }
}

// Enhanced CustomerService with caching
class CachedCustomerService extends CustomerService {
    constructor(authManager) {
        super(authManager);
        this.cache = new CacheManager();
    }

    async getCustomerCreditProfile(customerId) {
        const cacheKey = this.cache.generateKey('credit_profile', customerId);
        
        // Try cache first
        let profile = await this.cache.get(cacheKey);
        if (profile) {
            console.log('✅ Credit profile retrieved from cache');
            return profile;
        }

        // Fetch from API
        profile = await super.getCustomerCreditProfile(customerId);
        
        // Cache for 5 minutes
        await this.cache.set(cacheKey, profile, 300);
        
        return profile;
    }

    async updateCustomerIncome(customerId, newIncome, currency = 'AED') {
        const result = await super.updateCustomerIncome(customerId, newIncome, currency);
        
        // Invalidate cache after update
        const cacheKey = this.cache.generateKey('credit_profile', customerId);
        await this.cache.del(cacheKey);
        
        return result;
    }
}
```

---

*This comprehensive API implementation guide covers authentication, CRUD operations, error handling, real-time updates, testing, and performance optimization for both Traditional Banking and Islamic Finance platforms.*

*🤖 Generated with [Claude Code](https://claude.ai/code) - Last Updated: January 2024*