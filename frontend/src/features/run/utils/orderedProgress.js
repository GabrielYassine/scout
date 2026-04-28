/**
 * Applies progress packets in sequence order per run/problem/seed stream.
 * Duplicate or old packets are ignored, and future packets are buffered until
 * missing earlier packets arrive.
 */

export function makeStreamKey({ runId, problemId, seed }) {
  return `${runId}:${problemId}:${seed}`;
}

export function createOrderedProgressApplier({
  applyPacket,
  maxPending = 5000,
} = {}) {
  if (typeof applyPacket !== "function") {
    throw new Error("createOrderedProgressApplier requires applyPacket(packet)");
  }

  const streamStateByKey = new Map();

  function getOrCreateStreamState(streamKey) {
    let streamState = streamStateByKey.get(streamKey);

    if (!streamState) {
      streamState = {
        lastAppliedSequenceId: 0,
        pendingBySequenceId: new Map(),
      };
      streamStateByKey.set(streamKey, streamState);
    }

    return streamState;
  }

  function trimPendingPackets(streamState) {
    const pendingBySequenceId = streamState.pendingBySequenceId;
    if (pendingBySequenceId.size <= maxPending) return;

    const sequenceIdsDescending = [...pendingBySequenceId.keys()].sort((a, b) => b - a);

    for (const sequenceId of sequenceIdsDescending) {
      if (pendingBySequenceId.size <= maxPending) break;
      pendingBySequenceId.delete(sequenceId);
    }
  }

  function flushContiguousPackets(streamState) {
    const pendingBySequenceId = streamState.pendingBySequenceId;

    while (true) {
      const nextSequenceId = streamState.lastAppliedSequenceId + 1;
      const nextPacket = pendingBySequenceId.get(nextSequenceId);

      if (!nextPacket) return;

      pendingBySequenceId.delete(nextSequenceId);
      applyPacket(nextPacket);
      streamState.lastAppliedSequenceId = nextSequenceId;
    }
  }

  function ingest(packet) {
    const { runId, problemId, seed, sequenceId } = packet ?? {};
    if (!runId || !problemId || seed == null || sequenceId == null) return;

    const streamKey = makeStreamKey({ runId, problemId, seed });
    const streamState = getOrCreateStreamState(streamKey);

    if (sequenceId <= streamState.lastAppliedSequenceId) return;

    const expectedSequenceId = streamState.lastAppliedSequenceId + 1;

    if (sequenceId === expectedSequenceId) {
      applyPacket(packet);
      streamState.lastAppliedSequenceId = sequenceId;
      flushContiguousPackets(streamState);
      return;
    }

    if (!streamState.pendingBySequenceId.has(sequenceId)) {
      streamState.pendingBySequenceId.set(sequenceId, packet);
      trimPendingPackets(streamState);
    }
  }

  function resetStream({ runId, problemId, seed }) {
    streamStateByKey.delete(makeStreamKey({ runId, problemId, seed }));
  }

  function resetAll() {
    streamStateByKey.clear();
  }

  return {
    ingest,
    resetStream,
    resetAll,
  };
}