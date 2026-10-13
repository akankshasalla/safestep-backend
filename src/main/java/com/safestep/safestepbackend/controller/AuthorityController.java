package com.safestep.safestepbackend.controller;

import com.safestep.safestepbackend.model.Authority;
import com.safestep.safestepbackend.repository.AuthorityRepository;
import com.safestep.safestepbackend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/authority")
@CrossOrigin(origins = "*")
public class AuthorityController {

    private final AuthorityRepository repository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthorityController(AuthorityRepository repository, JwtUtil jwtUtil) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return repository.findByUsername(body.get("username"))
                .filter(a -> passwordMatches(a, body.get("password")))
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(Map.of(
                        "authorityId", a.getAuthorityId(),
                        "name", a.getName(),
                        "department", a.getDepartment() == null ? "" : a.getDepartment(),
                        "token", jwtUtil.createToken(a.getUsername()))))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Invalid username or password")));
    }

    private boolean passwordMatches(Authority a, String raw) {
        if (raw == null) return false;
        String stored = a.getPassword();
        if (stored.startsWith("$2")) {
            return encoder.matches(raw, stored);
        }
        if (stored.equals(raw)) {
            a.setPassword(encoder.encode(raw));
            repository.save(a);
            return true;
        }
        return false;
    }
}