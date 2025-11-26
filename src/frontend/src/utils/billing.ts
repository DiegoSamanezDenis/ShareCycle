export function computePreDiscountAmount(amount: number, discountRate?: number | null): number {
  if (!Number.isFinite(amount)) {
    return 0;
  }
  if (typeof discountRate !== "number" || discountRate <= 0) {
    return amount;
  }
  const multiplier = 1 - discountRate;
  if (multiplier <= 0) {
    return amount;
  }
  return amount / multiplier;
}
