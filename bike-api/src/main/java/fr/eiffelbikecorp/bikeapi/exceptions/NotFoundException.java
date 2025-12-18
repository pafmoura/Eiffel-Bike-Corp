package fr.eiffelbikecorp.bikeapi.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
