import { useEffect, useMemo, useRef, useState } from 'react';
import { Stage, Layer, Rect, Ellipse, Line as KonvaLine, Text as KonvaText, Arrow } from 'react-konva';
import Konva from 'konva';
import { BoardElement, ViewportState } from '@collaborative-whiteboard/shared';
import { Tool } from '../state/whiteboardStore';

interface WhiteboardCanvasProps {
  elements: BoardElement[];
  selectedId?: string;
  tool: Tool;
  strokeColor: string;
  fillColor: string;
  strokeWidth: number;
  fontSize: number;
  onCreate: (element: BoardElement) => void;
  onUpdate: (element: BoardElement) => void;
  onDelete: (id: string) => void;
  onSelect: (id?: string) => void;
  viewport: ViewportState;
  onViewportChange: (v: ViewportState) => void;
}

export default function WhiteboardCanvas({
  elements,
  selectedId,
  tool,
  strokeColor,
  fillColor,
  strokeWidth,
  fontSize,
  onCreate,
  onUpdate,
  onDelete,
  onSelect,
  viewport,
  onViewportChange,
}: WhiteboardCanvasProps) {
  const [draft, setDraft] = useState<BoardElement | null>(null);
  const [stageSize, setStageSize] = useState({ width: window.innerWidth - 320, height: window.innerHeight - 80 });
  const stageRef = useRef<Konva.Stage>(null);

  useEffect(() => {
    const onResize = () => setStageSize({ width: window.innerWidth - 320, height: window.innerHeight - 80 });
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const transformerRef = useRef<Konva.Transformer>(null);

  useEffect(() => {
    const stage = stageRef.current;
    const transformer = transformerRef.current;
    if (!stage || !transformer) return;
    const selectedNode = stage.findOne(`#${selectedId}`) as Konva.Node | undefined;
    if (selectedNode) {
      transformer.nodes([selectedNode]);
    } else {
      transformer.nodes([]);
    }
    transformer.getLayer()?.batchDraw();
  }, [selectedId, draft, elements]);

  const handleMouseDown = (e: Konva.KonvaEventObject<MouseEvent>) => {
    if (tool === 'select' || tool === 'eraser') return;
    const stage = stageRef.current;
    if (!stage) return;
    const pointer = stage.getPointerPosition();
    if (!pointer) return;

    const { x, y } = toWorld(pointer, viewport);
    const base = createElement(tool, x, y, strokeColor, fillColor, strokeWidth, fontSize);
    setDraft(base);
  };

  const handleMouseMove = (e: Konva.KonvaEventObject<MouseEvent>) => {
    if (!draft) return;
    const stage = stageRef.current;
    if (!stage) return;
    const pointer = stage.getPointerPosition();
    if (!pointer) return;
    const { x, y } = toWorld(pointer, viewport);

    if (draft.type === 'pen') {
      const penDraft = { ...draft, points: [...draft.points, { x, y }] } as BoardElement;
      setDraft(penDraft);
      onUpdate(penDraft);
    } else {
      const width = x - draft.x;
      const height = y - draft.y;
      setDraft({ ...draft, width, height });
    }
  };

  const handleMouseUp = () => {
    if (!draft) return;
    onCreate({ ...draft, width: draft.width, height: draft.height });
    setDraft(null);
  };

  const handleSelect = (id?: string) => {
    onSelect(id);
  };

  const handleTransform = (node: Konva.Node) => {
    const id = node.id();
    const element = elements.find((el) => el.id === id);
    if (!element) return;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    const updated: BoardElement = {
      ...element,
      x: node.x(),
      y: node.y(),
      width: element.width * scaleX,
      height: element.height * scaleY,
      rotation: node.rotation(),
    } as BoardElement;
    node.scaleX(1);
    node.scaleY(1);
    onUpdate(updated);
  };

  const shapes = useMemo(() => (draft ? [...elements, draft] : elements), [draft, elements]);

  const onWheel = (e: Konva.KonvaEventObject<WheelEvent>) => {
    e.evt.preventDefault();
    const scaleBy = 1.05;
    const stage = stageRef.current;
    if (!stage) return;
    const oldScale = viewport.scale;
    const pointer = stage.getPointerPosition();
    if (!pointer) return;
    const mousePointTo = {
      x: (pointer.x - viewport.x) / oldScale,
      y: (pointer.y - viewport.y) / oldScale,
    };
    const newScale = e.evt.deltaY > 0 ? oldScale / scaleBy : oldScale * scaleBy;
    const newPos = {
      x: pointer.x - mousePointTo.x * newScale,
      y: pointer.y - mousePointTo.y * newScale,
      scale: newScale,
    };
    onViewportChange(newPos);
  };

  const handleDragMove = (e: Konva.KonvaEventObject<DragEvent>) => {
    if (tool !== 'pan') return;
    const pos = e.target.position();
    onViewportChange({ ...viewport, x: pos.x, y: pos.y });
  };

  const handleStageClick = (e: Konva.KonvaEventObject<MouseEvent>) => {
    if (tool === 'eraser') {
      const target = e.target;
      if (target && target.id()) onDelete(target.id());
      return;
    }
    if (tool === 'select') {
      const id = e.target?.id();
      handleSelect(id || undefined);
    } else if (tool === 'text') {
      const stage = stageRef.current;
      if (!stage) return;
      const pointer = stage.getPointerPosition();
      if (!pointer) return;
      const { x, y } = toWorld(pointer, viewport);
      const text = window.prompt('Enter text', 'New text');
      if (text) {
        const element: BoardElement = {
          id: `${Date.now()}`,
          type: 'text',
          x,
          y,
          width: 120,
          height: 30,
          text,
          strokeColor,
          fillColor,
          strokeWidth,
          fontSize,
          createdAt: Date.now(),
          updatedAt: Date.now(),
          createdBy: 'local',
        } as BoardElement;
        onCreate(element);
      }
    }
  };

  return (
    <div className="canvas-wrapper">
      <Stage
        width={stageSize.width}
        height={stageSize.height}
        scaleX={viewport.scale}
        scaleY={viewport.scale}
        x={viewport.x}
        y={viewport.y}
        draggable={tool === 'pan'}
        onWheel={onWheel}
        onDragMove={handleDragMove}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onClick={handleStageClick}
        ref={stageRef}
      >
        <Layer>
          {shapes.map((el) => {
            const common = {
              id: el.id,
              x: el.x,
              y: el.y,
              stroke: el.strokeColor || '#111827',
              fill: el.fillColor || 'transparent',
              strokeWidth: el.strokeWidth ?? strokeWidth,
              rotation: el.rotation || 0,
              onClick: () => handleSelect(el.id),
              onTap: () => handleSelect(el.id),
            };
            if (el.type === 'pen') {
              const points = el.points.flatMap((p) => [p.x, p.y]);
              return <KonvaLine key={el.id} {...common} points={points} tension={0.5} lineCap="round" />;
            }
            if (
              el.type === 'rect' ||
              el.type === 'component' ||
              el.type === 'database' ||
              el.type === 'service' ||
              el.type === 'cloud' ||
              el.type === 'user'
            ) {
              const label = labelForIcon(el.type);
              return (
                <>
                  <Rect
                    key={el.id}
                    {...common}
                    width={el.width}
                    height={el.height}
                    cornerRadius={6}
                    fill={el.fillColor || '#fff'}
                  />
                  {label && (
                    <KonvaText
                      key={`${el.id}-text`}
                      text={label}
                      x={el.x + 8}
                      y={el.y + 8}
                      fontSize={14}
                      fill={el.strokeColor || '#111827'}
                    />
                  )}
                </>
              );
            }
            if (el.type === 'ellipse') {
              return (
                <Ellipse
                  key={el.id}
                  {...common}
                  radiusX={Math.abs(el.width)}
                  radiusY={Math.abs(el.height)}
                />
              );
            }
            if (el.type === 'line' || el.type === 'connector') {
              return <KonvaLine key={el.id} {...common} points={[el.x, el.y, el.x + el.width, el.y + el.height]} />;
            }
            if (el.type === 'arrow') {
              return <Arrow key={el.id} {...common} points={[el.x, el.y, el.x + el.width, el.y + el.height]} pointerLength={12} pointerWidth={12} />;
            }
            if (el.type === 'text') {
              const textEl = el as any;
              return (
                <KonvaText
                  key={el.id}
                  {...common}
                  text={textEl.text}
                  fontSize={textEl.fontSize || fontSize}
                  width={textEl.width}
                />
              );
            }
            return null;
          })}
          <Konva.Transformer
            ref={transformerRef}
            onTransformEnd={(e) => handleTransform(e.target)}
            rotateEnabled
            enabledAnchors={['top-left', 'top-right', 'bottom-left', 'bottom-right']}
          />
        </Layer>
      </Stage>
      <div className="mini-map">
        <Stage width={200} height={150} scaleX={0.2} scaleY={0.2} x={-viewport.x / 5} y={-viewport.y / 5}>
          <Layer>
            {elements.map((el) => (
              <Rect key={`mini-${el.id}`} x={el.x} y={el.y} width={el.width} height={el.height} stroke="#94a3b8" />
            ))}
          </Layer>
        </Stage>
        <button onClick={() => onViewportChange({ x: 0, y: 0, scale: 1 })}>Re-center</button>
      </div>
    </div>
  );
}

function createElement(
  tool: Tool,
  x: number,
  y: number,
  strokeColor: string,
  fillColor: string,
  strokeWidth: number,
  fontSize: number
): BoardElement {
  const base = {
    id: `${Date.now()}-${Math.random()}`,
    x,
    y,
    width: 0,
    height: 0,
    rotation: 0,
    strokeColor,
    fillColor,
    strokeWidth,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    createdBy: 'local',
  } as BoardElement;

  if (tool === 'pen') {
    return { ...base, type: 'pen', points: [{ x, y }] } as BoardElement;
  }

  if (tool === 'text') {
    return { ...base, type: 'text', width: 120, height: 40, fontSize, text: 'Text' } as BoardElement;
  }

  if (tool === 'component' || tool === 'database' || tool === 'service' || tool === 'cloud' || tool === 'user') {
    return { ...base, type: tool, width: 140, height: 80, fillColor: '#ffffff', strokeColor } as BoardElement;
  }

  return { ...base, type: tool } as BoardElement;
}

function toWorld(point: { x: number; y: number }, viewport: ViewportState) {
  return {
    x: (point.x - viewport.x) / viewport.scale,
    y: (point.y - viewport.y) / viewport.scale,
  };
}

function labelForIcon(type: BoardElement['type']) {
  switch (type) {
    case 'database':
      return 'Database';
    case 'service':
      return 'Service';
    case 'cloud':
      return 'Cloud';
    case 'user':
      return 'User';
    case 'component':
      return 'Component';
    default:
      return '';
  }
}
