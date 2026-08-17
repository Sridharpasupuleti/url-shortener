import { useState } from "react";
import { shortenUrl } from "../services/urlService";

function UrlForm() {
  const [url, setUrl] = useState("");
  const [columnAlias, setColumnAlias] = useState("");
  const [shortenedUrl, setShortenedUrl] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Clear previous result/error
    setShortenedUrl("");
    setError("");
    setCopied(false);

    // Validate URL
    if (!url.trim()) {
      setError("Please enter a URL.");
      return;
    }

    setLoading(true);

    try {
      const result = await shortenUrl(url, columnAlias);
     setShortenedUrl(result);
    } catch (err) {
      setError("Unable to connect to the server.");
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(shortenedUrl);

      setCopied(true);

      setTimeout(() => {
        setCopied(false);
      }, 2000);
    } catch (err) {
      setError("Failed to copy the URL.");
    }
  };

  return (
    <form className="url-form" onSubmit={handleSubmit}>

      <h2>Shorten your URL</h2>

      {/* URL Input */}
      <div className="input-group">
        <label>Long URL</label>

        <input
          type="text"
          placeholder="https://example.com/your-long-url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
      </div>

      {/* Alias Input */}
      <div className="input-group">
        <label>Custom Alias</label>

        <input
          type="text"
          placeholder="my-custom-url (optional)"
          value={columnAlias}
          onChange={(e) => setColumnAlias(e.target.value)}
        />

        <small>
          Leave empty to let the server generate a short code.
        </small>
      </div>

      {/* Error */}
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="shorten-button"
      >
        {loading ? "Shortening..." : "Shorten URL"}
      </button>

      {/* Result */}
      {shortenedUrl && (
        <div className="result">

          <h3>Your shortened URL</h3>

          <div className="result-row">

            <a
              href={shortenedUrl}
              target="_blank"
              rel="noreferrer"
            >
              {shortenedUrl}
            </a>

            <button
              type="button"
              onClick={handleCopy}
              className="copy-button"
            >
              {copied ? "Copied!" : "Copy"}
            </button>

          </div>

        </div>
      )}

    </form>
  );
}

export default UrlForm;