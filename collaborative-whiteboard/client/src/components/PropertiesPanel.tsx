import { useWhiteboardStore } from '../state/whiteboardStore';

export default function PropertiesPanel() {
  const strokeColor = useWhiteboardStore((s) => s.strokeColor);
  const fillColor = useWhiteboardStore((s) => s.fillColor);
  const strokeWidth = useWhiteboardStore((s) => s.strokeWidth);
  const fontSize = useWhiteboardStore((s) => s.fontSize);
  const updateStyle = useWhiteboardStore((s) => s.updateStyle);

  return (
    <aside className="properties">
      <h3>Properties</h3>
      <label>
        Stroke color
        <input type="color" value={strokeColor} onChange={(e) => updateStyle({ strokeColor: e.target.value })} />
      </label>
      <label>
        Fill color
        <input type="color" value={fillColor} onChange={(e) => updateStyle({ fillColor: e.target.value })} />
      </label>
      <label>
        Stroke width
        <input
          type="number"
          value={strokeWidth}
          min={1}
          max={12}
          onChange={(e) => updateStyle({ strokeWidth: Number(e.target.value) })}
        />
      </label>
      <label>
        Font size
        <input
          type="number"
          value={fontSize}
          min={8}
          max={64}
          onChange={(e) => updateStyle({ fontSize: Number(e.target.value) })}
        />
      </label>
    </aside>
  );
}
