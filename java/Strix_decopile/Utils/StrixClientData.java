package Strix_decopile.Utils;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */

public class StrixClientData {
    private String HWID;
    private String VMPKey;
    private String clientAccount;
    private long sessionId;
    private long HWIDChecksum;
    private long filesChecksum;
    private DetectionResponse detectionResponse;
    private LaunchStateResponse launchStateResponse;
    private ServerResponse serverResponse;
    private int clientSideVersion;
    private int activeWindowCount;

    public StrixClientData() {
    }

    public void setVMPKey(String vmpKey) {
        this.VMPKey = vmpKey;
    }

    public String getVMPKey() {
        return this.VMPKey;
    }

    public void setDetectionResponse(long detectionInfo) {
        this.detectionResponse = DetectionResponse.valueOf((int)detectionInfo);
    }

    public DetectionResponse getDetectionResponse() {
        return this.detectionResponse;
    }

    public void setLaunchStateResponse(long launchState) {
        this.launchStateResponse = LaunchStateResponse.valueOf((int)launchState);
    }

    public LaunchStateResponse getLaunchStateResponse() {
        return this.launchStateResponse;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public void setFilesChecksum(long filesChecksum) {
        this.filesChecksum = filesChecksum;
    }

    public long getFilesChecksum() {
        return this.filesChecksum;
    }

    public void setHWIDChecksum(long HWIDChecksum) {
        this.HWIDChecksum = HWIDChecksum;
    }

    public long getHWIDChecksum() {
        return this.HWIDChecksum;
    }

    public void setClientHWID(String HWID) {
        this.HWID = HWID;
    }

    public String getClientHWID() {
        return this.HWID;
    }

    public void setServerResponse(ServerResponse response) {
        this.serverResponse = response;
    }

    public ServerResponse getServerResponse() {
        return this.serverResponse;
    }

    public void setClientAccount(String clientAccount) {
        this.clientAccount = clientAccount;
    }

    public String getClientAccount() {
        return this.clientAccount;
    }

    public void setClientSideVersion(int clientSideVersion) {
        this.clientSideVersion = clientSideVersion;
    }

    public int getClientSideVersion() {
        return this.clientSideVersion;
    }

    public void setActiveWindowCount(int activeWindowCount) {
        this.activeWindowCount = activeWindowCount;
    }

    public int getActiveWindowCount() {
        return this.activeWindowCount;
    }

    public String toString() {
        String toString = "ClientAccount: [" + (this.clientAccount != null ? this.clientAccount : "UNKNOW") + "] HWID: [" + this.HWID + "] VMPKey: [" + this.VMPKey + "] DetectionResponse: [" + (this.detectionResponse != null ? this.detectionResponse.getDescription() : "NULL") + "] SessionID: [" + this.sessionId + "] FilesChecksum: [" + this.filesChecksum + "] ServerResponse: [" + (this.serverResponse != null ? this.serverResponse.toString() : "NULL") + "] LaunchStateResponse: [" + (this.launchStateResponse != null ? this.launchStateResponse.getDescription() : "NULL") + "]";
        return toString;
    }
}
