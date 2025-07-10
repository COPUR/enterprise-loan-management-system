package com.loanmanagement.architecture;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Port Analyzer
 * Analyzes ports and adapters for hexagonal architecture compliance
 */
@Slf4j
public class PortAnalyzer {

    /**
     * Analyze inbound ports
     */
    public PortAnalysisResult analyzeInboundPorts(List<Class<?>> inboundPorts) {
        log.debug("Analyzing {} inbound ports", inboundPorts.size());
        
        PortAnalysisResult.Builder resultBuilder = PortAnalysisResult.builder()
                .compliant(true)
                .violationCount(0)
                .totalPorts(inboundPorts.size());

        for (Class<?> port : inboundPorts) {
            PortCompliance compliance = analyzeInboundPortCompliance(port);
            
            if (!compliance.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addPortViolation(port.getSimpleName(), compliance.getViolations());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze outbound ports
     */
    public PortAnalysisResult analyzeOutboundPorts(List<Class<?>> outboundPorts) {
        log.debug("Analyzing {} outbound ports", outboundPorts.size());
        
        PortAnalysisResult.Builder resultBuilder = PortAnalysisResult.builder()
                .compliant(true)
                .violationCount(0)
                .totalPorts(outboundPorts.size());

        for (Class<?> port : outboundPorts) {
            PortCompliance compliance = analyzeOutboundPortCompliance(port);
            
            if (!compliance.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addPortViolation(port.getSimpleName(), compliance.getViolations());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze adapters
     */
    public AdapterAnalysisResult analyzeAdapters(List<AdapterMapping> adapterMappings) {
        log.debug("Analyzing {} adapters", adapterMappings.size());
        
        AdapterAnalysisResult.Builder resultBuilder = AdapterAnalysisResult.builder()
                .compliant(true)
                .violationCount(0)
                .totalAdapters(adapterMappings.size());

        for (AdapterMapping mapping : adapterMappings) {
            AdapterCompliance compliance = analyzeAdapterCompliance(mapping);
            
            if (!compliance.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addAdapterViolation(mapping.getAdapterClass().getSimpleName(), 
                                           compliance.getViolations());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze web adapters
     */
    public WebAdapterAnalysisResult analyzeWebAdapters(List<Class<?>> webAdapters) {
        log.debug("Analyzing {} web adapters", webAdapters.size());
        
        WebAdapterAnalysisResult.Builder resultBuilder = WebAdapterAnalysisResult.builder()
                .compliant(true)
                .violationCount(0)
                .totalAdapters(webAdapters.size());

        for (Class<?> adapter : webAdapters) {
            WebAdapterCompliance compliance = analyzeWebAdapterCompliance(adapter);
            
            if (!compliance.isCompliant()) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addWebAdapterViolation(adapter.getSimpleName(), compliance.getViolations());
            }
        }

        return resultBuilder.build();
    }

    // Helper methods for port analysis

    private PortCompliance analyzeInboundPortCompliance(Class<?> port) {
        PortCompliance.Builder complianceBuilder = PortCompliance.builder()
                .compliant(true);

        // Check if it's an interface
        if (!port.isInterface()) {
            complianceBuilder.compliant(false)
                    .addViolation("Inbound port must be an interface");
        }

        // Check naming convention
        if (!followsInboundPortNaming(port)) {
            complianceBuilder.compliant(false)
                    .addViolation("Port doesn't follow naming convention (should end with UseCase, Query, or Command)");
        }

        // Check method signatures
        for (Method method : port.getDeclaredMethods()) {
            if (!isBusinessMethod(method)) {
                complianceBuilder.compliant(false)
                        .addViolation("Method " + method.getName() + " is not a business method");
            }
            
            if (hasInfrastructureParameters(method)) {
                complianceBuilder.compliant(false)
                        .addViolation("Method " + method.getName() + " has infrastructure parameters");
            }
        }

        // Check for infrastructure dependencies
        if (hasInfrastructureDependencies(port)) {
            complianceBuilder.compliant(false)
                    .addViolation("Port has infrastructure dependencies");
        }

        return complianceBuilder.build();
    }

    private PortCompliance analyzeOutboundPortCompliance(Class<?> port) {
        PortCompliance.Builder complianceBuilder = PortCompliance.builder()
                .compliant(true);

        // Check if it's an interface
        if (!port.isInterface()) {
            complianceBuilder.compliant(false)
                    .addViolation("Outbound port must be an interface");
        }

        // Check naming convention
        if (!followsOutboundPortNaming(port)) {
            complianceBuilder.compliant(false)
                    .addViolation("Port doesn't follow naming convention (should end with Repository, Port, or Gateway)");
        }

        // Check method signatures
        for (Method method : port.getDeclaredMethods()) {
            if (!isTechnicalMethod(method)) {
                complianceBuilder.compliant(false)
                        .addViolation("Method " + method.getName() + " is not a technical method");
            }
            
            if (hasBusinessLogicInSignature(method)) {
                complianceBuilder.compliant(false)
                        .addViolation("Method " + method.getName() + " contains business logic in signature");
            }
        }

        // Check for technology-specific types
        if (hasTechnologySpecificTypes(port)) {
            complianceBuilder.compliant(false)
                    .addViolation("Port uses technology-specific types");
        }

        return complianceBuilder.build();
    }

    private AdapterCompliance analyzeAdapterCompliance(AdapterMapping mapping) {
        AdapterCompliance.Builder complianceBuilder = AdapterCompliance.builder()
                .compliant(true);

        Class<?> portInterface = mapping.getPortInterface();
        Class<?> adapterClass = mapping.getAdapterClass();

        // Check if adapter implements port
        if (!portInterface.isAssignableFrom(adapterClass)) {
            complianceBuilder.compliant(false)
                    .addViolation("Adapter doesn't implement port interface");
        }

        // Check for business logic in adapter
        if (hasBusinessLogicInAdapter(adapterClass)) {
            complianceBuilder.compliant(false)
                    .addViolation("Adapter contains business logic");
        }

        // Check for proper separation of concerns
        if (!hasProperSeparationOfConcerns(adapterClass)) {
            complianceBuilder.compliant(false)
                    .addViolation("Adapter doesn't have proper separation of concerns");
        }

        // Check adapter naming
        if (!followsAdapterNaming(adapterClass)) {
            complianceBuilder.compliant(false)
                    .addViolation("Adapter doesn't follow naming convention");
        }

        return complianceBuilder.build();
    }

    private WebAdapterCompliance analyzeWebAdapterCompliance(Class<?> adapter) {
        WebAdapterCompliance.Builder complianceBuilder = WebAdapterCompliance.builder()
                .compliant(true);

        // Check for domain logic
        if (hasDomainLogic(adapter)) {
            complianceBuilder.compliant(false)
                    .addViolation("Web adapter contains domain logic");
        }

        // Check for direct repository access
        if (hasDirectRepositoryAccess(adapter)) {
            complianceBuilder.compliant(false)
                    .addViolation("Web adapter has direct repository access");
        }

        // Check if uses only inbound ports
        if (!usesOnlyInboundPorts(adapter)) {
            complianceBuilder.compliant(false)
                    .addViolation("Web adapter doesn't use only inbound ports");
        }

        // Check for proper HTTP mapping
        if (!hasProperHttpMapping(adapter)) {
            complianceBuilder.compliant(false)
                    .addViolation("Web adapter doesn't have proper HTTP mapping");
        }

        return complianceBuilder.build();
    }

    // Helper methods for compliance checking

    private boolean followsInboundPortNaming(Class<?> port) {
        String name = port.getSimpleName();
        return name.endsWith("UseCase") || name.endsWith("Query") || name.endsWith("Command");
    }

    private boolean followsOutboundPortNaming(Class<?> port) {
        String name = port.getSimpleName();
        return name.endsWith("Repository") || name.endsWith("Port") || name.endsWith("Gateway");
    }

    private boolean isBusinessMethod(Method method) {
        String name = method.getName();
        return !name.startsWith("get") && 
               !name.startsWith("set") && 
               !name.equals("toString") && 
               !name.equals("hashCode") && 
               !name.equals("equals") &&
               (name.contains("process") || 
                name.contains("handle") || 
                name.contains("execute") || 
                name.contains("calculate") ||
                name.contains("validate"));
    }

    private boolean isTechnicalMethod(Method method) {
        String name = method.getName();
        return name.startsWith("save") || 
               name.startsWith("find") || 
               name.startsWith("delete") || 
               name.startsWith("update") || 
               name.startsWith("send") || 
               name.startsWith("notify") ||
               name.startsWith("fetch") ||
               name.startsWith("store");
    }

    private boolean hasInfrastructureParameters(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(this::isInfrastructureType);
    }

    private boolean hasBusinessLogicInSignature(Method method) {
        String name = method.getName();
        return name.contains("calculate") || 
               name.contains("validate") || 
               name.contains("approve") || 
               name.contains("reject");
    }

    private boolean hasInfrastructureDependencies(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> isInfrastructureType(field.getType()));
    }

    private boolean hasTechnologySpecificTypes(Class<?> port) {
        return Arrays.stream(port.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .anyMatch(type -> isTechnologySpecificType(type));
    }

    private boolean hasBusinessLogicInAdapter(Class<?> adapter) {
        return Arrays.stream(adapter.getDeclaredMethods())
                .anyMatch(method -> containsBusinessLogic(method));
    }

    private boolean hasProperSeparationOfConcerns(Class<?> adapter) {
        // Check if adapter only handles technical concerns
        return Arrays.stream(adapter.getDeclaredMethods())
                .allMatch(method -> isTechnicalMethod(method) || isInfrastructureMethod(method));
    }

    private boolean followsAdapterNaming(Class<?> adapter) {
        String name = adapter.getSimpleName();
        return name.endsWith("Adapter") || 
               name.endsWith("Controller") || 
               name.endsWith("Repository") ||
               name.endsWith("Gateway");
    }

    private boolean hasDomainLogic(Class<?> adapter) {
        return Arrays.stream(adapter.getDeclaredMethods())
                .anyMatch(method -> containsBusinessLogic(method));
    }

    private boolean hasDirectRepositoryAccess(Class<?> adapter) {
        return Arrays.stream(adapter.getDeclaredFields())
                .anyMatch(field -> isRepositoryType(field.getType()));
    }

    private boolean usesOnlyInboundPorts(Class<?> adapter) {
        return Arrays.stream(adapter.getDeclaredFields())
                .filter(field -> !isPrimitiveOrWrapper(field.getType()))
                .allMatch(field -> isInboundPortType(field.getType()) || isUtilityType(field.getType()));
    }

    private boolean hasProperHttpMapping(Class<?> adapter) {
        // Check if class has proper REST controller annotations and mapping
        return adapter.isAnnotationPresent(getRestControllerAnnotation()) ||
               Arrays.stream(adapter.getDeclaredMethods())
                       .anyMatch(method -> hasHttpMappingAnnotation(method));
    }

    // Type checking methods

    private boolean isInfrastructureType(Class<?> type) {
        return type.getPackageName().contains("infrastructure") ||
               type.getPackageName().contains("javax.persistence") ||
               type.getPackageName().contains("jakarta.persistence") ||
               type.getPackageName().contains("springframework.data");
    }

    private boolean isTechnologySpecificType(Class<?> type) {
        return type.getPackageName().contains("javax.persistence") ||
               type.getPackageName().contains("jakarta.persistence") ||
               type.getPackageName().contains("springframework.web") ||
               type.getPackageName().contains("javax.servlet") ||
               type.getPackageName().contains("jakarta.servlet");
    }

    private boolean containsBusinessLogic(Method method) {
        String name = method.getName();
        return name.contains("calculate") || 
               name.contains("validate") || 
               name.contains("process") || 
               name.contains("approve") || 
               name.contains("reject") ||
               name.contains("determine") ||
               name.contains("assess");
    }

    private boolean isInfrastructureMethod(Method method) {
        String name = method.getName();
        return name.startsWith("map") || 
               name.startsWith("convert") || 
               name.startsWith("serialize") || 
               name.startsWith("deserialize") ||
               name.contains("entity") ||
               name.contains("dto");
    }

    private boolean isRepositoryType(Class<?> type) {
        return type.getSimpleName().contains("Repository") && !type.isInterface();
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type == String.class || 
               type == Integer.class || 
               type == Long.class || 
               type == Boolean.class || 
               type == Double.class || 
               type == Float.class ||
               type.getPackageName().startsWith("java.time");
    }

    private boolean isInboundPortType(Class<?> type) {
        return type.getPackageName().contains("port.in");
    }

    private boolean isUtilityType(Class<?> type) {
        return type.getPackageName().startsWith("java.") ||
               type.getPackageName().startsWith("javax.") ||
               type.getPackageName().startsWith("jakarta.") ||
               type.getPackageName().contains("util");
    }

    private Class<? extends java.lang.annotation.Annotation> getRestControllerAnnotation() {
        // In real implementation, would return actual RestController annotation class
        return Deprecated.class; // Placeholder
    }

    private boolean hasHttpMappingAnnotation(Method method) {
        // In real implementation, would check for actual HTTP mapping annotations
        return method.getName().startsWith("get") || 
               method.getName().startsWith("post") || 
               method.getName().startsWith("put") || 
               method.getName().startsWith("delete");
    }
}