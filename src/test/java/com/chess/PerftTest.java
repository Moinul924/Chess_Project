package com.chess;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.chess.chessMove.Move;
import com.chess.chessMove.PawnPromotionMove;
import com.chess.chessPiece.Piece;
import com.chess.chessPiece.PieceColour;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Perft ("performance test") counts every position reachable at a given depth.
 *
 * The totals below are published, universally agreed reference numbers for the
 * standard starting position. They are only reachable if pins, checks, castling
 * rights, en passant and promotion are ALL generated correctly at once, which is
 * why perft finds move-generation bugs that testing pieces in isolation misses.
 *
 * If a count is wrong, use {@link #perftDivide} to see which root move has the
 * wrong subtree, then re-run divide from that position to walk down to the bug.
 *
 * Note this test lives in package com.chess (not com.chess.Chess_Project, where
 * the other test lives) because Board's constructor is package-private.
 */
class PerftTest {

    private static Board startingPosition() {
        Board board = new Board();
        board.initialisePieces();
        return board;
    }

    /**
     * Every legal move for whoever is to move, found by scanning all 64 squares.
     *
     * This deliberately does NOT use board.locationOfWhitePieces/locationOfBlackPieces:
     * scanning independently means that if those bookkeeping lists ever drift out of
     * sync with the real board, perft still reports the true count and the bug shows up
     * rather than being hidden by the same faulty list on both sides of the comparison.
     */
    private static List<Move> legalMovesForSideToMove(Board board) {
        PieceColour sideToMove = board.currentWhiteTurn ? PieceColour.WHITE : PieceColour.BLACK;
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                BoardSquare square = board.getSquare(row, col);
                if (!square.isOccupied()) {
                    continue;
                }
                Piece piece = square.getPiece();
                if (piece.getColour() != sideToMove) {
                    continue;
                }
                moves.addAll(piece.getLegalMoves(square, board));
            }
        }
        return moves;
    }

    /**
     * Counts leaf positions at the given depth, playing and undoing every move along
     * the way so that Move.execute/undo get exercised just as hard as generation does.
     */
    private static long perft(Board board, int depth) {
        if (depth == 0) {
            return 1L;
        }
        long nodes = 0L;
        for (Move move : legalMovesForSideToMove(board)) {
            board.movePiece(move);
            nodes += perft(board, depth - 1);
            board.UndoMove();
        }
        return nodes;
    }

    /**
     * Perft split by root move. This is the debugging tool: compare its output against
     * any reference engine's "divide" for the same position, find the single move whose
     * count differs, play that move, and repeat one ply deeper. A few rounds of this
     * narrows any move-generation bug down to the exact position that triggers it.
     */
    static void perftDivide(Board board, int depth) {
        long total = 0L;
        for (Move move : legalMovesForSideToMove(board)) {
            board.movePiece(move);
            long nodes = perft(board, depth - 1);
            board.UndoMove();
            total += nodes;
            System.out.println(describe(move) + ": " + nodes);
        }
        System.out.println("total: " + total);
    }

    /** Renders a move as long algebraic coordinates, e.g. "e2e4" or "a7a8q". */
    private static String describe(Move move) {
        String text = square(move.getStartSquare()) + square(move.getEndSquare());
        if (move instanceof PawnPromotionMove promotion) {
            text += promotion.getPromotedPiece().getName().substring(0, 1).toLowerCase();
        }
        return text;
    }

    private static String square(BoardSquare square) {
        // row 0 is rank 8 (black's back rank), so rank counts downward as row increases
        return "" + (char) ('a' + square.getCol()) + (8 - square.getRow());
    }

    @Test
    void perftDepth1() {
        assertThat(perft(startingPosition(), 1)).isEqualTo(20L);
    }

    @Test
    void perftDepth2() {
        assertThat(perft(startingPosition(), 2)).isEqualTo(400L);
    }

    @Test
    void perftDepth3() {
        assertThat(perft(startingPosition(), 3)).isEqualTo(8_902L);
    }

    @Test
    void perftDepth4() {
        assertThat(perft(startingPosition(), 4)).isEqualTo(197_281L);
    }
}
