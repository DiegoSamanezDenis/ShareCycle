import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { appConfig } from './config/env';
import { routes } from './routes';

describe('App routing', () => {
  it('renders the home page by default', () => {
    const router = createMemoryRouter(routes, { initialEntries: ['/'] });
    render(<RouterProvider router={router} />);

    expect(screen.getByRole('heading', { name: /home/i })).toBeInTheDocument();
  });

  it('renders the not found page for unknown routes', () => {
    const router = createMemoryRouter(routes, { initialEntries: ['/missing'] });
    render(<RouterProvider router={router} />);

    expect(screen.getByRole('heading', { name: /404 - not found/i })).toBeInTheDocument();
  });
});

describe('App configuration', () => {
  it('exposes an API base URL', () => {
    expect(appConfig.apiUrl).toBe('http://localhost:8080/api');
  });
});

