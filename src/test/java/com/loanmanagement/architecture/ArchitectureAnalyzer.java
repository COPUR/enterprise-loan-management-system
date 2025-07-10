package com.loanmanagement.architecture;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Architecture Analyzer
 * Analyzes code structure for hexagonal architecture compliance
 */
@Slf4j
public class ArchitectureAnalyzer {

    /**
     * Validate package structure against hexagonal architecture
     */
    public PackageStructureResult validatePackageStructure(PackageStructureDefinition definition) {
        log.debug("Validating package structure");
        
        PackageStructureResult.Builder resultBuilder = PackageStructureResult.builder()
                .compliant(true)
                .violationCount(0);

        // Check domain layer isolation
        if (!isDomainLayerIsolated(definition)) {
            resultBuilder.compliant(false)
                    .violationCount(resultBuilder.build().getViolationCount() + 1)
                    .addViolation("Domain layer is not properly isolated");
        }

        // Check application layer dependencies
        if (!isApplicationLayerCompliant(definition)) {
            resultBuilder.compliant(false)
                    .violationCount(resultBuilder.build().getViolationCount() + 1)
                    .addViolation("Application layer has invalid dependencies");
        }

        // Check infrastructure layer placement
        if (!isInfrastructureLayerCompliant(definition)) {
            resultBuilder.compliant(false)
                    .violationCount(resultBuilder.build().getViolationCount() + 1)
                    .addViolation("Infrastructure layer violates dependency rules");
        }

        return resultBuilder.build();
    }

    /**
     * Analyze layered architecture compliance
     */
    public LayerAnalysisResult analyzeLayerCompliance(List<Class<?>> classes) {
        log.debug("Analyzing layer compliance for {} classes", classes.size());
        
        LayerAnalysisResult.Builder resultBuilder = LayerAnalysisResult.builder()
                .compliant(true)
                .totalClasses(classes.size())
                .violationCount(0);

        for (Class<?> clazz : classes) {
            LayerViolation violation = checkLayerViolations(clazz);
            if (violation != null) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addViolation(violation);
            }
        }

