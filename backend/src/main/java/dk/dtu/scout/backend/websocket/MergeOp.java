package dk.dtu.scout.backend.websocket;

/**
 * How the frontend should merge a field/series update.
 */
public enum MergeOp {
    /** Append a single value to an existing list. */
    APPEND,

    /** Replace the last element in an existing list (or append if empty). */
    REPLACE_LAST
}
