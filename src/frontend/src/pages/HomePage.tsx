import { Link } from 'react-router-dom';

export default function HomePage() {
  return (
    <main>
      <h1>Home</h1>
      <p>Welcome to ShareCycle.</p>
      <nav>
        <ul>
          <li><Link to="/register">Register</Link></li>
          <li><Link to="/login">Login</Link></li>
          <li><Link to="/dashboard">Dashboard</Link></li>
        </ul>
      </nav>
    </main>
  );
}

