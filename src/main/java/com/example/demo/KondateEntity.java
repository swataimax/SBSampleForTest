package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "kondate")
@Data
public class KondateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column
	private Integer kondateid;

	@Column(length = 10, nullable = false)
	private LocalDate date; // 日付

	@Column
	private String memo;

	//    @Column
	//    private Integer[] dishids;
	@Column
	private List<Integer> dishids;

}
