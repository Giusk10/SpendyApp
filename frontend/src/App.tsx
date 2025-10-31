import { useMemo } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import AuthPage from './components/AuthPage';
import DashboardPage from './components/DashboardPage';

const App = () => {
  const { token } = useAuth();

  const defaultElement = useMemo(() => (token ? <DashboardPage /> : <AuthPage />), [token]);

  return (
    <Routes>
      <Route path="/" element={defaultElement} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
