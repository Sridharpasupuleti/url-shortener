const API_URL = "http://localhost:8080/urlshortener";

export async function shortenUrl(url, columnAlias) {
  const response = await fetch(`${API_URL}/posturl`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      url: url,
      columnAlias: columnAlias,
    }),
  });

  if (response.ok) {
    const shortenedUrl = await response.text();

    return shortenedUrl;
  }

  const errorData = await response.json();

  throw new Error(
    errorData.details || "Something went wrong."
  );
}