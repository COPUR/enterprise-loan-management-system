# Circuit Breaker Pattern Implementation
## Resilience4j Integration for High Availability

### Overview

The Enterprise Loan Management System implements Circuit Breaker patterns using Resilience4j to provide fault tolerance, prevent cascade failures, and maintain system stability during high-load scenarios or service degradation.

---

## Circuit Breaker Architecture

### Core Configuration

#### Application Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      customer-service:
        registerHealthIndicator: true
        slidingWindowSize: 100
        minimumNumberOfCalls: 20
        permittedNumberOfCallsInHalfOpenState: 10
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - com.bank.loanmanagement.exception.BusinessException
      
      loan-service:
        registerHealthIndicator: true
        slidingWindowSize: 100
        minimumNumberOfCalls: 15
        permittedNumberOfCallsInHalfOpenState: 8
        waitDurationInOpenState: 25s
        failureRateThreshold: 45
        eventConsumerBufferSize: 10
      
      payment-service:
        registerHealthIndicator: true
        slidingWindowSize: 80
        minimumNumberOfCalls: 12
        permittedNumberOfCallsInHalfOpenState: 6
        waitDurationInOpenState: 20s
        failureRateThreshold: 40
        eventConsumerBufferSize: 10
  
  retry:
    instances:
      customer-service:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
      
      loan-service:
        maxAttempts: 3
        waitDuration: 750ms
        exponentialBackoffMultiplier: 1.5
      
      payment-service:
        maxAttempts: 2
        waitDuration: 400ms
        exponentialBackoffMultiplier: 2
  
  timelimiter:
    instances:
      customer-service:
        timeoutDuration: 3s
        cancelRunningFuture: true
      loan-service:
        timeoutDuration: 5s
        cancelRunningFuture: true
      payment-service:
        timeoutDuration: 4s
        cancelRunningFuture: true
```

### Implementation Classes

#### 1. Circuit Breaker Service Client
```java
@Component
@Slf4j
public class ResilientServiceClient {
    
    private final WebClient webClient;
    private final CircuitBreaker customerCircuitBreaker;
    private final CircuitBreaker loanCircuitBreaker;
    private final CircuitBreaker paymentCircuitBreaker;
    private final MeterRegistry meterRegistry;
    
    public ResilientServiceClient(WebClient.Builder webClientBuilder,
                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                 MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.build();
        this.customerCircuitBreaker = circuitBreakerRegistry.circuitBreaker("customer-service");
        this.loanCircuitBreaker = circuitBreakerRegistry.circuitBreaker("loan-service");
        this.paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-service");
        this.meterRegistry = meterRegistry;
        
        registerEventListeners();
    }
    
    @CircuitBreaker(name = "customer-service", fallbackMethod = "fallbackCustomerResponse")
    @Retry(name = "customer-service")
    @TimeLimiter(name = "customer-service")
    public CompletableFuture<CustomerResponse> getCustomer(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling customer service for customer: {}", customerId);
            
            return webClient.get()
                    .uri("http://localhost:8081/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .bodyToMono(CustomerResponse.class)
                    .doOnNext(response -> recordSuccessMetric("customer-service"))
                    .doOnError(error -> recordErrorMetric("customer-service", error))
                    .block();
        });
    }
    
    @CircuitBreaker(name = "loan-service", fallbackMethod = "fallbackLoanResponse")
    @Retry(name = "loan-service")
    @TimeLimiter(name = "loan-service")
    public CompletableFuture<LoanResponse> createLoan(LoanCreationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling loan service for loan creation");
            
            return webClient.post()
                    .uri("http://localhost:8082/api/v1/loans")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LoanResponse.class)
                    .doOnNext(response -> recordSuccessMetric("loan-service"))
                    .doOnError(error -> recordErrorMetric("loan-service", error))
                    .block();
        });
    }
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPaymentResponse")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling payment service for payment processing");
            
            return webClient.post()
                    .uri("http://localhost:8083/api/v1/payments")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .doOnNext(response -> recordSuccessMetric("payment-service"))
                    .doOnError(error -> recordErrorMetric("payment-service", error))
                    .block();
        });
    }
}
```

#### 2. Fallback Mechanisms
```java
// Customer Service Fallback
public CompletableFuture<CustomerResponse> fallbackCustomerResponse(String customerId, Exception ex) {
    log.warn("Customer service fallback triggered for customer: {}, reason: {}", 
             customerId, ex.getMessage());
    
    // Try to get cached customer data
    CustomerResponse cachedCustomer = customerCacheService.getCachedCustomer(customerId);
    if (cachedCustomer != null) {
        log.info("Returning cached customer data for: {}", customerId);
        return CompletableFuture.completedFuture(cachedCustomer);
    }
    
    // Return degraded service response
    CustomerResponse fallbackResponse = CustomerResponse.builder()
            .customerId(customerId)
            .status(CustomerStatus.UNKNOWN)
            .serviceDegraded(true)
            .message("Customer service temporarily unavailable")
            .build();
    
    return CompletableFuture.completedFuture(fallbackResponse);
}

