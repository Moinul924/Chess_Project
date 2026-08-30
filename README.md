# Chess Engine built with min-max algorithm 

## A chess engine built in Java and Spring Boot with opening book theory, capable of playing chess at an intermediate level.

![Screenshot of the chess Board](./screenshot/ChessBoardImage.png)

This project is a chess engine that uses the minimax algorithm and alpha-beta pruning to evaluate future moves. The project can calculate legal moves for each piece and use minimax to explore all permutations of future moves that can be played after a specific move. This project has many other  features.

* **FEN STRING** Load standard FEN (Forsyth-Edwards Notation) string which will set up a specific board state.
* **UNDO BUTTON** A move can be undone and played from the previous position
*  **FLIP BOARD** The board can be flipped when you are playing with a human 



# Prerequisites

Before you begin, ensure you have met the following requirements:
* **Java 17** installed.
* Your preferred IDE (VS Code, IntelliJ IDEA, or Eclipse).


## How to run the project

1. Clone this repository to your local machine:
   ```bash
   git clone [https://github.com/Moinul924/Chess_Project.git](https://github.com/Moinul924/Chess_Project.git)
2. Navigate to project directory:
   ```bash
   cd Chess_Project
3. Run the Spring Boot application
   ``` bash
   mvn spring-boot:run
   ```
   Or you can open the project in your IDE and run the **ChessProjectApplication.java** file.

 4. Open your preferred web browser and navigate to:
      http://localhost:1000/


## Acknowledgments

A special thank you to [Sebastian Lague](https://www.youtube.com/c/SebastianLague) for the inspiration for this project. This work was made possible by his amazing "Chess Coding Adventures" video series, which helped me grasp the mechanics of the minimax algorithm, alpha-beta pruning, and how to improve the evaluation function.
    
