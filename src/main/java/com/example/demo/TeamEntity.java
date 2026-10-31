package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "team")
@Data
public class TeamEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Integer teamid; // ID
    
    @Column
    private String teamname;
    
    @Column
    private String country;
    
    
}