// Loan Service Fallback
public CompletableFuture<LoanResponse> fallbackLoanResponse(LoanCreationRequest request, Exception ex) {
    log.warn("Loan service fallback triggered, reason: {}", ex.getMessage());
    
    // Queue request for later processing
    loanRequestQueue.queueRequest(request);
    
    LoanResponse fallbackResponse = LoanResponse.builder()
            .status(LoanStatus.QUEUED)
            .message("Loan request queued for processing when service is available")
            .estimatedProcessingTime(Duration.ofMinutes(15))
            .serviceDegraded(true)
            .build();
    
    return CompletableFuture.completedFuture(fallbackResponse);
}

// Payment Service Fallback
public CompletableFuture<PaymentResponse> fallbackPaymentResponse(PaymentRequest request, Exception ex) {
    log.warn("Payment service fallback triggered, reason: {}", ex.getMessage());
    
    // For critical payment operations, fail fast
    if (request.isCritical()) {
        PaymentResponse failureResponse = PaymentResponse.builder()
                .status(PaymentStatus.FAILED)
                .errorCode("SERVICE_UNAVAILABLE")
                .message("Payment service temporarily unavailable for critical operations")
                .retryAfter(Duration.ofMinutes(5))
                .build();
        
        return CompletableFuture.completedFuture(failureResponse);
    }
    
    // For non-critical payments, queue for later processing
    paymentQueue.queuePayment(request);
    
    PaymentResponse queuedResponse = PaymentResponse.builder()
            .status(PaymentStatus.QUEUED)
            .message("Payment queued for processing")
            .estimatedProcessingTime(Duration.ofMinutes(10))
            .build();
    
    return CompletableFuture.completedFuture(queuedResponse);
}
```

---

## State Management and Monitoring

### Circuit Breaker States

#### 1. CLOSED State (Normal Operation)
- All requests pass through to the service
- Failure rate is monitored continuously
- Success/failure metrics are recorded

#### 2. OPEN State (Circuit Tripped)
- All requests immediately return fallback response
- No calls made to the failing service
- Wait duration timer is started

#### 3. HALF-OPEN State (Testing Waters)
- Limited number of test calls allowed
- If calls succeed, circuit returns to CLOSED
- If calls fail, circuit returns to OPEN

### State Transition Logic
```java
@Component
public class CircuitBreakerStateManager {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ApplicationEventPublisher eventPublisher;
    
    @EventListener
    public void handleCircuitBreakerStateChange(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker circuitBreaker = event.getCircuitBreaker();
        CircuitBreaker.State fromState = event.getStateTransition().getFromState();
        CircuitBreaker.State toState = event.getStateTransition().getToState();
        
        log.info("Circuit breaker '{}' state transition: {} -> {}", 
                 circuitBreaker.getName(), fromState, toState);
        
        // Record metrics
        recordStateTransition(circuitBreaker.getName(), fromState, toState);
        
        // Handle specific state transitions
        switch (toState) {
            case OPEN:
                handleCircuitOpen(circuitBreaker);
                break;
            case HALF_OPEN:
                handleCircuitHalfOpen(circuitBreaker);
                break;
            case CLOSED:
                handleCircuitClosed(circuitBreaker);
                break;
        }
        
        // Publish application event
        eventPublisher.publishEvent(new CircuitBreakerStateChangeEvent(
                circuitBreaker.getName(), fromState, toState));
    }
    
    private void handleCircuitOpen(CircuitBreaker circuitBreaker) {
        // Send alert
        alertService.sendCircuitBreakerAlert(circuitBreaker.getName(), "OPEN");
        
        // Activate fallback mechanisms
        fallbackService.activateFallback(circuitBreaker.getName());
        
        // Schedule health checks
        healthCheckScheduler.scheduleHealthCheck(circuitBreaker.getName());
    }
    
