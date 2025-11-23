import { useEffect, useState } from "react";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import LoyaltyBadge from "../components/LoyaltyBadge";
import AppShell from "../components/layout/AppShell";
import PageSection from "../components/layout/PageSection";

type AccountInfo = {
  userId: string;
  fullName: string;
  email: string;
  username: string;
  role: string;
  flexCredit: number;
  loyaltyTier: "ENTRY" | "BRONZE" | "SILVER" | "GOLD";
  loyaltyReason: string;
};

export default function AccountPage() {
  const { token, userId } = useAuth();
  const [account, setAccount] = useState<AccountInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token || !userId) {
      setLoading(false);
      return;
    }

    const fetchAccount = async () => {
      setLoading(true);
      try {
        const data = await apiRequest<AccountInfo>("/account", { token });
        setAccount(data);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to fetch account info");
      } finally {
        setLoading(false);
      }
    };

    void fetchAccount();
  }, [token, userId]);

  return (
    <AppShell
      heading="My account"
      subheading="View your subscription details, flex balance, and loyalty tier."
    >
      {loading ? (
        <PageSection>
          <p>Loading account info...</p>
        </PageSection>
      ) : error ? (
        <PageSection>
          <p role="alert" style={{ color: "var(--danger)" }}>
            Error: {error}
          </p>
        </PageSection>
      ) : !account ? (
        <PageSection>
          <p>No account info found.</p>
        </PageSection>
      ) : (
        <>
          <PageSection title="Profile">
            <dl
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                gap: "0.75rem",
              }}
            >
              <div>
                <dt style={{ fontWeight: 600, color: "var(--text-muted)" }}>Full name</dt>
                <dd style={{ margin: 0 }}>{account.fullName}</dd>
              </div>
              <div>
                <dt style={{ fontWeight: 600, color: "var(--text-muted)" }}>Username</dt>
                <dd style={{ margin: 0 }}>{account.username}</dd>
              </div>
              <div>
                <dt style={{ fontWeight: 600, color: "var(--text-muted)" }}>Email</dt>
                <dd style={{ margin: 0 }}>{account.email}</dd>
              </div>
              <div>
                <dt style={{ fontWeight: 600, color: "var(--text-muted)" }}>Role</dt>
                <dd style={{ margin: 0 }}>{account.role}</dd>
              </div>
            </dl>
          </PageSection>

          <PageSection title="Balance & loyalty">
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                gap: "1rem",
              }}
            >
              <div
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: 16,
                  padding: "1rem",
                  background: "var(--surface-muted)",
                }}
              >
                <h3 style={{ marginBottom: "0.35rem" }}>Flex balance</h3>
                <p style={{ margin: 0, fontSize: 24, fontWeight: 600 }}>
                  ${account.flexCredit.toFixed(2)}
                </p>
              </div>
              <div
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: 16,
                  padding: "1rem",
                  background: "var(--surface-muted)",
                }}
              >
                <h3 style={{ marginBottom: "0.35rem" }}>Loyalty tier</h3>
                {token && userId && (
                  <LoyaltyBadge userId={account.userId} token={token} />
                )}
                <p style={{ marginTop: 8, color: "var(--text-muted)" }}>
                  Reason: {account.loyaltyReason || "No reason provided"}
                </p>
              </div>
            </div>
          </PageSection>
        </>
      )}
    </AppShell>
  );
}
