import logo from './logo.svg';


import UrlForm from "./components/UrlForm";
import "./App.css";

function App() {
  return (
    <div className="App">
      <div className="url-container">
        <h1>URL Shortener</h1>

        <UrlForm />
      </div>
    </div>
  );
}

export default App;