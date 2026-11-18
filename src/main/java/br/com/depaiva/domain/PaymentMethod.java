package br.com.depaiva.domain;

public enum PaymentMethod {
    PIX,
    CARD,
    CASH;

    public String getDisplayName() {
        return switch (this) {
            case PIX -> "Pix";
            case CARD -> "Cartão";
            case CASH -> "Dinheiro";
        };
    }
}
