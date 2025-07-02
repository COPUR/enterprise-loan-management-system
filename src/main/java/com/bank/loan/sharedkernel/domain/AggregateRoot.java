package com.bank.loan.sharedkernel.domain;

public abstract class AggregateRoot<T> {

    private T id;

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }
}
