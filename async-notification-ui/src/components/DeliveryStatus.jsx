import { useEffect, useState } from "react";
import api from "../api/api";

function DeliveryStatus({ notificationId }) {
    const [status, setStatus] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!notificationId) {
            setStatus(null);
            setError("");
            return;
        }

        let intervalId;
        let cancelled = false;

        const fetchStatus = async () => {
            try {
                const response = await api.get(
                    `/notifications/${notificationId}/status`
                );

                if (cancelled) {
                    return;
                }

                const data = response.data;

                setStatus(data);
                setError("");

                // Stop polling once processing has completed.
                if (data.pending === 0 && intervalId) {
                    clearInterval(intervalId);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(
                        err.response?.data?.error ||
                        "Unable to retrieve delivery status."
                    );
                }
            }
        };

        // Fetch immediately instead of waiting 2 seconds
        fetchStatus();

        // Poll backend every 2 seconds
        intervalId = setInterval(fetchStatus, 2000);

        // Cleanup when component unmounts
        // or notificationId changes.
        return () => {
            cancelled = true;

            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [notificationId]);

    if (!notificationId) {
        return (
            <section className="card">
                <h2>Delivery Status</h2>

                <p className="description">
                    Send a newsletter to begin tracking delivery.
                </p>
            </section>
        );
    }

    const completed =
        status ? status.sent + status.failed : 0;

    const percentage =
        status && status.total > 0
            ? Math.round(
                (completed / status.total) * 100
            )
            : 0;

    return (
        <section className="card">
            <h2>Delivery Status</h2>

            <p className="notification-id">
                Notification: {notificationId}
            </p>

            {error && (
                <p className="error">
                    {error}
                </p>
            )}

            {!status && !error && (
                <p className="description">
                    Loading delivery status...
                </p>
            )}

            {status && (
                <>
                    <div className="stats">
                        <div>
                            <strong>{status.total}</strong>
                            <span>Total</span>
                        </div>

                        <div>
                            <strong>{status.sent}</strong>
                            <span>Sent</span>
                        </div>

                        <div>
                            <strong>{status.pending}</strong>
                            <span>Pending</span>
                        </div>

                        <div>
                            <strong>{status.failed}</strong>
                            <span>Failed</span>
                        </div>
                    </div>

                    <div className="progress-container">
                        <div
                            className="progress-bar"
                            style={{
                                width: `${percentage}%`,
                            }}
                        />
                    </div>

                    <p>
                        {percentage}% processed
                    </p>

                    {/* Successful completion */}
                    {status.pending === 0 &&
                        status.failed === 0 && (
                            <p className="success">
                                All notifications processed successfully.
                            </p>
                        )}

                    {/* Completed with failures */}
                    {status.pending === 0 &&
                        status.failed > 0 && (
                            <p className="error">
                                Processing completed with{" "}
                                {status.failed} failed{" "}
                                {status.failed === 1
                                    ? "delivery"
                                    : "deliveries"}
                                .
                            </p>
                        )}

                    {/* Still processing */}
                    {status.pending > 0 && (
                        <p className="processing">
                            Notification processing is in progress...
                        </p>
                    )}
                </>
            )}
        </section>
    );
}

export default DeliveryStatus;