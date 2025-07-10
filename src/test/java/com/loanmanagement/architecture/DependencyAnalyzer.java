package com.loanmanagement.architecture;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency Analyzer
 * Analyzes dependencies between classes and packages for architecture compliance
 */
@Slf4j
public class DependencyAnalyzer {

    /**
     * Analyze domain independence
     */
    public DomainIndependenceResult analyzeDomainIndependence(List<Class<?>> domainClasses) {
        log.debug("Analyzing domain independence for {} classes", domainClasses.size());
        
        DomainIndependenceResult.Builder resultBuilder = DomainIndependenceResult.builder()
                .independent(true)
                .infrastructureDependencyCount(0)
                .applicationDependencyCount(0)
                .webDependencyCount(0);

        for (Class<?> domainClass : domainClasses) {
            DomainDependencyAnalysis analysis = analyzeDomainDependencies(domainClass);
            
            if (analysis.hasInfrastructureDependencies()) {
                resultBuilder.independent(false)
                        .infrastructureDependencyCount(
                                resultBuilder.build().getInfrastructureDependencyCount() + 
                                analysis.getInfrastructureDependencies().size())
                        .addInfrastructureDependency(domainClass.getSimpleName(), 
                                analysis.getInfrastructureDependencies());
            }
            
            if (analysis.hasApplicationDependencies()) {
                resultBuilder.independent(false)
                        .applicationDependencyCount(
                                resultBuilder.build().getApplicationDependencyCount() + 
                                analysis.getApplicationDependencies().size())
                        .addApplicationDependency(domainClass.getSimpleName(), 
                                analysis.getApplicationDependencies());
            }
            
            if (analysis.hasWebDependencies()) {
                resultBuilder.independent(false)
                        .webDependencyCount(
                                resultBuilder.build().getWebDependencyCount() + 
                                analysis.getWebDependencies().size())
                        .addWebDependency(domainClass.getSimpleName(), 
                                analysis.getWebDependencies());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze domain services dependencies
     */
    public ServiceDependencyResult analyzeDomainServices(List<Class<?>> domainServices) {
        log.debug("Analyzing domain services dependencies for {} services", domainServices.size());
        
        ServiceDependencyResult.Builder resultBuilder = ServiceDependencyResult.builder()
                .compliant(true)
                .violationCount(0);

        for (Class<?> service : domainServices) {
            ServiceDependencyAnalysis analysis = analyzeServiceDependencies(service);
            
            if (!analysis.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addViolation(service.getSimpleName(), analysis.getViolations());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze application services dependencies
     */
    public ApplicationServiceAnalysisResult analyzeApplicationServices(List<Class<?>> applicationServices) {
        log.debug("Analyzing application services for {} services", applicationServices.size());
        
        ApplicationServiceAnalysisResult.Builder resultBuilder = ApplicationServiceAnalysisResult.builder()
                .compliant(true)
                .violationCount(0);

        for (Class<?> service : applicationServices) {
            ApplicationServiceAnalysis analysis = analyzeApplicationService(service);
            
            if (!analysis.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addViolation(service.getSimpleName(), analysis);
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze dependency direction
     */
    public DependencyDirectionResult analyzeDependencyDirection(ArchitectureMapping mapping) {
        log.debug("Analyzing dependency direction");
        
        DependencyDirectionResult.Builder resultBuilder = DependencyDirectionResult.builder()
                .compliant(true)
                .outwardDependencyCount(0);

        // Check domain layer dependencies
        Map<String, List<String>> domainDependencies = analyzeDomainLayerDependencies(mapping);
        for (Map.Entry<String, List<String>> entry : domainDependencies.entrySet()) {
            if (hasOutwardDependencies(entry.getValue())) {
                resultBuilder.compliant(false)
                        .outwardDependencyCount(resultBuilder.build().getOutwardDependencyCount() + 1)
                        .addOutwardDependency("domain", entry.getKey(), entry.getValue());
            }
        }

        // Check application layer dependencies
        Map<String, List<String>> applicationDependencies = analyzeApplicationLayerDependencies(mapping);
        for (Map.Entry<String, List<String>> entry : applicationDependencies.entrySet()) {
            if (hasInvalidApplicationDependencies(entry.getValue())) {
                resultBuilder.compliant(false)
                        .outwardDependencyCount(resultBuilder.build().getOutwardDependencyCount() + 1)
                        .addOutwardDependency("application", entry.getKey(), entry.getValue());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Detect circular dependencies
     */
    public CircularDependencyResult detectCircularDependencies(List<Class<?>> classes) {
        log.debug("Detecting circular dependencies in {} classes", classes.size());
        
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(classes);
        List<List<String>> cycles = detectCycles(dependencyGraph);
        
        return CircularDependencyResult.builder()
                .hasCircularDependencies(!cycles.isEmpty())
                .circularDependencyCount(cycles.size())
                .circularDependencies(cycles)
                .build();
    }

    /**
     * Analyze coupling metrics
     */
    public CouplingAnalysisResult analyzeCoupling(List<Class<?>> classes) {
        log.debug("Analyzing coupling for {} classes", classes.size());
        
        CouplingAnalysisResult.Builder resultBuilder = CouplingAnalysisResult.builder();
        
        for (Class<?> clazz : classes) {
            CouplingMetrics metrics = calculateCouplingMetrics(clazz);
            resultBuilder.addClassMetrics(clazz.getSimpleName(), metrics);
        }
        
        return resultBuilder.build();
    }

    // Helper methods

    private DomainDependencyAnalysis analyzeDomainDependencies(Class<?> domainClass) {
        DomainDependencyAnalysis.Builder analysisBuilder = DomainDependencyAnalysis.builder();
        
        // Analyze field dependencies
        for (Field field : domainClass.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            
            if (isInfrastructureType(fieldType)) {
                analysisBuilder.addInfrastructureDependency(fieldType.getSimpleName());
            } else if (isApplicationType(fieldType)) {
                analysisBuilder.addApplicationDependency(fieldType.getSimpleName());
            } else if (isWebType(fieldType)) {
                analysisBuilder.addWebDependency(fieldType.getSimpleName());
            }
        }
        
        // Analyze method parameter dependencies
        for (Method method : domainClass.getDeclaredMethods()) {
            for (Class<?> paramType : method.getParameterTypes()) {
                if (isInfrastructureType(paramType)) {
                    analysisBuilder.addInfrastructureDependency(paramType.getSimpleName());
                } else if (isApplicationType(paramType)) {
                    analysisBuilder.addApplicationDependency(paramType.getSimpleName());
                } else if (isWebType(paramType)) {
                    analysisBuilder.addWebDependency(paramType.getSimpleName());
                }
            }
        }
        
        return analysisBuilder.build();
    }

    private ServiceDependencyAnalysis analyzeServiceDependencies(Class<?> service) {
        ServiceDependencyAnalysis.Builder analysisBuilder = ServiceDependencyAnalysis.builder()
                .compliant(true);
        
        // Check field dependencies
        for (Field field : service.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            
            if (isInfrastructureType(fieldType) && !isPortInterface(fieldType)) {
                analysisBuilder.compliant(false)
                        .addViolation("Direct infrastructure dependency: " + fieldType.getSimpleName());
            }
            
            if (isWebType(fieldType)) {
                analysisBuilder.compliant(false)
                        .addViolation("Web layer dependency: " + fieldType.getSimpleName());
            }
        }
        
        return analysisBuilder.build();
    }

    private ApplicationServiceAnalysis analyzeApplicationService(Class<?> service) {
        ApplicationServiceAnalysis.Builder analysisBuilder = ApplicationServiceAnalysis.builder()
                .compliant(true);
        
        // Check if implements inbound port
        boolean implementsInboundPort = Arrays.stream(service.getInterfaces())
                .anyMatch(this::isInboundPortInterface);
        
        if (!implementsInboundPort) {
            analysisBuilder.compliant(false)
                    .addViolation("Application service should implement inbound port");
        }
        
        // Check dependencies
        for (Field field : service.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            
            if (isInfrastructureType(fieldType) && !isPortInterface(fieldType)) {
                analysisBuilder.compliant(false)
                        .addViolation("Direct infrastructure dependency: " + fieldType.getSimpleName());
            }
        }
        
        return analysisBuilder.build();
    }

    private Map<String, List<String>> analyzeDomainLayerDependencies(ArchitectureMapping mapping) {
        // In real implementation, this would analyze actual dependencies
        Map<String, List<String>> dependencies = new HashMap<>();
        dependencies.put("Loan", List.of("Money", "LoanId")); // Only domain dependencies
        dependencies.put("Payment", List.of("Money", "PaymentId")); // Only domain dependencies
        return dependencies;
    }

    private Map<String, List<String>> analyzeApplicationLayerDependencies(ArchitectureMapping mapping) {
        // In real implementation, this would analyze actual dependencies
        Map<String, List<String>> dependencies = new HashMap<>();
        dependencies.put("LoanApplicationService", List.of("Loan", "LoanRepository")); // Domain + ports
        return dependencies;
    }

    private boolean hasOutwardDependencies(List<String> dependencies) {
        return dependencies.stream()
                .anyMatch(dep -> !isDomainClass(dep));
    }

    private boolean hasInvalidApplicationDependencies(List<String> dependencies) {
        return dependencies.stream()
                .anyMatch(dep -> isInfrastructureClass(dep) && !isPortClass(dep));
    }

    private Map<String, Set<String>> buildDependencyGraph(List<Class<?>> classes) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (Class<?> clazz : classes) {
            String className = clazz.getSimpleName();
            Set<String> dependencies = new HashSet<>();
            
            // Analyze field dependencies
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.getType().isPrimitive() && 
                    !field.getType().getPackageName().startsWith("java.")) {
                    dependencies.add(field.getType().getSimpleName());
                }
            }
            
            // Analyze constructor dependencies
            if (clazz.getConstructors().length > 0) {
                for (Class<?> paramType : clazz.getConstructors()[0].getParameterTypes()) {
                    if (!paramType.isPrimitive() && 
                        !paramType.getPackageName().startsWith("java.")) {
                        dependencies.add(paramType.getSimpleName());
                    }
                }
            }
            
            graph.put(className, dependencies);
        }
        
        return graph;
    }

    private List<List<String>> detectCycles(Map<String, Set<String>> graph) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                detectCyclesRecursive(graph, node, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }
        
        return cycles;
    }

    private void detectCyclesRecursive(Map<String, Set<String>> graph, String node, 
                                     Set<String> visited, Set<String> recursionStack, 
                                     List<String> path, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);
        
        Set<String> neighbors = graph.getOrDefault(node, new HashSet<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                detectCyclesRecursive(graph, neighbor, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = path.indexOf(neighbor);
                cycles.add(new ArrayList<>(path.subList(cycleStart, path.size())));
            }
        }
        
        recursionStack.remove(node);
        path.remove(path.size() - 1);
    }

    private CouplingMetrics calculateCouplingMetrics(Class<?> clazz) {
        int afferentCoupling = 0; // Classes that depend on this class
        int efferentCoupling = 0; // Classes this class depends on
        
        // Simplified calculation - in real implementation would analyze all classes
        efferentCoupling = clazz.getDeclaredFields().length + 
                          (clazz.getConstructors().length > 0 ? 
                           clazz.getConstructors()[0].getParameterCount() : 0);
        
        double instability = efferentCoupling / (double) (afferentCoupling + efferentCoupling + 1);
        
        return CouplingMetrics.builder()
                .afferentCoupling(afferentCoupling)
                .efferentCoupling(efferentCoupling)
                .instability(instability)
                .build();
    }

    // Type checking helper methods

    private boolean isInfrastructureType(Class<?> type) {
        return type.getPackageName().contains("infrastructure") ||
               type.getPackageName().contains("javax.persistence") ||
               type.getPackageName().contains("jakarta.persistence");
    }

    private boolean isApplicationType(Class<?> type) {
        return type.getPackageName().contains("application");
    }

    private boolean isWebType(Class<?> type) {
        return type.getPackageName().contains("web") ||
               type.getPackageName().contains("controller") ||
               type.getPackageName().contains("springframework.web");
    }

    private boolean isPortInterface(Class<?> type) {
        return type.getPackageName().contains("port") && type.isInterface();
    }

    private boolean isInboundPortInterface(Class<?> type) {
        return type.getPackageName().contains("port.in") && type.isInterface();
    }

    private boolean isDomainClass(String className) {
        return className.matches("^[A-Z][a-zA-Z]*$") && 
               !className.contains("Service") && 
               !className.contains("Repository");
    }

    private boolean isInfrastructureClass(String className) {
        return className.contains("Adapter") || 
               className.contains("Repository") || 
               className.contains("Controller");
    }

    private boolean isPortClass(String className) {
        return className.contains("Port") || 
               className.contains("UseCase") || 
               className.contains("Query");
    }
}