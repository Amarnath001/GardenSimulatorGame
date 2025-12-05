package com.garden.sim.core.logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger class matching oops_project implementation.
 * Provides static logging with different log levels.
 */
public class Logger {
    private static final String LOG_FILE = "log.txt"; // Log file name (per API spec)
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Log levels
    public enum LogLevel {
        INFO, WARNING, ERROR, DEBUG
    }

    private static boolean enableConsoleLogging = true; // Toggle console logging

    /**
     * Log a message to the log file and optionally to the console.
     *
     * @param level   The log level (INFO, WARNING, ERROR, DEBUG).
     * @param message The message to log.
     */
    public static void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // Write to log file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }

        // Optionally log to console
        if (enableConsoleLogging) {
            System.out.println(logEntry);
        }
    }

    /**
     * Enable or disable console logging.
     *
     * @param enable True to enable console logging, false to disable.
     */
    public static void setConsoleLogging(boolean enable) {
        enableConsoleLogging = enable;
    }
}
