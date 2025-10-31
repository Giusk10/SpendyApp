import { Navigate, Route, Routes } from 'react-router-dom';
import { Navigation } from './components/Navigation';
import { useAuth } from './hooks/useAuth';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { UploadPage } from './pages/UploadPage';
import { HouseLinkPage } from './pages/HouseLinkPage';
import { CoinquiliniPage } from './pages/CoinquiliniPage';

const PrivateRoute = ({ element }: { element: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? element : <Navigate to="/login" replace />;
};

const PublicRoute = ({ element }: { element: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : element;
};

const App = () => {
  const { isAuthenticated } = useAuth();

  return (
    <>
      <Navigation />
      <Routes>
        <Route path="/" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
        <Route path="/login" element={<PublicRoute element={<LoginPage />} />} />
        <Route path="/register" element={<PublicRoute element={<RegisterPage />} />} />
        <Route path="/dashboard" element={<PrivateRoute element={<DashboardPage />} />} />
        <Route path="/expenses/import" element={<PrivateRoute element={<UploadPage />} />} />
        <Route path="/house/link" element={<PrivateRoute element={<HouseLinkPage />} />} />
        <Route path="/house/roommates" element={<PrivateRoute element={<CoinquiliniPage />} />} />
        <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </>
  );
};

export default App;
