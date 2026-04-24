# BANKNIFTY Real-Time Signal System

Full-stack BANKNIFTY-only signal platform with:

- Spring Boot backend
- Angel One SmartAPI market stream
- WebSocket broadcast to frontend
- React/Vite realtime dashboard
- ntfy notifications

## Stack

- Backend: Spring Boot 3.5.12, Java 21, Maven, STOMP WebSocket
- Frontend: React 19.2, Vite 8

## Required environment variables

Backend:

- `ANGEL_CLIENT_ID`
- `ANGEL_API_KEY`
- `ANGEL_PASSWORD`
- `ANGEL_TOTP_SECRET`

Optional:

- `NTFY_TOPIC`

Frontend:

- `VITE_API_BASE_URL` for deployed frontend pointing at the backend URL

## Project structure

```text
backend/
  src/main/java/com/banknifty/signal/
    controller/
    service/
    strategy/
    scheduler/
    repository/
    model/
    config/
frontend/
  src/
    components/
    services/
    pages/
```

## Local backend run

```bash
cd backend
./mvnw spring-boot:run
```

## Local frontend run

```bash
cd frontend
npm install
npm run dev
```

## Deployment

### Render backend

- Root directory: `backend`
- Build command: `./mvnw clean package -DskipTests`
- Start command: `java -jar target/banknifty-signal-backend-0.0.1-SNAPSHOT.jar`
- Add Angel and ntfy environment variables in Render dashboard

### Render via `render.yaml`

This repo now includes [render.yaml](/C:/Users/Vigneshwaran.G/Documents/Codex/2026-04-23-use-this-repository-https-github-com-2/render.yaml) with:

- a Java web service for the backend
- a static site for the frontend
- secret env-var prompts for:
  - `ANGEL_CLIENT_ID`
  - `ANGEL_API_KEY`
  - `ANGEL_PASSWORD`
  - `ANGEL_TOTP_SECRET`
  - `NTFY_TOPIC`
  - `VITE_API_BASE_URL`

Important: the Angel One credentials are not stored in the repo. Render will prompt for any `sync: false` values only when you create the Blueprint the first time, and for existing services you add them manually in the Render dashboard.

### Vercel or Netlify frontend

- Root directory: `frontend`
- Build command: `npm run build`
- Publish directory: `dist`
- Set `VITE_API_BASE_URL` to the Render backend URL

### Docker

This repo also includes:

- [backend/Dockerfile](/C:/Users/Vigneshwaran.G/Documents/Codex/2026-04-23-use-this-repository-https-github-com-2/backend/Dockerfile)
- [frontend/Dockerfile](/C:/Users/Vigneshwaran.G/Documents/Codex/2026-04-23-use-this-repository-https-github-com-2/frontend/Dockerfile)

So you can deploy with containers later if you want to move beyond the current Render blueprint.

## Realtime flow

1. SmartAPI WebSocket subscribes only to BANKNIFTY (`26009`)
2. Incoming ticks are aggregated into 1-minute OHLC candles
3. Every minute the scheduler finalizes candles and evaluates the tightened SMC breakout strategy
4. Fresh signals are stored in memory, pushed to ntfy, and broadcast to `/topic/market`
5. React dashboard updates live through `/ws/market`

## Notes

- `ANGEL_TOTP_SECRET` is used to generate the live TOTP code at runtime.
- The backend only tracks `BANKNIFTY`.
- Recent signals are held in memory only, so they reset on restart or redeploy.
- The Angel One SDK is vendored as a local Maven artifact under [backend/local-maven-repo](/C:/Users/Vigneshwaran.G/Documents/Codex/2026-04-23-use-this-repository-https-github-com-2/backend/local-maven-repo) because Angel's published Java dependency is not available from Maven Central.
