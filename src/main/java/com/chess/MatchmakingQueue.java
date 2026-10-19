package com.chess;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.chess.account.AccountService;

// Players waiting for an online opponent. The first two who are searching get paired together.
// Every method is synchronized: many players can join or leave at the same moment, and two
// of them must never both grab the same waiting opponent.
@Component
public class MatchmakingQueue {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingQueue.class);

    // A waiting browser checks in every 1.5 seconds. If we haven't heard from someone for this long,
    // they closed the tab or lost connection, so they're dropped instead of being paired with a real player.
    private static final Duration GONE_AFTER = Duration.ofSeconds(10);

    // What the browser gets back: WAITING, MATCHED (with the game id) or NOT_SEARCHING.
    // accountId says who the server thinks is asking. All tabs in a browser share one login, so if
    // someone signs in as a different account in another tab, this tab sees the id change and stops.
    public record QueueStatus(String status, String gameId, Long accountId) {
        static QueueStatus waiting(Long me)             { return new QueueStatus("WAITING", null, me); }
        static QueueStatus matched(String id, Long me)  { return new QueueStatus("MATCHED", id, me); }
        static QueueStatus notSearching(Long me)        { return new QueueStatus("NOT_SEARCHING", null, me); }
    }

    // account id -> when we last heard from them. LinkedHashMap keeps arrival order, so the
    // player who has waited longest is paired first.
    private final LinkedHashMap<Long, Instant> waiting = new LinkedHashMap<>();

    // account id -> the game they were paired into, kept until their browser picks it up.
    private final Map<Long, String> matchedGames = new HashMap<>();

    private final AccountService accounts;

    MatchmakingQueue(AccountService accounts) {
        this.accounts = accounts;
    }

    // "I want to play." Pairs the caller with the longest-waiting player if there is one,
    // otherwise puts them in the queue. startGame(a, b) creates the game and returns its id.
    synchronized QueueStatus join(Long me, BiFunction<Long, Long, String> startGame) {
        String alreadyMatched = matchedGames.remove(me);
        if (alreadyMatched != null) {
            return QueueStatus.matched(alreadyMatched, me);
        }

        removePlayersWhoLeft();

        Long opponent = longestWaitingExcept(me);
        if (opponent != null) {
            waiting.remove(opponent);
            waiting.remove(me);
            String gameId = startGame.apply(opponent, me);
            matchedGames.put(opponent, gameId);   // they find out on their next check-in
            log.info("Matchmaking: paired {} with {} in game {}. Players still waiting: {}",
                     name(opponent), name(me), gameId, waiting.size());
            return QueueStatus.matched(gameId, me);
        }

        if (waiting.put(me, Instant.now()) == null) {
            log.info("Matchmaking: {} is looking for an online game. Players waiting: {}", name(me), waiting.size());
        }
        return QueueStatus.waiting(me);
    }

    // A waiting browser checking in: "have I been paired yet?"
    synchronized QueueStatus check(Long me) {
        String gameId = matchedGames.remove(me);
        if (gameId != null) {
            return QueueStatus.matched(gameId, me);
        }
        if (waiting.containsKey(me)) {
            waiting.put(me, Instant.now());   // still here
            return QueueStatus.waiting(me);
        }
        return QueueStatus.notSearching(me);
    }

    // The player clicked Cancel or left the page.
    public synchronized void leave(Long me) {
        boolean wasWaiting = waiting.remove(me) != null;
        matchedGames.remove(me);
        if (wasWaiting) {
            log.info("Matchmaking: {} stopped searching. Players waiting: {}", name(me), waiting.size());
        }
    }

    private Long longestWaitingExcept(Long me) {
        for (Long accountId : waiting.keySet()) {
            if (!accountId.equals(me)) return accountId;
        }
        return null;
    }

    private void removePlayersWhoLeft() {
        Instant cutoff = Instant.now().minus(GONE_AFTER);
        Iterator<Map.Entry<Long, Instant>> entries = waiting.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Long, Instant> entry = entries.next();
            if (entry.getValue().isBefore(cutoff)) {
                entries.remove();
                log.info("Matchmaking: {} timed out of the queue (no check-in for {}s)",
                         name(entry.getKey()), GONE_AFTER.toSeconds());
            }
        }
    }

    private String name(Long accountId) {
        return accounts.displayNameOf(accountId) + " (#" + accountId + ")";
    }
}
