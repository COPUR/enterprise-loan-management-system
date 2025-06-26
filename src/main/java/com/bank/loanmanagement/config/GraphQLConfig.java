package com.bank.loanmanagement.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL Configuration for Enterprise Banking System
 * Configures extended scalars for Date, DateTime, BigDecimal, and JSON types
 */
@Configuration
@Profile({"enterprise", "docker", "default"})
public class GraphQLConfig {

    /**
     * Configure GraphQL runtime wiring with extended scalars
     * Fixes scalar implementation errors for banking data types
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                // Date and DateTime scalars for loan dates, creation dates, etc.
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.LocalTime)
                
                // Numeric scalars for banking amounts and calculations
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLBigInteger)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.PositiveInt)
                .scalar(ExtendedScalars.NonNegativeInt)
                
                // JSON scalar for flexible data structures
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.Object)
                
                // Additional useful scalars for banking
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.Url)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.Currency)
                .scalar(ExtendedScalars.CountryCode)
                
                // Custom banking data resolvers can be added here
                .build();
    }
}