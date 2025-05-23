package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.EventDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBEventDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartB_EventService {
    EventDTO addEvent(EventDTO dto);
    List<EventDTO> getEventsByFormId(UUID formId);
    EventDTO updateEvent(UUID id, EventDTO dto);
    void deleteEvent(UUID id);
    EventDTO hodUpdateEvent(UUID eventId, HodUpdatePartBEventDTO dto, UUID hodUserId); // Added method
}
