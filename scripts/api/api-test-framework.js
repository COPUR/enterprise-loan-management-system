#!/usr/bin/env node

/**
 * Enterprise Banking API Test Framework
 * 
 * Comprehensive testing framework for API validation including:
 * - Contract testing
 * - Performance testing
 * - Security testing
 * - Load testing
 * - Smoke testing
 * - Integration testing
 */

const newman = require('newman');
const fs = require('fs');
const path = require('path');
const axios = require('axios');
const chalk = require('chalk');
const commander = require('commander');
const { performance } = require('perf_hooks');

class APITestFramework {
    constructor() {
        this.program = new commander.Command();
        this.results = {
            total: 0,
            passed: 0,
            failed: 0,
            skipped: 0,
            errors: [],
            performance: {
                averageResponseTime: 0,
                minResponseTime: 0,
                maxResponseTime: 0,
                totalDuration: 0
            }
        };
        
        this.setupCommands();
    }
    
    setupCommands() {
        this.program
            .name('api-test-framework')
            .description('Enterprise Banking API Test Framework')
            .version('1.0.0');
        
        this.program
            .command('run')
            .description('Run API tests')
            .option('-e, --environment <env>', 'Environment to test against', 'development')
            .option('-c, --collection <path>', 'Postman collection file path')
            .option('-n, --iterations <number>', 'Number of iterations', '1')
            .option('-p, --parallel', 'Run tests in parallel')
            .option('-v, --verbose', 'Verbose output')
            .option('--load-test', 'Run load tests')
            .option('--smoke-test', 'Run smoke tests')
            .option('--contract-test', 'Run contract tests')
            .option('--security-test', 'Run security tests')
            .option('--performance-test', 'Run performance tests')
            .action((options) => this.runTests(options));
        
        this.program
            .command('validate')
            .description('Validate API endpoints')
            .option('-e, --environment <env>', 'Environment to validate', 'development')
            .option('-o, --output <path>', 'Output file for results')
            .action((options) => this.validateAPI(options));
        
        this.program
            .command('monitor')
            .description('Monitor API health')
            .option('-e, --environment <env>', 'Environment to monitor', 'development')
            .option('-i, --interval <seconds>', 'Monitoring interval', '60')
            .option('-d, --duration <minutes>', 'Monitoring duration', '10')
            .action((options) => this.monitorAPI(options));
        
        this.program
            .command('report')
            .description('Generate comprehensive test report')
            .option('-r, --results <path>', 'Test results file path')
            .option('-o, --output <path>', 'Output directory for report')
            .option('-f, --format <format>', 'Report format (html|json|pdf)', 'html')
            .action((options) => this.generateReport(options));
    }
    
    async runTests(options) {
        console.log(chalk.blue('🚀 Starting Enterprise Banking API Tests'));
        console.log(chalk.gray(`Environment: ${options.environment}`));
        console.log(chalk.gray(`Iterations: ${options.iterations}`));
        
        const startTime = performance.now();
        
        try {
            // Determine test type
            if (options.loadTest) {
                await this.runLoadTests(options);
            } else if (options.smokeTest) {
                await this.runSmokeTests(options);
            } else if (options.contractTest) {
                await this.runContractTests(options);
            } else if (options.securityTest) {
                await this.runSecurityTests(options);
            } else if (options.performanceTest) {
                await this.runPerformanceTests(options);
            } else {
                await this.runStandardTests(options);
            }
            
            const endTime = performance.now();
            this.results.performance.totalDuration = endTime - startTime;
            
            this.printResults();
            
        } catch (error) {
            console.error(chalk.red('❌ Test execution failed:'), error.message);
            process.exit(1);
        }
    }
    
