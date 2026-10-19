import { getSettings, saveSettings } from './accountApi.js';
import { THEMES } from './themes.js';

const form = document.getElementById('settings-form');
const themeOptions = document.getElementById('theme-options');
const message = document.getElementById('settings-message');
const saveButton = document.getElementById('save-settings-button');

// One row per theme: radio button, a 2 x 2 checkerboard in the theme's colours, and the name.
function renderThemeOptions() {
    for (const theme of THEMES) {
        const option = document.createElement('label');
        option.className = 'theme-option';
        option.innerHTML = `
            <input type="radio" name="boardTheme" value="${theme.key}">
            <span class="theme-preview" aria-hidden="true">
                <span style="background-color: ${theme.light}"></span>
                <span style="background-color: ${theme.dark}"></span>
                <span style="background-color: ${theme.dark}"></span>
                <span style="background-color: ${theme.light}"></span>
            </span>
            <span class="theme-name">${theme.name}</span>`;
        themeOptions.appendChild(option);
    }
}

// Ticks the theme that's saved in the database.
async function showSavedSettings() {
    const response = await getSettings();
    const { boardTheme } = response.ok ? await response.json() : { boardTheme: 'classic' };
    const saved = themeOptions.querySelector(`input[value="${boardTheme}"]`)
        ?? themeOptions.querySelector('input[value="classic"]');
    saved.checked = true;
}

themeOptions.addEventListener('change', () => {
    message.textContent = '';   // clear "Settings saved" once something new is picked
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const boardTheme = new FormData(form).get('boardTheme');

    saveButton.disabled = true;
    try {
        const response = await saveSettings({ boardTheme });
        message.textContent = response.ok ? 'Settings saved' : await response.text();
    } catch {
        message.textContent = 'Could not reach the server. Please try again.';
    } finally {
        saveButton.disabled = false;
    }
});

renderThemeOptions();
showSavedSettings();
