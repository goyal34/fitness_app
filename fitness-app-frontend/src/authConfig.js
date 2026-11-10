const KEYCLOAK = 'http://localhost:8181/realms/fitness-oauth2/protocol/openid-connect';
const APP_URL = 'http://localhost:5173';

export const authConfig = {
    clientId: 'oauth2-pkce-client',
    authorizationEndpoint: `${KEYCLOAK}/auth`,
    tokenEndpoint: `${KEYCLOAK}/token`,
    // Logging out also ends the Keycloak session; otherwise the next "Login"
    // signs the same person straight back in without asking anything.
    logoutEndpoint: `${KEYCLOAK}/logout`,
    logoutRedirect: APP_URL,
    redirectUri: APP_URL,
    scope: 'openid profile email offline_access',
    // Show our welcome screen (Login / Create account) instead of jumping to Keycloak.
    autoLogin: false,
    onRefreshTokenExpire: (event) => event.logIn(),
}

// Same flow, but starts on Keycloak's sign-up form instead of the sign-in form.
export const signUpConfig = {
    ...authConfig,
    authorizationEndpoint: `${KEYCLOAK}/registrations`,
}
