package Strix_decopile.database.impl;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import Strix_decopile.Utils.BannedHWIDInfo;
import Strix_decopile.database.DatabaseManager;
import Strix_decopile.logging.StrixLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BanDAO {
    private static final String LOAD_ALL_BAN = "SELECT `hwid`,`time_expire`,`reason`,`gm_name` FROM `strix_platform_hwid_ban` WHERE `hwid` IS NOT NULL";
    private static final String ADD_HWID = "INSERT INTO `strix_platform_hwid_ban` (hwid, time_expire, reason, gm_name) VALUES (?,?,?,?)";
    private static final String DELETE_HWID = "DELETE FROM strix_platform_hwid_ban WHERE hwid=?";

    public BanDAO() {
    }

    public static BanDAO getInstance() {
        return LazyHolder.INSTANCE;
    }

    public Map<String, BannedHWIDInfo> loadAllBannedHWID() {
        Map<String, BannedHWIDInfo> allBannedHWID = new ConcurrentHashMap();
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;

        try {
            con = DatabaseManager.getInstance().getConnection();
            statement = con.prepareStatement("SELECT `hwid`,`time_expire`,`reason`,`gm_name` FROM `strix_platform_hwid_ban` WHERE `hwid` IS NOT NULL");
            rset = statement.executeQuery();

            while(rset.next()) {
                String HWID = rset.getString("hwid");
                long timeExpire = rset.getLong("time_expire");
                String reason = rset.getString("reason");
                String gmName = rset.getString("gm_name");
                BannedHWIDInfo bhi = new BannedHWIDInfo(HWID, timeExpire, reason, gmName);
                if (!allBannedHWID.containsKey(HWID)) {
                    allBannedHWID.put(HWID, bhi);
                }
            }
        } catch (Exception var14) {
            StrixLog.error("Exception in function BanDAO::loadAllBannedHWID(). Exception: " + var14.getLocalizedMessage());
        } finally {
            DatabaseManager.closeQuietly(con, statement, rset);
        }

        return allBannedHWID;
    }

    public boolean insert(String HWID, long timeExpire, String reason, String gmName) {
        Connection con = null;
        PreparedStatement statement = null;

        boolean var9;
        try {
            con = DatabaseManager.getInstance().getConnection();
            statement = con.prepareStatement("INSERT INTO `strix_platform_hwid_ban` (hwid, time_expire, reason, gm_name) VALUES (?,?,?,?)");
            statement.setString(1, HWID);
            statement.setLong(2, timeExpire);
            statement.setString(3, reason);
            statement.setString(4, gmName);
            statement.executeUpdate();
            return true;
        } catch (Exception var13) {
            StrixLog.error("Exception in function BanDAO::insert(String, long, String, String). Exception: " + var13.getLocalizedMessage());
            var9 = false;
        } finally {
            DatabaseManager.closeQuietly(con, statement);
        }

        return var9;
    }

    public boolean insert(BannedHWIDInfo bannedHWIDInfo) {
        Connection con = null;
        PreparedStatement statement = null;

        boolean var5;
        try {
            con = DatabaseManager.getInstance().getConnection();
            statement = con.prepareStatement("INSERT INTO `strix_platform_hwid_ban` (hwid, time_expire, reason, gm_name) VALUES (?,?,?,?)");
            statement.setString(1, bannedHWIDInfo.getHWID());
            statement.setLong(2, bannedHWIDInfo.getTimeExpire());
            statement.setString(3, bannedHWIDInfo.getReason());
            statement.setString(4, bannedHWIDInfo.getGmName());
            statement.executeUpdate();
            return true;
        } catch (Exception var9) {
            StrixLog.error("Exception in function BanDAO::insert(BannedHWIDInfo). Exception: " + var9.getLocalizedMessage());
            var5 = false;
        } finally {
            DatabaseManager.closeQuietly(con, statement);
        }

        return var5;
    }

    public boolean delete(String HWID) {
        Connection con = null;
        PreparedStatement statement = null;

        boolean var5;
        try {
            con = DatabaseManager.getInstance().getConnection();
            statement = con.prepareStatement("DELETE FROM strix_platform_hwid_ban WHERE hwid=?");
            statement.setString(1, HWID);
            statement.execute();
            return true;
        } catch (Exception var9) {
            StrixLog.error("Exception in function BanDAO::deleteHWID(String). Exception: " + var9.getLocalizedMessage());
            var5 = false;
        } finally {
            DatabaseManager.closeQuietly(con, statement);
        }

        return var5;
    }

    private static class LazyHolder {
        private static final BanDAO INSTANCE = new BanDAO();

        private LazyHolder() {
        }
    }
}

