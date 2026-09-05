package com.limelight.binding.video;

/** Detects a decoder that consumes a live stream without returning decoded frames. */
final class DecoderOutputWatchdog {
    private static final long STALL_TIMEOUT_MS = 2000;
    private static final long MAX_INPUT_GAP_MS = 500;
    private static final int MIN_INPUT_FRAMES = 30;
    private static final long MAX_LOCAL_FRAME_AGE_MS = 150;
    private static final long LATE_OUTPUT_TIMEOUT_MS = 1000;

    private long firstInputTimeMs = -1;
    private long lastInputTimeMs = -1;
    private int inputFrames;
    private long lateOutputStartMs = -1;
    private int lateOutputFrames;

    static boolean isQueuedFrameStale(long nowMs, long enqueueTimeMs) {
        return enqueueTimeMs > 0 && nowMs - enqueueTimeMs > MAX_LOCAL_FRAME_AGE_MS;
    }

    // Input and output are reported by different codec threads.
    synchronized boolean onInput(long nowMs) {
        if (firstInputTimeMs < 0 || nowMs - lastInputTimeMs > MAX_INPUT_GAP_MS) {
            firstInputTimeMs = nowMs;
            inputFrames = 0;
        }
        lastInputTimeMs = nowMs;
        inputFrames++;
        return inputFrames >= MIN_INPUT_FRAMES && nowMs - firstInputTimeMs >= STALL_TIMEOUT_MS;
    }

    synchronized void reset() {
        firstInputTimeMs = -1;
        lastInputTimeMs = -1;
        inputFrames = 0;
        lateOutputStartMs = -1;
        lateOutputFrames = 0;
    }

    // Output can keep flowing even when a decoder has accumulated a persistent delay.
    // Use the original local enqueue timestamp, not FPS, to detect that case.
    synchronized boolean onOutput(long nowMs, long localFrameAgeMs) {
        firstInputTimeMs = -1;
        inputFrames = 0;
        if (lastInputTimeMs < 0 || nowMs - lastInputTimeMs > MAX_INPUT_GAP_MS ||
                localFrameAgeMs <= MAX_LOCAL_FRAME_AGE_MS) {
            lateOutputStartMs = -1;
            lateOutputFrames = 0;
            return false;
        }
        if (lateOutputStartMs < 0) {
            lateOutputStartMs = nowMs;
        }
        lateOutputFrames++;
        return lateOutputFrames >= MIN_INPUT_FRAMES &&
                nowMs - lateOutputStartMs >= LATE_OUTPUT_TIMEOUT_MS;
    }
}
