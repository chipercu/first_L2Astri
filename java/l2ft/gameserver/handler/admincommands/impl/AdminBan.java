package l2ft.gameserver.handler.admincommands.impl;

import java.sql.SQLException;
import java.util.Collection;
import java.util.StringTokenizer;

import Strix_decopile.Utils.BannedHWIDInfo;
import Strix_decopile.managers.ClientBanManager;
import Strix_decopile.logging.StrixLog;
import l2ft.gameserver.Announcements;
import l2ft.gameserver.Config;
import l2ft.gameserver.handler.admincommands.IAdminCommandHandler;
import l2ft.gameserver.instancemanager.ReflectionManager;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.World;
import l2ft.gameserver.model.items.ManufactureItem;
import l2ft.gameserver.model.items.TradeItem;
import l2ft.gameserver.network.authcomm.AuthServerCommunication;
import l2ft.gameserver.network.authcomm.gspackets.ChangeAccessLevel;
import l2ft.gameserver.network.l2.GameClient;
import l2ft.gameserver.network.l2.components.ChatType;
import l2ft.gameserver.network.l2.components.CustomMessage;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.utils.AdminFunctions;
import l2ft.gameserver.utils.AutoBan;
import l2ft.gameserver.utils.Location;
import l2ft.gameserver.utils.Log;

public class AdminBan implements IAdminCommandHandler {
    private static enum Commands {
        admin_ban,
        admin_unban,
        admin_cban,
        admin_chatban,
        admin_chatunban,
        admin_accban,
        admin_accunban,
        admin_trade_ban,
        admin_trade_unban,
        admin_jail,
        admin_unjail,
        admin_permaban,
        admin_banhwid,
        admin_ban_hwid,
        admin_unban_hwid,
        admin_unbanhwid
    }

