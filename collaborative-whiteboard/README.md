# Collaborative Whiteboard

A simple browser-based collaborative whiteboard with real-time multi-user drawing, built with TypeScript on both the client and server.

## Features
- Real-time collaboration over WebSockets (Socket.IO) with room-based sessions.
- Landing page to create or join rooms via short IDs or shareable URLs.
- Drawing tools: pen, rectangles, ellipses, lines, arrows, connectors, icons (component, database, user, service, cloud), text, selection, pan, eraser.
- Selection and transform (move, resize, rotate) using Konva transformer handles.
- Styling controls for stroke color, fill color, width, and font size.
- Undo/redo per room and board clear action.
- Mini-map and quick re-center control.
- User presence indicators with avatar chips in the top bar.

## Tech stack
- **Frontend:** React + Vite + TypeScript, react-konva for canvas rendering, socket.io-client for realtime.
- **Backend:** Express + Socket.IO + TypeScript.
- **Shared:** Reusable TypeScript types shared across client/server.

## Getting started

From the `collaborative-whiteboard` directory:

```bash
npm install
npm run dev
```

- The server starts on `http://localhost:4000`.
- The client dev server runs on `http://localhost:5173`.

### Individual scripts
- `npm run dev:server` – start the backend with hot reload.
- `npm run dev:client` – start the Vite dev server.
- `npm run build` – build shared types, server, and client bundles.
- `npm run test` – run basic shared/client/server tests (Vitest and type checks).

## Room links
- Room URLs follow `/whiteboard/:roomId`.
- Use the landing page to generate a fresh room ID or enter an existing one.
- The top bar inside a room includes a **Copy share link** button that copies the full URL to your clipboard.

## Persistence
Room state is stored in memory per server instance. Each room keeps the list of elements plus undo/redo history. The code centralizes room state in `server/src/index.ts`, making it straightforward to replace the in-memory map with a database later (e.g., Postgres or Redis).
