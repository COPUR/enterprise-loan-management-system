package com.bank.loanmanagement.infrastructure.graphql;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration following clean architecture principles
 * Configures custom scalars and runtime wiring
 */
@Configuration
public class GraphQLConfiguration {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.PositiveInt)
                .scalar(ExtendedScalars.Currency)
                .scalar(ExtendedScalars.NonEmptyString)
                .scalar(CustomScalars.Money)
                .scalar(CustomScalars.CreditScore)
                .scalar(CustomScalars.Percentage)
                .scalar(CustomScalars.PositiveBigDecimal);
    }
}