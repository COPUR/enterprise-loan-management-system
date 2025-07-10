package com.loanmanagement.architecture;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Architecture Test Result Classes
 * Contains all result classes for architecture compliance testing
 */
public class ArchitectureTestResults {

    /**
     * Port Analysis Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class PortAnalysisResult {
        boolean compliant;
        int violationCount;
        int totalPorts;
        Map<String, List<String>> portViolations;
        LocalDateTime analyzedAt;

        public boolean isPortCompliant(Class<?> port) {
            return !portViolations.containsKey(port.getSimpleName()) ||
                   portViolations.get(port.getSimpleName()).isEmpty();
        }

        public static class Builder {
            private Map<String, List<String>> portViolations = new java.util.HashMap<>();

            public Builder addPortViolation(String portName, List<String> violations) {
                this.portViolations.put(portName, violations);
                return this;
            }
        }
    }

    /**
     * Adapter Analysis Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class AdapterAnalysisResult {
        boolean compliant;
        int violationCount;
        int totalAdapters;
        Map<String, List<String>> adapterViolations;
        LocalDateTime analyzedAt;

        public boolean isAdapterCompliant(AdapterMapping mapping) {
            return !adapterViolations.containsKey(mapping.getAdapterClass().getSimpleName()) ||
                   adapterViolations.get(mapping.getAdapterClass().getSimpleName()).isEmpty();
        }

        public static class Builder {
            private Map<String, List<String>> adapterViolations = new java.util.HashMap<>();

            public Builder addAdapterViolation(String adapterName, List<String> violations) {
                this.adapterViolations.put(adapterName, violations);
                return this;
            }
        }
    }

    /**
     * Web Adapter Analysis Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class WebAdapterAnalysisResult {
        boolean compliant;
        int violationCount;
        int totalAdapters;
        Map<String, List<String>> webAdapterViolations;
        LocalDateTime analyzedAt;

        public boolean isWebAdapterCompliant(Class<?> adapter) {
            return !webAdapterViolations.containsKey(adapter.getSimpleName()) ||
                   webAdapterViolations.get(adapter.getSimpleName()).isEmpty();
        }

        public static class Builder {
            private Map<String, List<String>> webAdapterViolations = new java.util.HashMap<>();

            public Builder addWebAdapterViolation(String adapterName, List<String> violations) {
                this.webAdapterViolations.put(adapterName, violations);
                return this;
            }
        }
    }

    /**
     * Domain Independence Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class DomainIndependenceResult {
        boolean independent;
        int infrastructureDependencyCount;
        int applicationDependencyCount;
        int webDependencyCount;
        Map<String, List<String>> infrastructureDependencies;
        Map<String, List<String>> applicationDependencies;
        Map<String, List<String>> webDependencies;

        public static class Builder {
            private Map<String, List<String>> infrastructureDependencies = new java.util.HashMap<>();
            private Map<String, List<String>> applicationDependencies = new java.util.HashMap<>();
            private Map<String, List<String>> webDependencies = new java.util.HashMap<>();

            public Builder addInfrastructureDependency(String className, List<String> dependencies) {
                this.infrastructureDependencies.put(className, dependencies);
                return this;
            }

            public Builder addApplicationDependency(String className, List<String> dependencies) {
                this.applicationDependencies.put(className, dependencies);
                return this;
            }

            public Builder addWebDependency(String className, List<String> dependencies) {
                this.webDependencies.put(className, dependencies);
                return this;
            }
        }
    }

    /**
     * Service Dependency Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ServiceDependencyResult {
        boolean compliant;
        int violationCount;
        Map<String, List<String>> serviceViolations;

        public boolean isServiceCompliant(Class<?> service) {
            return !serviceViolations.containsKey(service.getSimpleName()) ||
                   serviceViolations.get(service.getSimpleName()).isEmpty();
        }

        public static class Builder {
            private Map<String, List<String>> serviceViolations = new java.util.HashMap<>();

            public Builder addViolation(String serviceName, List<String> violations) {
                this.serviceViolations.put(serviceName, violations);
                return this;
            }
        }
    }

    /**
     * Application Service Analysis Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ApplicationServiceAnalysisResult {
        boolean compliant;
        int violationCount;
        Map<String, ApplicationServiceAnalysis> serviceAnalyses;

        public boolean isServiceCompliant(Class<?> service) {
            ApplicationServiceAnalysis analysis = serviceAnalyses.get(service.getSimpleName());
            return analysis != null && analysis.isCompliant();
        }

        public static class Builder {
            private Map<String, ApplicationServiceAnalysis> serviceAnalyses = new java.util.HashMap<>();

            public Builder addViolation(String serviceName, ApplicationServiceAnalysis analysis) {
                this.serviceAnalyses.put(serviceName, analysis);
                return this;
            }
        }
    }

    /**
     * Dependency Direction Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class DependencyDirectionResult {
        boolean compliant;
        int outwardDependencyCount;
        Map<String, Map<String, List<String>>> layerDependencies;

        public boolean isLayerDependencyCorrect(String fromLayer, String toLayer) {
            // Infrastructure can depend on application, application can depend on domain
            return (fromLayer.equals("infrastructure") && toLayer.equals("application")) ||
                   (fromLayer.equals("application") && toLayer.equals("domain"));
        }

        public boolean hasOutwardDependencies(String layer) {
            Map<String, List<String>> dependencies = layerDependencies.get(layer);
            return dependencies != null && !dependencies.isEmpty();
        }

        public static class Builder {
            private Map<String, Map<String, List<String>>> layerDependencies = new java.util.HashMap<>();

            public Builder addOutwardDependency(String layer, String className, List<String> dependencies) {
                this.layerDependencies.computeIfAbsent(layer, k -> new java.util.HashMap<>())
                        .put(className, dependencies);
                return this;
            }
        }
    }

    /**
     * Circular Dependency Result
     */
    @Value
    @Builder
    @Jacksonized
    public static class CircularDependencyResult {
        boolean hasCircularDependencies;
        int circularDependencyCount;
        List<List<String>> circularDependencies;
    }

