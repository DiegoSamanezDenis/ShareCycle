import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { PricingInfo } from '../types/pricing';
import styles from './PricingPage.module.css';

export default function PricingPage() {
    const [pricingInfo, setPricingInfo] = useState<PricingInfo | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchPricingInfo = async () => {
            try {
                const data = await apiRequest<PricingInfo>('/public/pricing/info');
                setPricingInfo(data);
                setError(null);
            } catch (err) {
                setError('Failed to load pricing information. Please try again later.');
                console.error('Error fetching pricing info:', err);
            }
        };
        fetchPricingInfo();
    }, []);

    if (error) {
        return <div className={styles.error}>{error}</div>;
    }

    if (!pricingInfo) {
        return <div className={styles.loading}>Loading pricing information...</div>;
    }

    return (
        <div className={styles.container}>
            <h1 className={styles.title}>
                Pricing Plans
            </h1>
            <div className={styles.grid}>
                {/* Pay As You Go Plan */}
                <div className={styles.card}>
                    <h2 className={styles.cardTitle}>
                        Pay As You Go
                    </h2>
                    <pre className={styles.pricingInfo}>
                        {pricingInfo.payAsYouGo}
                    </pre>
                    <button className={styles.button}>
                        Choose Plan
                    </button>
                </div>

                {/* Monthly Subscriber Plan */}
                <div className={styles.card}>
                    <h2 className={styles.cardTitle}>
                        Monthly Subscription
                    </h2>
                    <pre className={styles.pricingInfo}>
                        {pricingInfo.monthlySubscriber}
                    </pre>
                    <button className={styles.button}>
                        Choose Plan
                    </button>
                </div>
            </div>
        </div>
    );
}
