package fr.univeiffel.bikerentalapi.catalog.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static volatile EntityManagerFactory emf;

    private JpaUtil() {
    }

    public static EntityManager em() {
        return emf().createEntityManager();
    }

    private static EntityManagerFactory emf() {
        if (emf == null) {
            synchronized (JpaUtil.class) {
                if (emf == null) {
                    Map<String, Object> props = new HashMap<>();
                    putIfPresent(props, "jakarta.persistence.jdbc.url", "DB_URL");
                    putIfPresent(props, "jakarta.persistence.jdbc.user", "DB_USER");
                    putIfPresent(props, "jakarta.persistence.jdbc.password", "DB_PASSWORD");
                    emf = Persistence.createEntityManagerFactory("ugeBikePU", props);
                }
            }
        }
        return emf;
    }

    private static void putIfPresent(Map<String, Object> props, String jpaKey, String name) {
        String v = System.getProperty(name);
        if (v == null || v.isBlank()) v = System.getenv(name);
        if (v != null && !v.isBlank()) props.put(jpaKey, v);
    }

    public static void shutdown() {
        EntityManagerFactory ref = emf;
        if (ref != null) ref.close();
    }
}
