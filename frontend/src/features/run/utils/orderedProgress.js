export function makeStreamKey({ runId, problemId, seed }) {
  return `${runId}:${problemId}:${seed}`;
}

/**
 * Creates a per-stream ordered packet applier.
 * Contract:
 * - Packets have { runId, problemId, seed, sequenceId }.
 * - Applies packet only if sequenceId === lastAppliedSequenceId + 1.
 * - Ignores stale/duplicate packets (sequenceId <= lastAppliedSequenceId).
 * - Buffers future packets and flushes contiguous sequence when gaps fill.
 */
export function createOrderedProgressApplier({ applyPacket, maxPending = 5000 } = {}) {
  if (typeof applyPacket !== "function") {
    throw new Error("createOrderedProgressApplier requires applyPacket(packet)");
  }

  const stateByStream = new Map();

  function getState(streamKey) {
    let s = stateByStream.get(streamKey);
    if (!s) {
      s = { lastAppliedSequenceId: 0, pending: new Map() };
      stateByStream.set(streamKey, s);
    }
    return s;
  }

  function flush(streamKey, s) {
    while (true) {
      const nextSeq = s.lastAppliedSequenceId + 1;
      const nextPacket = s.pending.get(nextSeq);
      if (!nextPacket) return;
      s.pending.delete(nextSeq);
      applyPacket(nextPacket);
      s.lastAppliedSequenceId = nextSeq;
    }
  }

  function ingest(packet) {
    const { runId, problemId, seed, sequenceId } = packet ?? {};
    if (!runId || !problemId || seed == null) return;
    if (sequenceId == null) return;

    const streamKey = makeStreamKey({ runId, problemId, seed });
    const s = getState(streamKey);

    if (sequenceId <= s.lastAppliedSequenceId) {
      return; // stale/duplicate
    }

    if (sequenceId === s.lastAppliedSequenceId + 1) {
      applyPacket(packet);
      s.lastAppliedSequenceId = sequenceId;
      flush(streamKey, s);
      return;
    }

    // out-of-order ahead: buffer
    if (!s.pending.has(sequenceId)) {
      s.pending.set(sequenceId, packet);
      // safety valve against unbounded growth
      if (s.pending.size > maxPending) {
        // Drop the farthest-ahead packet(s) to keep memory bounded.
        const keys = Array.from(s.pending.keys()).sort((a, b) => b - a);
        for (let i = 0; i < keys.length && s.pending.size > maxPending; i += 1) {
          s.pending.delete(keys[i]);
        }
      }
    }
  }

  function resetStream({ runId, problemId, seed }) {
    stateByStream.delete(makeStreamKey({ runId, problemId, seed }));
  }

  function resetAll() {
    stateByStream.clear();
  }

  return { ingest, resetStream, resetAll };
}
