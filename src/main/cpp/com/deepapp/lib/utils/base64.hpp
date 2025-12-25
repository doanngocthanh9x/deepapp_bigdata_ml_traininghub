/**
 * Base64 Encoding/Decoding Utilities
 * 
 * Shared utility for base64 operations across all workers
 */

#pragma once

#include <string>
#include <vector>
#include <stdexcept>

namespace deepapp {
namespace lib {
namespace utils {

class Base64 {
public:
    /**
     * Decode base64 string to binary data
     */
    static std::vector<unsigned char> decode(const std::string& encoded_string) {
        static const std::string base64_chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz"
            "0123456789+/";
        
        std::vector<unsigned char> decoded_data;
        std::vector<int> T(256, -1);
        
        for (int i = 0; i < 64; i++) {
            T[base64_chars[i]] = i;
        }
        
        int val = 0, valb = -8;
        for (unsigned char c : encoded_string) {
            if (T[c] == -1) break;
            val = (val << 6) + T[c];
            valb += 6;
            if (valb >= 0) {
                decoded_data.push_back(char((val >> valb) & 0xFF));
                valb -= 8;
            }
        }
        
        return decoded_data;
    }

    /**
     * Encode binary data to base64 string
     */
    static std::string encode(const std::vector<unsigned char>& data) {
        static const std::string base64_chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz"
            "0123456789+/";
        
        std::string encoded_string;
        int val = 0, valb = -6;
        
        for (unsigned char c : data) {
            val = (val << 8) + c;
            valb += 8;
            while (valb >= 0) {
                encoded_string.push_back(base64_chars[(val >> valb) & 0x3F]);
                valb -= 6;
            }
        }
        
        if (valb > -6) {
            encoded_string.push_back(base64_chars[((val << 8) >> (valb + 8)) & 0x3F]);
        }
        
        while (encoded_string.size() % 4) {
            encoded_string.push_back('=');
        }
        
        return encoded_string;
    }

    /**
     * Encode string to base64
     */
    static std::string encode(const std::string& str) {
        std::vector<unsigned char> data(str.begin(), str.end());
        return encode(data);
    }
};

} // namespace utils
} // namespace lib
} // namespace deepapp
