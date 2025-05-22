package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        log.info("Creating new role: {}", name);
        try {
            return new ResponseEntity<>(roleService.createRole(name), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }


    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        log.info("Fetching all roles");
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}
