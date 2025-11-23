import type { ReactNode } from "react";
import styles from "./AppShell.module.css";

type PageSectionProps = {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
};

export function PageSection({ title, description, actions, children }: PageSectionProps) {
  return (
    <section className={styles.contentSection}>
      {(title || description || actions) && (
        <header
          style={{
            display: "flex",
            flexWrap: "wrap",
            justifyContent: "space-between",
            gap: "1rem",
            marginBottom: "1rem",
          }}
        >
          <div>
            {title && <h2 style={{ marginBottom: description ? "0.35rem" : 0 }}>{title}</h2>}
            {description && <p style={{ margin: 0 }}>{description}</p>}
          </div>
          {actions && <div style={{ display: "flex", gap: "0.75rem" }}>{actions}</div>}
        </header>
      )}
      {children}
    </section>
  );
}

export default PageSection;
