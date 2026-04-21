/**
 * Small API wrapper for preparing and starting runs/runtime studies.
 * Keeps fetch/error parsing consistent across pages.
 */

const parseErrorMessage = async (res, fallback) => {
  try {
    const data = await res.json();
    if (data?.message) return data.message;
  } catch {
    // ignore JSON parse errors
  }
  return fallback;
};

export async function prepareRun(body = {}) {
  const res = await fetch("/api/run/prepare", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body ?? {}),
  });

  if (!res.ok) {
    throw new Error(
      await parseErrorMessage(res, `Run preparation failed with status ${res.status}`)
    );
  }

  return await res.json();
}

export async function startRun(body) {
  const res = await fetch("/api/run", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(await parseErrorMessage(res, `Run failed with status ${res.status}`));
  }

  return null;
}

export async function startRuntimeStudy(body) {
  const res = await fetch("/api/runtime-study", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(
      await parseErrorMessage(res, `Runtime study failed with status ${res.status}`)
    );
  }

  try {
    return await res.json();
  } catch {
    return null;
  }
}