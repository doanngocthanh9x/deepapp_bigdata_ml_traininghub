/**
 * Metrics and Monitoring
 * 
 * Unified metrics collection for C++ workers
 * Compatible with Prometheus and JSON logging
 */

#pragma once

#include <string>
#include <map>
#include <vector>
#include <chrono>
#include <atomic>
#include <mutex>
#include <iostream>
#include <sstream>
#include <iomanip>

namespace deepapp {
namespace monitoring {

/**
 * Metric types
 */
enum class MetricType {
    COUNTER,    // Monotonically increasing
    GAUGE,      // Can go up or down
    HISTOGRAM   // Distribution of values
};

/**
 * Single metric value
 */
struct Metric {
    std::string name;
    MetricType type;
    double value;
    std::map<std::string, std::string> labels;
    std::chrono::system_clock::time_point timestamp;
    
    Metric(const std::string& n, MetricType t, double v)
        : name(n), type(t), value(v), 
          timestamp(std::chrono::system_clock::now()) {}
};

/**
 * Metrics Registry - Singleton
 */
class MetricsRegistry {
public:
    static MetricsRegistry& getInstance() {
        static MetricsRegistry instance;
        return instance;
    }

    // Counter operations
    void incrementCounter(const std::string& name, 
                         const std::map<std::string, std::string>& labels = {},
                         double amount = 1.0) {
        std::lock_guard<std::mutex> lock(mutex_);
        std::string key = makeKey(name, labels);
        counters_[key] += amount;
    }

    // Gauge operations
    void setGauge(const std::string& name,
                  double value,
                  const std::map<std::string, std::string>& labels = {}) {
        std::lock_guard<std::mutex> lock(mutex_);
        std::string key = makeKey(name, labels);
        gauges_[key] = value;
    }

    void incrementGauge(const std::string& name,
                       double amount = 1.0,
                       const std::map<std::string, std::string>& labels = {}) {
        std::lock_guard<std::mutex> lock(mutex_);
        std::string key = makeKey(name, labels);
        gauges_[key] += amount;
    }

    // Histogram operations
    void recordHistogram(const std::string& name,
                        double value,
                        const std::map<std::string, std::string>& labels = {}) {
        std::lock_guard<std::mutex> lock(mutex_);
        std::string key = makeKey(name, labels);
        histograms_[key].push_back(value);
    }

    // Get all metrics in Prometheus format
    std::string getPrometheusMetrics() {
        std::lock_guard<std::mutex> lock(mutex_);
        std::stringstream ss;

        // Counters
        for (const auto& [key, value] : counters_) {
            auto [name, labels] = parseKey(key);
            ss << name << labels << " " << value << "\n";
        }

        // Gauges
        for (const auto& [key, value] : gauges_) {
            auto [name, labels] = parseKey(key);
            ss << name << labels << " " << value << "\n";
        }

        // Histograms (simplified - sum and count)
        for (const auto& [key, values] : histograms_) {
            auto [name, labels] = parseKey(key);
            double sum = 0;
            for (double v : values) sum += v;
            ss << name << "_sum" << labels << " " << sum << "\n";
            ss << name << "_count" << labels << " " << values.size() << "\n";
        }

        return ss.str();
    }

    // Get all metrics as JSON
    std::string getJsonMetrics() {
        std::lock_guard<std::mutex> lock(mutex_);
        std::stringstream ss;
        ss << "{";
        
        ss << "\"timestamp\":\"" << getCurrentTimestamp() << "\",";
        ss << "\"counters\":{";
        bool first = true;
        for (const auto& [key, value] : counters_) {
            if (!first) ss << ",";
            ss << "\"" << key << "\":" << value;
            first = false;
        }
        ss << "},";

        ss << "\"gauges\":{";
        first = true;
        for (const auto& [key, value] : gauges_) {
            if (!first) ss << ",";
            ss << "\"" << key << "\":" << value;
            first = false;
        }
        ss << "},";

        ss << "\"histograms\":{";
        first = true;
        for (const auto& [key, values] : histograms_) {
            if (!first) ss << ",";
            double sum = 0, min = values[0], max = values[0];
            for (double v : values) {
                sum += v;
                if (v < min) min = v;
                if (v > max) max = v;
            }
            ss << "\"" << key << "\":{";
            ss << "\"count\":" << values.size() << ",";
            ss << "\"sum\":" << sum << ",";
            ss << "\"avg\":" << (sum / values.size()) << ",";
            ss << "\"min\":" << min << ",";
            ss << "\"max\":" << max;
            ss << "}";
            first = false;
        }
        ss << "}";

        ss << "}";
        return ss.str();
    }

