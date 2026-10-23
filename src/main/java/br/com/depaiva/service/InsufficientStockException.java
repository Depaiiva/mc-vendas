package br.com.depaiva.service;

public class InsufficientStockException extends IllegalArgumentException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
