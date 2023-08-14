package Strix_decopile.managers;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */

import Strix_decopile.StrixPlatform;
import Strix_decopile.Utils.*;
import Strix_decopile.configs.MainConfig;
import Strix_decopile.logging.StrixLog;

import java.util.HashSet;
import java.util.Set;

public class ClientGameSessionManager {
    private final Set<Long> SESSION_ID_LIST = new HashSet();

    public ClientGameSessionManager() {
    }

    public static ClientGameSessionManager getInstance() {
        return ClientGameSessionManager.LazyHolder.INSTANCE;
    }

    public boolean addSessionId(long sessionId) {
        if (MainConfig.STRIX_PLATFORM_GAME_SESSION_CHECK_ENABLED) {
            Long longValue = sessionId;
            if (this.SESSION_ID_LIST.contains(longValue)) {
                return false;
            }

            this.SESSION_ID_LIST.add(longValue);
        }

        return true;
    }

    public boolean checkKeyInfo(StrixClientData clientData) {
        if (clientData != null && clientData.getVMPKey().length() >= 64) {
            return clientData.getVMPKey().equalsIgnoreCase(MainConfig.STRIX_PLATFORM_KEY);
        } else {
            return false;
        }
    }

    public boolean checkHWIDChecksum(StrixClientData clientData) {
        if (clientData == null) {
            return false;
        } else {
            String clientHWID = clientData.getClientHWID();
            return clientHWID.length() == 32 && (long) DataUtils.getDataChecksum(clientHWID.getBytes(), true) == clientData.getHWIDChecksum();
        }
    }

    public boolean checkFilesChecksum(StrixClientData clientData) {
        return MainConfig.STRIX_PLATFORM_FILES_CHECKSUM == 0L || clientData != null && clientData.getFilesChecksum() == MainConfig.STRIX_PLATFORM_FILES_CHECKSUM;
    }

    public boolean checkDetectionInfo(StrixClientData clientData) {
        return clientData != null && clientData.getDetectionResponse() == DetectionResponse.RESPONSE_OK;
    }

    public boolean checkLaunchState(StrixClientData clientData) {
        if (clientData == null) {
            return false;
        } else {
            switch (clientData.getLaunchStateResponse()) {
                case RESPONSE_LAUNCHED_ON_VIRTUAL_MACHINE:
                    if (MainConfig.STRIX_PLATFORM_VIRTUAL_MACHINE_CHECK_ENABLED) {
                        return false;
                    }
                    break;
                case RESPONSE_LAUNCHED_ON_VIRTUAL_MACHIME_AND_FROM_LAUNCHER:
                    if (MainConfig.STRIX_PLATFORM_VIRTUAL_MACHINE_CHECK_ENABLED) {
                        return false;
                    }
                case RESPONSE_LAUNCHED_FROM_LAUNCHER:
                default:
                    break;
                case RESPONSE_LAUNCHED_NORMAL:
                    if (MainConfig.STRIX_PLATFORM_ONLY_LAUNCHER_CHECK_ENABLED) {
                        return false;
                    }
            }

            return true;
        }
    }

