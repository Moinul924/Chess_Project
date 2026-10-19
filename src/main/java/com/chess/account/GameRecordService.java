package com.chess.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameRecordService {
    private final GameRecordRepository games;

    public GameRecordService(GameRecordRepository games) {
        this.games = games;
    }

    // Saves one finished game. A null account id means that side was the engine.
    @Transactional
    public GameRecord recordFinishedGame(Long whiteAccountId, Long blackAccountId, OpponentType opponentType,
                                         GameResult result, GameTermination termination) {
        GameRecord record = new GameRecord();
        record.setWhiteAccountId(whiteAccountId);
        record.setBlackAccountId(blackAccountId);
        record.setOpponentType(opponentType);
        record.setResult(result);
        record.setTermination(termination);
        // Not stored yet. Both columns are NOT NULL in V1, so they get empty strings for now.
        record.setMoveListSan("");
        record.setFinalFen("");
        return games.save(record);
    }
}
