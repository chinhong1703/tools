# Gemini Code Assistant Context

This document provides context for the Gemini Code Assistant to understand the `collaborative-whiteboard` project.

## Project Overview

The `collaborative-whiteboard` is a real-time, browser-based collaborative drawing application. It features a client-server architecture with a shared TypeScript codebase for types.

- **Frontend:** The client is a React application built with Vite and TypeScript. It uses `react-konva` for canvas rendering and `socket.io-client` for real-time communication.
- **Backend:** The server is an Express application with Socket.IO for handling WebSocket connections. It manages room state in memory.
- **Shared:** A dedicated `shared` directory contains TypeScript types that are used by both the client and the server, ensuring data consistency.

## Building and Running

The project is a monorepo managed with npm workspaces.

- **Install Dependencies:** From the `collaboratve-whiteboard` directory, run:
  ```bash
  npm install
  ```

- **Run in Development:** To start both the client and server for development, run:
  ```bash
  npm run dev
  ```
  - The server will be available at `http://localhost:4000`.
  - The client will be available at `http://localhost:5173`.

- **Build for Production:** To build the entire project, run:
  ```bash
  npm run build
  ```

- **Run Tests:** To run tests for all packages, run:
  ```bash
  npm run test
  ```

## Development Conventions

- **Code Style:** The project uses TypeScript across the stack. Code should be formatted according to the Prettier configuration (if available).
- **Testing:** Tests are written with Vitest. Each package (`client`, `server`, `shared`) has its own test suite.
- **Commits:** Follow conventional commit standards for commit messages.
- **Branching:** Use feature branches for new features and bug fixes.
