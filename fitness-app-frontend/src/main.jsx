import React, { useContext, useEffect, useRef, useState } from 'react'
import ReactDOM from 'react-dom/client'

import { Provider } from 'react-redux'
import { store } from './store/store'

import App from './App'
import { AuthContext, AuthProvider } from 'react-oauth2-code-pkce'
import { authConfig, signUpConfig } from './authConfig'
import { SignUpContext } from './signUp'

// The login library always sends people to the endpoint it was created with.
// To open the sign-up form instead, the provider is re-created with the
// sign-up config and the login started from there.

// Starts the redirect once the sign-up provider is in place.
const StartSignUp = ({ active }) => {
  const { logIn } = useContext(AuthContext);
  const started = useRef(false);
  useEffect(() => {
    if (active && !started.current) {
      started.current = true;
      logIn();
    }
  }, [active, logIn]);
  return null;
};

const Root = () => {
  const [signingUp, setSigningUp] = useState(false);

  return (
    <AuthProvider key={signingUp ? 'signup' : 'login'}
                  authConfig={signingUp ? signUpConfig : authConfig}
                  loadingComponent={<div>Loading...</div>}>
      <StartSignUp active={signingUp} />
      <SignUpContext.Provider value={() => setSigningUp(true)}>
        <Provider store={store}>
          <App />
        </Provider>
      </SignUpContext.Provider>
    </AuthProvider>
  );
};

ReactDOM.createRoot(document.getElementById('root')).render(<Root />)
