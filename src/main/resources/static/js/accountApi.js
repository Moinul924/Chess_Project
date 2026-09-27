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