# Production security checklist

- Set GEMINI_API_KEY only as a server secret/environment variable.
- Never commit .env or API keys.
- Keep request bodies text-only and small.
- Do not store raw voice recordings.
- Add authentication/rate limiting before public production deployment.
- Use HTTPS only.
- Configure a strict CORS allowlist for the released Android app/backend.
