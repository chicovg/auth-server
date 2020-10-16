const clientId = 'dev-client';
const clientSecret = 'secret';

const authorizeEndpoint = 'http://localhost:3000/api/authorize';
const tokenEndpoint = 'http://localhost:3000/api/token';

function addEventListenerIfElement(elementId, event, listenterFn) {
    let element = document.getElementById(elementId);
    if (element) {
        element.addEventListener(event, listenterFn);
    }
}

async function fetchAuthCode() {
    let queryParams = new URLSearchParams();

    queryParams.append('client_id', clientId);
    queryParams.append('redirect_uri', 'http://localhost:4000/redirect')
    queryParams.append('response_type', 'code')

    let uri = authorizeEndpoint + '?' + queryParams.toString();

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
    fetchAuthCode();
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

function handleTokenResponse(data) {
    window.localStorage.setItem('token', data.access_token);
    document.getElementsByClassName('modal')[0].classList.add('is-active');
}

if (document.getElementById('redirect')) {
    fetchTokenWithAuthCode().then(handleTokenResponse);
}

async function fetchTokenWithResourceOwnerCredentials() {
    let formData = new URLSearchParams();

    formData.append('grant_type', 'password');
    formData.append('username', document.getElementById('username').value);
    formData.append('password', document.getElementById('password').value);

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

addEventListenerIfElement('resource-owner', 'click', function() {
    fetchTokenWithResourceOwnerCredentials().then(handleTokenResponse);
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
    console.log('here');
    fetchTokenWithClientCredentials().then(handleTokenResponse);
});

document.getElementById('close').addEventListener('click', function() {
    document.getElementsByClassName('modal')[0].classList.remove('is-active');
});
