import { getSettings } from './accountApi.js';

// Every board theme the settings page offers. The keys must match AccountService.BOARD_THEMES on the server.
//
// lastMove and selected are see-through TINTS laid over the square colour (see .last-move-highlight in
// board.css), so a highlighted light square still looks lighter than a highlighted dark one. Each tint
// was picked to stand out clearly against BOTH of that theme's square colours.
// moveDot is the colour of the legal-move dots and capture rings.
export const THEMES = [
    {
        key: 'classic', name: 'Classic Green',
        light: '#F5F5F4', dark: '#6A9B41',
        lastMove: 'rgba(181, 205, 58, 0.8)', selected: 'rgba(249, 249, 148, 0.8)', moveDot: 'rgba(0, 0, 0, 0.2)',
    },
    {
        key: 'black-white', name: 'Black and White',
        light: '#F0F0F0', dark: '#303030',
        // gold shows on both near-white and near-black; grey dots stay visible on the dark squares
        lastMove: 'rgba(255, 196, 0, 0.55)', selected: 'rgba(0, 153, 255, 0.5)', moveDot: 'rgba(128, 128, 128, 0.6)',
    },
    {
        key: 'grey-white', name: 'Grey and White',
        light: '#F4F4F4', dark: '#A3A3A3',
        lastMove: 'rgba(255, 153, 0, 0.5)', selected: 'rgba(66, 133, 244, 0.45)', moveDot: 'rgba(0, 0, 0, 0.25)',
    },
    {
        key: 'carbon-antique', name: 'Carbon and Antique',
        light: '#E6DBC6', dark: '#5E544C',
        lastMove: 'rgba(255, 200, 40, 0.55)', selected: 'rgba(90, 170, 255, 0.45)', moveDot: 'rgba(0, 0, 0, 0.25)',
    },
    {
        key: 'oat-sand', name: 'Oat and Sand',
        light: '#F1E8D6', dark: '#CDB083',
        // the two squares are close in colour, so a cool teal is used to stand out from both warm tones
        lastMove: 'rgba(20, 150, 140, 0.45)', selected: 'rgba(230, 90, 60, 0.45)', moveDot: 'rgba(0, 0, 0, 0.22)',
    },
    {
        key: 'red-cream', name: 'Red and Soft Cream',
        light: '#F5ECD7', dark: '#A63A3F',
        lastMove: 'rgba(255, 215, 0, 0.6)', selected: 'rgba(70, 160, 255, 0.5)', moveDot: 'rgba(0, 0, 0, 0.25)',
    },
    {
        key: 'pink-blue', name: 'Ballet Pink and Dusty Blue',
        light: '#F3C9D3', dark: '#7D9BB6',
        lastMove: 'rgba(255, 235, 90, 0.65)', selected: 'rgba(110, 230, 160, 0.55)', moveDot: 'rgba(0, 0, 0, 0.22)',
    },
    {
        key: 'forest', name: 'Forest and Forest',
        light: '#9DBF8B', dark: '#4C7445',
        lastMove: 'rgba(255, 228, 90, 0.6)', selected: 'rgba(255, 140, 90, 0.5)', moveDot: 'rgba(0, 0, 0, 0.25)',
    },
];

export function findTheme(key) {
    return THEMES.find(theme => theme.key === key) ?? THEMES[0];
}

// The board's CSS reads its colours from these variables (see variables.css and board.css),
// so changing them recolours the whole board at once.
export function applyTheme(key) {
    const theme = findTheme(key);
    const root = document.documentElement.style;
    root.setProperty('--color-square-light', theme.light);
    root.setProperty('--color-square-dark', theme.dark);
    root.setProperty('--color-square-last-move', theme.lastMove);
    root.setProperty('--color-square-selected', theme.selected);
    root.setProperty('--color-move-indicator', theme.moveDot);
}

// Loads the player's saved theme from the database and applies it. Does nothing if not logged in.
export async function applySavedTheme() {
    try {
        const response = await getSettings();
        if (response.ok) {
            applyTheme((await response.json()).boardTheme);
        }
    } catch {
        // Server unreachable: keep the default colours.
    }
}