    public boolean checkClientSideVersion(StrixClientData clientData) {
        if (clientData == null) {
            return false;
        } else {
            if (MainConfig.STRIX_PLATFORM_CHECK_CLIENT_SIDE_VERSION) {
                if (MainConfig.STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION < 0) {
                    if (MainConfig.CLIENT_SIDE_VERSION_STORED != clientData.getClientSideVersion()) {
                        StrixPlatform.getInstance().checkClientSideVersion();
                        if (MainConfig.CLIENT_SIDE_VERSION_STORED != clientData.getClientSideVersion()) {
                            return false;
                        }
                    }
                } else if (MainConfig.STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION != clientData.getClientSideVersion()) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean checkActiveWindowCount(StrixClientData clientData) {
        if (clientData == null) {
            return false;
        } else {
            return MainConfig.STRIX_PLATFORM_ACTIVE_WINDOW_COUNT <= 0 || clientData.getActiveWindowCount() <= MainConfig.STRIX_PLATFORM_ACTIVE_WINDOW_COUNT;
        }
    }

    public void checkClientData(StrixClientData clientData) {
        if (!this.checkKeyInfo(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] incorrect license key");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_LICENSE_KEY_INFO_CHECK);
        } else if (!this.checkHWIDChecksum(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] incorrect HWID checksum]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_CLIENT_HWID_CHECKSUM_CHECK);
        } else if (!this.addSessionId(clientData.getSessionId())) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] game session [SessionID:" + clientData.getSessionId() + "] dublicated");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_GAME_SESSION_CHECK);
        } else if (!this.checkFilesChecksum(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] incorrect files checksum [ClientFilesChecksum:" + clientData.getFilesChecksum() + " != ServerFilesChecksum:" + MainConfig.STRIX_PLATFORM_FILES_CHECKSUM + "]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_CHECKSUM_CHECK);
        } else if (!this.checkDetectionInfo(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] detected soft [DetectionResponse:" + clientData.getDetectionResponse().toString() + "]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_DETECTION_INFO_CHECK);
        } else if (!this.checkLaunchState(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] bad launched state [LaunchStateResponse:" + clientData.getLaunchStateResponse().toString() + "]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_LAUNCHED_STATE_CHECK);
        } else if (!this.checkClientSideVersion(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] used old or incorrect version [ClientSideVersion:" + clientData.getClientSideVersion() + "|ServerSideVersion:" + (MainConfig.CLIENT_SIDE_VERSION_STORED > 0 ? MainConfig.CLIENT_SIDE_VERSION_STORED : MainConfig.STRIX_PLATFORM_MANUAL_CLIENT_SIDE_VERSION) + "]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_CLIENT_SIDE_VERSION_CHECK);
        } else if (!this.checkActiveWindowCount(clientData)) {
            StrixLog.audit("Client [HWID:" + clientData.getClientHWID() + "] loaded many window [ActiveWindowCount:" + clientData.getActiveWindowCount() + "]");
            clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_ACTIVE_WINDOW_COUNT);
        } else {
            clientData.setServerResponse(ServerResponse.RESPONSE_OK);
        }
    }

    public boolean checkServerResponse(StrixClientData clientData) {
        if (clientData != null && clientData.getServerResponse() != null) {
            switch (clientData.getServerResponse()) {
                case RESPONSE_FAILED_CLIENT_DATA_CHECKSUM_CHECK:
                case RESPONSE_FAILED_CLIENT_HWID_CHECKSUM_CHECK:
                    return false;
                case RESPONSE_FAILED_CHECKSUM_CHECK:
                    if (MainConfig.FAILED_CHECK_FILES_CHECKSUM != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_FILES_CHECKSUM, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_DETECTION_INFO_CHECK:
                    if (MainConfig.FAILED_CHECK_DETECTION_INFO != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_DETECTION_INFO, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_LICENSE_KEY_INFO_CHECK:
                    if (MainConfig.FAILED_CHECK_LICENSE_KEY != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_LICENSE_KEY, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_GAME_SESSION_CHECK:
                    if (MainConfig.FAILED_CHECK_GAME_SESSION != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_GAME_SESSION, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_LAUNCHED_STATE_CHECK:
                    if (MainConfig.FAILED_CHECK_LAUNCH_STATE != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_LAUNCH_STATE, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_CLIENT_SIDE_VERSION_CHECK:
                    if (MainConfig.FAILED_CHECK_CLIENT_SIDE_VERSION != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_CLIENT_SIDE_VERSION, clientData);
                        return false;
                    }
                    break;
                case RESPONSE_FAILED_ACTIVE_WINDOW_COUNT:
                    if (MainConfig.FAILED_CHECK_ACTIVE_WINDOW != FailedCheckResolve.NONE) {
                        ClientBanManager.getInstance().acceptResolve(MainConfig.FAILED_CHECK_ACTIVE_WINDOW, clientData);
                        return false;
                    }
            }

            return !ClientBanManager.getInstance().checkEnterFullHWIDBanned(clientData) && !ClientBanManager.getInstance().checkEnterBlockHWIDBanned(clientData);
        } else {
            return false;
        }
    }

    private static class LazyHolder {
        private static final ClientGameSessionManager INSTANCE = new ClientGameSessionManager();

        private LazyHolder() {
        }
    }
}