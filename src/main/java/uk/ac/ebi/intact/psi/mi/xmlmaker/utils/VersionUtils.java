package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

public class VersionUtils {

    private static final Logger LOGGER = Logger.getLogger(VersionUtils.class.getName());
//    private static final String VERSION_URL = "https://raw.githubusercontent.com/MICommunity/psi-mi-xml-maker/refs/heads/main/src/main/resources/xmlMaker.properties"; //todo: change for release
    private static final String VERSION_URL = "https://raw.githubusercontent.com/MICommunity/psi-mi-xml-maker/refs/heads/1.1/src/main/resources/xmlMaker.properties";
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
                XmlMakerUtils.showInfoDialog("Update available! You're on version: " + currentVersion + ", latest version: " + latestVersion);
                LOGGER.warning("Current version: " + currentVersion + ", latest version: " + latestVersion);
                downloadDependingOnOs();
            }
        } catch (Exception e) {
            LOGGER.warning("Error checking for updates: " + e.getMessage());
        }
    }

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
            boolean restartNow = XmlMakerUtils.showConfirmDialog(
                    "Update downloaded successfully. Restart now to apply the update?");

            if (restartNow) {
                Runtime.getRuntime().exec(command);
                System.exit(0);
            }
        } catch (IOException e) {
            LOGGER.severe("Error scheduling restart: " + e.getMessage());
        }
    }

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
