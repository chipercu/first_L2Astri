package fuzzy.Database_Util.DB_Util;

import events.EveryDayReward.EveryDayReward;
import fuzzy.Database_Util.model.PlayerVariables;
import l2ft.commons.dbutils.DbUtils;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by a.kiperku
 * Date: 10.08.2023
 */

public class DB_Player {
    private static final Logger _log = LoggerFactory.getLogger(EveryDayReward.class);



    public static List<Integer> loadPlayerIdByAccountName(Player player) {
        List<Integer> accountList = new ArrayList<>();
        Connection con = null;
        PreparedStatement offline = null;
        ResultSet rs = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            offline = con.prepareStatement("SELECT * FROM characters WHERE account_name = ?");
            offline.setString(1, player.getAccountName());
            rs = offline.executeQuery();
            while(rs.next()) {
                int playerId = rs.getInt("obj_Id");
                accountList.add(playerId);
            }
        }
        catch(Exception e) {
            _log.error("", e);
        } finally {
            DbUtils.closeQuietly(con, offline, rs);
        }
        return accountList;
    }

    public static List<PlayerVariables> loadVariables(int playerId) {
        Connection con = null;
        PreparedStatement offline = null;
        ResultSet rs = null;
        List<PlayerVariables> playerVariables = new ArrayList<>();
        try
        {
            con = DatabaseFactory.getInstance().getConnection();
            offline = con.prepareStatement("SELECT * FROM character_variables WHERE obj_id = ?");
            offline.setInt(1, playerId);
            rs = offline.executeQuery();
            while(rs.next()) {
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = Strings.stripSlashes(rs.getString("value"));
                long expire_time = rs.getLong("expire_time");
                playerVariables.add(new PlayerVariables(playerId, type, name, value, expire_time));
            }

        }
        catch(Exception e)
        {
            _log.error("", e);
        }
        finally
        {
            DbUtils.closeQuietly(con, offline, rs);
        }
        return playerVariables;
    }

}
