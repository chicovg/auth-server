const clientId = 'dev-client';
const clientSecret = 'secret';

const authorizeEndpoint = 'http//localhost:3000/api/authorize';
const tokenEndpoint = 'http://localhost:3000/api/token';

function addEventListenerIfElement(elementId, event, listenterFn) {
    let element = document.getElementById(elementId);
    if (element) {
        element.addEventListener(event, listenterFn);
    }
}

async function fetchAuthCode() {
    await fetch(uri)
        .then(function(response) {
            if (response.redirected) {
                window.location = response.url;
            } else {
                throw Error('Something went wrong, there should have been a redirect');
            }
        })
        .catch(function(error) {
            console.log(error);
        });
}

addEventListenerIfElement('authorization-code', 'click', function() {
    fetchAuthToken();
});

async function fetchTokenWithAuthCode() {
    let params = new URLSearchParams(window.location.search);
    let formData = new URLSearchParams();

    formData.append('client_id', clientId);
    formData.append('code', params.get('code'));
    formData.append('grant_type', 'authorization_code');
    formData.append('redirect_uri', 'http://localhost:4000');

    let response = await fetch(tokenEndpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            Authorization: 'Basic ' + btoa(clientId + ':' + clientSecret),
        },
        body: formData,
    });

    return response.json();
}

addEventListenerIfElement('redirect', 'load', function() {
    fetchTokenWithAuthCode().then(function(data) {
        window.localStorage.setItem('token', data.access_token);
        window.location = '/';
    });
});

async function fetchTokenWithClientCredentials() {
    let formData = new URLSearchParams();
    formData.append('grant_type', 'client_credentials');

    let response = await fetch(tokenEndpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            Authorization: 'Basic ' + btoa(clientId + ':' + clientSecret),
        },
        body: formData,
    });

    return response.json();
}

addEventListenerIfElement('client-credentials', 'click', function() {
    fetchTokenWithClientCredentials().then(function(data) {
        window.localStorage.setItem('token', data.access_token);
        window.location = '/';
    });
});