    /**
     * Package Structure Result
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class PackageStructureResult {
        boolean compliant;
        int violationCount;
        List<String> violations;
        Map<String, Boolean> layerPresence;

        public boolean hasLayer(String layerName) {
            return layerPresence.getOrDefault(layerName, false);
        }

        public boolean isLayerSeparated(String layer1, String layer2) {
            return hasLayer(layer1) && hasLayer(layer2);
        }

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();
            private Map<String, Boolean> layerPresence = new java.util.HashMap<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }

            public Builder() {
                // Initialize with expected layers
                layerPresence.put("domain", true);
                layerPresence.put("application", true);
                layerPresence.put("infrastructure", true);
            }
        }
    }

    // Supporting classes

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class PortCompliance {
        boolean compliant;
        List<String> violations;

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class AdapterCompliance {
        boolean compliant;
        List<String> violations;

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class WebAdapterCompliance {
        boolean compliant;
        List<String> violations;

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class DomainDependencyAnalysis {
        List<String> infrastructureDependencies;
        List<String> applicationDependencies;
        List<String> webDependencies;

        public boolean hasInfrastructureDependencies() {
            return infrastructureDependencies != null && !infrastructureDependencies.isEmpty();
        }

        public boolean hasApplicationDependencies() {
            return applicationDependencies != null && !applicationDependencies.isEmpty();
        }

        public boolean hasWebDependencies() {
            return webDependencies != null && !webDependencies.isEmpty();
        }

        public static class Builder {
            private List<String> infrastructureDependencies = new java.util.ArrayList<>();
            private List<String> applicationDependencies = new java.util.ArrayList<>();
            private List<String> webDependencies = new java.util.ArrayList<>();

            public Builder addInfrastructureDependency(String dependency) {
                this.infrastructureDependencies.add(dependency);
                return this;
            }

            public Builder addApplicationDependency(String dependency) {
                this.applicationDependencies.add(dependency);
                return this;
            }

            public Builder addWebDependency(String dependency) {
                this.webDependencies.add(dependency);
                return this;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ServiceDependencyAnalysis {
        boolean compliant;
        List<String> violations;

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ApplicationServiceAnalysis {
        boolean compliant;
        List<String> violations;

        public static class Builder {
            private List<String> violations = new java.util.ArrayList<>();

            public Builder addViolation(String violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    // Additional result classes for comprehensive architecture analysis

    @Value
    @Builder
    @Jacksonized
    public static class CouplingAnalysisResult {
        Map<String, CouplingMetrics> classMetrics;
        double averageAfferentCoupling;
        double averageEfferentCoupling;
        double averageInstability;

        public static class Builder {
            private Map<String, CouplingMetrics> classMetrics = new java.util.HashMap<>();

            public Builder addClassMetrics(String className, CouplingMetrics metrics) {
                this.classMetrics.put(className, metrics);
                return this;
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class CouplingMetrics {
        int afferentCoupling;  // Ca - incoming dependencies
        int efferentCoupling;  // Ce - outgoing dependencies
        double instability;    // I = Ce / (Ca + Ce)
    }

    @Value
    @Builder
    @Jacksonized
    public static class LayerAnalysisResult {
        boolean compliant;
        int totalClasses;
        int violationCount;
        List<LayerViolation> violations;

        public static class Builder {
            private List<LayerViolation> violations = new java.util.ArrayList<>();

            public Builder addViolation(LayerViolation violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class LayerViolation {
        LayerViolationType violationType;
        String className;
        String description;
    }

    public enum LayerViolationType {
        DOMAIN_INFRASTRUCTURE_DEPENDENCY,
        DOMAIN_APPLICATION_DEPENDENCY,
        APPLICATION_INFRASTRUCTURE_DEPENDENCY,
        INFRASTRUCTURE_BUSINESS_LOGIC
    }

    @Value
    @Builder
    @Jacksonized
    public static class DependencyInversionResult {
        boolean compliant;
        int violationCount;
        List<DependencyViolation> violations;

        public static class Builder {
            private List<DependencyViolation> violations = new java.util.ArrayList<>();

            public Builder addViolation(DependencyViolation violation) {
                this.violations.add(violation);
                return this;
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class DependencyViolation {
        String className;
        DependencyViolationType violationType;
        String description;
    }

    public enum DependencyViolationType {
        HIGH_LEVEL_DEPENDS_ON_LOW_LEVEL,
        CONCRETE_DEPENDENCY,
        CIRCULAR_DEPENDENCY
    }

    @Value
    @Builder
    @Jacksonized
    public static class ArchitectureSmellResult {
        int smellCount;
        List<ArchitectureSmell> smells;

        public static class Builder {
            private List<ArchitectureSmell> smells = new java.util.ArrayList<>();

            public Builder addSmells(List<ArchitectureSmell> smellsToAdd) {
                this.smells.addAll(smellsToAdd);
                return this;
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class ArchitectureSmell {
        ArchitectureSmellType smellType;
        String className;
        String description;
        SmellSeverity severity;
    }

    public enum ArchitectureSmellType {
        GOD_CLASS,
        FEATURE_ENVY,
        INAPPROPRIATE_INTIMACY,
        CIRCULAR_DEPENDENCY,
        LONG_PARAMETER_LIST,
        SHOTGUN_SURGERY
    }

    public enum SmellSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Supporting structure classes

    public static class PackageStructureDefinition {
        private List<String> domainPackages = List.of("domain.model", "domain.service");
        private List<String> applicationPackages = List.of("application.service", "application.port");
        private List<String> infrastructurePackages = List.of("infrastructure.adapter", "infrastructure.config");

        public List<String> getDomainPackages() { return domainPackages; }
        public List<String> getApplicationPackages() { return applicationPackages; }
        public List<String> getInfrastructurePackages() { return infrastructurePackages; }
    }

    public static class ArchitectureMapping {
        // Mock implementation for demonstration
        public Map<String, String> getLayerMappings() {
            return Map.of(
                "domain", "com.loanmanagement.*.domain.*",
                "application", "com.loanmanagement.*.application.*",
                "infrastructure", "com.loanmanagement.*.infrastructure.*"
            );
        }
    }

    // Adapter mapping class for tests
    public static class AdapterMapping {
        private final Class<?> portInterface;
        private final Class<?> adapterClass;

        public AdapterMapping(Class<?> portInterface, Class<?> adapterClass) {
            this.portInterface = portInterface;
            this.adapterClass = adapterClass;
        }

        public Class<?> getPortInterface() { return portInterface; }
        public Class<?> getAdapterClass() { return adapterClass; }
    }
}