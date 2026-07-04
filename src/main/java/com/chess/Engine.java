package com.chess;

import java.util.ArrayList;
import java.util.List;

import com.chess.chessMove.*;
import com.chess.chessPiece.*;


public class Engine {
    
    private Board board;    
    public int Checkmate = 100000000;
    private Move BestMove;
    private int MaxDepth = 8;
    int HighestDepth = 0;
    int PossitionEvaluated = 0;
    int numOfsimilarepossition = 0;

    public int[][] Knight_PieceSquareTables_MG = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-20,  5, 20, 15, 15, 20,  5,-20},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    public int[][] Bishop_PieceSquareTables_MG = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10, -5, -5, -5, -5, -5, -5,-10},
        {-20,-30,-30,-30,-30,-30,-30,-20}
    };

    public int[][] Rook_PieceSquareTables_MG = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        { 5, 10, 10, 10, 10, 10, 10,  5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0, -5,-10,-10,-5 , -5, -5},
        {-5,-10,-10,-10,-10,-10,-10, -5},
        { 0,  0,  3,  5,  5,  3,  0,  0}
    };

    public int[][] Queen_PieceSquareTables_MG = {
        { -20,-10,-10, -5, -5,-10,-10,-20 },
        { -10,-10,-10,-10,-10,-10,-10,-10 },
        { -10, -5, -5, -5, -5, -5, -5,-10 },
        {  -5, -5, -5, -5, -5, -5, -5, -5 },
        {   0,  0,  3,  3,  3,  3,  0,  0 },
        { -10,  3,  3,  3,  3,  3,  3, -10},
        { -10,  0,  5,  5,  5,  5,  0, -10},
        { -20,-10,-10, 20, 20,-10,-10, -20}
    };

    public  int[][] Queen_PieceSquareTables_EG = {
        {  0,  0,   0,  0,   0,   0,   0,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  5,   5,  5,   5,   5,   5,  0 },
        {  0,  0,   0,  0,   0,   0,   0,  0 }
    };

    public int[][] King_PieceSquareTables_MG = {
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -20,-30,-30,-40,-40,-30,-30,-20 },
        { -10,-20,-20,-20,-20,-20,-20,-10 },
        {   5,  5, -5, -5, -5, -5,  5,  5 },
        {  15, 30, 10,  0,  0, 10, 30, 15 },
    };

    public int[][] King_PieceSquareTables_EG = {
        { -50,-40,-30,-20,-20,-30,-40,-50 },
        { -30,-20,-10,  0,  0,-10,-20,-30 },
        { -30,-10, 20, 30, 30, 20,-10,-30 },
        { -30,-10, 30, 40, 40, 30,-10,-30 },
        { -30,-10, 30, 40, 40, 30,-10,-30 },
        { -30,-10, 20, 30, 30, 20,-10,-30 },
        { -30,-30,  0,  0,  0,  0,-30,-30 },
        { -50,-30,-30,-30,-30,-30,-30,-50 }
    };

    public int[][] Pawn_PieceSquareTables_MG = {
        { 99, 99, 99, 99, 99, 99, 99, 99 },
        { 50, 50, 50, 50, 50, 50, 50, 50 },
        { 10, 10, 20, 30, 30, 20, 10, 10 },
        {  5,  5, 10, 27, 27, 10,  5,  5 },
        { 0,   0,  0, 30, 30,  0,  0,  0 },
        {10,  15,-10, 10, 10,-10, 15, 10 },
        { 3,  15, 15,-20,-20, 15, 15,  3 },
        { 0,   0,  0,  0,  0,  0,  0,  0 },
    };

    public int[][] Pawn_PieceSquareTables_EG ={
        { 0, 0, 0, 0, 0, 0, 0, 0},
        {20,20,20,20,20,20,20,20},
        {20,20,20,20,20,20,20,20},
        {17,17,17,17,17,17,17,17},
        {14,14,14,14,14,14,14,14},
        {10,10,10,10,10,10,10,10},
        {10,10,10,10,10,10,10,10},
        { 0, 0, 0, 0, 0, 0, 0, 0},
    };



    

    public Engine(Board board) {
        this.board = board;
    }

    public List<Move> generateAllCurrentLegalMoves() {
        List<Move> allCurrentLegalMoves = new ArrayList<>();
        if(board.currentWhiteTurn){
            for(BoardSquare square : board.locationOfWhitePieces){
                List<Move> pieceLegalMoves = square.getPiece().getLegalMoves(square, board,true);
                allCurrentLegalMoves.addAll(pieceLegalMoves);
            }
        }
        else{
            for(BoardSquare square : board.locationOfBlackPieces){
                List<Move> pieceLegalMoves = square.getPiece().getLegalMoves(square, board,true);
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
        if(allCurrentCaptureMoves.isEmpty() || depthLevel  == MaxDepth){
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
        double endgameWeighting = getEndgameWeighting();


        int materialAdvantage = countMaterial();
        finalEvaluation += materialAdvantage;

        if (materialAdvantage > 0) {
            // White is winning, reward White for approaching the Black King
            finalEvaluation += forceKingToCornerEndgameEval(board.WhiteKingSquareLocation, board.BlackKingSquareLocation, endgameWeighting);
        } else if (materialAdvantage < 0) {
            // Black is winning, reward Black for approaching the White King
            finalEvaluation -= forceKingToCornerEndgameEval(board.BlackKingSquareLocation, board.WhiteKingSquareLocation, endgameWeighting);
        }
        

        finalEvaluation += getPieceSquareEvals(endgameWeighting);


        return finalEvaluation;
    }

    public double forceKingToCornerEndgameEval(int[] kingSquare, int[] opponentKingSquare,double endgameWeighting) {
        
        // Calculate the distance of the opponent king from the center of the board
        int evaluation = 0;
        int colDistanceFromCenter = Math.max(3-opponentKingSquare[1], opponentKingSquare[1]-4);
        int rowDistanceFromCenter = Math.max(3-opponentKingSquare[0], opponentKingSquare[0]-4);
        evaluation += (colDistanceFromCenter + rowDistanceFromCenter);
    
        
        // Calculate the distance between the two kings so that the evaluation is higher when the kings are closer together
        int colDistanceBetweenKings = Math.abs(kingSquare[1] - opponentKingSquare[1]);
        int rowDistanceBetweenKings = Math.abs(kingSquare[0] - opponentKingSquare[0]);
        int distanceBetweenKings = colDistanceBetweenKings + rowDistanceBetweenKings;
        evaluation += 14 - distanceBetweenKings;                // 14 is the max distance between kings on the board

        return (int) (evaluation*endgameWeighting);
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

    public double getEndgameWeighting(){
        double currentPhaseValue = 0;
        for(BoardSquare square : board.locationOfWhitePieces){
            currentPhaseValue += getPiecePhaseValue(square.getPiece());
        }
        for(BoardSquare square : board.locationOfBlackPieces){
            currentPhaseValue += getPiecePhaseValue(square.getPiece());
        }
        currentPhaseValue = Math.min(currentPhaseValue, 24); // if the player adds more pieces to the board, the phase value will be capped at 24
        return 1.0 - currentPhaseValue / 24.0;
    }


    public int getPiecePhaseValue(Piece piece){
        if(piece instanceof Pawn){
            return 0;
        }
        else if(piece instanceof Knight || piece instanceof Bishop){
            return 1;
        }
        else if(piece instanceof Rook){
            return 2;
        }
        else if(piece instanceof Queen){
            return 4;
        }
        return 0;
    }


    public int getPieceSquareEvals(double endgameWeighting){
        int whiteEval = 0;
        int blackEval = 0;
        for(BoardSquare square : board.locationOfWhitePieces){
            whiteEval += getValueForPieceSquareTable(square,square.getPiece(),endgameWeighting);
        }
        for(BoardSquare square : board.locationOfBlackPieces){
            blackEval += getValueForPieceSquareTable(square,square.getPiece(),endgameWeighting);
        }
        return whiteEval - blackEval;


    }


    public int getValueForPieceSquareTable(BoardSquare square,Piece piece,double endgameEval){
        int row = square.getRow();
        int col = square.getCol();
        if(piece.getColour() == PieceColour.WHITE){
            row = 7 - row;
            col = 7 - col;
        }
        switch (piece.getName()) {
            case "Pawn":
                if(endgameEval > 0.7){
                    return Pawn_PieceSquareTables_EG[row][col];
                }
                return Pawn_PieceSquareTables_MG[row][col];
            case "Bishop":
                return Bishop_PieceSquareTables_MG[row][col];
                
            case "Knight":
                return Knight_PieceSquareTables_MG[row][col];
            
            case "Rook":
                return Rook_PieceSquareTables_MG[row][col];
            case "Queen":   
            
                if(endgameEval > 0.7){
                    return Queen_PieceSquareTables_EG[row][col];
                }
                return Queen_PieceSquareTables_MG[row][col];
            case "King":
                if(endgameEval > 0.7){
                    return King_PieceSquareTables_EG[row][col];
                }
                return King_PieceSquareTables_MG[row][col];    
            default:
                return 0;
        }
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