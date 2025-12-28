package fr.eiffelbikecorp.bikeapi.security;

import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.domain.enums.CustomerType;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
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
        String userType = CustomerType.ORDINARY.name();
        if (studentRepository.existsById(user.getId())) {
            userType = ProviderType.STUDENT.name();
        } else if (employeeRepository.existsById(user.getId())) {
            userType = ProviderType.EMPLOYEE.name();
        } else if (eiffelBikeCorpRepository.existsById(user.getId())) {
            userType = ProviderType.EIFFEL_BIKE_CORP.name();
        }
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
        AuthenticatedUser u = validateAndGetUser(token);
        return (u == null) ? null : u.userId();
    }

    public AuthenticatedUser validateAndGetUser(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String userIdString = claims.getSubject();
            String type = claims.get("type", String.class);
            if (userIdString == null || userIdString.isBlank()) return null;
            if (type == null || type.isBlank()) return null;
            return new AuthenticatedUser(UUID.fromString(userIdString), type);
        } catch (Exception e) {
            return null;
        }
    }
}