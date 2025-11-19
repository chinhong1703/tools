import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { generateRoomId } from '@collaborative-whiteboard/shared';
import './styles/global.css';

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (location.pathname === '/whiteboard') {
      navigate(`/whiteboard/${generateRoomId()}`, { replace: true });
    }
  }, [location.pathname, navigate]);

  return (
    <div className="app-shell">
      <Outlet />
    </div>
  );
}
