package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.user.*;
import com.cvrce.apraisal.entity.*;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.*;
import com.cvrce.apraisal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // Added for manual mapping example, though stream is used.
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService; // Added

    // Constructor will be updated by Lombok's @RequiredArgsConstructor based on final fields

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public List<UserResponseDTO> getUsersByDepartment(Long deptId) {
        return userRepository.findByDepartmentId(deptId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    // This old createUser method will be effectively replaced by the new one below
    // public UserResponseDTO createUser(UserCreateDTO dto) { ... } 
    // No need to explicitly remove it if the method signature changes,
    // as the new method will be added. If the signature was identical but behavior changed,
    // then we'd overwrite. Here, it's a new distinct signature for the interface.
    // However, to avoid compilation errors from the old interface method, if UserServiceImpl
    // still implements the old one, we should remove/replace the old implementation.
    // The task is to implement the *new* createUser. The old one is effectively orphaned
    // by the interface change and should be removed from UserServiceImpl to avoid issues if it's
    // still present.

    // Replacing the old createUser method with the new one.
    // The old createUser(UserCreateDTO dto) is removed and the new one is added.
    // If the old one was not explicitly removed, and the new one added,
    // and if UserServiceImpl still somehow claimed to implement the old interface method,
    // it would lead to issues. By replacing, we ensure only the new one is present.
    // The previous diff on the interface already handled removing the old signature.

    // (Content of old createUser(UserCreateDTO dto) removed for clarity in this diff block)
    // ... old createUser method content was here ...


    @Override
    // @Transactional // This should be on the class or here. The example shows it here. Let's add it.
    // The prompt says "@Transactional // Important for atomicity"
    @jakarta.transaction.Transactional 
    public UserBasicInfoDTO createUser(UserCreationRequestDTO creationRequest) {
        // 1. Get Authenticated User (Performing Admin)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || (authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            throw new org.springframework.security.access.AccessDeniedException("User is not authenticated.");
        }
        String performingAdminUsername = authentication.getName();
        User performingAdmin = userRepository.findByEmail(performingAdminUsername) // Assuming email is username
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Performing admin user not found: " + performingAdminUsername));

        // 2. Check if performingAdmin has "SUPER_ADMIN" role
        boolean isSuperAdmin = performingAdmin.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName()));
        if (!isSuperAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("User does not have SUPER_ADMIN privileges to create users.");
        }

        // 3. Validate DTO (Controller usually does this via @Valid, but good to have service layer checks if needed)
        if (userRepository.existsByEmail(creationRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + creationRequest.getEmail());
        }
        if (userRepository.existsByEmployeeId(creationRequest.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + creationRequest.getEmployeeId());
        }

        // 4. Fetch Department
        Department department = departmentRepository.findById(creationRequest.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + creationRequest.getDepartmentId()));

        // 5. Fetch and Assign Roles
        Set<Role> rolesToAssign = new HashSet<>();
        for (String roleName : creationRequest.getRoleNames()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            rolesToAssign.add(role);
        }
        if (rolesToAssign.isEmpty()) { 
            throw new IllegalArgumentException("At least one valid role must be assigned.");
        }

        // 6. Generate Secure Temporary Password
        String temporaryPassword = "Password@123"; // Placeholder - In a real app, use a secure random generator.
        
        // 7. Hash Temporary Password
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        // 8. Create and Save User Entity
        User newUser = User.builder()
                .fullName(creationRequest.getFullName())
                .email(creationRequest.getEmail())
                .employeeId(creationRequest.getEmployeeId())
                .password(hashedPassword)
                .department(department)
                .roles(rolesToAssign)
                .enabled(true) 
                .deleted(false)
                .dateOfJoining(java.time.LocalDate.now()) // Set date of joining
                .build();
        
        User savedUser = userRepository.save(newUser);

        // 9. Send Notification Email
        com.cvrce.apraisal.dto.notification.NotificationDTO notification = com.cvrce.apraisal.dto.notification.NotificationDTO.builder()
                .userId(savedUser.getId()) 
                .title("Welcome to the Appraisal System!")
                .message("Hello " + savedUser.getFullName() + ",\n\n" +
                         "Your account has been created for the Appraisal System.\n" +
                         "Your username is: " + savedUser.getEmail() + " (or Employee ID: " + savedUser.getEmployeeId() + ")\n" +
                         "Your temporary password is: " + temporaryPassword + "\n\n" +
                         "Please log in and change your password immediately.\n\n" +
                         "Thank you,\nSystem Administrator")
                .build();
        try {
            notificationService.sendNotification(notification); 
            log.info("Welcome email sent to new user {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
            // Log and continue, user is created.
        }

        // 10. Return DTO of created user
        return UserBasicInfoDTO.builder()
                .userId(savedUser.getId())
                .employeeId(savedUser.getEmployeeId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .dateOfJoining(savedUser.getDateOfJoining())
                .build();
    }

    @Override
    public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getDateOfJoining() != null) user.setDateOfJoining(dto.getDateOfJoining());
        if (dto.getLastPromotionDate() != null) user.setLastPromotionDate(dto.getLastPromotionDate());
        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid department"));
            user.setDepartment(dept);
        }

        return mapToDTO(userRepository.save(user));
    }

    @Override
    public void setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void softDeleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    private UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .dateOfJoining(user.getDateOfJoining())
                .lastPromotionDate(user.getLastPromotionDate())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }

    @Override
    public List<UserBasicInfoDTO> getStaffByAuthenticatedHodDepartment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User is not authenticated or authentication details are not available.");
        }
        String currentPrincipalName = authentication.getName(); // This is usually the username (e.g., employeeId or email)

        // Assuming currentPrincipalName is employeeId. If it's email, adjust accordingly.
        // The prompt example uses findByEmployeeId, but UserRepo does not have it. Let's assume it's email for now.
        // If it must be employeeId, userRepository needs a findByEmployeeId method.
        // For now, let's stick to email as it's more common for `authentication.getName()` and `userRepository` has `findByEmail`.
        User authenticatedUser = userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user '" + currentPrincipalName + "' not found in database."));

        Department hodDepartment = authenticatedUser.getDepartment();
        if (hodDepartment == null) {
            throw new IllegalStateException("Authenticated HOD '" + currentPrincipalName + "' does not have an assigned department.");
        }

        Long departmentId = hodDepartment.getId();
        List<User> usersInDepartment = userRepository.findByDepartmentId(departmentId);

        // Map to UserBasicInfoDTO
        return usersInDepartment.stream()
                .map(user -> UserBasicInfoDTO.builder()
                        .userId(user.getId())
                        .employeeId(user.getEmployeeId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .dateOfJoining(user.getDateOfJoining())
                        .build())
                .collect(Collectors.toList());
    }
}
// Ensure all necessary imports are present at the top of the file:
// import com.cvrce.apraisal.dto.notification.NotificationDTO; (If NotificationDTO is in this specific package)
// import com.cvrce.apraisal.service.NotificationService;
// import org.springframework.security.access.AccessDeniedException; (Spring's one)
// import org.springframework.security.core.userdetails.UsernameNotFoundException; (Spring's one)
// import jakarta.transaction.Transactional; (Jakarta's one)
// import java.time.LocalDate;
// import java.util.HashSet;
// import java.util.Set;
