package com.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import com.chess.chessMove.*;
import com.chess.chessPiece.PieceColour;


public class Engine {
    
    private Board board;    
    public int Checkmate = 100000000;
    private Move BestMove;
    private int MaxDepth = 4;
    int HighestDepth = 0;
    int PossitionEvaluated = 0;
    int numOfsimilarepossition = 0;
    

    public Engine(Board board) {
        this.board = board;
    }

    public List<Move> generateAllCurrentLegalMoves() {
        List<Move> allCurrentLegalMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                BoardSquare square = board.getSquare(row, col);
                if (square.isOccupied() && ((board.currentWhiteTurn && square.getPiece().getColour() == PieceColour.WHITE) ||
                 (!board.currentWhiteTurn && square.getPiece().getColour() == PieceColour.BLACK))) {
                    allCurrentLegalMoves.addAll(square.getPiece().getLegalMoves(square, board , true));
                }
            }
        }
        allCurrentLegalMoves = reorderMovesBestToWorst(allCurrentLegalMoves);
        return allCurrentLegalMoves;
    }

    public Move getRandomMove() {
        List<Move> allCurrentLegalMoves = generateAllCurrentLegalMoves();
        for(Move move : allCurrentLegalMoves) {
            if(move instanceof PawnPromotionMove || move instanceof CastlingMove || move instanceof EnPassantMove) {
                return move; 
            }
        }
        if (!allCurrentLegalMoves.isEmpty()) {
            int randomIndex = (int) (Math.random() * allCurrentLegalMoves.size());
            return allCurrentLegalMoves.get(randomIndex);
        }
        return null; // No legal moves available
    }

    public Move getMoveUsingMinMax(int depth) {
        MaxDepth = depth;
        PossitionEvaluated = 0;
        int result = MinMax(depth, Integer.MIN_VALUE, Integer.MAX_VALUE, board.currentWhiteTurn);
        System.out.println("Positions evaluated: " + PossitionEvaluated);
        System.out.println("Best move evaluation: " + result);
        return BestMove;
    }



    public List<Move> reorderMovesBestToWorst(List<Move> allCurrentLegalMoves) {
        allCurrentLegalMoves.sort((move1, move2) -> {
        return Boolean.compare(move2.goodCapture(),move1.goodCapture()); 
        });
        return allCurrentLegalMoves;
    }


    public int MinMax(int depth,int alpha,int beta,boolean isMaximizingPlayer){
        if(depth == 0 || board.CheckMate || board.StaleMate){
            PossitionEvaluated++; 
            return evaluateBoard(isMaximizingPlayer, depth);
        }
        List<Move> allCurrentLegalMoves = generateAllCurrentLegalMoves();
        if(isMaximizingPlayer){
            int maxEval = Integer.MIN_VALUE;
            for(Move move : allCurrentLegalMoves){
                move.execute(board  );
                //printBoard();
                int eval = MinMax(depth - 1,alpha,beta,false);
                move.undo(board);
                if(depth == MaxDepth && eval > maxEval){ // Update the best move at the root level 
                    BestMove = move;                                              
                }
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if(beta <= alpha){
                    break;
                }
            }
            return maxEval;
        }else{
            int minEval = Integer.MAX_VALUE;
            for(Move move : allCurrentLegalMoves){
                move.execute(board);
                //printBoard();
                int eval = MinMax(depth - 1,alpha,beta,true);
                move.undo(board);
                if(depth == MaxDepth && eval < minEval){
                    BestMove = move;
                }
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if(beta <= alpha){
                    break;
                }
            }
            return minEval;
        }
    }

    public int evaluateBoard(boolean isMaximizingPlayer, int depth){
        int finalEvaluation = 0;
        //printBoard();
        if(board.CheckMate){
            return isMaximizingPlayer ? -Checkmate *(depth-1): Checkmate *(depth-1);
        }
        if(board.StaleMate){
            return 0;
        }
        finalEvaluation += countMaterial();



        return finalEvaluation;
    }

    public int countMaterial(){
        int whiteMaterialValue = 0;
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                BoardSquare square = board.getSquare(row, col);
                if(square.isOccupied() && square.getPiece().getColour() == PieceColour.WHITE){
                    whiteMaterialValue += square.getPiece().getPieceValue();
                }
            }
        }
        int blackMaterialValue = 0;
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                BoardSquare square = board.getSquare(row, col);
                if(square.isOccupied() && square.getPiece().getColour() == PieceColour.BLACK){
                    blackMaterialValue += square.getPiece().getPieceValue();
                }
            }
        }


        return whiteMaterialValue - blackMaterialValue;
    }


    public void printBoard() {
        System.out.println("--------------------------");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                BoardSquare square = board.getSquare(row, col);
                if (square.isOccupied()) {
                    System.out.print(square.getPiece().getName().charAt(0) + " ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }





}