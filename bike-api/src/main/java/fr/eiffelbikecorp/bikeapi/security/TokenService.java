package fr.eiffelbikecorp.bikeapi.security;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.enums.CustomerType;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenService {

    @Autowired
    StudentRepository studentRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    EiffelBikeCorpRepository eiffelBikeCorpRepository;

    private static final String SECRET_KEY_STRING = "s3cr3tK3yF0rJwTg3n3rati0nAndV4lidati0nInEiff3lBik3C0rpAppl1cat10n";

    public String generateToken(Customer user) {
        String userType = CustomerType.SILVER.name();
        if (studentRepository.existsById(user.getId())
                || employeeRepository.existsById(user.getId())
                || eiffelBikeCorpRepository.existsById(user.getId())) {
            userType = CustomerType.PROVIDER.name();
        }
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiration = now.plus(4, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("type", userType)
                .claim("fullName", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    public UUID validateAndGetUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
        try {
            String userIdString = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return UUID.fromString(userIdString);
        } catch (Exception e) {
            return null;
        }
    }
}