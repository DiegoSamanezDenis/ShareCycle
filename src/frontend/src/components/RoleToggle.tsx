import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import styles from "./RoleToggle.module.css";

export function RoleToggle() {
  const auth = useAuth();
  const [isToggling, setIsToggling] = useState(false);

  if (auth.role !== "OPERATOR") {
    return null;
  }

  const handleToggle = async () => {
    setIsToggling(true);
    try {
      await auth.toggleRole();
    } catch (error) {
      console.error("Failed to toggle role:", error);
      alert("Failed to toggle role. Please try again.");
    } finally {
      setIsToggling(false);
    }
  };

  const isInRiderMode = auth.currentMode === "RIDER";

  return (
    <div className={styles.roleToggleContainer}>
      <div className={styles.roleToggleCard}>
        <h3 className={styles.title}>Active Role</h3>
        <div className={styles.roleStatus}>
          <div className={styles.roleIndicator}>
            <span className={styles.baseRole}>Base: OPERATOR</span>
            <span 
              className={`${styles.currentMode} ${isInRiderMode ? styles.riderMode : styles.operatorMode}`}
            >
              Active: {auth.currentMode || "OPERATOR"}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={handleToggle}
          disabled={isToggling}
          className={`${styles.toggleButton} ${isInRiderMode ? styles.toOperator : styles.toRider}`}
        >
          {isToggling ? (
            "Switching..."
          ) : (
            <>
              Switch to {isInRiderMode ? "OPERATOR" : "RIDER"} mode
              <span className={styles.icon}>{isInRiderMode ? "🔧" : "🚴"}</span>
            </>
          )}
        </button>
        <p className={styles.description}>
          {isInRiderMode
            ? "You're currently using rider features. Switch back to access operator controls."
            : "You're in operator mode. Switch to test the rider experience."}
        </p>
      </div>
    </div>
  );
}
