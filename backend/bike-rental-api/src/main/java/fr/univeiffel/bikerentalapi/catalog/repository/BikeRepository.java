package fr.univeiffel.bikerentalapi.catalog.repository;

import fr.univeiffel.bikerentalapi.catalog.model.Bike;
import fr.univeiffel.bikerentalapi.catalog.persistence.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class BikeRepository {

    public Bike create(String description) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Bike bike = new Bike(description);
            em.persist(bike);
            em.getTransaction().commit();
            return bike;
        } finally {
            em.close();
        }
    }

    public Optional<Bike> findById(Long id) {
        EntityManager em = JpaUtil.em();
        try {
            return Optional.ofNullable(em.find(Bike.class, id));
        } finally {
            em.close();
        }
    }

    public List<Bike> findAll() {
        EntityManager em = JpaUtil.em();
        try {
            return em.createQuery("select b from Bike b order by b.id", Bike.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Optional<Bike> update(Long id, String description) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Bike bike = em.find(Bike.class, id);
            if (bike == null) {
                em.getTransaction().rollback();
                return Optional.empty();
            }
            bike.setDescription(description);
            em.getTransaction().commit();
            return Optional.of(bike);
        } finally {
            em.close();
        }
    }

    public boolean delete(Long id) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Bike bike = em.find(Bike.class, id);
            if (bike == null) {
                em.getTransaction().rollback();
                return false;
            }
            em.remove(bike);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }
}
