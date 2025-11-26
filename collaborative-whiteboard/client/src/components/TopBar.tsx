import { useNavigate } from 'react-router-dom';
import { useWhiteboardStore } from '../state/whiteboardStore';

interface TopBarProps {
  roomId: string;
  onCopy: () => void;
  connected: boolean;
  onUndo: () => void;
  onRedo: () => void;
}

export default function TopBar({ roomId, onCopy, connected, onUndo, onRedo }: TopBarProps) {
  const navigate = useNavigate();
  const users = useWhiteboardStore((s) => s.users);

  return (
    <header className="topbar">
      <div className="left">
        <button className="logo" onClick={() => navigate('/')}>Whiteboard</button>
        <div className="room">Room: <strong>{roomId}</strong></div>
        <span className={connected ? 'status online' : 'status offline'}>
          {connected ? 'Connected' : 'Offline'}
        </span>
        <button onClick={onCopy}>Copy share link</button>
        <button onClick={onUndo}>Undo</button>
        <button onClick={onRedo}>Redo</button>
      </div>
      <div className="right">
        <div className="avatars">
          {users.map((u) => (
            <div key={u.id} className="avatar" style={{ background: u.color }} title={u.name}>
              {u.name.substring(0, 2).toUpperCase()}
            </div>
          ))}
        </div>
      </div>
    </header>
  );
}
