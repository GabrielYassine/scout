/**
 * Small API wrapper for starting runs and runtime studies.
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

export async function startRun(body) {
  const res = await fetch("/api/run", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(await parseErrorMessage(res, `Run failed with status ${res.status}`));
  }

  // backend response isn't used by the UI right now, but return it for future use
  try {
    return await res.json();
  } catch {
    return null;
  }
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
