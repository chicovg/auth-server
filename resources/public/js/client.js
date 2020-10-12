var clientId = 'example-client';
var authorizeEndpoint = 'http://localhost:3000/api/authorize';
var redirectUri = window.location.toString();

var authCodeButton = document.getElementById('authorization-code');

authCodeButton.addEventListener('click', function() {
    fetch(authorizeEndpoint + '?response_type=code&client_id=' + clientId + "&redirect_uri=" + redirectUri,
         {
             redirect: 'follow'
         })
        .then(function(response) {
            if (response.redirected) {
                window.location = response.url;
            } else {
                throw Error('Something went wrong, there should have been a redirect');
            }
        })
        .error(function(error) {
            console.log(error);
        });

    console.log('hello');
});
