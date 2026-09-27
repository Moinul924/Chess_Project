package com.chess.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

	
	Page<GameRecord> findByWhiteAccountIdOrBlackAccountIdOrderByPlayedAtDesc(
			Long whiteAccountId, Long blackAccountId, Pageable pageable);
}
