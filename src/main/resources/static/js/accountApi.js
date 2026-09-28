const BASE_URL = '/api-account';
let csrf = null;
const csrfReady = fetch('/api/csrf')
    .then(response => response.json())
    .then(token => { csrf = token; });

export async function registerAccount(data) {
    await csrfReady;
    return fetch(BASE_URL + '/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token
        },
        body: JSON.stringify(data)
    });
}    

export async function loginAccount(data) {
    await csrfReady;
    return fetch(BASE_URL + '/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token
        },
        body: JSON.stringify(data)
    });
}


export async function logoutAccount() {
    await csrfReady;
    return fetch(BASE_URL + '/logout', {
        method: 'POST',
        headers: { [csrf.headerName]: csrf.token }
    });
}

export function getCurrentUser() {
    return fetch(BASE_URL + '/me');
}

