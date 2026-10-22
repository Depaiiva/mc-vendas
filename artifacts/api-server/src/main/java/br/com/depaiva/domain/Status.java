package br.com.depaiva.domain;

public enum Status {
    PENDING,
    CONFIRMED,
    DELIVERED,
    CANCELED;

    public boolean canTransitionTo(Status next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELED;
            case CONFIRMED -> next == DELIVERED || next == CANCELED;
            case DELIVERED -> next == CANCELED;
            case CANCELED -> false;
        };
    }
}
