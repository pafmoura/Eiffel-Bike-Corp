package fr.eiffelbikecorp.bikeapi.persistence;

import fr.eiffelbikecorp.bikeapi.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}

