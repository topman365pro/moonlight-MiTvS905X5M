package com.limelight.binding.video;

/** Converts native microsecond timestamps to uptime without erasing subsequent queue delay. */
final class VideoTimestampNormalizer {
    private boolean initialized;
    private long offsetUs;

    // Called only by the decode submission thread. Keep the epoch across codec recovery.
    long normalize(long nativeTimeUs, long uptimeMs) {
        if (!initialized) {
            offsetUs = uptimeMs * 1000 - nativeTimeUs;
            initialized = true;
        }
        return offsetUs + nativeTimeUs;
    }
}
