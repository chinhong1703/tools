import cors from 'cors';
import express from 'express';
import { createServer } from 'http';
import { Server, Socket } from 'socket.io';
import { BoardElement, UserInfo, roomIdIsValid } from '@collaborative-whiteboard/shared';
import { applyRedo, applyUndo, buildRoomState, ensureRoom, RoomStore } from './roomStore';

const rooms: Map<string, RoomStore> = new Map();

const app = express();
app.use(cors());
app.use(express.json());

app.get('/', (_req, res) => {
  res.json({ status: 'ok' });
});

const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: '*',
  },
});

const PORT = process.env.PORT ? Number(process.env.PORT) : 4000;

io.on('connection', (socket: Socket) => {
  socket.on('join_room', ({ roomId, user }: { roomId: string; user: UserInfo }) => {
    if (!roomIdIsValid(roomId)) {
      socket.emit('error_message', { message: 'Invalid room id' });
      return;
    }

    const room = ensureRoom(roomId);
    room.users.set(user.id, user);
    socket.join(roomId);

    socket.emit('initial_state', buildRoomState(roomId, room));
    socket.to(roomId).emit('user_joined', { user, roomId });

    socket.on('element_created', ({ element }: { element: BoardElement }) => {
      const now = Date.now();
      const normalized = { ...element, createdAt: element.createdAt ?? now, updatedAt: now } as BoardElement;
      room.elements.set(element.id, normalized);
      room.history.push({ type: 'create', element: normalized });
      room.future = [];
      io.to(roomId).emit('element_created', { element: normalized, roomId });
    });

    socket.on('element_updated', ({ element }: { element: BoardElement }) => {
      const existing = room.elements.get(element.id);
      const now = Date.now();
      const next = { ...element, updatedAt: now } as BoardElement;
      room.elements.set(element.id, next);
      room.history.push({ type: 'update', element: next, previous: existing });
      room.future = [];
      io.to(roomId).emit('element_updated', { element: next, roomId });
    });

    socket.on('element_deleted', ({ elementId }: { elementId: string }) => {
      const existing = room.elements.get(elementId);
      if (!existing) return;
      room.elements.delete(elementId);
      room.history.push({ type: 'delete', element: existing });
      room.future = [];
      io.to(roomId).emit('element_deleted', { elementId, roomId });
    });

    socket.on('board_cleared', () => {
      const snapshot = Array.from(room.elements.values());
      const clearAction = { type: 'clear' as const, snapshot };
      room.history.push(clearAction);
      room.elements.clear();
      room.future = [];
      io.to(roomId).emit('board_cleared', { roomId });
    });

    socket.on('undo', () => {
      const action = room.history.pop();
      if (!action) return;
      applyUndo(room, roomId, action, (event, payload) => io.to(roomId).emit(event, payload));
    });

    socket.on('redo', () => {
      const action = room.future.pop();
      if (!action) return;
      applyRedo(room, roomId, action, (event, payload) => io.to(roomId).emit(event, payload));
    });

    socket.on('disconnect', () => {
      room.users.delete(user.id);
      socket.to(roomId).emit('user_left', { roomId, userId: user.id });
    });
  });
});

httpServer.listen(PORT, () => {
  console.log(`server listening on ${PORT}`);
});
