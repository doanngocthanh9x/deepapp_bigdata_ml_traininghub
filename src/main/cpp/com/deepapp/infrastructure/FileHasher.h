#pragma once

#include <string>
#include <vector>
#include <cstdint>
#include <openssl/sha.h>
#include <openssl/md5.h>
#include <iomanip>
#include <sstream>

namespace deepapp {
namespace infrastructure {

/**
 * File hashing utilities using SHA256
 */
class FileHasher {
public:
    /**
     * Calculate SHA256 hash of file data
     * @param data File data
     * @param size Data size
     * @return Hex string of SHA256 hash (64 chars)
     */
    static std::string sha256(const uint8_t* data, size_t size) {
        unsigned char hash[SHA256_DIGEST_LENGTH];
        SHA256(data, size, hash);

        std::stringstream ss;
        for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
            ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
        }
        return ss.str();
    }

    /**
     * Calculate SHA256 hash of vector data
     */
    static std::string sha256(const std::vector<uint8_t>& data) {
        return sha256(data.data(), data.size());
    }

    /**
     * Calculate MD5 hash (faster but less secure - OK for caching)
     */
    static std::string md5(const uint8_t* data, size_t size) {
        unsigned char hash[MD5_DIGEST_LENGTH];
        MD5(data, size, hash);

        std::stringstream ss;
        for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
            ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
        }
        return ss.str();
    }

    /**
     * Calculate MD5 hash of vector data
     */
    static std::string md5(const std::vector<uint8_t>& data) {
        return md5(data.data(), data.size());
    }
};

} // namespace infrastructure
} // namespace deepapp
