import { useEffect, useMemo, useRef, useState } from 'react';
import { io, Socket } from 'socket.io-client';
import {
  BoardElement,
  RoomState,
  UserInfo,
  roomIdIsValid,
} from '@collaborative-whiteboard/shared';
import { syncRoomState, useWhiteboardStore } from '../state/whiteboardStore';

const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || 'http://localhost:4000';

export function useWhiteboardSocket(roomId: string) {
  const [connected, setConnected] = useState(false);
  const user = useMemo(() => useWhiteboardStore.getState().generateUser(), []);
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!roomIdIsValid(roomId)) return;
    const socket: Socket = io(SOCKET_URL, { transports: ['websocket'] });
    socketRef.current = socket;

    socket.on('connect', () => {
      setConnected(true);
      socket.emit('join_room', { roomId, user });
    });

    socket.on('initial_state', (payload: { state: RoomState }) => {
      syncRoomState(payload.state);
    });

    socket.on('user_joined', ({ user: joined }: { user: UserInfo }) => {
      useWhiteboardStore.getState().upsertUser(joined);
    });

    socket.on('user_left', ({ userId }: { userId: string }) => {
      useWhiteboardStore.getState().removeUser(userId);
    });

    socket.on('element_created', ({ element }: { element: BoardElement }) => {
      useWhiteboardStore.getState().addElement(element);
    });

        socket.on(

          'element_updated',

          ({ element }: { element: BoardElement }) => {

            useWhiteboardStore.getState().updateElement(element);

          }

        );

    socket.on('element_deleted', ({ elementId }: { elementId: string }) => {
      useWhiteboardStore.getState().deleteElement(elementId);
    });

        socket.on('board_cleared', () =>

          useWhiteboardStore.getState().clearBoard()

        );

    socket.on('disconnect', () => setConnected(false));

    return () => {
      socket.disconnect();
      socketRef.current = null;
    };
  }, [roomId, user]);

  const emit = (event: string, payload: unknown) => {
    socketRef.current?.emit(event, payload);
  };

  return { connected, user, emit };
}
