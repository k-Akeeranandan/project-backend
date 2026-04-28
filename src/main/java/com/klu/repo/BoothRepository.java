package com.klu.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klu.entity.Booth;

@Repository
public interface BoothRepository extends JpaRepository<Booth, Long> {
}