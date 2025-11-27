import { describe, it } from 'vitest';
import { render } from '@testing-library/react';
import WhiteboardCanvas from './WhiteboardCanvas';

describe('WhiteboardCanvas', () => {
  it('renders without crashing', () => {
    const mockViewport = { x: 0, y: 0, scale: 1 };
    render(
      <WhiteboardCanvas
        elements={[]}
        tool="select"
        strokeColor="#000"
        fillColor="#fff"
        strokeWidth={1}
        fontSize={16}
        onCreate={() => {}}
        onUpdate={() => {}}
        onDelete={() => {}}
        onSelect={() => {}}
        viewport={mockViewport}
        onViewportChange={() => {}}
      />
    );
  });
});