    @Override
    public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar) {
        Commands command = (Commands) comm;

        StringTokenizer st = new StringTokenizer(fullString);

        if (activeChar.getPlayerAccess().CanTradeBanUnban)
            switch (command) {
                case admin_trade_ban:
                    return tradeBan(st, activeChar);
                case admin_trade_unban:
                    return tradeUnban(st, activeChar);
            }

        if (activeChar.getPlayerAccess().CanBan)
            switch (command) {

                case admin_ban:
                    ban(st, activeChar);
                    break;
                case admin_accban: {
                    st.nextToken();

                    int level = 0;
                    int banExpire = 0;

                    String account = st.nextToken();

                    if (st.hasMoreTokens())
                        banExpire = (int) (System.currentTimeMillis() / 1000L) + Integer.parseInt(st.nextToken()) * 60;
                    else
                        level = -100;

                    AuthServerCommunication.getInstance().sendPacket(new ChangeAccessLevel(account, level, banExpire));
                    GameClient client = AuthServerCommunication.getInstance().getAuthedClient(account);
                    if (client != null) {
                        Player player = client.getActiveChar();
                        if (player != null) {
                            player.kick();
                            activeChar.sendMessage("Player " + player.getName() + " kicked.");
                        }
                    }
                    break;
                }
                case admin_accunban: {
                    st.nextToken();
                    String account = st.nextToken();
                    AuthServerCommunication.getInstance().sendPacket(new ChangeAccessLevel(account, 0, 0));
                    break;
                }
                case admin_trade_ban:
                    return tradeBan(st, activeChar);
                case admin_trade_unban:
                    return tradeUnban(st, activeChar);
                case admin_chatban:
                    try {
                        st.nextToken();
                        String player = st.nextToken();
                        String period = st.nextToken();
                        String bmsg = "admin_chatban " + player + " " + period + " ";
                        String msg = fullString.substring(bmsg.length(), fullString.length());

                        if (AutoBan.ChatBan(player, Integer.parseInt(period), msg, activeChar.getName())) {
                            activeChar.sendMessage("You ban chat for " + player + ".");
                            Announcements.getInstance().announceToAll("Игроку " + player + " было запрещено общение на " + Integer.parseInt(period) + " минут(ы). Заблокировал: " + activeChar.getName());
                        } else
                            activeChar.sendMessage("Can't find char " + player + ".");
                    } catch (Exception e) {
                        activeChar.sendMessage("Command syntax: //chatban char_name period reason");
                    }
                    break;
                case admin_chatunban:
                    try {
                        st.nextToken();
                        String player = st.nextToken();

                        if (AutoBan.ChatUnBan(player, activeChar.getName()))
                            activeChar.sendMessage("You unban chat for " + player + ".");
                        else
                            activeChar.sendMessage("Can't find char " + player + ".");
                    } catch (Exception e) {
                        activeChar.sendMessage("Command syntax: //chatunban char_name");
                    }
                    break;
                case admin_jail:
                    try {
                        st.nextToken();
                        String player = st.nextToken();
                        String period = st.nextToken();
                        String reason = st.nextToken();

                        Player target = World.getPlayer(player);

                        if (target != null) {
                            target.setVar("jailedFrom", target.getX() + ";" + target.getY() + ";" + target.getZ() + ";" + target.getReflectionId(), -1);
                            target.setVar("jailed", period, -1);
                            target.startUnjailTask(target, Integer.parseInt(period));
                            target.teleToLocation(Location.findPointToStay(target, AdminFunctions.JAIL_SPAWN, 50, 200), ReflectionManager.JAIL);
                            if (activeChar.isInStoreMode())
                                activeChar.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
                            target.sitDown(null);
                            target.block();
                            target.setNoChannel(-1);
                            target.sendMessage("You moved to jail, time to escape - " + period + " minutes, reason - " + reason + " .");
                            activeChar.sendMessage("You jailed " + player + ".");
                        } else
                            activeChar.sendMessage("Can't find char " + player + ".");
                    } catch (Exception e) {
                        activeChar.sendMessage("Command syntax: //jail char_name period reason");
                    }
                    break;
                case admin_unjail:
                    try {
                        st.nextToken();
                        String player = st.nextToken();

                        Player target = World.getPlayer(player);

                        if (target != null && target.getVar("jailed") != null) {
                            String[] re = target.getVar("jailedFrom").split(";");
                            target.teleToLocation(Integer.parseInt(re[0]), Integer.parseInt(re[1]), Integer.parseInt(re[2]));
                            target.setReflection(re.length > 3 ? Integer.parseInt(re[3]) : 0);
                            target.stopUnjailTask();
                            target.unsetVar("jailedFrom");
                            target.unsetVar("jailed");
                            target.unblock();
                            target.standUp();
                            activeChar.sendMessage("You unjailed " + player + ".");
                        } else
                            activeChar.sendMessage("Can't find char " + player + ".");
                    } catch (Exception e) {
                        activeChar.sendMessage("Command syntax: //unjail char_name");
                    }
                    break;
                case admin_cban:
                    activeChar.sendPacket(new NpcHtmlMessage(5).setFile("admin/cban.htm"));
                    break;
                case admin_permaban:
                    if (activeChar.getTarget() == null || !activeChar.getTarget().isPlayer()) {
                        Functions.sendDebugMessage(activeChar, "Target should be set and be a player instance");
                        return false;
                    }
                    Player banned = activeChar.getTarget().getPlayer();
                    String banaccount = banned.getAccountName();
                    AuthServerCommunication.getInstance().sendPacket(new ChangeAccessLevel(banaccount, -100, 0));
                    if (banned.isInOfflineMode())
                        banned.setOfflineMode(false);
                    banned.kick();
                    Functions.sendDebugMessage(activeChar, "Player account " + banaccount + " is banned, player " + banned.getName() + " kicked.");
                    break;
                case admin_banhwid:
//                case admin_ban_hwid:
//                    try {
//                        if (wordList[1].equals("$target"))
//                            if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
//                                wordList[1] = activeChar.getTarget().getName();
//                        activeChar.sendMessage(HWID.handleBanHWID(wordList));
//                    } catch (final Exception e) {
//                        activeChar.sendMessage("USAGE: //banhwid char_name|hwid [kick:true|false] [reason]");
//                    }
//                    break;
                case admin_unbanhwid:
//                case admin_unban_hwid:
//                    if (wordList.length < 2) {
//                        activeChar.sendMessage("USAGE: //unbanhwid hwid");
//                        return false;
//                    }
//                    if (wordList[1].length() != 32) {
//                        activeChar.sendMessage(wordList[1] + " is not like HWID");
//                        return false;
//                    }
//                    HWID.UnbanHWID(wordList[1]);
//                    activeChar.sendMessage("HWID " + wordList[1] + " unbanned");
//                    break;
                case admin_ban_hwid:
                    st.nextToken();
                    Player targetPlayer = null;
                    if (activeChar.getTarget() != null) {
                        targetPlayer = activeChar.getTarget().getPlayer();
                    } else {
                        final String playeraName = st.nextToken();
                        targetPlayer = World.getPlayer(playeraName);
                    }

                    if (targetPlayer != null) {
                        try {
                            final Long time = Long.parseLong(st.nextToken());
                            final String reason = st.nextToken();
                            final BannedHWIDInfo bhi = new BannedHWIDInfo(targetPlayer.getNetConnection().getStrixClientData().getClientHWID(), (System.currentTimeMillis() + time * 60 * 1000), reason, activeChar.getName());
                            ClientBanManager.getInstance().tryToStoreBan(bhi);
                            final String bannedOut = "Player [Name:{" + targetPlayer.getName() + "}HWID:{" + targetPlayer.getNetConnection().getStrixClientData().getClientHWID() + "}] banned on [" + time + "] minutes from [" + reason + "] reason.";
                            activeChar.sendMessage(bannedOut);
                            StrixLog.audit(bannedOut);
                            targetPlayer.sendMessage("You banned on [" + time + "] minutes. Reason: " + reason);
                            targetPlayer.kick();
                        } catch (final Exception e) {
                            if (e instanceof SQLException) {
                                activeChar.sendMessage("Unable to store ban in database. Please check Strix-Platform error log!");
                                StrixLog.error("Exception on GM trying store ban. Exception: " + e.getLocalizedMessage());
                            } else {
                                activeChar.sendMessage("Command syntax: //ban_hwid PLAYER_NAME(or target) TIME(in minutes) REASON(255 max)");
                            }
                            break;
                        }
                    } else {
                        activeChar.sendMessage("Unable to find this player.");
                    }
                    break;
                case admin_unban_hwid:
                    st.nextToken();
                    final String playeraHWID = st.nextToken();

                    if (playeraHWID != null && playeraHWID.length() == 32) {
                        try {
                            ClientBanManager.getInstance().tryToDeleteBan(playeraHWID);
                            activeChar.sendMessage("Player unbaned and delete from database.");
                        } catch (final Exception e) {
                            if (e instanceof SQLException) {
                                activeChar.sendMessage("Unable to delete ban from database. Please check Strix-Platform error log!");
                                StrixLog.error("Exception on GM trying delete ban. Exception: " + e.getLocalizedMessage());
                            }
                            break;
                        }
                    } else {
                        activeChar.sendMessage("Command syntax: //unban_hwid HWID_STRING(size 32)");
                    }
                    break;
            }

        return true;
    }

    private boolean tradeBan(StringTokenizer st, Player activeChar) {
        if (activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
            return false;
        st.nextToken();
        Player targ = (Player) activeChar.getTarget();
        long days = -1;
        long time = -1;
        if (st.hasMoreTokens()) {
            days = Long.parseLong(st.nextToken());
            time = days * 24 * 60 * 60 * 1000L + System.currentTimeMillis();
        }
        targ.setVar("tradeBan", String.valueOf(time), -1);
        String msg = activeChar.getName() + " заблокировал торговлю персонажу " + targ.getName() + (days == -1 ? " на бессрочный период." : " на " + days + " дней.");

        Log.add(targ.getName() + ":" + days + tradeToString(targ, targ.getPrivateStoreType()), "tradeBan", activeChar);

        if (targ.isInOfflineMode()) {
            targ.setOfflineMode(false);
            targ.kick();
        } else if (targ.isInStoreMode()) {
            targ.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
            targ.standUp();
            targ.broadcastCharInfo();
            targ.getBuyList().clear();
        }

        if (Config.BANCHAT_ANNOUNCE_FOR_ALL_WORLD)
            Announcements.getInstance().announceToAll(msg);
        else
            Announcements.shout(activeChar, msg, ChatType.CRITICAL_ANNOUNCE);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static String tradeToString(Player targ, int trade) {
        String ret;
        Collection list;
        switch (trade) {
            case Player.STORE_PRIVATE_BUY:
                list = targ.getBuyList();
                if (list == null || list.isEmpty())
                    return "";
                ret = ":buy:";
                for (TradeItem i : (Collection<TradeItem>) list)
                    ret += i.getItemId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
                return ret;
            case Player.STORE_PRIVATE_SELL:
            case Player.STORE_PRIVATE_SELL_PACKAGE:
                list = targ.getSellList();
                if (list == null || list.isEmpty())
                    return "";
                ret = ":sell:";
                for (TradeItem i : (Collection<TradeItem>) list)
                    ret += i.getItemId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
                return ret;
            case Player.STORE_PRIVATE_MANUFACTURE:
                list = targ.getCreateList();
                if (list == null || list.isEmpty())
                    return "";
                ret = ":mf:";
                for (ManufactureItem i : (Collection<ManufactureItem>) list)
                    ret += i.getRecipeId() + ";" + i.getCost() + ":";
                return ret;
            default:
                return "";
        }
    }

    private boolean tradeUnban(StringTokenizer st, Player activeChar) {
        if (activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
            return false;
        Player targ = (Player) activeChar.getTarget();

        targ.unsetVar("tradeBan");

        if (Config.BANCHAT_ANNOUNCE_FOR_ALL_WORLD)
            Announcements.getInstance().announceToAll(activeChar + " разблокировал торговлю персонажу " + targ + ".");
        else
            Announcements.shout(activeChar, activeChar + " разблокировал торговлю персонажу " + targ + ".", ChatType.CRITICAL_ANNOUNCE);

        Log.add(activeChar + " разблокировал торговлю персонажу " + targ + ".", "tradeBan", activeChar);
        return true;
    }

    private boolean ban(StringTokenizer st, Player activeChar) {
        try {
            st.nextToken();

            String player = st.nextToken();

            int time = 0;
            String msg = "";

            if (st.hasMoreTokens())
                time = Integer.parseInt(st.nextToken());

            if (st.hasMoreTokens()) {
                msg = "admin_ban " + player + " " + time + " ";
                while (st.hasMoreTokens())
                    msg += st.nextToken() + " ";
                msg.trim();
            }

            Player plyr = World.getPlayer(player);
            if (plyr != null) {
                plyr.sendMessage(new CustomMessage("admincommandhandlers.YoureBannedByGM", plyr));
                plyr.setAccessLevel(-100);
                AutoBan.Banned(plyr, time, msg, activeChar.getName());
                plyr.kick();
                activeChar.sendMessage("You banned " + plyr.getName());
            } else if (AutoBan.Banned(player, -100, time, msg, activeChar.getName()))
                activeChar.sendMessage("You banned " + player);
            else
                activeChar.sendMessage("Can't find char: " + player);
        } catch (Exception e) {
            activeChar.sendMessage("Command syntax: //ban char_name days reason");
        }
        return true;
    }

    @Override
    public Enum[] getAdminCommandEnum() {
        return Commands.values();
    }
}