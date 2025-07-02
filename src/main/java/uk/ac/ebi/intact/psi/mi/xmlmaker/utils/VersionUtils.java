package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import static uk.ac.ebi.intact.psi.mi.xmlmaker.utils.GuiUtils.*;


/**
 * Utility class for managing the versioning and update functionality of the PSI-MI XML Maker application.
 * <p>
 * This class checks for newer versions of the application on GitHub, downloads the appropriate artifact based on
 * the operating system, and optionally schedules a restart to complete the update process.
 * </p>
 */
public class VersionUtils {

    private static final Logger LOGGER = Logger.getLogger(VersionUtils.class.getName());
    private static final String VERSION_URL = "https://raw.githubusercontent.com/MICommunity/psi-mi-xml-maker/refs/heads/main/src/main/resources/xmlMaker.properties";
    private static final String GITHUB_URL = "https://github.com/MICommunity/psi-mi-xml-maker/releases/download/";

    private static String latestVersion = "UNKNOWN";

    private static String DOWNLOAD_JAR_URL = GITHUB_URL + latestVersion + "/PSI-MI-XML-maker-" + latestVersion + "-runnable.3.jar";
    private static String DOWNLOAD_DMG_URL = GITHUB_URL + latestVersion + "/PSI-MI-XML-maker_" + latestVersion + "-MacOS.dmg";
    private static String DOWNLOAD_ZIP_URL = GITHUB_URL + latestVersion + "/PSI-MI-XML-maker_" + latestVersion + "-windows.zip";
    private static String DOWNLOAD_TAR_URL = GITHUB_URL + latestVersion + "/PSI-MI-XML-maker_" + latestVersion + "-linux.tar.gz";

    private static String SAVE_JAR_PATH = "downloadableFiles/PSI-MI-XML-maker" + latestVersion + ".jar";
    private static String SAVE_DMG_PATH = "downloadableFiles/PSI-MI-XML-maker" + latestVersion + "-MacOS.dmg";
    private static String SAVE_ZIP_PATH = "downloadableFiles/PSI-MI-XML-maker" + latestVersion + "-windows.zip";
    private static String SAVE_TAR_PATH = "downloadableFiles/PSI-MI-XML-maker" + latestVersion + "-linux.tar.gz";

    /**
     * Reads the current application version from the internal resource file {@code xmlMaker.properties}.
     *
     * @return the current version as specified in the properties file, or {@code "UNKNOWN"} if not found or unreadable.
     */
    public static String getCurrentVersion() {
        try (InputStream inputStream = VersionUtils.class.getResourceAsStream("/xmlMaker.properties")) {
            if (inputStream == null) {
                LOGGER.warning("Could not find xmlMaker.properties");
                return "UNKNOWN";
            }
            Properties props = new Properties();
            props.load(inputStream);
            return props.getProperty("version", "UNKNOWN");
        } catch (IOException e) {
            LOGGER.warning("Error reading version: " + e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Checks for a newer version by comparing the current version to the latest version available on GitHub.
     * If an update is available, prompts the user to download it and restart the application.
     */
    public static void checkForUpdates() {
        String currentVersion = getCurrentVersion();
        try {
            latestVersion = getString();
            if (latestVersion == null) {
                LOGGER.warning("Could not find latest version");
                return;
            }

            initialiseUrls(latestVersion);

            if (!latestVersion.equals(currentVersion)) {
                showInfoDialog("Update available! You're on version: " + currentVersion + ", latest version: " + latestVersion);
                LOGGER.warning("Current version: " + currentVersion + ", latest version: " + latestVersion);
                downloadDependingOnOs();
            }
        } catch (Exception e) {
            LOGGER.warning("Error checking for updates: " + e.getMessage());
        }
    }

    /**
     * Downloads the appropriate artifact depending on the user's operating system:
     * <ul>
     *     <li>macOS → DMG</li>
     *     <li>Windows → ZIP</li>
     *     <li>Linux → TAR.GZ</li>
     *     <li>Other/Unknown → runnable JAR</li>
     * </ul>
     */
    public static void downloadDependingOnOs(){
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            downloadLatestVersion(DOWNLOAD_DMG_URL, SAVE_DMG_PATH);
        } else if (osName.contains("win")) {
            downloadLatestVersion(DOWNLOAD_ZIP_URL, SAVE_ZIP_PATH);
        } else if (osName.contains("linux")) {
            downloadLatestVersion(DOWNLOAD_TAR_URL, SAVE_TAR_PATH);
        }  else {
            downloadLatestVersion(DOWNLOAD_JAR_URL, SAVE_JAR_PATH);
        }
    }

    /**
     * Downloads and parses the remote {@code xmlMaker.properties} file to extract the latest version string.
     *
     * @return the version string (e.g., "1.1.3") if available, or {@code null} if not found or error occurs.
     * @throws IOException if the URL cannot be read or the content is malformed.
     */
    private static String getString() throws IOException {
        URL url = new URL(VERSION_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        String latestVersion = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("version=")) {
                    latestVersion = line.split("=")[1].trim();
                    break;
                }
            }
        }
        return latestVersion;
    }

