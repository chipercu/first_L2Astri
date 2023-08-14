package handler.admincommands;

import ai.adept.AdeptGiran;
import fuzzy.Database_Util.DB_Util.DB_Player;
import l2ft.commons.dbutils.DbUtils;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.ResidenceHolder;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.database.mysql;
import l2ft.gameserver.handler.admincommands.AdminCommandHandler;
import l2ft.gameserver.handler.admincommands.IAdminCommandHandler;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.entity.residence.Residence;
import l2ft.gameserver.model.pledge.Clan;
import l2ft.gameserver.network.l2.s2c.ExShowScreenMessage;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.scripts.ScriptFile;
import l2ft.gameserver.utils.HtmlUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static l2ft.loginserver.network.gameservercon.gspackets.ChangeAccessLevel._log;

public class AdminFakePlayers implements IAdminCommandHandler, ScriptFile {
    private enum Commands {
        admin_spawn_old_players,
        admin_spawn_fake,
        admin_set_fake,
        admin_set_old_fake,
        admin_unset_old_fake
    }
    private void sendInfoMessage(Player player, String text){
        int time = 3000 + text.length() * 100; // 3 секунды + 100мс на символ
        ExShowScreenMessage sm = new ExShowScreenMessage(text, time, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, text.length() < 64);
        player.sendPacket(sm);
    }


    public static List<Integer> getOfflinePlayers() {
        List<Integer> accountList = new ArrayList<>();
        Connection con = null;
        PreparedStatement offline = null;
        ResultSet rs = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            offline = con.prepareStatement("SELECT * FROM characters WHERE online = ?");
            offline.setInt(1, 0);
            rs = offline.executeQuery();
            while (rs.next()) {
                int playerId = rs.getInt("obj_Id");
                accountList.add(playerId);
            }
        } catch (Exception e) {
            _log.error("", e);
        } finally {
            DbUtils.closeQuietly(con, offline, rs);
        }
        return accountList;
    }

    @Override
    public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar) {
        Commands command = (Commands) comm;

        if (!activeChar.getPlayerAccess().IsGM)
            return false;

        NpcHtmlMessage msg;
        switch (command) {
            case admin_unset_old_fake:
                for (Integer objectId: getOfflinePlayers()){
                    mysql.set("DELETE FROM `character_variables` WHERE `obj_id`=? AND `type`='user-var' AND `name`=? LIMIT 1", objectId, "bot");
                }
                break;
            case admin_set_old_fake:
                for (Integer objectId: getOfflinePlayers()){
                    mysql.set("REPLACE INTO character_variables (obj_id, type, name, value, expire_time) VALUES (?,'user-var',?,?,?)", objectId, "bot", "false", -1);
                }
                break;
            case admin_set_fake:
                final GameObject target = activeChar.getTarget();
                if (target.isPlayer()){
                    if (target.getPlayer().getVar("bot") != null){
                        final boolean bot = Boolean.parseBoolean(target.getPlayer().getVar("bot"));
                        if (!bot){
                            target.getPlayer().setVar("bot", "true", -1);
                            sendInfoMessage(activeChar, "Метка о том что игрок " + target.getName() + " факе - установлена");
                        }else {
                            target.getPlayer().setVar("bot", "false", -1);
                            sendInfoMessage(activeChar, "Метка о том что игрок " + target.getName() + " факе - убрана");
                        }
                    }else {
                        target.getPlayer().setVar("bot", "true", -1);
                        sendInfoMessage(activeChar, "Метка о том что игрок " + target.getName() + " факе - установлена");
                    }
                }else {
                    sendInfoMessage(activeChar, "Цель " + target.getName() + " не является игроком");
                }
                break;
            case admin_spawn_fake:
                spawnFake();
                break;
            case admin_spawn_old_players:
                int count = 0;
                for (Integer playerId: getOfflinePlayers()){
                    if (wordList[0] == null){
                        sendInfoMessage(activeChar, "Не введено количество игроков - пример //spawn_old_players 50");
                        break;
                    }

                    if (count > Integer.parseInt(wordList[1])){
                        continue;
                    }
                    Player player = Player.restore(playerId);
                    if (player.getVar("bot") != null){
                        continue;
                    }
                    if (player.getAccessLevel() < 0){
                        continue;
                    }

                    final long last = System.currentTimeMillis() - player.getLastAccess();
                    if ( last > 24 * 60 * 60 * 1000L){
                        player.setIsPhantom(true);
                        if (player == null){
                            continue;
                        }
//                        player.setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
                        player.setOfflineMode(false);
                        player.setIsOnline(true);
                        player.updateOnlineStatus();
                        player.spawnMe();
                        if (player.getClan() != null && player.getClan().getAnyMember(player.getObjectId()) != null){
                            player.getClan().getAnyMember(player.getObjectId()).setPlayerInstance(player, false);
                        }
                        count++;
                    }
                }
                sendInfoMessage(activeChar, "Загружены в игру " + count + " реальных игроков");
                break;
        }
        return true;
    }


    public void spawnFake(){

        int count = 0;
        for (Integer playerId: getOfflinePlayers()){
            Player player = Player.restore(playerId);
            if (player.getVar("bot") != null){
                final boolean isBot = Boolean.parseBoolean(player.getVar("bot"));
                if (isBot){
                    player.setIsPhantom(true);
                    if (player == null){
                        continue;
                    }
//                    player.setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
                    player.setOfflineMode(false);
                    player.setIsOnline(true);
                    player.updateOnlineStatus();
                    player.spawnMe();
                    if (player.getClan() != null && player.getClan().getAnyMember(player.getObjectId()) != null){
                        player.getClan().getAnyMember(player.getObjectId()).setPlayerInstance(player, false);
                    }
                    count++;
                }
            }
        }
        _log.info("Restored " + count + " fake players");




//        Connection con = null;
//        PreparedStatement statement = null;
//        ResultSet rset = null;
//        try {
//            con = DatabaseFactory.getInstance().getConnection();
//
//            statement = con.prepareStatement("SELECT obj_id, value FROM character_variables WHERE name = 'bot'");
//            rset = statement.executeQuery();
//
//            int objectId;
//            boolean isFake;
////            int expireTimeSecs;
//            Player p;
//
//            while (rset.next()) {
//
//
//                objectId = rset.getInt("obj_id");
//                isFake = rset.getBoolean("value");
//                if (isFake){
//                    p = Player.restore(objectId);
//
//
//
//
//
//                    p.setIsPhantom(true);
//                    if (p == null){
//                        continue;
//                    }
//                    p.setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
//                    p.setOfflineMode(false);
//                    p.setIsOnline(true);
//                    p.updateOnlineStatus();
//                    p.spawnMe();
//                    if (p.getClan() != null && p.getClan().getAnyMember(p.getObjectId()) != null){
//                        p.getClan().getAnyMember(p.getObjectId()).setPlayerInstance(p, false);
//                    }
//                    count++;
//                }
//            }
//        } catch (Exception e) {
//            _log.error("Error while restoring fake players!", e);
//        } finally {
//            DbUtils.closeQuietly(con, statement, rset);
//        }

    }

    @Override
    public Enum[] getAdminCommandEnum() {
        return Commands.values();
    }

    @Override
    public void onLoad() {
        AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
    }

    @Override
    public void onReload() {

    }

    @Override
    public void onShutdown() {

    }
}
