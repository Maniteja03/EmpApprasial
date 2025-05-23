package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.HodUpdatePartAPublicationDTO;
import com.cvrce.apraisal.dto.parta.PublicationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.entity.User;
// Assuming Department is not directly needed for User.builder() if not set.
// import com.cvrce.apraisal.entity.Department; 
import com.cvrce.apraisal.entity.partA.PartA_Publication;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.PublicationType;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
// import com.cvrce.apraisal.repo.AppraisalFormRepository; // Not directly used by service, but form is fetched via entity
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.PartA_PublicationRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
// import org.mockito.Spy; // Changed to @Mock as per instruction
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartA_PublicationServiceImplTests {

    @Mock
    private PartA_PublicationRepository publicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppraisalVersionRepository versionRepository;
    
    @Mock // Changed from @Spy to @Mock as per instruction
    private ObjectMapper objectMapper; 

    @InjectMocks
    private PartA_PublicationServiceImpl publicationService;

    private UUID publicationId;
    private UUID hodUserId;
    private UUID formId;
    private User hodUser;
    private PartA_Publication publication;
    private AppraisalForm appraisalForm;
    private HodUpdatePartAPublicationDTO updateDTO;
    
    // ArgumentCaptor for detailed verification - declared as class members
    ArgumentCaptor<PartA_Publication> publicationCaptor = ArgumentCaptor.forClass(PartA_Publication.class);
    ArgumentCaptor<AppraisalVersion> versionCaptor = ArgumentCaptor.forClass(AppraisalVersion.class);

    @BeforeEach
    void setUp() {
        publicationId = UUID.randomUUID();
        hodUserId = UUID.randomUUID();
        formId = UUID.randomUUID();

        hodUser = User.builder().id(hodUserId).fullName("HOD Name").build();
        
        // Simple AppraisalForm for testing snapshot creation target.
        // If specific fields from AppraisalForm are used in versionRemark beyond ID, mock them here.
        appraisalForm = AppraisalForm.builder().id(formId).status(AppraisalStatus.REUPLOAD_REQUIRED).academicYear("2023-24").build();
        
        publication = PartA_Publication.builder()
                .id(publicationId)
                .appraisalForm(appraisalForm)
                .title("Old Title")
                .authors("Old Authors")
                .build();

        updateDTO = new HodUpdatePartAPublicationDTO();
        updateDTO.setTitle("New Updated Title by HOD");
        updateDTO.setAuthors("New Authors by HOD");
        updateDTO.setPublicationType(PublicationType.JOURNAL_INTERNATIONAL);
        updateDTO.setPublicationDate(LocalDate.now().minusMonths(1));
        updateDTO.setOrcidId("1234-5678-9012-3456");
        updateDTO.setDoiNumber("doi/test");
        updateDTO.setCvrAuthorCount(1);
        updateDTO.setIndexedInScopusDate(LocalDate.now().minusDays(10));
        updateDTO.setPointsClaimed(10.0);
        updateDTO.setProofFilePath("/path/to/proof.pdf");
    }

    @Test
    void hodUpdatePublication_Success_WhenStatusIsReuploadRequired() throws JsonProcessingException {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(publicationRepository.save(any(PartA_Publication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any(AppraisalForm.class))).thenReturn("{\"snapshot\":\"dummy\"}"); // Mocked as per instruction

        // Act
        PublicationDTO result = publicationService.hodUpdatePublication(publicationId, updateDTO, hodUserId);

        // Assert
        assertNotNull(result);
        assertEquals(updateDTO.getTitle(), result.getTitle());
        assertEquals(updateDTO.getAuthors(), result.getAuthors());
        assertEquals(updateDTO.getPublicationType(), result.getPublicationType());
        assertEquals(updateDTO.getPublicationDate(), result.getPublicationDate());
        assertEquals(updateDTO.getPointsClaimed(), result.getPointsClaimed());


        verify(publicationRepository).save(publicationCaptor.capture());
        PartA_Publication savedPublication = publicationCaptor.getValue();
        assertEquals("New Updated Title by HOD", savedPublication.getTitle());
        assertEquals("New Authors by HOD", savedPublication.getAuthors());
        assertEquals(PublicationType.JOURNAL_INTERNATIONAL, savedPublication.getPublicationType());


        verify(versionRepository).save(versionCaptor.capture());
        AppraisalVersion savedVersion = versionCaptor.getValue();
        assertEquals(formId, savedVersion.getAppraisalForm().getId());
        assertTrue(savedVersion.getRemarks().contains("HOD HOD Name modified Part A Publication: New Updated Title by HOD"));
        assertTrue(savedVersion.getRemarks().contains("Previous status: REUPLOAD_REQUIRED"));
        assertEquals(AppraisalStatus.REUPLOAD_REQUIRED, savedVersion.getStatusAtVersion());
        assertEquals("{\"snapshot\":\"dummy\"}", savedVersion.getSerializedSnapshot()); 
    }

    @Test
    void hodUpdatePublication_ThrowsIllegalStateException_WhenStatusNotReuploadRequired() {
        // Arrange
        appraisalForm.setStatus(AppraisalStatus.SUBMITTED); // Not REUPLOAD_REQUIRED
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            publicationService.hodUpdatePublication(publicationId, updateDTO, hodUserId);
        });
        assertTrue(exception.getMessage().contains("HOD can only edit publications when the appraisal form status is REUPLOAD_REQUIRED"));
        verify(publicationRepository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }
    
    @Test
    void hodUpdatePublication_ThrowsResourceNotFoundException_WhenHodUserNotFound() {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            publicationService.hodUpdatePublication(publicationId, updateDTO, hodUserId);
        });
        assertEquals("HOD User not found: " + hodUserId, exception.getMessage());
        verify(publicationRepository, never()).findById(any());
    }

    @Test
    void hodUpdatePublication_ThrowsResourceNotFoundException_WhenPublicationNotFound() {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            publicationService.hodUpdatePublication(publicationId, updateDTO, hodUserId);
        });
        assertEquals("Publication not found: " + publicationId, exception.getMessage());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void hodUpdatePublication_HandlesJsonProcessingException_WhenSerializingSnapshot() throws JsonProcessingException {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(publicationRepository.save(any(PartA_Publication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any(AppraisalForm.class))).thenThrow(new JsonProcessingException("Test Error") {});

        // Act
        PublicationDTO result = publicationService.hodUpdatePublication(publicationId, updateDTO, hodUserId);

        // Assert
        assertNotNull(result); // Should still succeed in updating publication
        verify(versionRepository).save(versionCaptor.capture());
        AppraisalVersion savedVersion = versionCaptor.getValue();
        assertNull(savedVersion.getSerializedSnapshot()); // Snapshot should be null due to exception
        assertTrue(savedVersion.getRemarks().contains("HOD HOD Name modified Part A Publication: New Updated Title by HOD"));
    }
}
