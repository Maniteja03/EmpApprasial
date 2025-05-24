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
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserServiceImpl userService;

    private User hodUser;
    private Department hodDepartment;
    private final String hodEmail = "hod@example.com";
    private final Long hodDepartmentId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        hodDepartment = Department.builder().id(hodDepartmentId).name("HOD Department").build();
        hodUser = User.builder()
                .id(UUID.randomUUID())
                .email(hodEmail)
                .fullName("HOD User")
                .department(hodDepartment)
                .employeeId("HOD001")
                .dateOfJoining(LocalDate.now().minusYears(5))
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
}
</tbody>
</table>
