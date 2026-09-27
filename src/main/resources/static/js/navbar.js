const pageRoot = window.location.pathname.includes('/sing-in/') ? '../' : './';
const isPlayPage = window.location.pathname.endsWith('/play.html');
const isRegisterPage = window.location.pathname.endsWith('/singInOrRegister.html');
const navbarContainer = document.getElementById('navbar');

if (isRegisterPage) {
    document.body.classList.add('auth-page');
}

navbarContainer.innerHTML = `
    <nav class="navbar">
        <div class="navbar-brand">
            <a class="navbar-brand-link" href="${pageRoot}index.html" aria-label="Go to home page">
                <span class="navbar-title">Chess</span>
            </a>
        </div>

        <ul class="navbar-links" id="navbar-links">
            <li><a href="${pageRoot}play.html" class="navbar-link${isPlayPage ? ' active' : ''}">Play</a></li>
            <li><a href="#" class="navbar-link">Stats</a></li>
            <li><a href="#" class="navbar-link">Settings</a></li>
            <li><a href="${pageRoot}singInOrRegister.html" class="navbar-link${isRegisterPage ? ' active' : ''}">Register</a></li>
        </ul>

        <button class="navbar-menu-button" id="navbar-menu-button" type="button" aria-label="Open navigation menu" aria-expanded="false" aria-controls="navbar-links">
            <span></span>
            <span></span>
            <span></span>
        </button>

        <button class="navbar-profile" title="Profile" aria-label="Profile">
            <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5.52 19c.64-2.2 1.84-3 3.22-3h6.52c1.38 0 2.58.8 3.22 3"/>
                <circle cx="12" cy="10" r="3"/>
                <circle cx="12" cy="12" r="10"/>
            </svg>
        </button>
    </nav>`;

const menuButton = document.getElementById('navbar-menu-button');
const navigationMenu = document.getElementById('navbar-links');

function closeNavigationMenu() {
    navigationMenu.classList.remove('is-open');
    document.body.classList.remove('menu-open');
    menuButton.setAttribute('aria-expanded', 'false');
    menuButton.setAttribute('aria-label', 'Open navigation menu');
}

menuButton.addEventListener('click', () => {
    const isOpen = navigationMenu.classList.toggle('is-open');
    document.body.classList.toggle('menu-open', isOpen);
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
