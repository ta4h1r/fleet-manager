import React from 'react';
import {
  BrowserRouter as Router,
  Switch,
  Route
} from 'react-router-dom';

import FleetPage from './pages/FleetManager.js';
import LoginPage from './pages/Login.js';

export default function App() {
  return (
    <Router>
        <Switch>
          <Route path="/landing/fleet">
            <FleetPage />
          </Route>
          <Route path="/landing/login">
            <LoginPage />
          </Route>
        
        </Switch>
    </Router>
  );
}
