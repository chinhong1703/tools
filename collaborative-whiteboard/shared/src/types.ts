export type ElementType =
  | 'pen'
  | 'rect'
  | 'ellipse'
  | 'line'
  | 'arrow'
  | 'text'
  | 'component'
  | 'database'
  | 'user'
  | 'service'
  | 'cloud'
  | 'connector';

export interface ElementBase {
  id: string;
  type: ElementType;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation?: number;
  strokeColor?: string;
  fillColor?: string;
  strokeWidth?: number;
  createdAt: number;
  updatedAt: number;
  createdBy: string;
}

export interface PenElement extends ElementBase {
  type: 'pen';
  points: Array<{ x: number; y: number }>;
}

export interface ShapeElement extends ElementBase {
  type: 'rect' | 'ellipse' | 'line' | 'arrow' | 'connector';
}

export interface TextElement extends ElementBase {
  type: 'text';
  text: string;
  fontSize?: number;
  fontFamily?: string;
  align?: 'left' | 'center' | 'right';
}

export interface IconElement extends ElementBase {
  type: 'component' | 'database' | 'user' | 'service' | 'cloud';
  label?: string;
}

export type BoardElement =
  | PenElement
  | ShapeElement
  | TextElement
  | IconElement;

export interface UserInfo {
  id: string;
  name: string;
  color: string;
  isActive?: boolean;
}

export interface RoomState {
  roomId: string;
  elements: BoardElement[];
  users: UserInfo[];
}

export type BoardEvent =
  | { type: 'join_room'; roomId: string; user: UserInfo }
  | { type: 'initial_state'; roomId: string; state: RoomState }
  | { type: 'element_created'; roomId: string; element: BoardElement }
  | { type: 'element_updated'; roomId: string; element: BoardElement }
  | { type: 'element_deleted'; roomId: string; elementId: string }
  | { type: 'board_cleared'; roomId: string }
  | { type: 'user_joined'; roomId: string; user: UserInfo }
  | { type: 'user_left'; roomId: string; userId: string }
  | { type: 'undo'; roomId: string; userId: string }
  | { type: 'redo'; roomId: string; userId: string };

export interface HistoryAction {
  type: 'create' | 'update' | 'delete' | 'clear';
  element?: BoardElement;
  previous?: BoardElement;
}

export interface ViewportState {
  x: number;
  y: number;
  scale: number;
}

export const DEFAULT_COLORS = ['#2563eb', '#ea580c', '#16a34a', '#db2777', '#0891b2', '#7c3aed'];

export function roomIdIsValid(roomId: string): boolean {
  return /^[a-zA-Z0-9_-]{4,20}$/.test(roomId);
}

export function generateRoomId(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let id = '';
  for (let i = 0; i < 6; i++) {
    id += chars[Math.floor(Math.random() * chars.length)];
  }
  return id;
}