    // Reset all metrics
    void reset() {
        std::lock_guard<std::mutex> lock(mutex_);
        counters_.clear();
        gauges_.clear();
        histograms_.clear();
    }

private:
    MetricsRegistry() = default;
    
    std::mutex mutex_;
    std::map<std::string, double> counters_;
    std::map<std::string, double> gauges_;
    std::map<std::string, std::vector<double>> histograms_;

    std::string makeKey(const std::string& name,
                       const std::map<std::string, std::string>& labels) {
        std::stringstream ss;
        ss << name;
        if (!labels.empty()) {
            ss << "{";
            bool first = true;
            for (const auto& [k, v] : labels) {
                if (!first) ss << ",";
                ss << k << "=" << v;
                first = false;
            }
            ss << "}";
        }
        return ss.str();
    }

    std::pair<std::string, std::string> parseKey(const std::string& key) {
        size_t pos = key.find('{');
        if (pos == std::string::npos) {
            return {key, ""};
        }
        std::string name = key.substr(0, pos);
        std::string labels = key.substr(pos);
        return {name, labels};
    }

    std::string getCurrentTimestamp() {
        auto now = std::chrono::system_clock::now();
        auto time_t = std::chrono::system_clock::to_time_t(now);
        std::stringstream ss;
        ss << std::put_time(std::localtime(&time_t), "%Y-%m-%dT%H:%M:%S");
        return ss.str();
    }
};

/**
 * RAII Timer for measuring execution time
 */
class Timer {
public:
    Timer(const std::string& metric_name,
          const std::map<std::string, std::string>& labels = {})
        : metric_name_(metric_name), labels_(labels),
          start_(std::chrono::high_resolution_clock::now()) {}

    ~Timer() {
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
            end - start_
        ).count();
        
        MetricsRegistry::getInstance().recordHistogram(
            metric_name_, duration, labels_
        );
    }

    double elapsed() const {
        auto now = std::chrono::high_resolution_clock::now();
        return std::chrono::duration_cast<std::chrono::milliseconds>(
            now - start_
        ).count();
    }

private:
    std::string metric_name_;
    std::map<std::string, std::string> labels_;
    std::chrono::high_resolution_clock::time_point start_;
};

/**
 * Predefined metrics for workers
 */
class WorkerMetrics {
public:
    static void recordRequest(const std::string& worker_id,
                              const std::string& event_type) {
        auto& registry = MetricsRegistry::getInstance();
        registry.incrementCounter("worker_requests_total", {
            {"worker_id", worker_id},
            {"event_type", event_type}
        });
    }

    static void recordSuccess(const std::string& worker_id,
                             const std::string& event_type) {
        auto& registry = MetricsRegistry::getInstance();
        registry.incrementCounter("worker_requests_success", {
            {"worker_id", worker_id},
            {"event_type", event_type}
        });
    }

    static void recordError(const std::string& worker_id,
                           const std::string& event_type,
                           const std::string& error_type = "unknown") {
        auto& registry = MetricsRegistry::getInstance();
        registry.incrementCounter("worker_requests_error", {
            {"worker_id", worker_id},
            {"event_type", event_type},
            {"error_type", error_type}
        });
    }

    static void recordDuration(const std::string& worker_id,
                              const std::string& event_type,
                              double duration_ms) {
        auto& registry = MetricsRegistry::getInstance();
        registry.recordHistogram("worker_request_duration_ms", duration_ms, {
            {"worker_id", worker_id},
            {"event_type", event_type}
        });
    }

    static void setActiveRequests(const std::string& worker_id, int count) {
        auto& registry = MetricsRegistry::getInstance();
        registry.setGauge("worker_active_requests", count, {
            {"worker_id", worker_id}
        });
    }

    static void recordModelLoad(const std::string& model_name,
                               double duration_ms) {
        auto& registry = MetricsRegistry::getInstance();
        registry.recordHistogram("model_load_duration_ms", duration_ms, {
            {"model_name", model_name}
        });
        registry.incrementCounter("model_loads_total", {
            {"model_name", model_name}
        });
    }

    static void setModelMemory(const std::string& model_name,
                              size_t bytes) {
        auto& registry = MetricsRegistry::getInstance();
        registry.setGauge("model_memory_bytes", bytes, {
            {"model_name", model_name}
        });
    }
};

} // namespace monitoring
} // namespace deepapp
