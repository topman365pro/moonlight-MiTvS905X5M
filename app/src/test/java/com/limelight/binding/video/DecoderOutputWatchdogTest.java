package com.limelight.binding.video;

/** Standalone regression tests; run with javac/java without an Android device. */
public final class DecoderOutputWatchdogTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        DecoderOutputWatchdog watchdog = new DecoderOutputWatchdog();

        // Normal streaming, including an output frame racing ahead of the next input report.
        for (long time = 0; time < 10000; time += 16) {
            check(!watchdog.onInput(time), "Healthy output must prevent recovery");
            watchdog.reset();
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
        System.out.println("DecoderOutputWatchdog regression tests passed");
    }
}
