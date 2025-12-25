/**
 * Structured JSON Logger
 * 
 * Unified logging format compatible with ELK, Loki, and other log aggregators
 */

#pragma once

#include <string>
#include <sstream>
#include <iostream>
#include <iomanip>
#include <chrono>
#include <map>

namespace deepapp {
namespace monitoring {

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
};

class Logger {
public:
    static Logger& getInstance() {
        static Logger instance;
        return instance;
    }

    void setLevel(LogLevel level) {
        min_level_ = level;
    }

    void setService(const std::string& service) {
        service_name_ = service;
    }

    void setVersion(const std::string& version) {
        version_ = version;
    }

    // Log methods
    void log(LogLevel level, 
             const std::string& message,
             const std::map<std::string, std::string>& context = {}) {
        if (level < min_level_) return;

        std::stringstream ss;
        ss << "{";
        ss << "\"timestamp\":\"" << getCurrentTimestamp() << "\",";
        ss << "\"level\":\"" << levelToString(level) << "\",";
        ss << "\"service\":\"" << service_name_ << "\",";
        ss << "\"version\":\"" << version_ << "\",";
        ss << "\"message\":\"" << escapeJson(message) << "\"";
        
        if (!context.empty()) {
            ss << ",\"context\":{";
            bool first = true;
            for (const auto& [key, value] : context) {
                if (!first) ss << ",";
                ss << "\"" << key << "\":\"" << escapeJson(value) << "\"";
                first = false;
            }
            ss << "}";
        }
        
        ss << "}\n";
        
        std::cout << ss.str() << std::flush;
    }

    void trace(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::TRACE, msg, ctx);
    }

    void debug(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::DEBUG, msg, ctx);
    }

    void info(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::INFO, msg, ctx);
    }

    void warn(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::WARN, msg, ctx);
    }

    void error(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::ERROR, msg, ctx);
    }

    void fatal(const std::string& msg, const std::map<std::string, std::string>& ctx = {}) {
        log(LogLevel::FATAL, msg, ctx);
    }

private:
    Logger() : min_level_(LogLevel::INFO), 
               service_name_("deepapp-worker"),
               version_("1.0.0") {}

    LogLevel min_level_;
    std::string service_name_;
    std::string version_;

    std::string levelToString(LogLevel level) {
        switch (level) {
            case LogLevel::TRACE: return "TRACE";
            case LogLevel::DEBUG: return "DEBUG";
            case LogLevel::INFO:  return "INFO";
            case LogLevel::WARN:  return "WARN";
            case LogLevel::ERROR: return "ERROR";
            case LogLevel::FATAL: return "FATAL";
            default: return "UNKNOWN";
        }
    }

    std::string getCurrentTimestamp() {
        auto now = std::chrono::system_clock::now();
        auto time_t = std::chrono::system_clock::to_time_t(now);
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
            now.time_since_epoch()
        ) % 1000;
        
        std::stringstream ss;
        ss << std::put_time(std::localtime(&time_t), "%Y-%m-%dT%H:%M:%S");
        ss << '.' << std::setfill('0') << std::setw(3) << ms.count();
        ss << std::put_time(std::localtime(&time_t), "%z");
        return ss.str();
    }

    std::string escapeJson(const std::string& str) {
        std::stringstream ss;
        for (char c : str) {
            switch (c) {
                case '\"': ss << "\\\""; break;
                case '\\': ss << "\\\\"; break;
                case '\b': ss << "\\b"; break;
                case '\f': ss << "\\f"; break;
                case '\n': ss << "\\n"; break;
                case '\r': ss << "\\r"; break;
                case '\t': ss << "\\t"; break;
                default: ss << c; break;
            }
        }
        return ss.str();
    }
};

} // namespace monitoring
} // namespace deepapp
