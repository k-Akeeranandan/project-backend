package com.klu.service;

import com.klu.dto.EventDto;
import com.klu.entity.Event;
import com.klu.exception.ResourceNotFoundException;
import com.klu.repo.EventRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository repo;

    @Autowired
    private ModelMapper mapper;

    public EventDto create(EventDto dto){
        Event event = mapper.map(dto, Event.class);
        Event saved = repo.save(event);

        return mapper.map(saved, EventDto.class);
    }

    public List<EventDto> getAll(){
        return repo.findAll()
                .stream()
                .map(event -> mapper.map(event, EventDto.class))
                .collect(Collectors.toList());
    }

    public void deleteEvent(Long eventId) {
        Event event = repo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        repo.delete(event);
    }
}