    async runStandardTests(options) {
        const collectionPath = options.collection || path.join(__dirname, '../../postman/Enterprise-Banking-API-v1.postman_collection.json');
        const environmentPath = path.join(__dirname, '../../postman/Enterprise-Banking-Environment.postman_environment.json');
        
        return new Promise((resolve, reject) => {
            newman.run({
                collection: collectionPath,
                environment: environmentPath,
                iterationCount: parseInt(options.iterations),
                delayRequest: 100,
                timeoutRequest: 30000,
                timeoutScript: 10000,
                reporters: ['cli', 'json'],
                reporter: {
                    json: {
                        export: path.join(__dirname, '../../test-reports/api/newman-results.json')
                    }
                }
            }, (err, summary) => {
                if (err) {
                    reject(err);
                } else {
                    this.processNewmanResults(summary);
                    resolve(summary);
                }
            });
        });
    }
    
    async runLoadTests(options) {
        console.log(chalk.yellow('🔥 Running Load Tests'));
        
        const testConfig = {
            ...options,
            iterations: 100,
            parallel: true,
            concurrency: 10
        };
        
        const promises = [];
        for (let i = 0; i < testConfig.concurrency; i++) {
            promises.push(this.runConcurrentTest(testConfig, i));
        }
        
        const results = await Promise.allSettled(promises);
        this.processLoadTestResults(results);
    }
    
    async runConcurrentTest(config, threadId) {
        const startTime = performance.now();
        
        try {
            // Simulate concurrent API calls
            const baseUrl = this.getBaseUrl(config.environment);
            const endpoints = [
                '/actuator/health',
                '/api/v1/customers',
                '/api/v1/loans',
                '/api/v1/payments'
            ];
            
            const results = [];
            for (let i = 0; i < 10; i++) {
                const endpoint = endpoints[i % endpoints.length];
                const response = await axios.get(`${baseUrl}${endpoint}`, {
                    timeout: 5000,
                    headers: {
                        'X-Request-ID': `load-test-${threadId}-${i}`,
                        'Accept': 'application/json'
                    }
                });
                
                results.push({
                    endpoint,
                    status: response.status,
                    responseTime: response.headers['x-response-time'] || 0,
                    success: response.status < 400
                });
            }
            
            const endTime = performance.now();
            return {
                threadId,
                duration: endTime - startTime,
                results,
                success: true
            };
            
        } catch (error) {
            const endTime = performance.now();
            return {
                threadId,
                duration: endTime - startTime,
                error: error.message,
                success: false
            };
        }
    }
    
