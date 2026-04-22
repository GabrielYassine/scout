export function makeStreamKey({ runId, problemId, seed }) {
  return `${runId}:${problemId}:${seed}`;
}

/**
 * Creates a per-stream ordered packet applier.
 *
 * Packet contract:
 * - Packets contain { runId, problemId, seed, sequenceId }.
 * - A packet is applied only when its sequenceId is exactly the next expected one.
 * - Older or duplicate packets are ignored.
 * - Future packets are buffered until missing sequence numbers arrive.
 */
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
    if (streamState.pendingBySequenceId.size <= maxPending) {
      return;
    }

    const sequenceIdsDescending = Array.from(streamState.pendingBySequenceId.keys()).sort(
      (a, b) => b - a
    );

    for (const sequenceId of sequenceIdsDescending) {
      if (streamState.pendingBySequenceId.size <= maxPending) {
        break;
      }
      streamState.pendingBySequenceId.delete(sequenceId);
    }
  }

  function flushContiguousPackets(streamState) {
    while (true) {
      const nextSequenceId = streamState.lastAppliedSequenceId + 1;
      const nextPacket = streamState.pendingBySequenceId.get(nextSequenceId);

      if (!nextPacket) {
        return;
      }

      streamState.pendingBySequenceId.delete(nextSequenceId);
      applyPacket(nextPacket);
      streamState.lastAppliedSequenceId = nextSequenceId;
    }
  }

  function ingest(packet) {
    const { runId, problemId, seed, sequenceId } = packet ?? {};

    if (!runId || !problemId || seed == null || sequenceId == null) {
      return;
    }

    const streamKey = makeStreamKey({ runId, problemId, seed });
    const streamState = getOrCreateStreamState(streamKey);

    if (sequenceId <= streamState.lastAppliedSequenceId) {
      return;
    }

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
    const streamKey = makeStreamKey({ runId, problemId, seed });
    streamStateByKey.delete(streamKey);
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