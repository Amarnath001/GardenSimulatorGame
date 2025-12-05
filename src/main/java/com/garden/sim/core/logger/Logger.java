package com.garden.sim.core.logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Logger class with LogLevel enum, similar to oops_project design pattern.
 * Provides centralized logging with different log levels.
 */
public class Logger {
    private final File file;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static boolean enableConsoleLogging = false; // Disable by default to avoid clutter

    /**
     * Log levels similar to oops_project.
     */
    public enum LogLevel {
        INFO, WARNING, ERROR, DEBUG
    }

    public Logger(String path) {
        this.file = new File(path);
    }

    /**
     * Log with LogLevel enum (similar to oops_project pattern).
     */
    public synchronized void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(fmt);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        write(logEntry, null);
        
        if (enableConsoleLogging) {
            System.out.println(logEntry);
        }
    }

    // Convenience methods for backward compatibility
    public synchronized void info(String msg) { 
        log(LogLevel.INFO, msg); 
    }
    
    public synchronized void warn(String msg) { 
        log(LogLevel.WARNING, msg); 
    }
    
    public synchronized void error(String msg, Throwable t) { 
        log(LogLevel.ERROR, msg);
        if (t != null) {
            write("", t); // Write stack trace
        }
    }
    
    public synchronized void debug(String msg) { 
        log(LogLevel.DEBUG, msg); 
    }

    private void write(String logEntry, Throwable t) {
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            if (!logEntry.isEmpty()) {
                out.println(logEntry);
            }
            if (t != null) {
                t.printStackTrace(out);
            }
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    /**
     * Enable or disable console logging.
     */
    public static void setConsoleLogging(boolean enable) {
        enableConsoleLogging = enable;
    }
}




