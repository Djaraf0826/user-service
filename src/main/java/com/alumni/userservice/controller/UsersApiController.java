package com.alumni.userservice.controller;

import com.alumni.userservice.api.UsersApi;
import com.alumni.userservice.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UsersApiController implements UsersApi {

    private final Map<UUID, User> users = new HashMap<>();

    @Override
    public ResponseEntity<User> createUser(CreateUserRequest createUserRequest) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId(createUserRequest.getKeycloakId());
        user.setEmail(createUserRequest.getEmail());
        user.setFirstName(createUserRequest.getFirstName());
        user.setLastName(createUserRequest.getLastName());
        user.setPhone(createUserRequest.getPhone());
        user.setType(createUserRequest.getType());
        user.setAddress(createUserRequest.getAddress());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        users.put(user.getId(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Override
    public ResponseEntity<UserPage> searchUsers(String name, String email, UserType type, Integer page, Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 20 : size;

        List<User> filtered = users.values().stream()
                .filter(u -> name == null || (u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(name.toLowerCase()))
                .filter(u -> email == null || u.getEmail().equalsIgnoreCase(email))
                .filter(u -> type == null || u.getType() == type)
                .collect(Collectors.toList());

        int fromIndex = Math.min(p * s, filtered.size());
        int toIndex = Math.min(fromIndex + s, filtered.size());
        List<User> pageContent = filtered.subList(fromIndex, toIndex);

        UserPage result = new UserPage();
        result.setContent(pageContent);
        result.setPage(p);
        result.setSize(s);
        result.setTotalElements(filtered.size());
        int totalPages = (int) Math.ceil((double) filtered.size() / s);
        result.setTotalPages(totalPages);
        result.setHasNext(p + 1 < totalPages);
        result.setHasPrevious(p > 0);

        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<User> getUserById(UUID userId) {
        User user = users.get(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<User> updateUser(UUID userId, UpdateUserRequest updateUserRequest) {
        User user = users.get(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (updateUserRequest.getFirstName() != null) user.setFirstName(updateUserRequest.getFirstName());
        if (updateUserRequest.getLastName() != null) user.setLastName(updateUserRequest.getLastName());
        if (updateUserRequest.getPhone() != null) user.setPhone(updateUserRequest.getPhone());
        if (updateUserRequest.getBiography() != null) user.setBiography(updateUserRequest.getBiography());
        if (updateUserRequest.getPhotoUrl() != null) user.setPhotoUrl(updateUserRequest.getPhotoUrl());
        if (updateUserRequest.getAddress() != null) user.setAddress(updateUserRequest.getAddress());
        user.setUpdatedAt(OffsetDateTime.now());
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        if (!users.containsKey(userId)) {
            return ResponseEntity.notFound().build();
        }
        users.remove(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<User> getCurrentUser() {
        // Simplification pour le TP : on renvoie le premier utilisateur en mémoire
        // (en production, l'identité viendrait du token JWT décodé)
        return users.values().stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<User> updateCurrentUser(UpdateUserRequest updateUserRequest) {
        return users.values().stream()
                .findFirst()
                .map(user -> updateUser(user.getId(), updateUserRequest))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}