package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.HodUpdatePartAProjectDTO; // Ensure this DTO exists
import com.cvrce.apraisal.dto.parta.ProjectDTO; // Standard DTO for PartA_Project
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.partA.PartA_Project;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ProjectStatus; // Added based on entity
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.PartA_ProjectRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate; 
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartA_ProjectServiceImplTests {

    @Mock
    private PartA_ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppraisalVersionRepository versionRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PartA_ProjectServiceImpl projectService;

    private UUID projectId;
    private UUID hodUserId;
    private UUID formId;
    private User hodUser;
    private PartA_Project project;
    private AppraisalForm appraisalForm;
    private HodUpdatePartAProjectDTO updateDTO;

    // ArgumentCaptors
    ArgumentCaptor<PartA_Project> projectCaptor = ArgumentCaptor.forClass(PartA_Project.class);
    ArgumentCaptor<AppraisalVersion> versionCaptor = ArgumentCaptor.forClass(AppraisalVersion.class);

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        hodUserId = UUID.randomUUID();
        formId = UUID.randomUUID();

        hodUser = User.builder().id(hodUserId).fullName("HOD ProjectReviewer").build();
        appraisalForm = AppraisalForm.builder().id(formId).status(AppraisalStatus.REUPLOAD_REQUIRED).academicYear("2023-P").build();
        project = PartA_Project.builder()
                .id(projectId)
                .appraisalForm(appraisalForm)
                .projectTitle("Old Project Title")
                .investigators("Old Investigators")
                .fundingAgency("Old Agency")
                .status(ProjectStatus.SUBMITTED)
                .submissionDate(LocalDate.now().minusMonths(2))
                .sanctionedYear(2022)
                .amountSanctioned(50000.0)
                .pointsClaimed(5.0)
                .proofFilePath("/old/path.pdf")
                .build();

        updateDTO = new HodUpdatePartAProjectDTO();
        updateDTO.setProjectTitle("New Project Title by HOD");
        updateDTO.setInvestigators("New Investigators by HOD");
        updateDTO.setFundingAgency("New Agency by HOD");
        updateDTO.setStatus(ProjectStatus.ONGOING);
        updateDTO.setSubmissionDate(LocalDate.now().minusMonths(1));
        updateDTO.setSanctionedYear(2023);
        updateDTO.setAmountSanctioned(75000.0);
        updateDTO.setPointsClaimed(7.5);
        updateDTO.setProofFilePath("/new/path.pdf");
    }

    @Test
    void hodUpdateProject_Success_WhenStatusIsReuploadRequired() throws JsonProcessingException {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(PartA_Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any(AppraisalForm.class))).thenReturn("{\"snapshot\":\"project_dummy\"}");

        // Act
        ProjectDTO result = projectService.hodUpdateProject(projectId, updateDTO, hodUserId);

        // Assert
        assertNotNull(result);
        assertEquals(updateDTO.getProjectTitle(), result.getProjectTitle());
        assertEquals(updateDTO.getInvestigators(), result.getInvestigators());
        assertEquals(updateDTO.getFundingAgency(), result.getFundingAgency());
        assertEquals(updateDTO.getStatus(), result.getStatus());
        assertEquals(updateDTO.getSubmissionDate(), result.getSubmissionDate());
        assertEquals(updateDTO.getSanctionedYear(), result.getSanctionedYear());
        assertEquals(updateDTO.getAmountSanctioned(), result.getAmountSanctioned());
        assertEquals(updateDTO.getPointsClaimed(), result.getPointsClaimed());
        assertEquals(updateDTO.getProofFilePath(), result.getProofFilePath());


        verify(projectRepository).save(projectCaptor.capture());
        PartA_Project savedProject = projectCaptor.getValue();
        assertEquals("New Project Title by HOD", savedProject.getProjectTitle());
        assertEquals("New Investigators by HOD", savedProject.getInvestigators());
        assertEquals(ProjectStatus.ONGOING, savedProject.getStatus());


        verify(versionRepository).save(versionCaptor.capture());
        AppraisalVersion savedVersion = versionCaptor.getValue();
        assertEquals(formId, savedVersion.getAppraisalForm().getId());
        assertTrue(savedVersion.getRemarks().contains("HOD HOD ProjectReviewer modified Part A Project: New Project Title by HOD"));
        assertTrue(savedVersion.getRemarks().contains("Previous status: REUPLOAD_REQUIRED"));
        assertEquals(AppraisalStatus.REUPLOAD_REQUIRED, savedVersion.getStatusAtVersion());
        assertEquals("{\"snapshot\":\"project_dummy\"}", savedVersion.getSerializedSnapshot());
    }

    @Test
    void hodUpdateProject_ThrowsIllegalStateException_WhenStatusNotReuploadRequired() {
        // Arrange
        appraisalForm.setStatus(AppraisalStatus.SUBMITTED); // Not REUPLOAD_REQUIRED
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            projectService.hodUpdateProject(projectId, updateDTO, hodUserId);
        });
        assertTrue(exception.getMessage().contains("HOD can only edit projects when the appraisal form status is REUPLOAD_REQUIRED"));
        verify(projectRepository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }
    
    @Test
    void hodUpdateProject_ThrowsResourceNotFoundException_WhenHodUserNotFound() {
        when(userRepository.findById(hodUserId)).thenReturn(Optional.empty());
        
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            projectService.hodUpdateProject(projectId, updateDTO, hodUserId);
        });
        assertEquals("HOD User not found: " + hodUserId, exception.getMessage());
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void hodUpdateProject_ThrowsResourceNotFoundException_WhenProjectNotFound() {
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            projectService.hodUpdateProject(projectId, updateDTO, hodUserId);
        });
        assertEquals("Project not found: " + projectId, exception.getMessage());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void hodUpdateProject_HandlesJsonProcessingException_WhenSerializingSnapshot() throws JsonProcessingException {
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(PartA_Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any(AppraisalForm.class))).thenThrow(new JsonProcessingException("Test JSON Error") {});

        ProjectDTO result = projectService.hodUpdateProject(projectId, updateDTO, hodUserId);
        
        assertNotNull(result);
        verify(versionRepository).save(versionCaptor.capture());
        AppraisalVersion savedVersion = versionCaptor.getValue();
        assertNull(savedVersion.getSerializedSnapshot());
        assertTrue(savedVersion.getRemarks().contains("HOD HOD ProjectReviewer modified Part A Project: New Project Title by HOD"));
    }
}
