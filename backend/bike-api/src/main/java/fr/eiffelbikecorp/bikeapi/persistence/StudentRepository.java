package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
