import { useEffect, useState } from "react";
import { apiRequest } from "../api/client";

type LoyaltyStatus = {
    tier: "ENTRY" | "BRONZE" | "SILVER" | "GOLD";
    perks: string;
};

const TIER_COLORS = {
    ENTRY: "#6b7280",
    BRONZE: "#cd7f32",
    SILVER: "#94a3b8",
    GOLD: "#eab308"
};

export default function LoyaltyBadge({ userId, token }: { userId: string; token: string }) {
    const [status, setStatus] = useState<LoyaltyStatus | null>(null);

    useEffect(() => {
        apiRequest<LoyaltyStatus>(`/loyalty/status?riderId=${userId}`, { token })
            .then(setStatus)
            .catch(console.error);
    }, [userId, token]);

    if (!status) return null;

    return (
        <div style={{
            marginTop: 16,
            padding: 12,
            border: `1px solid ${TIER_COLORS[status.tier]}`,
            borderRadius: 8,
            background: "#fff"
        }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span style={{
                    fontWeight: "bold",
                    color: TIER_COLORS[status.tier],
                    textTransform: "uppercase"
                }}>
                    {status.tier} MEMBER
                </span>
            </div>
            <div style={{ fontSize: 13, color: "#666", marginTop: 4 }}>
                {status.perks}
            </div>
        </div>
    );
}