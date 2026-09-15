import { GameController } from './gameController.js';
import * as api from './api.js';

const menuButton = document.getElementById('navbar-menu-button');
const navigationMenu = document.getElementById('navbar-links');

function closeNavigationMenu() {
    navigationMenu.classList.remove('is-open');
    menuButton.setAttribute('aria-expanded', 'false');
    menuButton.setAttribute('aria-label', 'Open navigation menu');
}

menuButton.addEventListener('click', () => {
    const isOpen = navigationMenu.classList.toggle('is-open');
    menuButton.setAttribute('aria-expanded', String(isOpen));
    menuButton.setAttribute('aria-label', isOpen ? 'Close navigation menu' : 'Open navigation menu');
});

document.addEventListener('click', (event) => {
    if (!navigationMenu.contains(event.target) && !menuButton.contains(event.target)) {
        closeNavigationMenu();
    }
});

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
        closeNavigationMenu();
    }
});

window.addEventListener('pagehide', () => {
    api.closeGameOnExit();
});

async function initializeGame() {
    const started = await api.startNewGame();
    if (!started) {
        throw new Error('Unable to start a new game.');
    }

    const game = new GameController();
    game.createGrid();
    await game.fetchBoard();
    await game.updateGameOverState();
    return game;
}

const robotAvatar = `
    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="currentColor" viewBox="0 0 16 16" aria-label="Bot avatar">
        <path d="M6 12.5a.5.5 0 0 1 .5-.5h3a.5.5 0 0 1 0 1h-3a.5.5 0 0 1-.5-.5M3 8.062C3 6.76 4.235 5.765 5.53 5.886a26.6 26.6 0 0 0 4.94 0C11.765 5.765 13 6.76 13 8.062v1.157a.93.93 0 0 1-.765.935c-.845.147-2.34.346-4.235.346s-3.39-.2-4.235-.346A.93.93 0 0 1 3 9.219zm4.542-.827a.25.25 0 0 0-.217.068l-.92.9a25 25 0 0 1-1.871-.183.25.25 0 0 0-.068.495c.55.076 1.232.149 2.02.193a.25.25 0 0 0 .189-.071l.754-.736.847 1.71a.25.25 0 0 0 .404.062l.932-.97a25 25 0 0 0 1.922-.188.25.25 0 0 0-.068-.495c-.538.074-1.207.145-1.98.189a.25.25 0 0 0-.166.076l-.754.785-.842-1.7a.25.25 0 0 0-.182-.135"/>
        <path d="M8.5 1.866a1 1 0 1 0-1 0V3h-2A4.5 4.5 0 0 0 1 7.5V8a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1v1a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-1a1 1 0 0 0 1-1V9a1 1 0 0 0-1-1v-.5A4.5 4.5 0 0 0 10.5 3h-2zM14 7.5V13a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V7.5A3.5 3.5 0 0 1 5.5 4h5A3.5 3.5 0 0 1 14 7.5"/>
    </svg>`;

const playerAvatar = `
    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="currentColor" viewBox="0 0 16 16" aria-label="Player avatar">
        <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6"/>
    </svg>`;

function configureProfiles(isBotGame) {
    const profiles = [
        { name: document.getElementById('player1-name'), avatar: document.getElementById('player1-avatar') },
        { name: document.getElementById('player2-name'), avatar: document.getElementById('player2-avatar') }
    ];

    profiles[0].name.textContent = isBotGame ? 'Bot' : 'Player 1';
    profiles[0].avatar.innerHTML = isBotGame ? robotAvatar : playerAvatar;
    profiles[1].name.textContent = 'Player 2';
    profiles[1].avatar.innerHTML = playerAvatar;
}

const botButton = document.getElementById('play-bot-button');
const humanButton = document.getElementById('play-human-button');
if (botButton) {
    botButton.addEventListener('click', async () => {
        document.querySelector('.game-mode').classList.add('is-hidden');
        document.querySelector('.game-board-screen').classList.add('is-visible');
        configureProfiles(true);

        const game = await initializeGame();
        game.playEngine = true;
        game.setGameModeLabel('Playing against Bot');

        if (game.playEngine && !game.player2White && !game.isGameOver) {
            await game.handleEngineMove();
        }
    });
    humanButton?.addEventListener('click', async () => {
        document.querySelector('.game-mode').classList.add('is-hidden');
        document.querySelector('.game-board-screen').classList.add('is-visible');
        configureProfiles(false);

        const game = await initializeGame();
        game.playEngine = false;
        game.setGameModeLabel('Playing against Human');
    });
} else if (document.querySelector('.grid-board')) {
    initializeGame().catch(error => console.error(error));
}