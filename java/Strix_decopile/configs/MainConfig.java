package Strix_decopile.configs;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import Strix_decopile.Utils.FailedCheckResolve;
import Strix_decopile.logging.StrixLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class MainConfig {
    public static final String CONFIG_FILE = "strix-platform/config/strix_platform_main.properties";
    public static final String LOG_FILE = "strix-platform/log/general.log";
    public static final String DEBUG_LOG_FILE = "strix-platform/log/debug.log";
    public static final String AUDIT_LOG_FILE = "strix-platform/log/audit.log";
    public static final String ERROR_LOG_FILE = "strix-platform/log/error.log";
    public static final String AUTH_LOG_FILE = "strix-platform/log/auth.log";
    public static final String STRIX_CLIENT_UPDATE_CHECK_URL = "http://l2j-project.su/info/client_version.txt";
    public static final int PROTOCOL_VERSION_DATA_SIZE = 264;
    public static final int CLIENT_DATA_SIZE = 260;
    public static boolean STRIX_PLATFORM_ENABLED;
    public static boolean STRIX_PLATFORM_DEBUG_ENABLED;
    public static boolean STRIX_PLATFORM_ENABLED_AUTHLOG;
    public static boolean STRIX_PLATFORM_GAME_SESSION_CHECK_ENABLED;
    public static boolean STRIX_PLATFORM_CLIENT_BACK_NOTIFICATION_ENABLED;
    public static boolean STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION;
    public static int STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION;
    public static int CLIENT_SIDE_VERSION_STORED;
    public static FailedCheckResolve FAILED_CHECK_LICENSE_KEY;
    public static FailedCheckResolve FAILED_CHECK_GAME_SESSION;
    public static FailedCheckResolve FAILED_CHECK_FILES_CHECKSUM;
    public static FailedCheckResolve FAILED_CHECK_DETECTION_INFO;
    public static FailedCheckResolve FAILED_CHECK_LAUNCH_STATE;
    public static FailedCheckResolve FAILED_CHECK_CLIENT_SIDE_VERSION;
    public static FailedCheckResolve FAILED_CHECK_ACTIVE_WINDOW;
    public static String STRIX_PLATFORM_DATABASE_DRIVER;
    public static String STRIX_PLATFORM_DATABASE_ADDRESS;
    public static int STRIX_PLATFORM_DATABASE_PORT;
    public static String STRIX_PLATFORM_DATABASE_NAME;
    public static String STRIX_PLATFORM_DATABASE_URL;
    public static String STRIX_PLATFORM_DATABASE_USER;
    public static String STRIX_PLATFORM_DATABASE_PASSWORD;
    public static int STRIX_PLATFORM_DATABASE_MAX_CONNECTIONS;
    public static int STRIX_PLATFORM_DATABASE_MAX_IDLE_TIMEOUT;
    public static int STRIX_PLATFORM_DATABASE_IDLE_TEST_PERIOD;
    public static String STRIX_PLATFORM_KEY;
    public static int STRIX_PLATFORM_SECOND_KEY;
    public static int STRIX_PLATFORM_ACTIVE_WINDOW_COUNT;
    public static int STRIX_PLATFORM_HWID_BLOCK_TO_LOCK;
    public static long STRIX_PLATFORM_AUTOMATICAL_BAN_TIME;
    public static long STRIX_PLATFORM_FILES_CHECKSUM;
    public static boolean STRIX_PLATFORM_VIRTUAL_MACHINE_CHECK_ENABLED;
    public static boolean STRIX_PLATFORM_ONLY_LAUNCHER_CHECK_ENABLED;
    public static boolean STRIX_PLATFORM_ANTIBRUTE;
    public static boolean STRIX_PLATFORM_DRAW;
    public static String STRIX_PLATFORM_DRAW_TEXT;
    public static String STX_PF_XOR_KEY;

    public MainConfig() {
    }

    private static boolean parseConfigFile() {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        try {
            try {
                fileInputStream = new FileInputStream(new File("strix-platform/config/strix_platform_main.properties"));
                inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                bufferedReader = new BufferedReader(inputStreamReader);
                Properties protectionSettings = new Properties();
                protectionSettings.load(bufferedReader);
                STRIX_PLATFORM_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.globalconfig.StrixPlatformEnabled", "false"));
                if (!STRIX_PLATFORM_ENABLED) {
                    return STRIX_PLATFORM_ENABLED;
                }

                try {
                    STRIX_PLATFORM_KEY = protectionSettings.getProperty("strixplatform.globalconfig.StrixPlatformKey", "NOT_INITED_KEY");
                    STRIX_PLATFORM_SECOND_KEY = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.StrixPlatformSecondKey", "-1"));
                    STX_PF_XOR_KEY = "" + STRIX_PLATFORM_SECOND_KEY;
                } catch (Exception var29) {
                    StrixLog.error("Error on load key string from guard config. Please check StrixPlatformKey and StrixPlatformSecondKey in strix-platform/config/strix_platform_main.properties");
                    STRIX_PLATFORM_ENABLED = false;
                    return STRIX_PLATFORM_ENABLED;
                }

                STRIX_PLATFORM_DATABASE_DRIVER = protectionSettings.getProperty("strixplatform.globalconfig.database.DatabaseDriver", "com.mysql.jdbc.Driver");
                STRIX_PLATFORM_DATABASE_ADDRESS = protectionSettings.getProperty("strixplatform.globalconfig.database.DatabaseAddress", "localhost");
                STRIX_PLATFORM_DATABASE_PORT = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.database.DatabasePort", "3306"));
                STRIX_PLATFORM_DATABASE_NAME = protectionSettings.getProperty("strixplatform.globalconfig.database.DatabaseName", "l2jdatabase_game_server");
                STRIX_PLATFORM_DATABASE_USER = protectionSettings.getProperty("strixplatform.globalconfig.database.DatabaseUser", "root");
                STRIX_PLATFORM_DATABASE_PASSWORD = protectionSettings.getProperty("strixplatform.globalconfig.database.DatabasePassword", "");
                STRIX_PLATFORM_DATABASE_MAX_CONNECTIONS = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.database.MaxConnection", "600"));
                STRIX_PLATFORM_DATABASE_MAX_IDLE_TIMEOUT = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.database.MaxIdleTimeout", "600"));
                STRIX_PLATFORM_DATABASE_IDLE_TEST_PERIOD = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.database.IdleTestPeriod", "60"));
                STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.globalconfig.EnableCheckClientSideVersion", "false"));
                STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.ManualClientSideVersion", "0"));

                try {
                    STRIX_PLATFORM_DATABASE_URL = "jdbc:mysql://";
                    STRIX_PLATFORM_DATABASE_URL = STRIX_PLATFORM_DATABASE_URL + STRIX_PLATFORM_DATABASE_ADDRESS + ":";
                    STRIX_PLATFORM_DATABASE_URL = STRIX_PLATFORM_DATABASE_URL + STRIX_PLATFORM_DATABASE_PORT + "/";
                    STRIX_PLATFORM_DATABASE_URL = STRIX_PLATFORM_DATABASE_URL + STRIX_PLATFORM_DATABASE_NAME;
                    STRIX_PLATFORM_DATABASE_URL = STRIX_PLATFORM_DATABASE_URL + "?characterEncoding=utf-8";
                } catch (Exception ignored) {
                }

                STRIX_PLATFORM_DEBUG_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.guard.DebugEnabled", "false"));
                STRIX_PLATFORM_ENABLED_AUTHLOG = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.guard.AuthLogEnabled", "false"));
                STRIX_PLATFORM_HWID_BLOCK_TO_LOCK = Integer.parseInt(protectionSettings.getProperty("strixplatform.guard.HWIDBlockToLock", "2"));
                STRIX_PLATFORM_AUTOMATICAL_BAN_TIME = (long)Integer.parseInt(protectionSettings.getProperty("strixplatform.guard.AutomaticalBanTime", "365"));
                STRIX_PLATFORM_GAME_SESSION_CHECK_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.guard.GameSessionCheckEnabled", "false"));
                STRIX_PLATFORM_CLIENT_BACK_NOTIFICATION_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.guard.BackToClientNotificationEnabled", "false"));
                STRIX_PLATFORM_ACTIVE_WINDOW_COUNT = Integer.parseInt(protectionSettings.getProperty("strixplatform.guard.ActiveWindowCount", "32"));
                STRIX_PLATFORM_FILES_CHECKSUM = Long.parseLong(protectionSettings.getProperty("strixplatform.extend.FilesChecksum", "0"));
                STRIX_PLATFORM_VIRTUAL_MACHINE_CHECK_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.extend.VirtualMachineCheckEnabled", "false"));
                STRIX_PLATFORM_ONLY_LAUNCHER_CHECK_ENABLED = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.extend.OnlyLauncherCheckEnabled", "false"));
                STRIX_PLATFORM_ANTIBRUTE = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.extend.antibrute", "false"));
                STRIX_PLATFORM_DRAW = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.extend.draw", "false"));
                STRIX_PLATFORM_DRAW_TEXT = protectionSettings.getProperty(protectionSettings.getProperty("strixplatform.extend.draw.text", "Strix-Platform"));
                FAILED_CHECK_LICENSE_KEY = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.LicenseFile", "NONE"));
                FAILED_CHECK_GAME_SESSION = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.GameSession", "NONE"));
                FAILED_CHECK_FILES_CHECKSUM = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.FilesChecksum", "NONE"));
                FAILED_CHECK_DETECTION_INFO = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.DetectionInfo", "NONE"));
                FAILED_CHECK_LAUNCH_STATE = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.LaunchState", "NONE"));
                FAILED_CHECK_CLIENT_SIDE_VERSION = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.ClientSideVersion", "NONE"));
                FAILED_CHECK_ACTIVE_WINDOW = FailedCheckResolve.valueOf(protectionSettings.getProperty("strixplatform.check.ActiveWindow", "NONE"));
            } catch (IOException var30) {
                StrixLog.error("Config file not found or parser return error. Please check strix-platform/config/strix_platform_main.properties file");
            }

            return STRIX_PLATFORM_ENABLED;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException var27) {
                    var27.printStackTrace();
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException var26) {
                    var26.printStackTrace();
                }
            }

            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException var25) {
                    var25.printStackTrace();
                }
            }

        }
    }

    public static void init() {
        boolean loaded = parseConfigFile();
        StrixLog.info("Configuration file loaded! Strix-Platform: " + (loaded ? "Enabled" : "Disabled"));
    }

    public static void reparseClientSideVersion() {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        try {
            fileInputStream = new FileInputStream(new File("strix-platform/config/strix_platform_main.properties"));
            inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(inputStreamReader);
            Properties protectionSettings = new Properties();
            protectionSettings.load(bufferedReader);
            boolean SPEnabled = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.globalconfig.StrixPlatformEnabled", "false"));
            if (SPEnabled) {
                STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION = Boolean.parseBoolean(protectionSettings.getProperty("strixplatform.globalconfig.EnableCheckClientSideVersion", "false"));
                STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION = Integer.parseInt(protectionSettings.getProperty("strixplatform.globalconfig.ManualClientSideVersion", "0"));
            }
        } catch (IOException var21) {
            StrixLog.error("Config file not found or parser return error. Please check strix-platform/config/strix_platform_main.properties file");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}

