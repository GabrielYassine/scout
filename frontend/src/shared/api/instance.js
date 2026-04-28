/*
 * API functions for importing and exporting instance files.
 */
export async function importInstanceFile({ fileName, content }) {
  const res = await fetch("/api/instance/import", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ fileName, content }),
  });

  if (!res.ok) {
    const message = await res.text();
    throw new Error(message || "Failed to import instance");
  }

  return res.json();
}

export async function exportInstanceFile(payload) {
  const res = await fetch("/api/instance/export", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const message = await res.text();
    throw new Error(message || "Failed to export instance");
  }

  return res.text();
}