package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<DiaryEntity, Integer> {
	
	// titleカラムを引数のキーワードであいまい検索をする
	public List<DiaryEntity> findByTitleLike(String keyword);
	
	public List<DiaryEntity> findByDateBetween(LocalDate since, LocalDate until);
	
	
	// titleカラムの昇順で検索
	public List<DiaryEntity> findByTitleNotNullOrderByTitleAsc();
	public List<DiaryEntity> findAllByOrderByTitleAsc();	// findAllOrderByTitleAsc()ではダメ、Byが必要
	
	// titleカラムの降順で検索
	public List<DiaryEntity> findByTitleNotNullOrderByTitleDesc();
	public List<DiaryEntity> findAllByOrderByTitleDesc();

}
