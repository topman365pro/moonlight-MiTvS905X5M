#!/bin/sh
set -eu
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
javac -d "$test_dir" \
    "$repo_dir/app/src/main/java/com/limelight/binding/video/VideoTimestampNormalizer.java" \
    "$repo_dir/app/src/main/java/com/limelight/binding/video/DecoderOutputWatchdog.java" \
    "$repo_dir/app/src/test/java/com/limelight/binding/video/DecoderOutputWatchdogTest.java"
java -cp "$test_dir" com.limelight.binding.video.DecoderOutputWatchdogTest
