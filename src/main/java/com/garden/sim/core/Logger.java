package com.garden.sim.core;

/**
 * Legacy Logger wrapper for backward compatibility.
 * Delegates to the enhanced Logger in logger package.
 */
public class Logger {
    private final com.garden.sim.core.logger.Logger enhancedLogger;

    public Logger(String path) {
        this.enhancedLogger = new com.garden.sim.core.logger.Logger(path);
    }

    public synchronized void info(String msg) { 
        enhancedLogger.log(com.garden.sim.core.logger.Logger.LogLevel.INFO, msg); 
    }
    
    public synchronized void warn(String msg) { 
        enhancedLogger.log(com.garden.sim.core.logger.Logger.LogLevel.WARNING, msg); 
    }
    
    public synchronized void error(String msg, Throwable t) { 
        enhancedLogger.log(com.garden.sim.core.logger.Logger.LogLevel.ERROR, msg);
        if (t != null) {
            enhancedLogger.error(msg, t);
        }
    }
    
    // Expose the enhanced logger for advanced usage
    public com.garden.sim.core.logger.Logger getEnhancedLogger() {
        return enhancedLogger;
    }
}
