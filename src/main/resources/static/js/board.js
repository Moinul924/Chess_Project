import { GameController } from './gameController.js';

// Initialize the Game
const game = new GameController();
game.createGrid();
game.fetchBoard().then(() => game.updateGameOverState());
