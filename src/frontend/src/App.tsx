// src/App.tsx
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { appConfig } from './config/env';
import { routes } from './routes';

const router = createBrowserRouter(routes);
const { apiUrl } = appConfig;

if (import.meta.env.DEV && !import.meta.env.VITE_API_URL) {
  console.warn('VITE_API_URL is not set; falling back to ' + apiUrl + '.');
}

export default function App() {
  return <RouterProvider router={router} />;
}

