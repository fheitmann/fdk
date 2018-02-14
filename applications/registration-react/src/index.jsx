import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { BrowserRouter, Route, Switch } from 'react-router-dom';

import configureStore from './store/configureStore';
import RegDataset from './containers/reg-dataset';
import Header from './components/app-header';
import Footer from './components/app-footer';

const store = configureStore();

const routes =
  (
    <Switch>
      <Route path="/react" component={RegDataset} />
      <Route path="/catalogs/:catalogId/datasets/:id" component={RegDataset} />
    </Switch>
  );

ReactDOM.render((
  <Provider store={store}>
    <BrowserRouter>
      <div>

        <Footer />
      </div>
    </BrowserRouter>
  </Provider>
), document.getElementById('root'))
