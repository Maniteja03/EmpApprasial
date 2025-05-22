package com.cvrce.apraisal.service;

import com.cvrce.apraisal.entity.Role;

import java.util.List;

public interface RoleService {
    Role createRole(String name);
    List<Role> getAllRoles();
}
