import { useState } from "react";
import api from "../api/api";

function BulkImport() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleImport = async () => {
    if (!file) {
      setError("Please select a CSV file.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      setLoading(true);
      setError("");
      setResult(null);

      const response = await api.post(
        "/subscribers/import",
        formData
      );

      setResult(response.data);
    } catch (err) {
      setError(
        err.response?.data?.error ||
          "Unable to import subscribers."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="card">
      <h2>Bulk Subscriber Import</h2>

      <p className="description">
        Upload a CSV containing an email column.
      </p>

      <input
        type="file"
        accept=".csv,text/csv"
        onChange={(event) =>
          setFile(event.target.files?.[0] || null)
        }
      />

      <button
        onClick={handleImport}
        disabled={loading}
      >
        {loading ? "Importing..." : "Import CSV"}
      </button>

      {error && <p className="error">{error}</p>}

      {result && (
        <div className="stats">
          <div>
            <strong>{result.totalRecords}</strong>
            <span>Total</span>
          </div>

          <div>
            <strong>{result.imported}</strong>
            <span>Imported</span>
          </div>

          <div>
            <strong>{result.duplicates}</strong>
            <span>Duplicates</span>
          </div>

          <div>
            <strong>{result.invalid}</strong>
            <span>Invalid</span>
          </div>
        </div>
      )}
    </section>
  );
}

export default BulkImport;