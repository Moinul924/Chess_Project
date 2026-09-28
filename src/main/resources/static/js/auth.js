import { registerAccount, loginAccount } from './accountApi.js';

const authTabs = document.querySelectorAll('.auth-tab');
const authTrack = document.querySelector('.auth-track');
const confirmPassword = document.getElementById('register-confirm-password');
const registerPassword = document.getElementById('register-password');
const registerUsername = document.getElementById('register-username');

const signinForm = document.getElementById('signin-form');
const signinMessage = document.getElementById('signin-message');

const registerForm = document.getElementById('register-form');
const registerMessage = document.getElementById('register-message');


registerPassword.addEventListener('input', validatePasswords);
confirmPassword.addEventListener('input', validatePasswords);
registerUsername.addEventListener('input', validateUsername);

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

function validateUsername() {
    registerUsername.setCustomValidity(
        registerUsername.value.includes('@')? 'Username cannot contain the "@" character.' : registerUsername.value.length < 3
                                            ? 'Username must be at least 3 characters long.' : ''
    );
}

function clearAuthForm(form) {
    form.reset();
    form.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]').forEach((input) => {
        input.value = '';
    });
}

registerForm.addEventListener('submit', async (event) => {
    // Stop the browser's old-style submit (which reloads the page) and send it with fetch instead.
    event.preventDefault();

    // The input names (email, username, password, confirmPassword) match the fields in RegisterRequest.
    const data = Object.fromEntries(new FormData(registerForm));

    try {
        const response = await registerAccount(data);
        registerMessage.textContent = await response.text();
    } catch (error) {
        registerMessage.textContent = 'Could not reach the server. Please try again.';
    } finally {
        clearAuthForm(registerForm);
    }
});

signinForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const data = Object.fromEntries(new FormData(signinForm));

    try {
        const responce = await loginAccount(data);
        const responseMessage = await responce.text();
        signinMessage.textContent = responseMessage;
        if (responce.ok) {
            window.location.href = './index.html';
        }
    } catch (error) {
        signinMessage.textContent = 'Could not reach the server. Please try again.';
    } finally {
        clearAuthForm(signinForm);
    }


});
