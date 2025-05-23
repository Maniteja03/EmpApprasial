package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.EventDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_Event;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_EventRepository;
import com.cvrce.apraisal.service.PartB_EventService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBEventDTO; // Added
import com.cvrce.apraisal.entity.AppraisalVersion; // Added
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import com.fasterxml.jackson.core.JsonProcessingException; // Added
import java.time.LocalDateTime; // Added

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_EventServiceImpl implements PartB_EventService {

    private final PartB_EventRepository eventRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public EventDTO addEvent(EventDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        PartB_Event event = PartB_Event.builder()
                .appraisalForm(form)
                .eventTitle(dto.getEventTitle())
                .organization(dto.getOrganization())
                .role(dto.getRole())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .eventType(dto.getEventType())
                .venue(dto.getVenue())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        event = eventRepo.save(event);
        log.info("Event added with ID {}", event.getId());
        return mapToDTO(event);
    }

    @Override
    public List<EventDTO> getEventsByFormId(UUID formId) {
        return eventRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventDTO updateEvent(UUID id, EventDTO dto) {
        PartB_Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        event.setEventTitle(dto.getEventTitle());
        event.setOrganization(dto.getOrganization());
        event.setRole(dto.getRole());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setEventType(dto.getEventType());
        event.setVenue(dto.getVenue());
        event.setPointsClaimed(dto.getPointsClaimed());
        event.setProofFilePath(dto.getProofFilePath());

        event = eventRepo.save(event);
        log.info("Event updated: {}", id);
        return mapToDTO(event);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID id) {
        if (!eventRepo.existsById(id)) {
            throw new ResourceNotFoundException("Event not found");
        }
        eventRepo.deleteById(id);
        log.info("Event deleted: {}", id);
    }

    private EventDTO mapToDTO(PartB_Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setAppraisalFormId(event.getAppraisalForm().getId());
        dto.setEventTitle(event.getEventTitle());
        dto.setOrganization(event.getOrganization());
        dto.setRole(event.getRole());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setEventType(event.getEventType());
        dto.setVenue(event.getVenue());
        dto.setPointsClaimed(event.getPointsClaimed());
        dto.setProofFilePath(event.getProofFilePath());
        return dto;
    }

    @Override
    @Transactional
    public EventDTO hodUpdateEvent(UUID eventId, HodUpdatePartBEventDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        AppraisalForm form = event.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit events when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        event.setEventType(dto.getEventType());
        event.setEventTitle(dto.getEventTitle());
        event.setRole(dto.getRole());
        event.setOrganization(dto.getOrganization());
        event.setVenue(dto.getVenue());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setPointsClaimed(dto.getPointsClaimed());
        event.setProofFilePath(dto.getProofFilePath());
        
        PartB_Event updatedEvent = eventRepo.save(event);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Event: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedEvent.getEventTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of event: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated event {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, eventId, form.getId());
        return mapToDTO(updatedEvent);
    }
}
