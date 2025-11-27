import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { generateRoomId, roomIdIsValid } from '@collaborative-whiteboard/shared';

export default function Landing() {
  const [roomId, setRoomId] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleCreate = () => {
    const id = generateRoomId();
    navigate(`/whiteboard/${id}`);
  };

  const handleJoin = (e: FormEvent) => {
    e.preventDefault();
    if (!roomIdIsValid(roomId)) {
      setError('Enter a valid room ID (alphanumeric, 4-20 chars)');
      return;
    }
    navigate(`/whiteboard/${roomId}`);
  };

  return (
    <div className="landing">
      <div className="card">
        <h1>Collaborative Whiteboard</h1>
        <p>Draw, diagram, and collaborate in real time.</p>
        <div className="actions">
          <button onClick={handleCreate}>Create new whiteboard</button>
        </div>
        <form onSubmit={handleJoin} className="join-form">
          <label htmlFor="room">Join existing room</label>
          <div className="row">
            <input
              id="room"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value.trim())}
              placeholder="Room ID"
            />
            <button type="submit">Join</button>
          </div>
          {error && <p className="error">{error}</p>}
        </form>
      </div>
    </div>
  );
}
