package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.user.UserBasicInfoDTO;
import com.cvrce.apraisal.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.HashSet; // Added for UserCreationRequestDTO
import com.cvrce.apraisal.dto.user.UserCreationRequestDTO; // Added

import static org.mockito.ArgumentMatchers.any; // Added for any()
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // Added for post()
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; // Added for CSRF
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper; // For comparing JSON response

    @Test
    @WithMockUser(username = "hoduser", authorities = {"HOD"})
    void testGetHodDepartmentStaff_AsHod_ReturnsStaffListAndOk() throws Exception {
        // Arrange
        UserBasicInfoDTO staff1 = UserBasicInfoDTO.builder().userId(UUID.randomUUID()).employeeId("S001").fullName("Staff One").email("s1@example.com").dateOfJoining(LocalDate.now()).build();
        UserBasicInfoDTO staff2 = UserBasicInfoDTO.builder().userId(UUID.randomUUID()).employeeId("S002").fullName("Staff Two").email("s2@example.com").dateOfJoining(LocalDate.now()).build();
        List<UserBasicInfoDTO> staffList = Arrays.asList(staff1, staff2);

        when(userService.getStaffByAuthenticatedHodDepartment()).thenReturn(staffList);

        // Act & Assert
        mockMvc.perform(get("/api/users/hod/department/staff")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(staffList)));
    }
    
    @Test
    @WithMockUser(username = "hoduser", authorities = {"HOD"})
    void testGetHodDepartmentStaff_AsHod_ServiceReturnsEmptyList_ReturnsEmptyListAndOk() throws Exception {
        // Arrange
        when(userService.getStaffByAuthenticatedHodDepartment()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/users/hod/department/staff")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(Collections.emptyList())));
    }


    @Test
    @WithMockUser(username = "staffuser", authorities = {"STAFF"})
    void testGetHodDepartmentStaff_AsNonHod_ReturnsForbidden() throws Exception {
        // Arrange
        // No need to mock userService as the request should be denied before calling it.

        // Act & Assert
        mockMvc.perform(get("/api/users/hod/department/staff")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetHodDepartmentStaff_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Arrange
        // No @WithMockUser, so unauthenticated.

        // Act & Assert
        mockMvc.perform(get("/api/users/hod/department/staff")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Or .isForbidden() depending on global config
                                                     // Spring Security typically returns 401 if no auth at all,
                                                     // and 403 if authenticated but wrong authority.
    }

    @Test
    @WithMockUser(username = "hoduser", authorities = {"HOD"})
    void testGetHodDepartmentStaff_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(userService.getStaffByAuthenticatedHodDepartment()).thenThrow(new RuntimeException("Service layer error"));

        // Act & Assert
        mockMvc.perform(get("/api/users/hod/department/staff")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
                // Depending on global exception handling, the response body might also be checked.
                // For now, just checking status 500.
    }

    // --- Tests for UserController.createUserBySuperAdmin() ---

    @Test
    @WithMockUser(username = "superadmin", authorities = {"SUPER_ADMIN"})
    void testCreateUserBySuperAdmin_ValidRequest_ReturnsCreatedUser() throws Exception {
        // Arrange
        UserCreationRequestDTO requestDTO = UserCreationRequestDTO.builder()
                .fullName("Test User")
                .email("test.user@example.com")
                .employeeId("EMPTEST")
                .departmentId(1L)
                .roleNames(new HashSet<>(Collections.singletonList("STAFF")))
                .build();

        UserBasicInfoDTO responseDTO = UserBasicInfoDTO.builder()
                .userId(UUID.randomUUID())
                .fullName(requestDTO.getFullName())
                .email(requestDTO.getEmail())
                .employeeId(requestDTO.getEmployeeId())
                .dateOfJoining(LocalDate.now())
                .build();

        when(userService.createUser(any(UserCreationRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/users/superadmin/create").with(csrf()) // Added CSRF for POST
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated()) // Expect 201 CREATED
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(responseDTO.getUserId().toString()))
                .andExpect(jsonPath("$.fullName").value(responseDTO.getFullName()))
                .andExpect(jsonPath("$.email").value(responseDTO.getEmail()));
    }

    @Test
    @WithMockUser(username = "hoduser", authorities = {"HOD"}) // Non-SUPER_ADMIN user
    void testCreateUserBySuperAdmin_NonSuperAdmin_ReturnsForbidden() throws Exception {
        // Arrange
        UserCreationRequestDTO requestDTO = UserCreationRequestDTO.builder().build(); // Content doesn't matter much

        // Act & Assert
        mockMvc.perform(post("/api/users/superadmin/create").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden()); // Expect 403 FORBIDDEN
    }

    @Test
    @WithMockUser(username = "superadmin", authorities = {"SUPER_ADMIN"})
    void testCreateUserBySuperAdmin_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        // Arrange
        UserCreationRequestDTO invalidRequestDTO = UserCreationRequestDTO.builder()
                .fullName("") // Invalid: FullName is @NotBlank
                .email("notanemail") // Invalid: Email format
                .employeeId(null) // Invalid: EmployeeId is @NotBlank
                // departmentId and roleNames also have constraints
                .build();
        
        // Note: @Valid in the controller method triggers bean validation.
        // We don't need to mock userService.createUser for this as validation should fail before.

        // Act & Assert
        mockMvc.perform(post("/api/users/superadmin/create").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
                .andExpect(status().isBadRequest()); // Expect 400 BAD REQUEST
    }
}
</tbody>
</table>
