package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "player")
@Data
public class PlayerEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Integer playerid; // ID
    
    @Column
    private String playername;
    
    @Column
    private String type;
    
    @Column
    private Integer teamid;
    
    
    
}
