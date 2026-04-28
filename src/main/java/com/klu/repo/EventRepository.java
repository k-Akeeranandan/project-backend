package com.klu.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klu.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
	
}

