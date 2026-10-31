package com.example.demo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Entity
@Table(name = "diary")  // 「diary」がこのエンティティで扱うテーブル名
@Data
public class DiaryEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Integer id; // ID
    
    @Column(length = 10,nullable = false)
    @NotNull(message = "日付を入力してください")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date; // 日付
    
    @Column(length = 20,nullable = false)
    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 20, message = "20文字以下で入力してください")
    private String title; // タイトル
    
    @Column(length = 200,nullable = true)
    @Size(max = 200, message = "200文字以下で入力してください")
    private String memo; // 日記の内容
    
    @Column
    private String category;	// カテゴリ
    
    @Column
    private String[] type;		// タイプ
    
    @Column
    private String filename; // filename
    
}
