/**
  *
  * @author s235257
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
