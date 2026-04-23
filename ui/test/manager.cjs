#!/usr/bin/env node

const { spawn } = require("node:child_process");
const path = require("node:path");
const os = require("node:os");

// Change working directory to repository root
process.chdir(path.resolve(__dirname, "../.."));

// --- Default Environment Variables ---
process.env.OR_STORAGE_DIR = process.env.OR_STORAGE_DIR || "ui/test/tmp"; // Changed to relative local directory
process.env.OR_APP_DOCROOT = process.env.OR_APP_DOCROOT || "manager/build/install/manager/web";
// Variables without defaults
process.env.OR_LOGGING_CONFIG_FILE = process.env.OR_LOGGING_CONFIG_FILE || path.join("ui", "test", "logging.properties");

// --- Local Paths ---
// Ensure you have run `./gradlew :manager:installDist` so these folders exist.
const APP_LIB_DIR = path.join("manager", "build", "install", "manager", "lib", "*");
const EXTENSIONS_DIR = path.join("deployment", "manager", "extensions", "*");
// Handle Classpath Separator (':' on Unix, ';' on Windows)
const separator = os.platform() === "win32" ? ";" : ":";
const classPath = `${APP_LIB_DIR}${separator}${EXTENSIONS_DIR}`;

// --- Execution ---
console.log("Starting OpenRemote Manager...");
console.log(`Classpath: ${classPath}`);

// --- Prepare Java arguments ---
// We split OR_JAVA_OPTS by space if it exists, or use the default JVM options defined in our Dockerfile
const javaOpts = process.env.OR_JAVA_OPTS ? process.env.OR_JAVA_OPTS.split(" ") : [
    "-Xms500m",
    "-Xmx2g",
    "-XX:NativeMemoryTracking=summary",
    "-Xlog:all=warning:stdout:uptime,level,tags",
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:HeapDumpPath=./dump.hprof",
];
const args = [...javaOpts, "-cp", classPath, "org.openremote.manager.Main"];

// --- Execute Java ---
const child = spawn("java", args, {
    stdio: "inherit", // Forwards all output/input to the terminal
    shell: false
});
process.on("SIGINT", () => child.kill());
process.on("SIGTERM", () => child.kill());
