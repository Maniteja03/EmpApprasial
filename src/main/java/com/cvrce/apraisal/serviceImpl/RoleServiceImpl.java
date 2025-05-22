package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role createRole(String name) {
        if (roleRepository.existsByName(name)) {
            throw new IllegalStateException("Role already exists");
        }
        Role role = new Role();
        role.setName(name);
        return roleRepository.save(role);
    }


    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
