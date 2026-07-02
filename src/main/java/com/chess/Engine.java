package com.chess;

import java.util.ArrayList;
import java.util.List;

import com.chess.chessMove.*;


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
        if(board.currentWhiteTurn){
            for(BoardSquare square : board.locationOfWhitePieces){
                List<Move> pieceLegalMoves = square.getPiece().getLegalMoves(square, board);
                allCurrentLegalMoves.addAll(pieceLegalMoves);
            }
        }
        else{
            for(BoardSquare square : board.locationOfBlackPieces){
                List<Move> pieceLegalMoves = square.getPiece().getLegalMoves(square, board);
                allCurrentLegalMoves.addAll(pieceLegalMoves);
            }
        }
        allCurrentLegalMoves = reorderMovesBestToWorst(allCurrentLegalMoves);
        return allCurrentLegalMoves;
    }

    public List<Move> generateAllCaptureMoves(){
        List<Move> allCurrentLegalMoves =  generateAllCurrentLegalMoves();
        allCurrentLegalMoves.removeIf(move -> !move.isCaptureMove());
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
        int result = MinMax(depth, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, board.currentWhiteTurn);
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


    public int MinMax(int depth,int depthLevel,int alpha,int beta,boolean isMaximizingPlayer){  
        if(depth == 0 || board.CheckMate || board.StaleMate){
            PossitionEvaluated++; 
            if(board.CheckMate){
                return isMaximizingPlayer ? -(Checkmate-depthLevel ): (Checkmate - depthLevel); //depthLevel is added to find the fastest way to checkmate
            }
            if(board.StaleMate){
                return 0;
            }
            return searchAllCaptureMoves(depthLevel,alpha,beta,isMaximizingPlayer);
        }
        List<Move> allCurrentLegalMoves = generateAllCurrentLegalMoves();
        if(isMaximizingPlayer){
            int maxEval = Integer.MIN_VALUE;
            for(Move move : allCurrentLegalMoves){
                board.movePiece(move);
                //printBoard();
                int eval = MinMax(depth - 1,depthLevel+1,alpha,beta,false);
                board.UndoMove();
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
                board.movePiece(move);
                //printBoard();
                int eval = MinMax(depth - 1,depthLevel+1,alpha,beta,true);
                board.UndoMove();
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

    public int searchAllCaptureMoves(int depthLevel,int alpha,int beta,boolean isMaximizingPlayer){
        if(board.CheckMate){
            PossitionEvaluated++; 
            return isMaximizingPlayer ? -(Checkmate - depthLevel) : (Checkmate - depthLevel);
        }
        if(board.StaleMate){
            PossitionEvaluated++;
            return 0;
        }
        List<Move> allCurrentCaptureMoves = generateAllCaptureMoves();
        if(allCurrentCaptureMoves.isEmpty()){
            PossitionEvaluated++; 
            return evaluateBoard(isMaximizingPlayer);
        }
        if(isMaximizingPlayer){
            int maxEval = Integer.MIN_VALUE;
            for(Move move : allCurrentCaptureMoves){
                board.movePiece(move);
                int eval = searchAllCaptureMoves(depthLevel+1,alpha,beta,false);
                board.UndoMove();
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if(beta <= alpha){
                    break;
                }
            }
            return maxEval;
        }else{
            int minEval = Integer.MAX_VALUE;
            for(Move move : allCurrentCaptureMoves){
                board.movePiece(move);
                int eval = searchAllCaptureMoves(depthLevel+1,alpha,beta,true);
                board.UndoMove();
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if(beta <= alpha){
                    break;
                }
            }
            return minEval;
        }
    }

    public int evaluateBoard(boolean isMaximizingPlayer){
        int finalEvaluation = 0;
        //printBoard();
        
        finalEvaluation += countMaterial();



        return finalEvaluation;
    }

    public int countMaterial(){
        int whiteMaterialValue = 0;
        for(BoardSquare square : board.locationOfWhitePieces){
            whiteMaterialValue += square.getPiece().getPieceValue();
        }
        int blackMaterialValue = 0;
        for(BoardSquare square : board.locationOfBlackPieces){
            blackMaterialValue += square.getPiece().getPieceValue();
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