package com.deepapp.utils;

/**
 * Path utilities for the DeepApp project
 * Provides functions to get consistent paths relative to the src directory
 */
public class PathUtils {

    /**
     * Get the base path of the project (src directory)
     * Assumes we're working within the src directory structure
     *
     * @return Absolute path to the src directory
     */
    public static String getProjectBasePath() {
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");

        // If we're in a subdirectory of src, find the src directory
        if (currentDir.contains("/src/")) {
            return currentDir.substring(0, currentDir.indexOf("/src/") + 4);
        } else if (currentDir.endsWith("/src")) {
            return currentDir;
        } else {
            // Fallback: assume we're in the project root and navigate to src
            return currentDir + "/src";
        }
    }

    /**
     * Get path to test data directory
     * @return Path to test directory relative to src
     */
    public static String getTestDataPath() {
        return getProjectBasePath() + "/main/python/test";
    }

    /**
     * Get path to a test image file
     * @param filename The filename
     * @return Full path to the test image file
     */
    public static String getTestImagePath(String filename) {
        return getTestDataPath() + "/" + filename;
    }

    /**
     * Get path to resources directory
     * @return Path to resources directory
     */
    public static String getResourcesPath() {
        return getProjectBasePath() + "/main/resources";
    }

    /**
     * Get path to models directory
     * @return Path to models directory
     */
    public static String getModelsPath() {
        return getResourcesPath() + "/models";
    }
}