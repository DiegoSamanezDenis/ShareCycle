import { useEffect, useState } from "react";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import LoyaltyBadge from "../components/LoyaltyBadge";

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

  if (loading) return <div style={centerStyle}>Loading account info...</div>;
  if (error) return <div style={{ ...centerStyle, color: "#ef4444" }}>Error: {error}</div>;
  if (!account) return <div style={centerStyle}>No account info found</div>;

  return (
    <div style={pageStyle}>
      <h1 style={titleStyle}>My Account</h1>

     <div style={{ ...cardStyle, color: "#000000" }}>
  <h2 style={cardTitleStyle}>Profile</h2>
  <p><strong>Full Name:</strong> {account.fullName}</p>
  <p><strong>Username:</strong> {account.username}</p>
  <p><strong>Email:</strong> {account.email}</p>
  <p><strong>Role:</strong> {account.role}</p>
</div>


      <div style={cardStyle}>
        <h2 style={cardTitleStyle}>Flex Balance</h2>
        <p style={{ fontSize: 22, fontWeight: 600, color: "#1f2937" }}>
          ${account.flexCredit.toFixed(2)}
        </p>
      </div>

      <div style={cardStyle}>
        <h2 style={cardTitleStyle}>Loyalty Tier</h2>
        {token && userId && <LoyaltyBadge userId={account.userId} token={token} />}
        <p style={{ marginTop: 8, fontSize: 14, color: "#555" }}>
          Reason: {account.loyaltyReason || "No reason provided"}
        </p>
      </div>
    </div>
  );
}

// Styles
const pageStyle: React.CSSProperties = {
  minHeight: "100vh",
  background: "linear-gradient(135deg, #6589d0ff, #08152fff)", // soft gradient
  padding: "40px 20px",
  fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
};

const titleStyle: React.CSSProperties = {
  textAlign: "center",
  fontSize: 32,
  fontWeight: 700,
  color: "#111827",
  marginBottom: 30,
};

const cardStyle: React.CSSProperties = {
  background: "#ffffffff",
  padding: 24,
  borderRadius: 12,
  marginBottom: 20,
  boxShadow: "0 6px 18px rgba(0, 0, 0, 0.08)",
  transition: "transform 0.2s ease",
};

const cardTitleStyle: React.CSSProperties = {
  fontSize: 20,
  fontWeight: 600,
  marginBottom: 12,
  color: "#1f2937",
};

const centerStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  height: "60vh",
  fontSize: 18,
  color: "#374151",
};
