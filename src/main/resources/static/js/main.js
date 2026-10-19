import { GameController } from './gameController.js';
import * as api from './api.js';
import { applySavedTheme } from './themes.js';

// Colour the board with the theme saved on the settings page.
if (document.querySelector('.grid-board')) {
    applySavedTheme();
}

window.addEventListener('pagehide', () => {
    api.closeGameOnExit();
});

// mode: 'ENGINE' (vs bot) or 'LOCAL' (pass and play on one screen)
async function initializeGame(mode = 'LOCAL') {
    const game = new GameController();
    // Against the bot, the player is "player 2" (the bottom of the screen), whose colour was picked at random.
    const colour = game.player2White ? 'WHITE' : 'BLACK';
    const started = await api.startNewGame(mode, colour);
    if (!started) {
        throw new Error('Unable to start a new game.');
    }

    await drawBoard(game);
    return game;
}

async function drawBoard(game) {
    game.createGrid();
    await game.fetchBoard();
    await game.updateCapturedPieces();
    await game.updateGameOverState();
}

function showBoardScreen() {
    document.querySelector('.game-mode').classList.add('is-hidden');
    document.querySelector('.game-board-screen').classList.add('is-visible');
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

// ==========================================
// MATCHMAKING: waiting in the queue for an opponent
// ==========================================

const matchmakingBox = document.getElementById('matchmaking');
const matchmakingStatus = document.getElementById('matchmaking-status');
const cancelSearchButton = document.getElementById('cancel-search-button');
const onlineStatus = document.getElementById('online-status');
// The three mode buttons, which are hidden while searching
const menuOptions = [...document.querySelectorAll('.game-mode > div:not(#matchmaking)')];

let searchTimer = null;
let isSearching = false;
let searchingAs = null;       // the account id this tab started searching as
let accountChanged = false;   // set when another tab signed this browser in as someone else

function showSearching(searching) {
    isSearching = searching;
    menuOptions.forEach(option => { option.hidden = searching; });
    if (matchmakingBox) matchmakingBox.hidden = !searching;
}

async function startSearching() {
    showSearching(true);
    searchingAs = null;
    matchmakingStatus.textContent = 'Searching for an opponent…';
    await handleQueueStatus(await api.joinQueue());
}

// All tabs in one browser share ONE login. If someone signs in as a different account in another
// tab, this tab is suddenly that account too, so stop rather than carry on searching as someone else.
function stopBecauseAccountChanged() {
    stopSearchTimer();
    accountChanged = true;
    matchmakingStatus.textContent =
        'This browser was signed in as a different account in another tab, so this tab stopped searching. ' +
        'To play two accounts at once, use a separate browser or a private/incognito window.';
    cancelSearchButton.textContent = 'Reload page';
}

function stopSearchTimer() {
    clearInterval(searchTimer);
    searchTimer = null;
}

// Reacts to what the server says about our place in the queue.
async function handleQueueStatus(result) {
    if (!isSearching) return;   // Cancel was clicked while this request was on its way

    if (!result) {
        stopSearchTimer();
        matchmakingStatus.textContent = 'Could not reach the server. Press Cancel and try again.';
        return;
    }
    if (searchingAs === null) {
        searchingAs = result.accountId;
    } else if (result.accountId !== searchingAs) {
        stopBecauseAccountChanged();
        return;
    }
    if (result.status === 'MATCHED') {
        stopSearchTimer();
        showSearching(false);
        await enterOnlineGame(result.gameId);
        return;
    }
    if (result.status === 'NOT_SEARCHING') {
        // The server dropped us, e.g. the browser slowed this tab's timer down while it was in
        // the background and we missed our check-ins. Just get back in the queue.
        await handleQueueStatus(await api.joinQueue());
        return;
    }
    // WAITING: keep checking in. This both asks "paired yet?" and tells the server we're still here.
    if (!searchTimer) {
        searchTimer = setInterval(async () => handleQueueStatus(await api.checkQueue()), 1500);
    }
}

cancelSearchButton?.addEventListener('click', async () => {
    if (accountChanged) {
        // Don't call leaveQueue here: the browser is now the OTHER account, which may be searching in its own tab.
        window.location.reload();
        return;
    }
    stopSearchTimer();
    showSearching(false);
    await api.leaveQueue();
});

// Closing the page while searching takes us out of the queue straight away,
// rather than waiting for the server to notice we've stopped checking in.
window.addEventListener('pagehide', () => {
    if (isSearching) api.leaveQueueOnExit();
});

// ==========================================
// ONLINE GAMES
// ==========================================

function showOnlineMessage(text) {
    onlineStatus.textContent = text;
    onlineStatus.hidden = false;
}

// Updates the names and the "whose move" line from the server's state.
function showOnlineState(state) {
    const iAmWhite = state.myColour === 'WHITE';
    const myName = iAmWhite ? state.whiteName : state.blackName;
    const opponentName = iAmWhite ? state.blackName : state.whiteName;

    // Top of the screen is the opponent, bottom is you.
    document.getElementById('player1-name').textContent = opponentName;
    document.getElementById('player1-avatar').innerHTML = playerAvatar;
    document.getElementById('player2-name').textContent = `${myName} (you)`;
    document.getElementById('player2-avatar').innerHTML = playerAvatar;

    if (state.gameOver) {
        showOnlineMessage('Game over');
    } else {
        showOnlineMessage(state.whiteToMove === iAmWhite ? 'Your move' : `${opponentName} is thinking…`);
    }
}

// Called once the server has paired us into a game.
async function enterOnlineGame(gameId) {
    api.useOnlineGame(gameId);
    // Put the id in the address bar, so refreshing the page comes back to this game.
    history.replaceState(null, '', `?game=${gameId}`);
    await startOnlineGame();
}

async function startOnlineGame() {
    showBoardScreen();

    const state = await api.getOnlineState();
    if (!state) {
        showOnlineMessage('That game no longer exists.');
        return;
    }

    const game = new GameController();
    game.enableOnline(state, showOnlineState, () => showOnlineMessage(
        "You're no longer in this game. Either this browser was signed in as a different account in " +
        'another tab, or the server restarted.'));
    await drawBoard(game);
    showOnlineState(state);
    game.startPolling();
}

// ==========================================
// GAME MENU
// ==========================================

const botButton = document.getElementById('play-bot-button');
const humanButton = document.getElementById('play-human-button');
const onlineButton = document.getElementById('play-online-button');
if (botButton) {
    botButton.addEventListener('click', async () => {
        showBoardScreen();
        configureProfiles(true);

        const game = await initializeGame('ENGINE');
        game.playEngine = true;
        game.setGameModeLabel('Playing against Bot');

        if (game.playEngine && !game.player2White && !game.isGameOver) {
            await game.handleEngineMove();
        }
    });
    humanButton?.addEventListener('click', async () => {
        showBoardScreen();
        configureProfiles(false);

        const game = await initializeGame('LOCAL');
        game.playEngine = false;
        game.setGameModeLabel('Playing against Human');
    });
    onlineButton?.addEventListener('click', () => startSearching());

    // Refreshed the page during an online game: go straight back into it.
    if (api.getOnlineGameIdFromLink()) {
        startOnlineGame();
    }
} else if (document.querySelector('.grid-board')) {
    initializeGame().catch(error => console.error(error));
}