package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Integer> {
	
	public List<PlayerEntity> findByPlayername(String keyword);
	
	public List<PlayerEntity> findByTeamid(Integer teamid);
	

}
