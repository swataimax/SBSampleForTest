package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<TeamEntity, Integer> {
	
	public List<TeamEntity> findByTeamname(String keyword);
	
	

}
