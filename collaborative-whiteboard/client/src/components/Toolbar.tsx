import { Tool, useWhiteboardStore } from '../state/whiteboardStore';

const tools: { id: Tool; label: string }[] = [
  { id: 'select', label: 'Select' },
  { id: 'pan', label: 'Pan' },
  { id: 'pen', label: 'Pen' },
  { id: 'rect', label: 'Rectangle' },
  { id: 'ellipse', label: 'Ellipse' },
  { id: 'line', label: 'Line' },
  { id: 'arrow', label: 'Arrow' },
  { id: 'connector', label: 'Connector' },
  { id: 'text', label: 'Text' },
  { id: 'component', label: 'Component' },
  { id: 'database', label: 'Database' },
  { id: 'service', label: 'Service' },
  { id: 'cloud', label: 'Cloud' },
  { id: 'user', label: 'User' },
  { id: 'eraser', label: 'Eraser' },
];

export default function Toolbar() {
  const tool = useWhiteboardStore((s) => s.currentTool);
  const setTool = useWhiteboardStore((s) => s.setTool);

  return (
    <nav className="toolbar">
      {tools.map((t) => (
        <button key={t.id} className={tool === t.id ? 'active' : ''} onClick={() => setTool(t.id)}>
          {t.label}
        </button>
      ))}
    </nav>
  );
}
