package com.chess.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {

	List<RatingHistory> findByAccountIdOrderByRecordedAtAsc(Long accountId);
}
