import { Navigate, type RouteObject } from 'react-router';
import { ConnectionsPage } from '../features/connections/ConnectionsPage';
import { ProducePage } from '../features/produce/ProducePage';
import { AppLayout } from './AppLayout';
import { NotFoundPage } from './NotFoundPage';

/** Every screen has its own URL; the backend serves the app for any of them (FrontendConfiguration). */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/connections" replace /> },
      { path: 'connections', element: <ConnectionsPage /> },
      { path: 'produce', element: <ProducePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
