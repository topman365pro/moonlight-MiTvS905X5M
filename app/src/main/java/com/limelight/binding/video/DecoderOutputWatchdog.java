package com.limelight.binding.video;

/** Detects a decoder that consumes a live stream without returning decoded frames. */
final class DecoderOutputWatchdog {
    private static final long STALL_TIMEOUT_MS = 2000;
    private static final long MAX_INPUT_GAP_MS = 500;
    private static final int MIN_INPUT_FRAMES = 30;

    private long firstInputTimeMs = -1;
    private long lastInputTimeMs = -1;
    private int inputFrames;

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
    }
}
