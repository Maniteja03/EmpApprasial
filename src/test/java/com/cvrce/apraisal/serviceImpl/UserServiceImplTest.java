package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.user.UserBasicInfoDTO;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.DepartmentRepository;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository; // Dependency of UserServiceImpl
    @Mock
    private DepartmentRepository departmentRepository; // Dependency of UserServiceImpl
    @Mock
    private PasswordEncoder passwordEncoder; // Dependency of UserServiceImpl
    @Mock
    private NotificationService notificationService; // Added for createUser tests

    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserServiceImpl userService;

    private User hodUser; // Used for getStaffByAuthenticatedHodDepartment tests
    private User superAdminUser; // For createUser tests
    private Department hodDepartment;
    private final String hodEmail = "hod@example.com"; // For getStaffByAuthenticatedHodDepartment tests
    private final String superAdminEmail = "superadmin@example.com";
    private final Long hodDepartmentId = 1L;
    private Department testDepartment;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // For getStaffByAuthenticatedHodDepartment tests
        testDepartment = Department.builder().id(hodDepartmentId).name("HOD Department").build();
        hodUser = User.builder()
                .id(UUID.randomUUID())
                .email(hodEmail)
                .fullName("HOD User")
                .department(testDepartment)
                .employeeId("HOD001")
                .dateOfJoining(LocalDate.now().minusYears(5))
                .build();
        
        // For createUser tests
        com.cvrce.apraisal.entity.Role superAdminRole = com.cvrce.apraisal.entity.Role.builder().id(1L).name("SUPER_ADMIN").build();
        superAdminUser = User.builder()
                .id(UUID.randomUUID())
                .email(superAdminEmail)
                .fullName("Super Admin User")
                .roles(Collections.singleton(superAdminRole))
                .build();
    }

    private UserCreationRequestDTO createSampleUserCreationRequestDTO() {
        return UserCreationRequestDTO.builder()
                .fullName("New User")
                .email("newuser@example.com")
                .employeeId("EMP001")
                .departmentId(hodDepartmentId) // Using hodDepartmentId for simplicity
                .roleNames(Collections.singleton("STAFF"))
                .build();
    }

    @Test
    void testGetStaffByAuthenticatedHodDepartment_ValidHod_ReturnsStaffListDTOs() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(hodEmail);
        when(userRepository.findByEmail(hodEmail)).thenReturn(Optional.of(hodUser));

        User staff1 = User.builder().id(UUID.randomUUID()).employeeId("S001").fullName("Staff One").email("s1@example.com").dateOfJoining(LocalDate.now()).department(hodDepartment).build();
        User staff2 = User.builder().id(UUID.randomUUID()).employeeId("S002").fullName("Staff Two").email("s2@example.com").dateOfJoining(LocalDate.now()).department(hodDepartment).build();
        // The HOD themselves will also be in this list if they belong to the department
        List<User> usersInDepartment = Arrays.asList(hodUser, staff1, staff2);
        when(userRepository.findByDepartmentId(hodDepartmentId)).thenReturn(usersInDepartment);

        // Act
        List<UserBasicInfoDTO> result = userService.getStaffByAuthenticatedHodDepartment();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        UserBasicInfoDTO hodDto = result.stream().filter(dto -> dto.getEmployeeId().equals("HOD001")).findFirst().orElse(null);
        assertNotNull(hodDto);
        assertEquals(hodUser.getFullName(), hodDto.getFullName());
        assertEquals(hodUser.getEmail(), hodDto.getEmail());

        verify(userRepository).findByEmail(hodEmail);
        verify(userRepository).findByDepartmentId(hodDepartmentId);
    }

    @Test
    void testGetStaffByAuthenticatedHodDepartment_UserNotAuthenticated_ThrowsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);
        // or when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.getStaffByAuthenticatedHodDepartment();
        });
        assertEquals("User is not authenticated or authentication details are not available.", exception.getMessage());
    }
    
    @Test
    void testGetStaffByAuthenticatedHodDepartment_AnonymousUser_ThrowsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true); // Authenticated but as anonymous
        when(authentication.getPrincipal()).thenReturn("anonymousUser");


        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.getStaffByAuthenticatedHodDepartment();
        });
        assertEquals("User is not authenticated or authentication details are not available.", exception.getMessage());
    }


    @Test
    void testGetStaffByAuthenticatedHodDepartment_AuthenticatedUserNotFoundInDb_ThrowsException() {
        // Arrange
        String nonExistentEmail = "unknown@example.com";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(nonExistentEmail);
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getStaffByAuthenticatedHodDepartment();
        });
        assertEquals("Authenticated user '" + nonExistentEmail + "' not found in database.", exception.getMessage());
    }

    @Test
    void testGetStaffByAuthenticatedHodDepartment_HodHasNoDepartment_ThrowsIllegalStateException() {
        // Arrange
        hodUser.setDepartment(null); // HOD has no department
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(hodEmail);
        when(userRepository.findByEmail(hodEmail)).thenReturn(Optional.of(hodUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.getStaffByAuthenticatedHodDepartment();
        });
        assertEquals("Authenticated HOD '" + hodEmail + "' does not have an assigned department.", exception.getMessage());
    }

    @Test
    void testGetStaffByAuthenticatedHodDepartment_DepartmentHasNoOtherStaff_ReturnsJustHod() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(hodEmail);
        when(userRepository.findByEmail(hodEmail)).thenReturn(Optional.of(hodUser));

        // Department only contains the HOD
        List<User> usersInDepartment = Collections.singletonList(hodUser);
        when(userRepository.findByDepartmentId(hodDepartmentId)).thenReturn(usersInDepartment);

        // Act
        List<UserBasicInfoDTO> result = userService.getStaffByAuthenticatedHodDepartment();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(hodUser.getEmployeeId(), result.get(0).getEmployeeId());
        assertEquals(hodUser.getFullName(), result.get(0).getFullName());
    }
    
    @Test
    void testGetStaffByAuthenticatedHodDepartment_DepartmentIsEmpty_ReturnsEmptyList() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(hodEmail);
        when(userRepository.findByEmail(hodEmail)).thenReturn(Optional.of(hodUser));

        // Department is empty (realistically HOD should be there, but testing edge case of repo method)
        List<User> usersInDepartment = Collections.emptyList();
        when(userRepository.findByDepartmentId(hodDepartmentId)).thenReturn(usersInDepartment);

        // Act
        List<UserBasicInfoDTO> result = userService.getStaffByAuthenticatedHodDepartment();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- Tests for UserServiceImpl.createUser() ---

    @Test
    void testCreateUser_BySuperAdmin_Successful() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));

        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByEmployeeId(requestDTO.getEmployeeId())).thenReturn(false);
        when(departmentRepository.findById(requestDTO.getDepartmentId())).thenReturn(Optional.of(testDepartment));
        com.cvrce.apraisal.entity.Role staffRole = com.cvrce.apraisal.entity.Role.builder().id(2L).name("STAFF").build();
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName(requestDTO.getFullName())
                .email(requestDTO.getEmail())
                .employeeId(requestDTO.getEmployeeId())
                .department(testDepartment)
                .roles(Collections.singleton(staffRole))
                .password("hashedPassword")
                .dateOfJoining(LocalDate.now())
                .enabled(true)
                .deleted(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        UserBasicInfoDTO resultDTO = userService.createUser(requestDTO);

        // Assert
        assertNotNull(resultDTO);
        assertEquals(savedUser.getId(), resultDTO.getUserId());
        assertEquals(requestDTO.getFullName(), resultDTO.getFullName());
        assertEquals(requestDTO.getEmail(), resultDTO.getEmail());
        assertEquals(requestDTO.getEmployeeId(), resultDTO.getEmployeeId());
        assertEquals(LocalDate.now(), resultDTO.getDateOfJoining());

        verify(userRepository).save(any(User.class));
        
        ArgumentCaptor<com.cvrce.apraisal.dto.notification.NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(com.cvrce.apraisal.dto.notification.NotificationDTO.class);
        verify(notificationService).sendNotification(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().getMessage().contains("Your temporary password is: Password@123"));
    }

    @Test
    void testCreateUser_ByNonSuperAdmin_ThrowsAccessDeniedException() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        User nonAdminUser = User.builder().id(UUID.randomUUID()).email("nonadmin@example.com").roles(Collections.emptySet()).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonadmin@example.com");
        when(userRepository.findByEmail("nonadmin@example.com")).thenReturn(Optional.of(nonAdminUser));

        // Act & Assert
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            userService.createUser(requestDTO);
        });
    }

    @Test
    void testCreateUser_EmailAlreadyExists_ThrowsIllegalArgumentException() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(requestDTO);
        });
        assertEquals("Email already exists: " + requestDTO.getEmail(), exception.getMessage());
    }

    @Test
    void testCreateUser_EmployeeIdAlreadyExists_ThrowsIllegalArgumentException() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false); // Email is fine
        when(userRepository.existsByEmployeeId(requestDTO.getEmployeeId())).thenReturn(true); // EmployeeId exists

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(requestDTO);
        });
        assertEquals("Employee ID already exists: " + requestDTO.getEmployeeId(), exception.getMessage());
    }

    @Test
    void testCreateUser_DepartmentNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByEmployeeId(requestDTO.getEmployeeId())).thenReturn(false);
        when(departmentRepository.findById(requestDTO.getDepartmentId())).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.createUser(requestDTO);
        });
        assertEquals("Department not found with ID: " + requestDTO.getDepartmentId(), exception.getMessage());
    }

    @Test
    void testCreateUser_RoleNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO(); // Requests "STAFF" role
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByEmployeeId(requestDTO.getEmployeeId())).thenReturn(false);
        when(departmentRepository.findById(requestDTO.getDepartmentId())).thenReturn(Optional.of(testDepartment));
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.empty()); // Role "STAFF" not found

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.createUser(requestDTO);
        });
        assertEquals("Role not found: STAFF", exception.getMessage());
    }

    @Test
    void testCreateUser_NotificationFailure_UserStillCreated() {
        // Arrange
        UserCreationRequestDTO requestDTO = createSampleUserCreationRequestDTO();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminEmail);
        when(userRepository.findByEmail(superAdminEmail)).thenReturn(Optional.of(superAdminUser));
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByEmployeeId(requestDTO.getEmployeeId())).thenReturn(false);
        when(departmentRepository.findById(requestDTO.getDepartmentId())).thenReturn(Optional.of(testDepartment));
        com.cvrce.apraisal.entity.Role staffRole = com.cvrce.apraisal.entity.Role.builder().id(2L).name("STAFF").build();
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("hashedPassword");

        User savedUser = User.builder() // Same as successful creation
                .id(UUID.randomUUID()).fullName(requestDTO.getFullName()).email(requestDTO.getEmail())
                .employeeId(requestDTO.getEmployeeId()).department(testDepartment)
                .roles(Collections.singleton(staffRole)).password("hashedPassword")
                .dateOfJoining(LocalDate.now()).enabled(true).deleted(false).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Mock notificationService to throw an exception
        doThrow(new RuntimeException("Simulated Notification Service Error"))
            .when(notificationService).sendNotification(any(com.cvrce.apraisal.dto.notification.NotificationDTO.class));
        
        // Act
        UserBasicInfoDTO resultDTO = userService.createUser(requestDTO);

        // Assert
        assertNotNull(resultDTO); // User creation should still succeed
        assertEquals(savedUser.getId(), resultDTO.getUserId());
        verify(userRepository).save(any(User.class)); // Ensure user was saved
        verify(notificationService).sendNotification(any(com.cvrce.apraisal.dto.notification.NotificationDTO.class)); // Verify it was called
        // Further log verification is tricky without specific setup, but the successful DTO return is key.
    }
}
</tbody>
</table>