    private void handleCircuitClosed(CircuitBreaker circuitBreaker) {
        // Send recovery notification
        alertService.sendCircuitBreakerRecovery(circuitBreaker.getName());
        
        // Deactivate fallback mechanisms
        fallbackService.deactivateFallback(circuitBreaker.getName());
        
        // Clear queued requests
        requestQueue.processQueuedRequests(circuitBreaker.getName());
    }
}
```

---

## Redis Integration for State Persistence

### Circuit Breaker State Caching
```java
@Component
public class CircuitBreakerStateCache {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CB_STATE_PREFIX = "circuit_breaker:state:";
    private static final String CB_METRICS_PREFIX = "circuit_breaker:metrics:";
    
    public void saveCircuitBreakerState(String name, CircuitBreaker.State state, 
                                       CircuitBreaker.Metrics metrics) {
        String stateKey = CB_STATE_PREFIX + name;
        String metricsKey = CB_METRICS_PREFIX + name;
        
        CircuitBreakerStateData stateData = CircuitBreakerStateData.builder()
                .name(name)
                .state(state)
                .timestamp(Instant.now())
                .failureRate(metrics.getFailureRate())
                .numberOfCalls(metrics.getNumberOfCalls())
                .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
                .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
                .build();
        
        // Cache for 1 hour
        redisTemplate.opsForValue().set(stateKey, stateData, Duration.ofHours(1));
        
        // Store metrics history
        redisTemplate.opsForList().leftPush(metricsKey, stateData);
        redisTemplate.opsForList().trim(metricsKey, 0, 99); // Keep last 100 entries
        redisTemplate.expire(metricsKey, Duration.ofDays(1));
    }
    
    public Optional<CircuitBreakerStateData> getCircuitBreakerState(String name) {
        String key = CB_STATE_PREFIX + name;
        CircuitBreakerStateData stateData = 
                (CircuitBreakerStateData) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(stateData);
    }
    
    public List<CircuitBreakerStateData> getCircuitBreakerHistory(String name) {
        String key = CB_METRICS_PREFIX + name;
        return redisTemplate.opsForList().range(key, 0, -1).stream()
                .map(obj -> (CircuitBreakerStateData) obj)
                .collect(Collectors.toList());
    }
}
```

---

## Metrics and Observability

### Prometheus Metrics Export
```java
@Component
public class CircuitBreakerMetrics {
    
    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @PostConstruct
    public void bindCircuitBreakerMetrics() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String name = circuitBreaker.getName();
            
            // State gauge
            Gauge.builder("circuit_breaker_state")
                    .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                    .tag("name", name)
                    .register(meterRegistry, circuitBreaker, cb -> {
                        switch (cb.getState()) {
                            case CLOSED: return 0;
                            case OPEN: return 1;
                            case HALF_OPEN: return 2;
                            default: return -1;
                        }
                    });
            
            // Failure rate gauge
            Gauge.builder("circuit_breaker_failure_rate")
                    .description("Circuit breaker failure rate percentage")
                    .tag("name", name)
                    .register(meterRegistry, circuitBreaker, 
                             cb -> cb.getMetrics().getFailureRate());
            
            // Call counters
            Gauge.builder("circuit_breaker_calls_total")
                    .description("Total number of calls")
                    .tag("name", name)
                    .tag("result", "total")
                    .register(meterRegistry, circuitBreaker, 
                             cb -> cb.getMetrics().getNumberOfCalls());
            
            Gauge.builder("circuit_breaker_calls_total")
                    .description("Number of successful calls")
                    .tag("name", name)
                    .tag("result", "successful")
                    .register(meterRegistry, circuitBreaker, 
                             cb -> cb.getMetrics().getNumberOfSuccessfulCalls());
            
