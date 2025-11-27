import { describe, expect, it, vi, beforeEach } from 'vitest';
import { BoardElement } from '@collaborative-whiteboard/shared';
import { applyRedo, applyUndo, buildRoomState, createRoomStore, ensureRoom } from './roomStore';

const baseElement: BoardElement = {
  id: 'one',
  type: 'rect',
  x: 0,
  y: 0,
  width: 100,
  height: 80,
  createdAt: 1,
  updatedAt: 1,
  createdBy: 'tester',
};

function makeElement(id: string, overrides: Partial<BoardElement> = {}): BoardElement {
  return { ...baseElement, id, ...overrides };
}

describe('room store helpers', () => {
  const rooms = new Map<string, ReturnType<typeof createRoomStore>>();

  beforeEach(() => {
    rooms.clear();
  });

  it('ensures rooms exist and builds state snapshots', () => {
    const room = ensureRoom(rooms, 'abc123');
    room.elements.set(baseElement.id, baseElement);
    room.users.set('u1', { id: 'u1', name: 'User 1', color: '#000' });

    const state = buildRoomState('abc123', room);
    expect(state.roomId).toBe('abc123');
    expect(state.elements).toHaveLength(1);
    expect(state.users).toHaveLength(1);
  });

  it('undos and redoes create actions', () => {
    const room = createRoomStore();
    const element = makeElement('create');
    room.elements.set(element.id, element);
    room.history.push({ type: 'create', element });

    const emit = vi.fn();
    const action = room.history.pop()!;
    applyUndo(room, 'room', action, emit);

    expect(room.elements.has(element.id)).toBe(false);
    expect(room.future).toHaveLength(1);
    expect(emit).toHaveBeenCalledWith('element_deleted', { roomId: 'room', elementId: element.id });

    const redo = room.future.pop()!;
    applyRedo(room, 'room', redo, emit);
    expect(room.elements.has(element.id)).toBe(true);
    expect(room.history.at(-1)).toEqual(redo);
  });

  it('undos updates with previous snapshots', () => {
    const room = createRoomStore();
    const original = makeElement('update', { width: 50 });
    const updated = { ...original, width: 120, updatedAt: 2 };
    room.elements.set(original.id, updated);
    room.history.push({ type: 'update', element: updated, previous: original });

    const emit = vi.fn();
    const action = room.history.pop()!;
    applyUndo(room, 'room', action, emit);

    expect(room.elements.get(original.id)).toEqual(original);
    expect(room.future).toHaveLength(1);
    expect(emit).toHaveBeenCalledWith('element_updated', { roomId: 'room', element: original });

    const redo = room.future.pop()!;
    applyRedo(room, 'room', redo, emit);
    expect(room.elements.get(original.id)).toEqual(updated);
  });

  it('undos deletes by restoring elements', () => {
    const room = createRoomStore();
    const removed = makeElement('delete');
    room.history.push({ type: 'delete', element: removed });

    const emit = vi.fn();
    const action = room.history.pop()!;
    applyUndo(room, 'room', action, emit);

    expect(room.elements.get(removed.id)).toEqual(removed);
    expect(room.future).toHaveLength(1);
    expect(emit).toHaveBeenCalledWith('element_created', { roomId: 'room', element: removed });

    const redo = room.future.pop()!;
    applyRedo(room, 'room', redo, emit);
    expect(room.elements.has(removed.id)).toBe(false);
  });

  it('captures clear snapshots for undo and redo', () => {
    const room = createRoomStore();
    const first = makeElement('first');
    const second = makeElement('second');
    room.elements.set(first.id, first);
    room.elements.set(second.id, second);
    room.history.push({ type: 'clear', snapshot: [first, second] });

    const emit = vi.fn();
    const action = room.history.pop()!;
    applyUndo(room, 'room', action, emit);

    expect(Array.from(room.elements.values())).toEqual([first, second]);
    expect(room.future).toHaveLength(1);
    expect(emit).toHaveBeenCalledWith('element_created', { roomId: 'room', element: first });
    expect(emit).toHaveBeenCalledWith('element_created', { roomId: 'room', element: second });

    const redo = room.future.pop()!;
    applyRedo(room, 'room', redo, emit);
    expect(room.elements.size).toBe(0);
    expect(emit).toHaveBeenCalledWith('board_cleared', { roomId: 'room' });
  });
});
