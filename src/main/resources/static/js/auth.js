import { registerAccount } from './accountApi.js';

const authTabs = document.querySelectorAll('.auth-tab');
const authTrack = document.querySelector('.auth-track');
const confirmPassword = document.getElementById('register-confirm-password');
const registerPassword = document.getElementById('register-password');

const registerForm = document.getElementById('register-form');
const registerMessage = document.getElementById('register-message');

registerPassword.addEventListener('input', validatePasswords);
confirmPassword.addEventListener('input', validatePasswords);

function showAuthForm(formName) {
    const showRegister = formName === 'register';
    authTrack.classList.toggle('show-register', showRegister);

    authTabs.forEach((tab) => {
        const isActive = tab.dataset.form === formName;
        tab.classList.toggle('active', isActive);
        tab.setAttribute('aria-selected', String(isActive));
    });
}

authTabs.forEach((tab) => {
    tab.addEventListener('click', () => showAuthForm(tab.dataset.form));
});

function validatePasswords() {
    confirmPassword.setCustomValidity(
        confirmPassword.value === registerPassword.value ? '' : 'Passwords do not match.'
    );
}


registerForm.addEventListener('submit', async (event) => {
    // Stop the browser's old-style submit (which reloads the page) and send it with fetch instead.
    event.preventDefault();

    // The input names (email, username, password, confirmPassword) match the fields in RegisterRequest.
    const data = Object.fromEntries(new FormData(registerForm));

    try {
        const response = await registerAccount(data);
        registerMessage.textContent = await response.text();
        if (response.ok) {
            registerForm.reset();
        }
    } catch (error) {
        // fetch only throws when the server can't be reached at all (e.g. the app isn't running).
        registerMessage.textContent = 'Could not reach the server. Please try again.';
    }
});
