const KEYCLOAK = 'http://localhost:8181/realms/fitness-oauth2/protocol/openid-connect';
const APP_URL = 'http://localhost:5173';

export const authConfig = {
    clientId: 'oauth2-pkce-client',
    authorizationEndpoint: `${KEYCLOAK}/auth`,
    tokenEndpoint: `${KEYCLOAK}/token`,
    logoutEndpoint: `${KEYCLOAK}/logout`,
    logoutRedirect: APP_URL,
    redirectUri: APP_URL,
    scope: 'openid profile email offline_access',
    autoLogin: false,
    onRefreshTokenExpire: (event) => event.logIn(),
}

export const signUpConfig = {
    ...authConfig,
    authorizationEndpoint: `${KEYCLOAK}/registrations`,
}