    /**
     * Downloads a release artifact from a given URL and saves it to the provided path.
     * Upon successful download, schedule an application restart using the downloaded artifact.
     *
     * @param downloadUrl The URL of the file to download.
     * @param savingPath  The local path where the file should be saved.
     */
    public static void downloadLatestVersion(String downloadUrl, String savingPath){
        try {
            LOGGER.info("Downloading latest jar: " + downloadUrl);

            URL url = new URL(downloadUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty("Accept", "application/octet-stream");

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = httpConn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(savingPath)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    LOGGER.info("Downloaded " + savingPath + " from " + downloadUrl);
                    scheduleRestart(savingPath);
                }
            } else {
                LOGGER.warning("No file to download. Server replied with code: " + responseCode);
            }
            httpConn.disconnect();
        } catch (IOException e) {
            LOGGER.severe("Error downloading latest jar: " + e.getMessage());
        }
    }

    /**
     * Schedules a restart of the application using the specified path, tailored to the operating system.
     * Shows a confirmation dialog to the user before initiating the restart.
     *
     * @param savingPath Path to the downloaded artifact used for restarting.
     */
    private static void scheduleRestart(String savingPath) {
        String[] command;
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            command = new String[] {
                    "cmd", "/c",
                    "timeout /t 3 && " +
                            "start \"\" \"" + savingPath + "\""
            };
        } else if (osName.contains("mac")) {
            command = new String[] {
                    "/bin/bash", "-c",
                    "sleep 3 && open \"" + savingPath + "\""
            };
        } else {
            command = new String[] {
                    "/bin/bash", "-c",
                    "sleep 3 && java -jar \"" + savingPath + "\""
            };
        }

        try {
            boolean restartNow = showConfirmDialog(
                    "Update downloaded successfully. Restart now to apply the update?");

            if (restartNow) {
                Runtime.getRuntime().exec(command);
                System.exit(0);
            }
        } catch (IOException e) {
            LOGGER.severe("Error scheduling restart: " + e.getMessage());
        }
    }

    /**
     * Initializes the download URLs and local save paths with the provided version number.
     * This method updates all previously defined static paths that include the version string.
     *
     * @param version The version string to use in the URLs and save paths.
     */
    private static void initialiseUrls(String version) {
        DOWNLOAD_JAR_URL = GITHUB_URL + version + "/PSI-MI-XML-maker-" + version + "-runnable.3.jar";
        DOWNLOAD_DMG_URL = GITHUB_URL + version + "/PSI-MI-XML-maker_" + version + "-MacOS.dmg";
        DOWNLOAD_ZIP_URL = GITHUB_URL + version + "/PSI-MI-XML-maker_" + version + "-windows.zip";
        DOWNLOAD_TAR_URL = GITHUB_URL + version + "/PSI-MI-XML-maker_" + version + "-linux.tar.gz";

        SAVE_JAR_PATH = "downloadableFiles/PSI-MI-XML-maker-" + version + ".jar";
        SAVE_DMG_PATH = "downloadableFiles/PSI-MI-XML-maker_" + version + "-MacOS.dmg";
        SAVE_ZIP_PATH = "downloadableFiles/PSI-MI-XML-maker_" + version + "-windows.zip";
        SAVE_TAR_PATH = "downloadableFiles/PSI-MI-XML-maker_" + version + "-linux.tar.gz";
    }
}
