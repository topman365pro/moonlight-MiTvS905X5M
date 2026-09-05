package com.limelight.binding.video;

/** Standalone regression tests; run with javac/java without an Android device. */
public final class DecoderOutputWatchdogTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        VideoTimestampNormalizer timestamps = new VideoTimestampNormalizer();
        check(timestamps.normalize(9000000000L, 1000) == 1000000L, "Normalize an arbitrary native epoch");
        long queuedMs = timestamps.normalize(9000016000L, 1300) / 1000;
        check(queuedMs == 1016, "Retain queue delay instead of rebasing every frame");
        check(DecoderOutputWatchdog.isQueuedFrameStale(1300, queuedMs), "Detect stale microsecond timestamps after normalization");
        timestamps = new VideoTimestampNormalizer();
        timestamps.normalize(1000000, 1000);
        check(timestamps.normalize(1016000, 1300) == 1016000, "A zero offset is still initialized");
        DecoderOutputWatchdog watchdog = new DecoderOutputWatchdog();

        // Normal streaming, including an output frame racing ahead of the next input report.
        for (long time = 0; time < 10000; time += 16) {
            check(!watchdog.onInput(time), "Healthy output must prevent recovery");
            check(!watchdog.onOutput(time + 8, 8), "Healthy frames must not trigger latency recovery");
        }

        // The captured failure: input keeps succeeding but no output appears.
        watchdog.reset();
        for (long time = 0; time < 2000; time += 16) {
            check(!watchdog.onInput(time), "Must allow two seconds for output");
        }
        check(watchdog.onInput(2000), "Continuous input without output must trigger recovery");

        // Reconfiguration and a returning output frame both restart observation.
        watchdog.reset();
        check(!watchdog.onInput(2016), "Recovery must not immediately retrigger");
        for (long time = 2032; time < 3900; time += 16) {
            check(!watchdog.onInput(time), "Recovery needs a fresh grace period");
        }
        watchdog.reset();
        check(!watchdog.onInput(4016), "Returning output must clear the previous stall");

        // A network interruption must not turn the first returning packet into a decoder reset.
        watchdog.reset();
        for (long time = 0; time < 1500; time += 16) {
            check(!watchdog.onInput(time), "Short stall should not trigger");
        }
        check(!watchdog.onInput(10000), "Network pause must reset observation");
        for (long time = 10016; time < 12000; time += 16) {
            check(!watchdog.onInput(time), "Network recovery needs a fresh observation window");
        }
        check(watchdog.onInput(12000), "A real stall after network recovery must still be detected");

        // Sparse input (for example a mostly idle source) alone is insufficient evidence.
        watchdog.reset();
        for (long time = 0; time <= 4000; time += 500) {
            check(!watchdog.onInput(time), "Too few input frames must not trigger recovery");
        }
        watchdog.reset();
        for (long time = 0; time < 60000; time += 1000) {
            check(!watchdog.onInput(time), "Repeated input gaps must not accumulate stall time");
        }

        // Short decoder hiccups must not accumulate across healthy output.
        watchdog.reset();
        for (int window = 0; window < 20; window++) {
            for (int frame = 0; frame < 60; frame++) {
                check(!watchdog.onInput(window * 1000L + frame * 16), "Short hiccups must not accumulate");
            }
            watchdog.reset();
        }
        // Flowing output can still be stale. Require sustained lateness, not a single spike.
        watchdog.reset();
        for (long time = 0; time < 1000; time += 16) {
            check(!watchdog.onInput(time), "Flowing output must not trip the no-output guard");
            check(!watchdog.onOutput(time, 200), "Allow one second before latency recovery");
        }
        watchdog.onInput(1008);
        check(watchdog.onOutput(1008, 200), "Sustained stale output must trigger recovery");
        watchdog.reset();
        watchdog.onInput(1024);
        check(!watchdog.onOutput(1024, 200), "Reset must clear accumulated lateness");

        // Isolated late frames, invalid timestamps, and gaps cannot accumulate a false alarm.
        watchdog.reset();
        for (long time = 0; time < 5000; time += 16) {
            watchdog.onInput(time);
            check(!watchdog.onOutput(time, time % 160 == 0 ? 500 : 10), "Ignore isolated spikes");
        }
        watchdog.reset();
        for (long time = 0; time < 2000; time += 16) {
            watchdog.onInput(time);
            check(!watchdog.onOutput(time, -1), "Ignore invalid frame ages");
        }
        watchdog.reset();
        watchdog.onInput(0);
        for (long time = 1000; time < 3000; time += 16) {
            check(!watchdog.onOutput(time, 2000), "Draining after network silence is not a live stall");
        }

        // The native decode queue should be refreshed only when a valid timestamp is stale.
        check(!DecoderOutputWatchdog.isQueuedFrameStale(1150, 1000), "Allow 150 ms exactly");
        check(DecoderOutputWatchdog.isQueuedFrameStale(1151, 1000), "Reject stale queued frames");
        check(!DecoderOutputWatchdog.isQueuedFrameStale(1000, 0), "Ignore unset enqueue time");
        check(!DecoderOutputWatchdog.isQueuedFrameStale(1000, 1001), "Ignore future enqueue time");
        System.out.println("DecoderOutputWatchdog regression tests passed");
    }
}
