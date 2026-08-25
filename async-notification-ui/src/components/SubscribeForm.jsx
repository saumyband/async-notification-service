import { useState } from "react";
import api from "../api/api";

function SubscribeForm() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubscribe = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setMessage("Please enter an email address.");
      return;
    }

    try {
      setLoading(true);
      setMessage("");

      await api.post("/subscribers", {
        email: email.trim(),
      });

      setMessage("Subscriber added successfully.");
      setEmail("");
    } catch (error) {
      setMessage(
        error.response?.data?.error ||
          "Unable to add subscriber."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="card">
      <h2>Subscriber Management</h2>
      <p className="description">
        Add an individual subscriber to receive notifications.
      </p>

      <form onSubmit={handleSubscribe}>
        <input
          type="email"
          placeholder="subscriber@example.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />

        <button disabled={loading}>
          {loading ? "Adding..." : "Subscribe"}
        </button>
      </form>

      {message && <p className="feedback">{message}</p>}
    </section>
  );
}

export default SubscribeForm;