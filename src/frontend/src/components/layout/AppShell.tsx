import type { ReactNode } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import styles from "./AppShell.module.css";

type AppShellProps = {
  heading?: string;
  subheading?: string;
  actions?: ReactNode;
  children: ReactNode;
};

type NavItem = {
  path: string;
  label: string;
  requiresAuth?: boolean;
  requiresRole?: "RIDER" | "OPERATOR";
};

export function AppShell({ heading, subheading, actions, children }: AppShellProps) {
  const auth = useAuth();
  const isAuthenticated = Boolean(auth.token);
  const effectiveRole = auth.effectiveRole;

  const navItems: NavItem[] = [
    { path: "/", label: "Home" },
    { path: "/dashboard", label: "Dashboard", requiresAuth: true },
    {
      path: "/trip-summary",
      label: "Trip Summary",
      requiresAuth: true,
    },
    { path: "/pricing", label: "Pricing" },
    { path: "/account", label: "Account", requiresAuth: true },
  ];

  const visibleNavItems = navItems.filter((item) => {
    if (item.requiresAuth && !isAuthenticated) {
      return false;
    }
    if (item.requiresRole && item.requiresRole !== effectiveRole) {
      return false;
    }
    return true;
  });

  return (
    <div className={styles.wrapper}>
      <header className={styles.topBar}>
        <Link to="/" className={styles.branding}>
          Share<span>Cycle</span>
        </Link>
        <nav aria-label="Primary">
          <div className={styles.navList}>
            {visibleNavItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  [
                    styles.navLink,
                    isActive ? styles.navLinkActive : "",
                  ]
                    .filter(Boolean)
                    .join(" ")
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>
        <div className={styles.authArea}>
          {isAuthenticated ? (
            <>
              <span className={styles.authSummary}>
                Signed in as <strong>{auth.username}</strong>
              </span>
              <button
                type="button"
                className={styles.ghostButton}
                onClick={() => {
                  void auth.logout();
                }}
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className={styles.linkButton}>
                Sign in
              </Link>
              <Link
                to="/register"
                className={`${styles.linkButton} ${styles.linkButtonPrimary}`}
              >
                Create account
              </Link>
            </>
          )}
        </div>
      </header>
      <main className={styles.main}>
        {(heading || subheading || actions) && (
          <section className={styles.pageHero}>
            <div>
              {heading && <h1>{heading}</h1>}
              {subheading && <p>{subheading}</p>}
            </div>
            {actions && <div className={styles.heroActions}>{actions}</div>}
          </section>
        )}
        {children}
      </main>
      <footer className={styles.footer}>
        © {new Date().getFullYear()} ShareCycle. Built for dependable city rides.
      </footer>
    </div>
  );
}

export default AppShell;