            Gauge.builder("circuit_breaker_calls_total")
                    .description("Number of failed calls")
                    .tag("name", name)
                    .tag("result", "failed")
                    .register(meterRegistry, circuitBreaker, 
                             cb -> cb.getMetrics().getNumberOfFailedCalls());
        });
    }
}
```

### Health Check Integration
```java
@Component
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean anyCircuitOpen = false;
        
        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", state.toString());
            cbDetails.put("failure_rate", metrics.getFailureRate());
            cbDetails.put("calls", metrics.getNumberOfCalls());
            cbDetails.put("failed_calls", metrics.getNumberOfFailedCalls());
            cbDetails.put("successful_calls", metrics.getNumberOfSuccessfulCalls());
            
            details.put(name, cbDetails);
            
            if (state == CircuitBreaker.State.OPEN) {
                anyCircuitOpen = true;
            }
        }
        
        if (anyCircuitOpen) {
            return Health.down()
                    .withDetail("circuit_breakers", details)
                    .withDetail("message", "One or more circuit breakers are OPEN")
                    .build();
        } else {
            return Health.up()
                    .withDetail("circuit_breakers", details)
                    .build();
        }
    }
}
```

---

## API Gateway Integration

### Gateway Circuit Breaker Filter
```java
@Component
public class CircuitBreakerGatewayFilter implements GatewayFilter, Ordered {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = extractServiceName(exchange.getRequest());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        
        return circuitBreaker.executeSupplier(() -> 
            chain.filter(exchange).then(Mono.empty())
        ).cast(Void.class)
        .onErrorResume(CallNotPermittedException.class, ex -> 
            handleCircuitBreakerOpen(exchange, serviceName))
        .onErrorResume(Exception.class, ex -> 
            handleServiceError(exchange, serviceName, ex));
    }
    
    private Mono<Void> handleCircuitBreakerOpen(ServerWebExchange exchange, String serviceName) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("X-Circuit-Breaker-State", "OPEN");
        response.getHeaders().add("X-Service-Name", serviceName);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("SERVICE_UNAVAILABLE")
                .message("Service temporarily unavailable due to circuit breaker")
                .serviceName(serviceName)
                .timestamp(Instant.now())
                .retryAfter(30) // seconds
                .build();
        
        DataBuffer buffer = response.bufferFactory().wrap(
                objectMapper.writeValueAsBytes(errorResponse));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}
```

---

## Testing Circuit Breaker Patterns

### Integration Tests
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class CircuitBreakerIntegrationTest {
    
    @Autowired
    private ResilientServiceClient serviceClient;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @MockBean
    private WebClient webClient;
    
    @Test
    @Order(1)
    void testCircuitBreakerClosed() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("customer-service");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // When
        CompletableFuture<CustomerResponse> future = serviceClient.getCustomer("123");
        CustomerResponse response = future.join();
        
        // Then
        assertThat(response).isNotNull();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
    
    @Test
    @Order(2)
    void testCircuitBreakerOpensOnFailures() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("customer-service");
        
        // Configure WebClient to fail
        when(webClient.get()).thenThrow(new RuntimeException("Service failure"));
        
        // When - Generate enough failures to trip the circuit breaker
        for (int i = 0; i < 25; i++) {
            try {
                serviceClient.getCustomer("123").join();
            } catch (Exception e) {
                // Expected failures
            }
        }
        
        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
    
    @Test
    @Order(3)
    void testFallbackMechanismWhenCircuitOpen() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("customer-service");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        
        // When
        CompletableFuture<CustomerResponse> future = serviceClient.getCustomer("123");
        CustomerResponse response = future.join();
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isServiceDegraded()).isTrue();
        assertThat(response.getMessage()).contains("temporarily unavailable");
    }
}
```

### Load Testing Circuit Breaker
```bash
#!/bin/bash
# Circuit Breaker Load Test Script

API_GATEWAY="http://localhost:8080"
CONCURRENT_REQUESTS=100
TEST_DURATION=60

echo "Starting Circuit Breaker Load Test..."

# Function to generate load
generate_load() {
    local service_endpoint=$1
    local request_count=$2
    
    for i in $(seq 1 $request_count); do
        curl -s -w "%{http_code}," "$API_GATEWAY$service_endpoint" >> results.csv &
        
        # Limit concurrent connections
        if (( i % 20 == 0 )); then
            wait
        fi
    done
    wait
}

# Test customer service circuit breaker
echo "Testing Customer Service Circuit Breaker..."
generate_load "/api/v1/customers/123" 200

# Analyze results
success_count=$(grep -o "200" results.csv | wc -l)
failure_count=$(grep -o "5[0-9][0-9]" results.csv | wc -l)
circuit_open_count=$(grep -o "503" results.csv | wc -l)

echo "Load Test Results:"
echo "- Successful requests: $success_count"
echo "- Failed requests: $failure_count"
echo "- Circuit breaker responses: $circuit_open_count"

# Check circuit breaker status
curl -s "$API_GATEWAY/actuator/circuitbreakers" | jq '.'

rm -f results.csv
```

This Circuit Breaker implementation provides comprehensive fault tolerance with intelligent fallback mechanisms, state persistence through Redis, detailed monitoring, and seamless integration with the microservices architecture to ensure high availability and system resilience.