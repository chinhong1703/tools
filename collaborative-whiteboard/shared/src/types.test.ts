import { describe, expect, it } from 'vitest';
import { generateRoomId, roomIdIsValid } from './types';

describe('room utilities', () => {
  it('generates valid room ids', () => {
    for (let i = 0; i < 5; i++) {
      const id = generateRoomId();
      expect(roomIdIsValid(id)).toBe(true);
      expect(id.length).toBe(6);
    }
  });

  it('rejects invalid ids', () => {
    expect(roomIdIsValid('!bad')).toBe(false);
    expect(roomIdIsValid('abc')).toBe(false);
    expect(roomIdIsValid('waytoolongroomidthatexceedslimit')).toBe(false);
  });
});