    async runSmokeTests(options) {
        console.log(chalk.green('💨 Running Smoke Tests'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        const smokeTests = [
            { name: 'Health Check', endpoint: '/actuator/health', method: 'GET' },
            { name: 'API Version', endpoint: '/api/v1', method: 'GET' },
            { name: 'OpenAPI Docs', endpoint: '/v3/api-docs', method: 'GET' },
            { name: 'Metrics', endpoint: '/actuator/metrics', method: 'GET' }
        ];
        
        for (const test of smokeTests) {
            try {
                const startTime = performance.now();
                const response = await axios({
                    method: test.method,
                    url: `${baseUrl}${test.endpoint}`,
                    timeout: 5000,
                    headers: {
                        'X-Request-ID': `smoke-test-${Date.now()}`,
                        'Accept': 'application/json'
                    }
                });
                
                const endTime = performance.now();
                const responseTime = endTime - startTime;
                
                this.results.total++;
                if (response.status < 400) {
                    this.results.passed++;
                    console.log(chalk.green(`✅ ${test.name}: ${response.status} (${responseTime.toFixed(2)}ms)`));
                } else {
                    this.results.failed++;
                    console.log(chalk.red(`❌ ${test.name}: ${response.status} (${responseTime.toFixed(2)}ms)`));
                }
                
            } catch (error) {
                this.results.total++;
                this.results.failed++;
                this.results.errors.push({
                    test: test.name,
                    error: error.message
                });
                console.log(chalk.red(`❌ ${test.name}: ${error.message}`));
            }
        }
    }
    
    async runContractTests(options) {
        console.log(chalk.blue('📋 Running Contract Tests'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        
        try {
            // Fetch OpenAPI specification
            const response = await axios.get(`${baseUrl}/v3/api-docs`);
            const openApiSpec = response.data;
            
            // Validate OpenAPI spec structure
            this.validateOpenAPISpec(openApiSpec);
            
            // Test each endpoint in the spec
            await this.testOpenAPIEndpoints(baseUrl, openApiSpec);
            
        } catch (error) {
            console.error(chalk.red('Contract test failed:'), error.message);
            this.results.errors.push({
                test: 'Contract Test',
                error: error.message
            });
        }
    }
    
    async runSecurityTests(options) {
        console.log(chalk.red('🔒 Running Security Tests'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        const securityTests = [
            { name: 'HTTPS Enforcement', test: () => this.testHTTPS(baseUrl) },
            { name: 'Security Headers', test: () => this.testSecurityHeaders(baseUrl) },
            { name: 'Rate Limiting', test: () => this.testRateLimiting(baseUrl) },
            { name: 'Authentication', test: () => this.testAuthentication(baseUrl) },
            { name: 'CORS Policy', test: () => this.testCORS(baseUrl) }
        ];
        
        for (const test of securityTests) {
            try {
                await test.test();
                this.results.total++;
                this.results.passed++;
                console.log(chalk.green(`✅ ${test.name}`));
            } catch (error) {
                this.results.total++;
                this.results.failed++;
                this.results.errors.push({
                    test: test.name,
                    error: error.message
                });
                console.log(chalk.red(`❌ ${test.name}: ${error.message}`));
            }
        }
    }
    
    async runPerformanceTests(options) {
        console.log(chalk.yellow('⚡ Running Performance Tests'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        const performanceTests = [
            { name: 'Response Time', threshold: 200, test: () => this.testResponseTime(baseUrl) },
            { name: 'Throughput', threshold: 100, test: () => this.testThroughput(baseUrl) },
            { name: 'Memory Usage', threshold: 85, test: () => this.testMemoryUsage(baseUrl) },
            { name: 'Database Performance', threshold: 100, test: () => this.testDatabasePerformance(baseUrl) }
        ];
        
        for (const test of performanceTests) {
            try {
                const result = await test.test();
                this.results.total++;
                
                if (result.value <= test.threshold) {
                    this.results.passed++;
                    console.log(chalk.green(`✅ ${test.name}: ${result.value}${result.unit} (threshold: ${test.threshold}${result.unit})`));
                } else {
                    this.results.failed++;
                    console.log(chalk.red(`❌ ${test.name}: ${result.value}${result.unit} (threshold: ${test.threshold}${result.unit})`));
                }
                
            } catch (error) {
                this.results.total++;
                this.results.failed++;
                this.results.errors.push({
                    test: test.name,
                    error: error.message
                });
                console.log(chalk.red(`❌ ${test.name}: ${error.message}`));
            }
        }
    }
    
    async validateAPI(options) {
        console.log(chalk.blue('🔍 Validating API Endpoints'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        const validationResults = [];
        
        try {
            // Fetch OpenAPI specification
            const response = await axios.get(`${baseUrl}/v3/api-docs`);
            const openApiSpec = response.data;
            
            // Validate each endpoint
            for (const [path, methods] of Object.entries(openApiSpec.paths)) {
                for (const [method, spec] of Object.entries(methods)) {
                    const validation = await this.validateEndpoint(baseUrl, path, method, spec);
                    validationResults.push(validation);
                }
            }
            
            // Generate validation report
            if (options.output) {
                fs.writeFileSync(options.output, JSON.stringify(validationResults, null, 2));
                console.log(chalk.green(`✅ Validation report saved to: ${options.output}`));
            }
            
        } catch (error) {
            console.error(chalk.red('API validation failed:'), error.message);
        }
    }
    
    async monitorAPI(options) {
        console.log(chalk.blue('👁️ Monitoring API Health'));
        
        const baseUrl = this.getBaseUrl(options.environment);
        const interval = parseInt(options.interval) * 1000;
        const duration = parseInt(options.duration) * 60 * 1000;
        const startTime = Date.now();
        
        const monitoringData = [];
        
        while (Date.now() - startTime < duration) {
            const timestamp = new Date().toISOString();
            
            try {
                const healthCheck = await this.checkHealth(baseUrl);
                monitoringData.push({
                    timestamp,
                    ...healthCheck
                });
                
                console.log(chalk.green(`${timestamp}: API Health - ${healthCheck.status}`));
                
            } catch (error) {
                monitoringData.push({
                    timestamp,
                    status: 'DOWN',
                    error: error.message
                });
                
                console.log(chalk.red(`${timestamp}: API Health - DOWN (${error.message})`));
            }
            
            await new Promise(resolve => setTimeout(resolve, interval));
        }
        
        // Generate monitoring report
        const reportPath = path.join(__dirname, '../../test-reports/api/monitoring-report.json');
        fs.writeFileSync(reportPath, JSON.stringify(monitoringData, null, 2));
        console.log(chalk.green(`📊 Monitoring report saved to: ${reportPath}`));
    }
    
    async generateReport(options) {
        console.log(chalk.blue('📄 Generating Test Report'));
        
        const resultsPath = options.results || path.join(__dirname, '../../test-reports/api/newman-results.json');
        const outputDir = options.output || path.join(__dirname, '../../test-reports/api');
        
        if (!fs.existsSync(resultsPath)) {
            console.error(chalk.red('Results file not found:'), resultsPath);
            return;
        }
        
        const results = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
        
        if (options.format === 'html') {
            await this.generateHTMLReport(results, outputDir);
        } else if (options.format === 'json') {
            await this.generateJSONReport(results, outputDir);
        } else if (options.format === 'pdf') {
            await this.generatePDFReport(results, outputDir);
        }
        
        console.log(chalk.green(`✅ Report generated in: ${outputDir}`));
    }
    
    // Helper methods
    
    getBaseUrl(environment) {
        const urls = {
            development: 'http://localhost:8080',
            staging: 'https://api-staging.banking.example.com',
            production: 'https://api.banking.example.com'
        };
        
        return urls[environment] || urls.development;
    }
    
    processNewmanResults(summary) {
        this.results.total = summary.run.stats.tests.total;
        this.results.passed = summary.run.stats.tests.passed;
        this.results.failed = summary.run.stats.tests.failed;
        this.results.skipped = summary.run.stats.tests.skipped;
        
        if (summary.run.timings) {
            this.results.performance.averageResponseTime = summary.run.timings.responseAverage;
            this.results.performance.minResponseTime = summary.run.timings.responseMin;
            this.results.performance.maxResponseTime = summary.run.timings.responseMax;
        }
        
        if (summary.run.failures) {
            this.results.errors = summary.run.failures.map(failure => ({
                test: failure.source.name,
                error: failure.error.message
            }));
        }
    }
    
    printResults() {
        console.log(chalk.blue('\n📊 Test Results Summary'));
        console.log(chalk.gray('═'.repeat(50)));
        
        console.log(`${chalk.green('✅ Passed:')} ${this.results.passed}`);
        console.log(`${chalk.red('❌ Failed:')} ${this.results.failed}`);
        console.log(`${chalk.yellow('⏭️ Skipped:')} ${this.results.skipped}`);
        console.log(`${chalk.blue('📈 Total:')} ${this.results.total}`);
        
        if (this.results.performance.averageResponseTime) {
            console.log(`${chalk.cyan('⏱️ Avg Response Time:')} ${this.results.performance.averageResponseTime}ms`);
        }
        
        if (this.results.errors.length > 0) {
            console.log(chalk.red('\n🚨 Errors:'));
            this.results.errors.forEach(error => {
                console.log(`  ${chalk.red('•')} ${error.test}: ${error.error}`);
            });
        }
        
        const successRate = (this.results.passed / this.results.total * 100).toFixed(1);
        console.log(`${chalk.blue('\n🎯 Success Rate:')} ${successRate}%`);
        
        if (this.results.failed > 0) {
            process.exit(1);
        }
    }
    
    run() {
        this.program.parse(process.argv);
    }
}

// Run the framework
if (require.main === module) {
    const framework = new APITestFramework();
    framework.run();
}

module.exports = APITestFramework;