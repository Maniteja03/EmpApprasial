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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
</tbody>
</table>
