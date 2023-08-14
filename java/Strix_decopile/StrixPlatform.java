package Strix_decopile;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import Strix_decopile.Utils.ThreadPoolManager;
import Strix_decopile.configs.MainConfig;
import Strix_decopile.database.DatabaseManager;
import Strix_decopile.logging.StrixLog;
import Strix_decopile.managers.ClientBanManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class StrixPlatform {

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            StrixLog.error("Strix-Platform: Main class not specified!");
        } else {
            Class<?> clazz = null;

            try {
                clazz = Class.forName(args[0]);
            } catch (Exception ignored) {
            }

            if (clazz == null) {
                StrixLog.error("Strix-Platform: Main class not found : " + args[0] + "!");
            } else {
                getInstance();
                Method main = clazz.getDeclaredMethod("main", String[].class);
                args = Arrays.copyOfRange(args, 1, args.length);
                main.invoke(null, args);
            }
        }
    }

    public static StrixPlatform getInstance() {
        return LazyHolder.INSTANCE;
    }

    public boolean isPlatformEnabled() {
        return MainConfig.STRIX_PLATFORM_ENABLED;
    }

    public boolean isAuthLogEnabled() {
        return MainConfig.STRIX_PLATFORM_ENABLED_AUTHLOG;
    }

    public boolean isBackNotificationEnabled() {
        return MainConfig.STRIX_PLATFORM_CLIENT_BACK_NOTIFICATION_ENABLED;
    }

    public boolean isPlatformAntibrute() {
        return MainConfig.STRIX_PLATFORM_ANTIBRUTE;
    }

    public boolean isPlatformDraw() {
        return MainConfig.STRIX_PLATFORM_DRAW;
    }

    public String isPlatformDrawText() {
        return MainConfig.STRIX_PLATFORM_DRAW_TEXT;
    }

    public int getProtocolVersionDataSize() {
        return 264;
    }

    public int getClientDataSize() {
        return 260;
    }

    public void checkClientSideVersion() {
        if (!MainConfig.STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION) {
            StrixLog.info("Automatical update check disabled. If needed, see Strix-Platform configuration file from path strix-platform/config/strix_platform_main.properties");
        } else if (MainConfig.STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION < 0) {
            BufferedReader in = null;

            try {
                URL url = new URL("http://l2j-project.su/info/client_version.txt");
                URLConnection conn = url.openConnection();
                conn.setDefaultUseCaches(false);
                conn.setRequestProperty("User-Agent", "StrixPlatform/" + MainConfig.STRIX_PLATFORM_KEY + "/" + MainConfig.STRIX_PLATFORM_SECOND_KEY);
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                String buffer;
                int loadedVersion;
                for (loadedVersion = -1; (buffer = in.readLine()) != null; loadedVersion = Integer.parseInt(buffer)) {
                    if (buffer.length() > 3) {
                        StrixLog.error("Update server Strix-Platform not avaliable on this time, or your firewall or server configuration cannot use out connection to Strix-Platform server. This option seted to DISABLED...");
                        MainConfig.STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION = false;
                        return;
                    }
                }

                MainConfig.CLIENT_SIDE_VERSION_STORED = loadedVersion;
            } catch (Exception var16) {
                StrixLog.error("Error on check client side version. Please, check your server configuration, firewall, network, etc... Exception: " + var16.getLocalizedMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception var15) {
                        StrixLog.error("Error on close loaded buffer. Send this info to Strix-Platform team support! Exception: " + var15.getLocalizedMessage());
                    }
                }

            }
        } else {
            this.startClientSideVersionCheckThread();
        }

    }

    private void startClientSideVersionCheckThread() {
        ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable() {
            public void run() {
                MainConfig.reparseClientSideVersion();
            }
        }, 5000L, 30000L);
    }

    private StrixPlatform() {
        try {
            StrixLog.info("|============= Strix-Platform =============|");
            MainConfig.init();
            if (MainConfig.STRIX_PLATFORM_ENABLED) {
                this.checkClientSideVersion();
                DatabaseManager.getInstance().getConnection().close();
                ClientBanManager.getInstance();
            }
        } catch (Exception e) {
            StrixLog.error("An error occurred during initialization. Disabling protection ...");
            MainConfig.STRIX_PLATFORM_ENABLED = false;
        } finally {
            StrixLog.info("|============= Strix-Platform =============|");
        }

    }

    private static class LazyHolder {
        private static final StrixPlatform INSTANCE = new StrixPlatform();

        private LazyHolder() {
        }
    }
}
