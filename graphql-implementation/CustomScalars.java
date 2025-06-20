package com.bank.loanmanagement.infrastructure.graphql;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.language.StringValue;

import java.math.BigDecimal;

/**
 * Custom GraphQL scalars for banking domain types
 * Following clean architecture principles
 */
public class CustomScalars {

    public static final GraphQLScalarType Money = GraphQLScalarType.newScalar()
            .name("Money")
            .description("A monetary amount with proper precision for banking operations")
            .coercing(new Coercing<BigDecimal, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof BigDecimal) {
                        return ((BigDecimal) dataFetcherResult).toPlainString();
                    }
                    throw new CoercingSerializeException("Expected BigDecimal but was " + dataFetcherResult.getClass().getSimpleName());
                }

                @Override
                public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    try {
                        return new BigDecimal(input.toString());
                    } catch (NumberFormatException e) {
                        throw new CoercingParseValueException("Invalid money format: " + input);
                    }
                }

                @Override
                public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return new BigDecimal(((StringValue) input).getValue());
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Invalid money format: " + input);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue but was " + input.getClass().getSimpleName());
                }
            })
            .build();

    public static final GraphQLScalarType CreditScore = GraphQLScalarType.newScalar()
            .name("CreditScore")
            .description("Credit score value between 300-850")
            .coercing(new Coercing<Integer, Integer>() {
                @Override
                public Integer serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Integer) {
                        Integer score = (Integer) dataFetcherResult;
                        if (score >= 300 && score <= 850) {
                            return score;
                        }
                        throw new CoercingSerializeException("Credit score must be between 300-850, got: " + score);
                    }
                    throw new CoercingSerializeException("Expected Integer but was " + dataFetcherResult.getClass().getSimpleName());
                }

                @Override
                public Integer parseValue(Object input) throws CoercingParseValueException {
                    try {
                        Integer score = Integer.valueOf(input.toString());
                        if (score >= 300 && score <= 850) {
                            return score;
                        }
                        throw new CoercingParseValueException("Credit score must be between 300-850, got: " + score);
                    } catch (NumberFormatException e) {
                        throw new CoercingParseValueException("Invalid credit score format: " + input);
                    }
                }

                @Override
                public Integer parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof graphql.language.IntValue) {
                        Integer score = ((graphql.language.IntValue) input).getValue().intValue();
                        if (score >= 300 && score <= 850) {
                            return score;
                        }
                        throw new CoercingParseLiteralException("Credit score must be between 300-850, got: " + score);
                    }
                    throw new CoercingParseLiteralException("Expected IntValue but was " + input.getClass().getSimpleName());
                }
            })
            .build();

    public static final GraphQLScalarType Percentage = GraphQLScalarType.newScalar()
            .name("Percentage")
            .description("Percentage value between 0-100")
            .coercing(new Coercing<BigDecimal, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof BigDecimal) {
                        BigDecimal percentage = (BigDecimal) dataFetcherResult;
                        if (percentage.compareTo(BigDecimal.ZERO) >= 0 && percentage.compareTo(new BigDecimal("100")) <= 0) {
                            return percentage.toPlainString();
                        }
                        throw new CoercingSerializeException("Percentage must be between 0-100, got: " + percentage);
                    }
                    throw new CoercingSerializeException("Expected BigDecimal but was " + dataFetcherResult.getClass().getSimpleName());
                }

                @Override
                public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    try {
                        BigDecimal percentage = new BigDecimal(input.toString());
                        if (percentage.compareTo(BigDecimal.ZERO) >= 0 && percentage.compareTo(new BigDecimal("100")) <= 0) {
                            return percentage;
                        }
                        throw new CoercingParseValueException("Percentage must be between 0-100, got: " + percentage);
                    } catch (NumberFormatException e) {
                        throw new CoercingParseValueException("Invalid percentage format: " + input);
                    }
                }

                @Override
                public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            BigDecimal percentage = new BigDecimal(((StringValue) input).getValue());
                            if (percentage.compareTo(BigDecimal.ZERO) >= 0 && percentage.compareTo(new BigDecimal("100")) <= 0) {
                                return percentage;
                            }
                            throw new CoercingParseLiteralException("Percentage must be between 0-100, got: " + percentage);
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Invalid percentage format: " + input);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue but was " + input.getClass().getSimpleName());
                }
            })
            .build();

    public static final GraphQLScalarType PositiveBigDecimal = GraphQLScalarType.newScalar()
            .name("PositiveBigDecimal")
            .description("Positive BigDecimal value greater than zero")
            .coercing(new Coercing<BigDecimal, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof BigDecimal) {
                        BigDecimal value = (BigDecimal) dataFetcherResult;
                        if (value.compareTo(BigDecimal.ZERO) > 0) {
                            return value.toPlainString();
                        }
                        throw new CoercingSerializeException("Value must be positive, got: " + value);
                    }
                    throw new CoercingSerializeException("Expected BigDecimal but was " + dataFetcherResult.getClass().getSimpleName());
                }

                @Override
                public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    try {
                        BigDecimal value = new BigDecimal(input.toString());
                        if (value.compareTo(BigDecimal.ZERO) > 0) {
                            return value;
                        }
                        throw new CoercingParseValueException("Value must be positive, got: " + value);
                    } catch (NumberFormatException e) {
                        throw new CoercingParseValueException("Invalid number format: " + input);
                    }
                }

                @Override
                public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            BigDecimal value = new BigDecimal(((StringValue) input).getValue());
                            if (value.compareTo(BigDecimal.ZERO) > 0) {
                                return value;
                            }
                            throw new CoercingParseLiteralException("Value must be positive, got: " + value);
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Invalid number format: " + input);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue but was " + input.getClass().getSimpleName());
                }
            })
            .build();
}