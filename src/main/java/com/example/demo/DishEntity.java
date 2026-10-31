package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "dish")
@Data
public class DishEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Integer dishid; // ID
    
    @Column
    private String dishname;
    
    @Column
    private String type;
    
    
}