        return resultBuilder.build();
    }

    /**
     * Analyze dependency inversion compliance
     */
    public DependencyInversionResult analyzeDependencyInversion(List<Class<?>> classes) {
        log.debug("Analyzing dependency inversion for {} classes", classes.size());
        
        DependencyInversionResult.Builder resultBuilder = DependencyInversionResult.builder()
                .compliant(true)
                .violationCount(0);

        for (Class<?> clazz : classes) {
            if (violatesDependencyInversion(clazz)) {
                resultBuilder.compliant(false)
                        .violationCount(resultBuilder.build().getViolationCount() + 1)
                        .addViolation(createDependencyViolation(clazz));
            }
        }

        return resultBuilder.build();
    }

    /**
     * Check for architecture smells
     */
    public ArchitectureSmellResult detectArchitectureSmells(List<Class<?>> classes) {
        log.debug("Detecting architecture smells in {} classes", classes.size());
        
        ArchitectureSmellResult.Builder resultBuilder = ArchitectureSmellResult.builder()
                .smellCount(0);

        for (Class<?> clazz : classes) {
            List<ArchitectureSmell> smells = detectSmellsInClass(clazz);
            resultBuilder.smellCount(resultBuilder.build().getSmellCount() + smells.size())
                    .addSmells(smells);
        }

        return resultBuilder.build();
    }

    // Helper methods

    private boolean isDomainLayerIsolated(PackageStructureDefinition definition) {
        // Check if domain layer has no outward dependencies
        return definition.getDomainPackages().stream()
                .allMatch(this::hasNoOutwardDependencies);
    }

    private boolean isApplicationLayerCompliant(PackageStructureDefinition definition) {
        // Check if application layer only depends on domain and ports
        return definition.getApplicationPackages().stream()
                .allMatch(this::hasValidApplicationDependencies);
    }

    private boolean isInfrastructureLayerCompliant(PackageStructureDefinition definition) {
        // Check if infrastructure layer implements ports and doesn't contain business logic
        return definition.getInfrastructurePackages().stream()
                .allMatch(this::hasValidInfrastructureDependencies);
    }

    private LayerViolation checkLayerViolations(Class<?> clazz) {
        String packageName = clazz.getPackageName();
        String className = clazz.getSimpleName();

        // Check domain layer violations
        if (packageName.contains("domain")) {
            if (hasInfrastructureDependencies(clazz)) {
                return LayerViolation.builder()
                        .violationType(LayerViolationType.DOMAIN_INFRASTRUCTURE_DEPENDENCY)
                        .className(className)
                        .description("Domain class has infrastructure dependencies")
                        .build();
            }
            if (hasApplicationDependencies(clazz)) {
                return LayerViolation.builder()
                        .violationType(LayerViolationType.DOMAIN_APPLICATION_DEPENDENCY)
                        .className(className)
                        .description("Domain class has application dependencies")
                        .build();
            }
        }

        // Check application layer violations
        if (packageName.contains("application")) {
            if (hasInfrastructureDependencies(clazz) && !isPortInterface(clazz)) {
                return LayerViolation.builder()
                        .violationType(LayerViolationType.APPLICATION_INFRASTRUCTURE_DEPENDENCY)
                        .className(className)
                        .description("Application class has direct infrastructure dependencies")
                        .build();
            }
        }

        // Check infrastructure layer violations
        if (packageName.contains("infrastructure")) {
            if (hasBusinessLogic(clazz)) {
                return LayerViolation.builder()
                        .violationType(LayerViolationType.INFRASTRUCTURE_BUSINESS_LOGIC)
                        .className(className)
                        .description("Infrastructure class contains business logic")
                        .build();
            }
        }

        return null;
    }

    private boolean violatesDependencyInversion(Class<?> clazz) {
        // Check if high-level modules depend on low-level modules
        if (isHighLevelModule(clazz)) {
            return Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(field -> isLowLevelModule(field.getType()));
        }
        return false;
    }

    private DependencyViolation createDependencyViolation(Class<?> clazz) {
        return DependencyViolation.builder()
                .className(clazz.getSimpleName())
                .violationType(DependencyViolationType.HIGH_LEVEL_DEPENDS_ON_LOW_LEVEL)
                .description("High-level module depends on low-level module")
                .build();
    }

    private List<ArchitectureSmell> detectSmellsInClass(Class<?> clazz) {
        List<ArchitectureSmell> smells = new java.util.ArrayList<>();

        // God Class smell
        if (isGodClass(clazz)) {
            smells.add(ArchitectureSmell.builder()
                    .smellType(ArchitectureSmellType.GOD_CLASS)
                    .className(clazz.getSimpleName())
                    .description("Class has too many responsibilities")
                    .severity(SmellSeverity.HIGH)
                    .build());
        }

        // Feature Envy smell
        if (hasFeatureEnvy(clazz)) {
            smells.add(ArchitectureSmell.builder()
                    .smellType(ArchitectureSmellType.FEATURE_ENVY)
                    .className(clazz.getSimpleName())
                    .description("Class uses methods of another class excessively")
                    .severity(SmellSeverity.MEDIUM)
                    .build());
        }

        // Inappropriate Intimacy smell
        if (hasInappropriateIntimacy(clazz)) {
            smells.add(ArchitectureSmell.builder()
                    .smellType(ArchitectureSmellType.INAPPROPRIATE_INTIMACY)
                    .className(clazz.getSimpleName())
                    .description("Class knows too much about another class")
                    .severity(SmellSeverity.MEDIUM)
                    .build());
        }

        // Circular Dependency smell
        if (hasCircularDependency(clazz)) {
            smells.add(ArchitectureSmell.builder()
                    .smellType(ArchitectureSmellType.CIRCULAR_DEPENDENCY)
                    .className(clazz.getSimpleName())
                    .description("Class participates in circular dependency")
                    .severity(SmellSeverity.HIGH)
                    .build());
        }

        return smells;
    }

    // Additional helper methods

    private boolean hasNoOutwardDependencies(String packageName) {
        // In real implementation, this would analyze package dependencies
        return !packageName.contains("infrastructure") && !packageName.contains("web");
    }

    private boolean hasValidApplicationDependencies(String packageName) {
        // Application layer should only depend on domain and ports
        return true; // Simplified implementation
    }

    private boolean hasValidInfrastructureDependencies(String packageName) {
        // Infrastructure should implement ports but not contain business logic
        return true; // Simplified implementation
    }

    private boolean hasInfrastructureDependencies(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> isInfrastructureType(field.getType()));
    }

    private boolean hasApplicationDependencies(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> isApplicationType(field.getType()));
    }

    private boolean hasBusinessLogic(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(this::isBusinessMethod);
    }

    private boolean isPortInterface(Class<?> clazz) {
        return clazz.getPackageName().contains("port");
    }

    private boolean isHighLevelModule(Class<?> clazz) {
        return clazz.getPackageName().contains("application") || 
               clazz.getPackageName().contains("domain");
    }

    private boolean isLowLevelModule(Class<?> clazz) {
        return clazz.getPackageName().contains("infrastructure");
    }

    private boolean isInfrastructureType(Class<?> type) {
        return type.getPackageName().contains("infrastructure") ||
               type.getPackageName().contains("javax.persistence") ||
               type.getPackageName().contains("jakarta.persistence");
    }

    private boolean isApplicationType(Class<?> type) {
        return type.getPackageName().contains("application");
    }

    private boolean isBusinessMethod(Method method) {
        return method.getName().contains("calculate") ||
               method.getName().contains("validate") ||
               method.getName().contains("process") ||
               method.getName().contains("approve");
    }

    private boolean isGodClass(Class<?> clazz) {
        // Simple heuristic: class with too many methods or fields
        return clazz.getDeclaredMethods().length > 20 || 
               clazz.getDeclaredFields().length > 15;
    }

    private boolean hasFeatureEnvy(Class<?> clazz) {
        // Simplified: check if class has many dependencies
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.getType().isPrimitive())
                .count() > 10;
    }

    private boolean hasInappropriateIntimacy(Class<?> clazz) {
        // Simplified: check for excessive field access to other classes
        return Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(method -> method.getName().startsWith("get") && 
                         !method.getReturnType().getPackageName().equals(clazz.getPackageName()));
    }

    private boolean hasCircularDependency(Class<?> clazz) {
        // Simplified implementation - in real scenario would do deeper analysis
        return false;
    }
}