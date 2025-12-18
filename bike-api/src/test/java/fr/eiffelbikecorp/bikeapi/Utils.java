package fr.eiffelbikecorp.bikeapi;

public class Utils {
    public static String randomEmail() {
        return "user_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
