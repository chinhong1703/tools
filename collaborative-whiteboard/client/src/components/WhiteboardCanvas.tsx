import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Stage,
  Layer,
  Rect,
  Ellipse,
  Line as KonvaLine,
  Text as KonvaText,
  Arrow,
  Group,
  Path,
  Circle,
  Transformer,
} from 'react-konva';
import Konva from 'konva';
import { BoardElement, ViewportState } from '@collaborative-whiteboard/shared';
import { Tool } from '../state/whiteboardStore';

type RenderContext = {
  onSelect: (id?: string) => void;
  selectedId?: string;
  tool: Tool;
  strokeWidth: number;
  fontSize: number;
  onUpdate: (el: BoardElement) => void;
};

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
  const [stageSize, setStageSize] = useState({ width: 0, height: 0 });
  const stageRef = useRef<Konva.Stage>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [isErasing, setIsErasing] = useState(false);

  useEffect(() => {
    const resize = () => {
      const bounds = wrapperRef.current?.getBoundingClientRect();
      if (bounds) {
        setStageSize({ width: bounds.width, height: bounds.height });
      }
    };
    resize();
    const observer = new ResizeObserver(resize);
    if (wrapperRef.current) observer.observe(wrapperRef.current);
    window.addEventListener('resize', resize);
    return () => {
      observer.disconnect();
      window.removeEventListener('resize', resize);
    };
  }, []);

  const transformerRef = useRef<Konva.Transformer>(null);

  useEffect(() => {
    const stage = stageRef.current;
    const transformer = transformerRef.current;
    if (!stage || !transformer) return;
    const selectedNode = selectedId ? stage.findOne(`#${selectedId}`) : undefined;
    if (selectedNode) {
      transformer.nodes([selectedNode]);
    } else {
      transformer.nodes([]);
    }
    transformer.getLayer()?.batchDraw();
  }, [selectedId, draft, elements]);

  useEffect(() => {
    const stage = stageRef.current;
    if (!stage) return;
    const cursorByTool: Record<Tool, string> = {
      select: 'default',
      pen: 'crosshair',
      rect: 'crosshair',
      ellipse: 'crosshair',
      line: 'crosshair',
      arrow: 'crosshair',
      connector: 'crosshair',
      text: 'text',
      eraser: 'cell',
      pan: 'grab',
      component: 'crosshair',
      database: 'crosshair',
      user: 'crosshair',
      service: 'crosshair',
      cloud: 'crosshair',
    };
    stage.container().style.cursor = cursorByTool[tool];
  }, [tool]);

  const eraseAtPointer = () => {
    const stage = stageRef.current;
    if (!stage) return;
    const pointer = stage.getRelativePointerPosition();
    if (!pointer) return;
    const node = stage.getIntersection(pointer);
    if (node?.id()) {
      onDelete(node.id());
    }
  };

  const handleMouseDown = () => {
    if (tool === 'select') return;
    if (tool === 'eraser') {
      setIsErasing(true);
      eraseAtPointer();
      return;
    }
    const stage = stageRef.current;
    if (!stage) return;
    const pointer = getWorldPointer(stage, viewport);
    if (!pointer) return;

    const { x, y } = pointer;
    const base = createElement(tool, x, y, strokeColor, fillColor, strokeWidth, fontSize);
    setDraft(base);
  };

  const handleMouseMove = () => {
    if (isErasing) {
      eraseAtPointer();
    }
    if (!draft) return;
    const stage = stageRef.current;
    if (!stage) return;
    const pointer = getWorldPointer(stage, viewport);
    if (!pointer) return;
    const { x, y } = pointer;

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
    setIsErasing(false);
    if (!draft) return;
    onCreate({ ...draft, width: draft.width, height: draft.height });
    setDraft(null);
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
      onSelect(id || undefined);
    } else if (tool === 'text') {
      const stage = stageRef.current;
      if (!stage) return;
      const pointer = getWorldPointer(stage, viewport);
      if (!pointer) return;
      const { x, y } = pointer;
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
    <div className="canvas-wrapper" ref={wrapperRef}>
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
          {shapes.map((el) =>
            renderElement(el, {
              onSelect,
              selectedId,
              tool,
              strokeWidth,
              fontSize,
              onUpdate,
            })
          )}
          <Transformer
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

function renderElement(el: BoardElement, context: RenderContext) {
  const { onSelect, selectedId, tool, strokeWidth, fontSize, onUpdate } = context;
  const isSelected = selectedId === el.id;
  const draggable = tool === 'select' && isSelected;
  const common = {
    id: el.id,
    stroke: el.strokeColor || '#111827',
    fill: el.fillColor || 'transparent',
    strokeWidth: el.strokeWidth ?? strokeWidth,
    rotation: el.rotation || 0,
    draggable,
    onClick: () => onSelect(el.id),
    onTap: () => onSelect(el.id),
    onDragEnd: (e: Konva.KonvaEventObject<DragEvent>) => {
      const node = e.target;
      const updated = { ...el, x: node.x(), y: node.y() } as BoardElement;
      onUpdate(updated);
    },
  };

  if (el.type === 'pen') {
    const points = el.points.flatMap((p) => [p.x, p.y]);
    return <KonvaLine key={el.id} {...common} points={points} tension={0.5} lineCap="round" />;
  }

  if (el.type === 'rect') {
    const box = normalizeBox(el);
    return <Rect key={el.id} {...common} {...box} cornerRadius={6} />;
  }

  if (el.type === 'ellipse') {
    const box = normalizeBox(el);
    return (
      <Ellipse
        key={el.id}
        {...common}
        x={box.x + box.width / 2}
        y={box.y + box.height / 2}
        radiusX={box.width / 2}
        radiusY={box.height / 2}
      />
    );
  }

  if (el.type === 'line' || el.type === 'connector') {
    const box = normalizeBox(el);
    return <KonvaLine key={el.id} {...common} points={[box.x, box.y, box.x + box.width, box.y + box.height]} />;
  }

  if (el.type === 'arrow') {
    const box = normalizeBox(el);
    return (
      <Arrow
        key={el.id}
        {...common}
        points={[box.x, box.y, box.x + box.width, box.y + box.height]}
        pointerLength={12}
        pointerWidth={12}
      />
    );
  }

  if (el.type === 'text') {
    const textEl = el as any;
    return (
      <KonvaText
        key={el.id}
        {...common}
        x={el.x}
        y={el.y}
        text={textEl.text}
        fontSize={textEl.fontSize || fontSize}
        width={textEl.width}
      />
    );
  }

  if (el.type === 'component' || el.type === 'database' || el.type === 'service' || el.type === 'cloud' || el.type === 'user') {
    const icon = renderIcon(el);
    return (
      <Group key={el.id} {...common}>
        {icon}
      </Group>
    );
  }

  return null;
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

function normalizeBox(el: BoardElement) {
  const width = Math.abs(el.width || 0);
  const height = Math.abs(el.height || 0);
  const x = el.width >= 0 ? el.x : el.x + el.width;
  const y = el.height >= 0 ? el.y : el.y + el.height;
  return { x, y, width, height };
}

function renderIcon(el: BoardElement) {
  const box = normalizeBox(el);
  const color = el.strokeColor || '#111827';
  const fill = el.fillColor || '#fff';

  if (el.type === 'database') {
    return (
      <Group>
        <Rect x={box.x} y={box.y + box.height * 0.15} width={box.width} height={box.height * 0.7} stroke={color} fill={fill} cornerRadius={8} />
        <Ellipse x={box.x + box.width / 2} y={box.y + box.height * 0.15} radiusX={box.width / 2} radiusY={Math.max(8, box.height * 0.15)} stroke={color} fill={fill} />
        <Ellipse
          x={box.x + box.width / 2}
          y={box.y + box.height * 0.85}
          radiusX={box.width / 2}
          radiusY={Math.max(6, box.height * 0.12)}
          stroke={color}
          fill={fill}
        />
      </Group>
    );
  }

  if (el.type === 'user') {
    const radius = Math.min(box.width, box.height) / 2;
    return (
      <Group>
        <Circle x={box.x + box.width / 2} y={box.y + radius * 0.9} radius={radius * 0.4} stroke={color} fill={fill} />
        <Path
          x={box.x + box.width / 2 - radius * 0.8}
          y={box.y + radius}
          data={`M${radius * 0.8},${radius * 0.6} q${radius * 0.8},${radius * 0.5} ${radius * 0.8},${radius * 1.4} h-${radius * 1.6} q0-${radius * 0.9} ${radius * 0.8}-${radius * 1.4} z`}
          stroke={color}
          fill={fill}
        />
      </Group>
    );
  }

  if (el.type === 'service' || el.type === 'component') {
    return (
      <Group>
        <Rect x={box.x} y={box.y} width={box.width} height={box.height} stroke={color} fill={fill} cornerRadius={10} />
        <Rect x={box.x + 8} y={box.y + 10} width={box.width - 16} height={box.height - 20} stroke={color} fill={fill} cornerRadius={6} />
        <KonvaLine points={[box.x + 12, box.y + box.height / 2, box.x + box.width - 12, box.y + box.height / 2]} stroke={color} />
      </Group>
    );
  }

  if (el.type === 'cloud') {
    return (
      <Group>
        <Path
          x={box.x}
          y={box.y + box.height * 0.3}
          data={`M0 ${box.height * 0.4} q${box.width * 0.2}-${box.height * 0.3} ${box.width * 0.4} 0 q${box.width * 0.05}-${box.height * 0.3} ${box.width * 0.25}-${box.height * 0.2} q${box.width * 0.2}-${box.height * 0.05} ${box.width * 0.2} ${box.height * 0.2} q${box.width * 0.2} 0 ${box.width * 0.2} ${box.height * 0.3} q-${box.width * 0.1} ${box.height * 0.2}-${box.width * 0.3} ${box.height * 0.15} h-${box.width * 0.55} q-${box.width * 0.25} 0-${box.width * 0.2}-${box.height * 0.45} z`}
          stroke={color}
          fill={fill}
        />
      </Group>
    );
  }

  return <Rect x={box.x} y={box.y} width={box.width} height={box.height} stroke={color} fill={fill} cornerRadius={6} />;
}

function getWorldPointer(stage: Konva.Stage, viewport: ViewportState) {
  const relative = stage.getRelativePointerPosition();
  if (relative) return relative;
  const pointer = stage.getPointerPosition();
  if (!pointer) return null;
  return toWorld(pointer, viewport);
}
