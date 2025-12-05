package com.garden.sim.core.logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Logger class matching oops_project implementation.
 * Provides static logging with different log levels.
 * Includes log rotation to prevent unbounded file growth during long-running sessions.
 */
public class Logger {
    private static final String LOG_FILE = "log.txt"; // Log file name (per API spec)
    private static final long MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB max file size
    private static final int KEEP_LINES_ON_ROTATION = 1000; // Keep last 1000 lines when rotating
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Log levels
    public enum LogLevel {
        INFO, WARNING, ERROR, DEBUG
    }

    private static boolean enableConsoleLogging = true; // Toggle console logging

    /**
     * Log a message to the log file and optionally to the console.
     * Automatically rotates log file if it exceeds MAX_LOG_SIZE_BYTES.
     *
     * @param level   The log level (INFO, WARNING, ERROR, DEBUG).
     * @param message The message to log.
     */
    public static void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // Check if log rotation is needed before writing
        rotateLogIfNeeded();

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
     * Rotates the log file if it exceeds the maximum size.
     * Keeps the last KEEP_LINES_ON_ROTATION lines and archives the rest.
     */
    private static void rotateLogIfNeeded() {
        try {
            Path logPath = Paths.get(LOG_FILE);
            if (!Files.exists(logPath)) {
                return; // No log file yet, nothing to rotate
            }

            long fileSize = Files.size(logPath);
            if (fileSize < MAX_LOG_SIZE_BYTES) {
                return; // File size is within limits
            }

            // Read all lines from the log file
            List<String> allLines = Files.readAllLines(logPath);
            
            if (allLines.size() <= KEEP_LINES_ON_ROTATION) {
                return; // Not enough lines to rotate
            }

            // Keep only the last KEEP_LINES_ON_ROTATION lines
            List<String> keptLines = allLines.subList(
                allLines.size() - KEEP_LINES_ON_ROTATION, 
                allLines.size()
            );

            // Archive old log with timestamp
            String archiveName = "log_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            ) + ".txt";
            Files.write(Paths.get(archiveName), allLines.subList(0, allLines.size() - KEEP_LINES_ON_ROTATION));

            // Write kept lines back to log file
            Files.write(logPath, keptLines);

            // Log rotation event (to console only to avoid recursion)
            String rotationMsg = String.format(
                "[%s] [INFO] Log file rotated: archived %d lines to %s, kept last %d lines",
                LocalDateTime.now().format(formatter),
                allLines.size() - KEEP_LINES_ON_ROTATION,
                archiveName,
                KEEP_LINES_ON_ROTATION
            );
            System.out.println(rotationMsg);
        } catch (IOException e) {
            System.err.println("Error rotating log file: " + e.getMessage());
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
