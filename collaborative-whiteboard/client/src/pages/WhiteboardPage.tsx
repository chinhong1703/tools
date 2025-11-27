import { useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { BoardElement } from '@collaborative-whiteboard/shared';
import TopBar from '../components/TopBar';
import Toolbar from '../components/Toolbar';
import PropertiesPanel from '../components/PropertiesPanel';
import WhiteboardCanvas from '../components/WhiteboardCanvas';
import { useWhiteboardSocket } from '../hooks/useWhiteboardSocket';
import { useWhiteboardStore } from '../state/whiteboardStore';

export default function WhiteboardPage() {
  const { roomId = '' } = useParams();
  const { connected, user, emit } = useWhiteboardSocket(roomId);
  const store = useWhiteboardStore();

  const handleCreate = (element: BoardElement) => {
    const enriched = {
      ...element,
      strokeColor: element.strokeColor || store.strokeColor,
      fillColor: element.fillColor ?? store.fillColor,
      strokeWidth: element.strokeWidth ?? store.strokeWidth,
      createdBy: user.id,
      updatedAt: Date.now(),
    } as BoardElement;
    store.addElement(enriched);
    emit('element_created', { roomId, element: enriched });
  };

  const handleUpdate = (element: BoardElement) => {
    const updated = { ...element, updatedAt: Date.now() } as BoardElement;
    store.updateElement(updated);
    emit('element_updated', { roomId, element: updated });
  };

  const handleDelete = (id: string) => {
    store.deleteElement(id);
    emit('element_deleted', { roomId, elementId: id });
  };

  const handleClear = () => {
    store.clearBoard();
    emit('board_cleared', { roomId });
  };

  const handleCopy = async () => {
    const url = `${window.location.origin}/whiteboard/${roomId}`;
    await navigator.clipboard.writeText(url);
  };

  const selected = useMemo(() => store.elements.find((el) => el.id === store.selectedId), [store.elements, store.selectedId]);

  const handleUndo = () => emit('undo', { roomId, userId: user.id });
  const handleRedo = () => emit('redo', { roomId, userId: user.id });

  return (
    <div className="whiteboard-page">
      <TopBar roomId={roomId} onCopy={handleCopy} connected={connected} onUndo={handleUndo} onRedo={handleRedo} />
      <div className="workspace">
        <Toolbar />
        <WhiteboardCanvas
          elements={store.elements}
          selectedId={store.selectedId}
          tool={store.currentTool}
          strokeColor={store.strokeColor}
          fillColor={store.fillColor}
          strokeWidth={store.strokeWidth}
          fontSize={store.fontSize}
          onCreate={handleCreate}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
          onSelect={store.setSelected}
          viewport={store.viewport}
          onViewportChange={store.setViewport}
        />
        <PropertiesPanel />
      </div>
      <div className="footer">
        <div className="controls">
          <button onClick={handleUndo}>Undo</button>
          <button onClick={handleRedo}>Redo</button>
          <button onClick={handleClear}>Clear</button>
        </div>
        {selected && <div className="selection-info">Selected: {selected.type}</div>}
      </div>
    </div>
  );
}
