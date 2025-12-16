package fr.eiffelbikecorp.bikeapi.domain;

import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@ToString(callSuper = true)
@Entity
public class Employee extends Customer {
}
