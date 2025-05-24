package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.user.*;
import com.cvrce.apraisal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added for @PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching profile for user: {}", email);
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        log.info("Fetching user by ID: {}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<UserResponseDTO>> getUsersByDepartment(@PathVariable Long deptId) {
        log.info("Listing users from department ID: {}", deptId);
        return ResponseEntity.ok(userService.getUsersByDepartment(deptId));
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserCreateDTO dto) {
        log.info("Creating new user with email: {}", dto.getEmail());
        return new ResponseEntity<>(userService.createUser(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID id, @RequestBody UserUpdateDTO dto) {
        log.info("Updating user: {}", id);
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PostMapping("/enable/{id}")
    public ResponseEntity<String> toggleUserStatus(@PathVariable UUID id, @RequestParam boolean enable) {
        log.info("Toggling enabled status for user {} => {}", id, enable);
        userService.setUserEnabled(id, enable);
        return ResponseEntity.accepted().body("User status updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> softDeleteUser(@PathVariable UUID id) {
        log.info("Soft deleting user {}", id);
        userService.softDeleteUser(id);
        return ResponseEntity.ok("User deleted (soft)");
    }

    @GetMapping("/hod/department/staff")
    @PreAuthorize("hasAuthority('HOD')")
    public ResponseEntity<List<UserBasicInfoDTO>> getHodDepartmentStaff() {
        log.info("HOD fetching staff list for their department.");
        List<UserBasicInfoDTO> staffList = userService.getStaffByAuthenticatedHodDepartment();
        return ResponseEntity.ok(staffList);
    }
}
