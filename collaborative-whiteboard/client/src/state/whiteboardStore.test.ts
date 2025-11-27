import { beforeEach, describe, expect, it } from 'vitest';
import { BoardElement, RoomState, UserInfo } from '@collaborative-whiteboard/shared';
import { iconForType, syncRoomState, useWhiteboardStore } from './whiteboardStore';

type Store = typeof useWhiteboardStore.getState;

function resetStore(getState: Store) {
  const state = getState();
  state.setRoom(null);
  state.setElements([]);
  state.setUsers([]);
  state.setSelected(undefined);
  state.setTool('select');
  state.updateStyle({ strokeColor: '#2563eb', fillColor: '#ffffff', strokeWidth: 3, fontSize: 18 });
  state.setViewport({ x: 0, y: 0, scale: 1 });
}

describe('whiteboard store state operations', () => {
  beforeEach(() => resetStore(useWhiteboardStore.getState));

  it('adds, updates, and deletes elements', () => {
    const element: BoardElement = {
      id: '1',
      type: 'rect',
      x: 0,
      y: 0,
      width: 10,
      height: 10,
      createdAt: 1,
      updatedAt: 1,
      createdBy: 'tester',
    };

    useWhiteboardStore.getState().addElement(element);
    expect(useWhiteboardStore.getState().elements).toHaveLength(1);

    const updated = { ...element, width: 20 };
    useWhiteboardStore.getState().addElement(updated);
    expect(useWhiteboardStore.getState().elements[0]).toEqual(updated);

    const moved = { ...updated, x: 5 };
    useWhiteboardStore.getState().updateElement(moved);
    expect(useWhiteboardStore.getState().elements[0]).toEqual(moved);

    useWhiteboardStore.getState().deleteElement(moved.id);
    expect(useWhiteboardStore.getState().elements).toHaveLength(0);
  });

  it('clears board and tracks selection/tool state', () => {
    useWhiteboardStore.getState().setElements([
      {
        id: 'pen',
        type: 'pen',
        x: 0,
        y: 0,
        width: 0,
        height: 0,
        points: [],
        createdAt: 1,
        updatedAt: 1,
        createdBy: 'tester',
      },
    ]);

    useWhiteboardStore.getState().setSelected('pen');
    useWhiteboardStore.getState().setTool('eraser');
    expect(useWhiteboardStore.getState().selectedId).toBe('pen');
    expect(useWhiteboardStore.getState().currentTool).toBe('eraser');

    useWhiteboardStore.getState().clearBoard();
    expect(useWhiteboardStore.getState().elements).toHaveLength(0);
    useWhiteboardStore.getState().setSelected();
    expect(useWhiteboardStore.getState().selectedId).toBeUndefined();
  });

  it('manages user presence and syncs room state', () => {
    const user: UserInfo = { id: 'u1', name: 'User One', color: '#000' };
    useWhiteboardStore.getState().upsertUser(user);
    expect(useWhiteboardStore.getState().users[0]).toEqual(user);

    const updatedUser = { ...user, name: 'Updated', isActive: true };
    useWhiteboardStore.getState().upsertUser(updatedUser);
    expect(useWhiteboardStore.getState().users[0]).toEqual(updatedUser);

    useWhiteboardStore.getState().removeUser(user.id);
    expect(useWhiteboardStore.getState().users).toHaveLength(0);

    const roomState: RoomState = {
      roomId: 'room-1',
      users: [user],
      elements: [
        {
          id: 'shape',
          type: 'line',
          x: 1,
          y: 2,
          width: 3,
          height: 4,
          createdAt: 1,
          updatedAt: 1,
          createdBy: 'tester',
        },
      ],
    };

    syncRoomState(roomState);
    expect(useWhiteboardStore.getState().roomId).toBe(roomState.roomId);
    expect(useWhiteboardStore.getState().users).toEqual(roomState.users);
    expect(useWhiteboardStore.getState().elements).toEqual(roomState.elements);
  });

  it('generates user identities and maps icons', () => {
    const generated = useWhiteboardStore.getState().generateUser();
    expect(generated.id).toBeTruthy();
    expect(generated.name).toMatch(/User/);
    expect(generated.color).toMatch(/^#/);

    expect(iconForType('database')).toBe('DB');
    expect(iconForType('user')).toBe('User');
    expect(iconForType('service')).toBe('API');
    expect(iconForType('cloud')).toBe('Cloud');
    expect(iconForType('component')).toBe('Component');
    expect(iconForType('rect' as any)).toBe('');
  });
});
