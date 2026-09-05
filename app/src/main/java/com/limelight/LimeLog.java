package com.limelight;

import android.util.Log;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LimeLog {
    private static final Logger LOGGER = Logger.getLogger(LimeLog.class.getName());
    private static final String TAG = "Moonlight";
    private static volatile boolean fileLoggingEnabled;

    static {
        // Android's parent JUL handler writes to logcat too. Keep one logcat entry,
        // while retaining explicit file handlers for callers that request them.
        LOGGER.setUseParentHandlers(false);
    }

    public static void info(String msg) {
        Log.i(TAG, msg);
        if (fileLoggingEnabled) {
            LOGGER.info(msg);
        }
    }
    
    public static void warning(String msg) {
        Log.w(TAG, msg);
        if (fileLoggingEnabled) {
            LOGGER.warning(msg);
        }
    }
    
    public static void severe(String msg) {
        Log.e(TAG, msg);
        if (fileLoggingEnabled) {
            LOGGER.severe(msg);
        }
    }
    
    public static void setFileHandler(String fileName) throws IOException {
        LOGGER.addHandler(new FileHandler(fileName));
        fileLoggingEnabled = true;
    }
}
