import { useState } from "react";
import api from "../api/api";

function NewsletterForm({ onNotificationCreated }) {
    const [subject, setSubject] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        // Clear previous messages
        setError("");
        setSuccess("");

        // Basic frontend validation
        if (!subject.trim() || !message.trim()) {
            setError("Subject and message are required.");
            return;
        }

        try {
            setLoading(true);

            const response = await api.post("/notifications", {
                subject: subject.trim(),
                message: message.trim(),
            });

            const notificationId = response.data.id;

            // Send notification ID to parent App component so DeliveryStatus can start polling.
            onNotificationCreated(notificationId);

            setSuccess("Newsletter accepted for asynchronous processing.");
            setSubject("");
            setMessage("");
        } catch (err) {
            setError(
                err.response?.data?.error ||
                "Unable to send notification."
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <section className="card">
            <h2>Send Newsletter</h2>

            <p className="description">
                Publish an asynchronous notification to all active subscribers.
            </p>

            <form
                className="newsletter-form"
                onSubmit={handleSubmit}
            >
                <input
                    type="text"
                    placeholder="Newsletter subject"
                    value={subject}
                    onChange={(event) =>
                        setSubject(event.target.value)
                    }
                    disabled={loading}
                />

                <textarea
                    rows="6"
                    placeholder="Write your message..."
                    value={message}
                    onChange={(event) =>
                        setMessage(event.target.value)
                    }
                    disabled={loading}
                />

                <button
                    type="submit"
                    disabled={loading}
                >
                    {loading
                        ? "Submitting..."
                        : "Send Newsletter"}
                </button>
            </form>

            {success && (
                <p className="success">
                    {success}
                </p>
            )}

            {error && (
                <p className="error">
                    {error}
                </p>
            )}
        </section>
    );
}

export default NewsletterForm;