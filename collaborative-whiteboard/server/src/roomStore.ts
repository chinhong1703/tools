import { BoardElement, HistoryAction, RoomState, UserInfo } from '@collaborative-whiteboard/shared';

export type RoomStore = {
  elements: Map<string, BoardElement>;
  history: HistoryAction[];
  future: HistoryAction[];
  users: Map<string, UserInfo>;
};

export type EmitFn = (event: string, payload: Record<string, unknown>) => void;

export function createRoomStore(): RoomStore {
  return {
    elements: new Map(),
    history: [],
    future: [],
    users: new Map(),
  };
}

export function ensureRoom(rooms: Map<string, RoomStore>, roomId: string): RoomStore {
  if (!rooms.has(roomId)) {
    rooms.set(roomId, createRoomStore());
  }
  return rooms.get(roomId)!;
}

export function buildRoomState(roomId: string, room: RoomStore): RoomState {
  return {
    roomId,
    elements: Array.from(room.elements.values()),
    users: Array.from(room.users.values()),
  };
}

export function applyUndo(room: RoomStore, roomId: string, action: HistoryAction, emit: EmitFn) {
  switch (action.type) {
    case 'create': {
      if (action.element) {
        room.elements.delete(action.element.id);
        room.future.push(action);
        emit('element_deleted', { roomId, elementId: action.element.id });
      }
      break;
    }
    case 'update': {
      if (action.previous) {
        room.elements.set(action.previous.id, action.previous);
        room.future.push(action);
        emit('element_updated', { roomId, element: action.previous });
      }
      break;
    }
    case 'delete': {
      if (action.element) {
        room.elements.set(action.element.id, action.element);
        room.future.push(action);
        emit('element_created', { roomId, element: action.element });
      }
      break;
    }
    case 'clear': {
      if (action.snapshot) {
        room.elements.clear();
        action.snapshot.forEach((el) => room.elements.set(el.id, el));
        room.future.push(action);
        action.snapshot.forEach((element) => emit('element_created', { roomId, element }));
      }
      break;
    }
  }
}

export function applyRedo(room: RoomStore, roomId: string, action: HistoryAction, emit: EmitFn) {
  switch (action.type) {
    case 'create': {
      if (action.element) {
        room.elements.set(action.element.id, action.element);
        emit('element_created', { roomId, element: action.element });
      }
      break;
    }
    case 'update': {
      if (action.element) {
        room.elements.set(action.element.id, action.element);
        emit('element_updated', { roomId, element: action.element });
      }
      break;
    }
    case 'delete': {
      if (action.element) {
        room.elements.delete(action.element.id);
        emit('element_deleted', { roomId, elementId: action.element.id });
      }
      break;
    }
    case 'clear': {
      room.elements.clear();
      emit('board_cleared', { roomId });
      break;
    }
  }
  room.history.push(action);
}
