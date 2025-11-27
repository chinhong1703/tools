import { create } from 'zustand';
import { nanoid } from 'nanoid';
import {
  BoardElement,
  DEFAULT_COLORS,
  ElementType,
  RoomState,
  UserInfo,
  ViewportState,
} from '@collaborative-whiteboard/shared';

export type Tool =
  | 'select'
  | 'pan'
  | 'pen'
  | 'rect'
  | 'ellipse'
  | 'line'
  | 'arrow'
  | 'text'
  | 'eraser'
  | 'component'
  | 'database'
  | 'user'
  | 'service'
  | 'cloud'
  | 'connector';

export interface WhiteboardState {
  roomId: string | null;
  elements: BoardElement[];
  users: UserInfo[];
  selectedId?: string;
  currentTool: Tool;
  strokeColor: string;
  fillColor: string;
  strokeWidth: number;
  fontSize: number;
  viewport: ViewportState;
  setRoom: (roomId: string) => void;
  setElements: (elements: BoardElement[]) => void;
  addElement: (element: BoardElement) => void;
  updateElement: (element: BoardElement) => void;
  deleteElement: (id: string) => void;
  clearBoard: () => void;
  setUsers: (users: UserInfo[]) => void;
  upsertUser: (user: UserInfo) => void;
  removeUser: (userId: string) => void;
  setTool: (tool: Tool) => void;
  setSelected: (id?: string) => void;
  updateStyle: (style: Partial<Pick<WhiteboardState, 'strokeColor' | 'fillColor' | 'strokeWidth' | 'fontSize'>>) => void;
  setViewport: (v: ViewportState) => void;
  generateUser: () => UserInfo;
}

export const useWhiteboardStore = create<WhiteboardState>((set, get) => ({
  roomId: null,
  elements: [],
  users: [],
  currentTool: 'select',
  strokeColor: '#2563eb',
  fillColor: '#ffffff',
  strokeWidth: 3,
  fontSize: 18,
  viewport: { x: 0, y: 0, scale: 1 },
  setRoom: (roomId) => set({ roomId }),
  setElements: (elements) => set({ elements }),
  addElement: (element) =>
    set((state) => {
      const existing = state.elements.find((el) => el.id === element.id);
      if (existing) {
        return {
          elements: state.elements.map((el) => (el.id === element.id ? element : el)),
        };
      }
      return { elements: [...state.elements, element] };
    }),
  updateElement: (element) =>
    set((state) => ({
      elements: state.elements.map((el) => (el.id === element.id ? element : el)),
    })),
  deleteElement: (id) => set((state) => ({ elements: state.elements.filter((el) => el.id !== id) })),
  clearBoard: () => set({ elements: [] }),
  setUsers: (users) => set({ users }),
  upsertUser: (user) =>
    set((state) => {
      const existing = state.users.find((u) => u.id === user.id);
      if (existing) {
        return { users: state.users.map((u) => (u.id === user.id ? user : u)) };
      }
      return { users: [...state.users, user] };
    }),
  removeUser: (userId) => set((state) => ({ users: state.users.filter((u) => u.id !== userId) })),
  setTool: (tool) => set({ currentTool: tool }),
  setSelected: (selectedId) => set({ selectedId }),
  updateStyle: (style) => set(style),
  setViewport: (viewport) => set({ viewport }),
  generateUser: () => {
    const colors = DEFAULT_COLORS || ['#2563eb', '#ea580c', '#16a34a', '#db2777', '#0891b2', '#7c3aed'];
    try {
      const name = `User ${Math.floor(Math.random() * 90) + 10}`;
      const color = colors[Math.floor(Math.random() * colors.length)];
      return { id: nanoid(), name, color };
    } catch (e) {
      console.error('Error generating user:', e);
      return { id: 'fallback-' + Date.now(), name: 'Guest', color: colors[0] };
    }
  },
}));

export function syncRoomState(state: RoomState) {
  const store = useWhiteboardStore.getState();
  store.setRoom(state.roomId);
  store.setElements(state.elements);
  store.setUsers(state.users);
}

export function iconForType(type: ElementType): string {
  switch (type) {
    case 'database':
      return 'DB';
    case 'user':
      return 'User';
    case 'service':
      return 'API';
    case 'cloud':
      return 'Cloud';
    case 'component':
      return 'Component';
    default:
      return '';
  }
}
