const clientId = 'dev-client';
const clientSecret = 'secret';

const authorizeEndpoint = 'http://localhost:3000/api/authorize';
const tokenEndpoint = 'http://localhost:3000/api/token';

/* Utility Functions */

function addEventListenerIfElement(elementId, event, listenterFn) {
    let element = document.getElementById(elementId);

    if (element) {
        element.addEventListener(event, listenterFn);
    }
}

function handleToken(token) {
    window.localStorage.setItem('token', token);
    document.getElementsByClassName('modal')[0].classList.add('is-active');
}

function handleTokenResponse(data) {
    handleToken(data.access_token);
}

/* Authorization Code Flow */

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

async function fetchTokenWithAuthCode(code) {
    let formData = new URLSearchParams();

    formData.append('client_id', clientId);
    formData.append('code', code);
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

if (document.getElementById('redirect')) {
    let params = new URLSearchParams(window.location.search);
    let code = params.get('code');

    if (code) {
        fetchTokenWithAuthCode(code).then(handleTokenResponse);
    } else if (window.location.hash) {
        let token = window.location.hash
                          .substring(1)
                          .split('#')
                          .find(function(item) {
                              return item.startsWith("token");
                          })
                          .split("=")[1];
        handleToken(token);
    } else {
        // error here
    }
}

/* Implicit Grant Flow */

async function fetchToken() {
    let queryParams = new URLSearchParams();

    queryParams.append('client_id', clientId);
    queryParams.append('redirect_uri', 'http://localhost:4000/redirect')
    queryParams.append('response_type', 'token');

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

addEventListenerIfElement('implicit', 'click', function() {
    fetchToken();
});

/* Resource Owner Credentials Flow */

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

/* Client Credentials Flow */

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
    fetchTokenWithClientCredentials().then(handleTokenResponse);
});

addEventListenerIfElement('close', 'click', function() {
    document.getElementsByClassName('modal')[0].classList.remove('is-active');
});
