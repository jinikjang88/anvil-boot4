package com.devsmith.anvil.ch07.repository;

import com.devsmith.anvil.ch07.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> idByEmail = new ConcurrentHashMap<>();

    public User save(User user) {
        byId.put(user.id(), user);
        idByEmail.put(user.email().toLowerCase(), user.id());
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<User> findByEmail(String email) {
        String id = idByEmail.get(email == null ? "" : email.toLowerCase());
        return id == null ? Optional.empty() : findById(id);
    }

    public List<User> findAll() {
        return List.copyOf(byId.values());
    }
}
