import { useState } from "react";
import SubscribeForm from "./components/SubscribeForm";
import BulkImport from "./components/BulkImport";
import NewsletterForm from "./components/NewsletterForm";
import DeliveryStatus from "./components/DeliveryStatus";
import "./App.css";

function App() {
  const [notificationId, setNotificationId] =
    useState(null);

  return (
    <main className="app">
      <header className="header">
        <h1>Notification Management</h1>

        <p>
          Asynchronous bulk notification processing
          with delivery tracking
        </p>
      </header>

      <div className="dashboard">
        <SubscribeForm />

        <BulkImport />

        <NewsletterForm
          onNotificationCreated={setNotificationId}
        />

        <DeliveryStatus
          notificationId={notificationId}
        />
      </div>
    </main>
  );
}

export default App;