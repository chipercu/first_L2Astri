package Strix_decopile.Utils;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


public class BannedHWIDInfo {
    private final String HWID;
    private final long timeExpire;
    private final String reason;
    private final String gmName;

    public BannedHWIDInfo(String HWID, long timeExpire, String reason, String gmName) {
        this.HWID = HWID;
        this.timeExpire = timeExpire;
        this.reason = reason;
        this.gmName = gmName;
    }

    public String getHWID() {
        return this.HWID;
    }

    public long getTimeExpire() {
        return this.timeExpire;
    }

    public String getReason() {
        return this.reason;
    }

    public String getGmName() {
        return this.gmName;
    }

    public String toString() {
        return "Banned info: HWID=[" + this.HWID + "] TimeExpire=[" + this.timeExpire + "] Reason=[" + this.reason + "] GMName=[" + this.gmName + "]";
    }
}

