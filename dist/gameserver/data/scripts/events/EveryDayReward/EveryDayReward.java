package events.EveryDayReward;

import fuzzy.Database_Util.DB_Util.DB_Player;
import fuzzy.Database_Util.model.PlayerVariables;
import fuzzy.Html_Constructor.tags.Button;
import fuzzy.Html_Constructor.tags.Img;
import fuzzy.Html_Constructor.tags.Table;
import fuzzy.Html_Constructor.tags.parameters.Parameters;
import l2ft.commons.configuration.ExProperties;
import l2ft.commons.dbutils.DbUtils;
import l2ft.commons.time.cron.SchedulingPattern;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.ItemHolder;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.actor.listener.CharListenerList;
import l2ft.gameserver.network.l2.components.ChatType;
import l2ft.gameserver.network.l2.s2c.ExShowScreenMessage;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.network.l2.s2c.Say2;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.scripts.ScriptFile;
import l2ft.gameserver.templates.item.ItemTemplate;
import l2ft.gameserver.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

import static fuzzy.Database_Util.DB_Util.DB_Player.*;

public class EveryDayReward extends Functions implements ScriptFile, OnPlayerEnterListener, Parameters {

    private static final Logger _log = LoggerFactory.getLogger(EveryDayReward.class);
    private static final String configPath = "config/events/EveryDayReward.ini";
    private static boolean isActive;
    private static final String EVENT_NAME = "EveryDayReward";
    private static final String REWARD_RECEIVED = "EveryDayReward_Received";
    private static final String CURRENT_DAY_VAR = "EveryDayReward_CurrentDay";
    private static boolean checkIP;
    private static boolean checkHWID;
    private static boolean checkAccount;

    private static final List<String> itemFromDays = new ArrayList<>();
    private static Map<Integer, Integer> rewardListItems = new HashMap<Integer, Integer>();
    private static String rewardInterval;

    enum RewardStatus{
        COMPLETE, RECEIVED, ACTIVE, NEXTED

    }


    @Override
    public void onPlayerEnter(Player player) {
        if (!IsActive(EVENT_NAME)){
            if (player.isGM()){
                Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Награда за вход отключена");
                player.sendPacket(cs);
            }
            return;
        }
        if (player.getVar(CURRENT_DAY_VAR) == null){
            player.setVar(CURRENT_DAY_VAR, 1, -1);
        }else {
            if (Integer.parseInt(player.getVar(CURRENT_DAY_VAR)) > itemFromDays.size()){
                player.setVar(CURRENT_DAY_VAR, 1, -1);
            }
        }
        showPage(player);
    }
    public void getPage(){
        showPage(getSelf());
    }

    @Override
    public void onLoad() {
        CharListenerList.addGlobal(this);
        ExProperties everyDayReward = Config.load(configPath);
        for (String prop : everyDayReward.getProperty("rewardListItems", "57:10").split(",")) {
            final String[] split = prop.split(":");
            final int itemId = Integer.parseInt(split[0]);
            final int itemCount = Integer.parseInt(split[1]);
            rewardListItems.put(itemId, itemCount);
        }
        for (int i = 1; i < everyDayReward.size(); i++) {
            if (everyDayReward.getProperty("rewardDay" + i) != null){
                String key = "rewardDay" + i;
                final String itemIdAndCount = everyDayReward.getProperty(key, "57:1");
                itemFromDays.add(itemIdAndCount);
            }
        }
        checkIP = everyDayReward.getProperty("checkIP", false);
        checkHWID = everyDayReward.getProperty("checkHWID", false);
        checkAccount = everyDayReward.getProperty("checkAccount", false);

        isActive = everyDayReward.getProperty("isActive", false);
        rewardInterval = everyDayReward.getProperty("rewardInterval", "*/5 * * * *");

        if (IsActive(EVENT_NAME)) {
            _log.info("Loaded Event: Every Day Reward [state: activated] " +
                    "\n ограничение по IP - " + checkIP +
                    "\n ограничение по HWID - " + checkHWID +
                    "\n ограничение по Аккаунту - " + checkAccount
            );
        } else
            _log.info("Loaded Event: Every Day Reward [state: deactivated]");
    }

    private void sendInfoMessage(Player player, String text){
        int time = 3000 + text.length() * 100; // 3 секунды + 100мс на символ
        ExShowScreenMessage sm = new ExShowScreenMessage(text, time, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, text.length() < 64);
        player.sendPacket(sm);
    }



    public void getReward(String[] param){
        Player player = getSelf();
        if (param == null || param.length < 1){
            showPage(player);
            return;
        }
        if (checkAccount){
            final List<Integer> players = loadPlayerIdByAccountName(player);
            for (Integer playerId: players){
                for (PlayerVariables variables: loadVariables(playerId)){
                    if (variables.getName().equals(REWARD_RECEIVED)){
                        player.sendMessage("С данного аккаунта ежедневная награда уже получена");
                        sendInfoMessage(player, "С данного аккаунта ежедневная награда уже получена");
                        return;
                    }
                }
            }
        }

        //реализовать ограничение по аккаунту, по ИП;
        if (checkIP) {
            for (Player p : GameObjectsStorage.getAllPlayers()) {
                if (p.getIP().equals(player.getIP()) && p.getObjectId() != player.getObjectId()){
                    p.sendMessage("С данного IP адреса ежедневная награда уже получена");
                    sendInfoMessage(player,"С данного IP адреса ежедневная награда уже получена");
                    return;
                }
            }
        }
//        if (checkHWID) {
//            for (Player p : GameObjectsStorage.getAllPlayers()) {
//                if (p.getHWID().equals(player.getHWID()) && p.getObjectId() != player.getObjectId()){
//                    player.sendMessage("С данного компьютера ежедневная награда уже получена");
//                    sendInfoMessage(player, "С данного компьютера ежедневная награда уже получена");
//                    return;
//                }
//            }
//        }
//        if (checkAccount) {
//            for (Player p : GameObjectsStorage.getAllPlayers()) {
//                if (p.getAccountName().equals(player.getAccountName()) && p.getObjectId() != player.getObjectId()){
//                    player.sendMessage("С данного аккаунта ежедневная награда уже получена");
//                    sendInfoMessage(player, "С данного аккаунта ежедневная награда уже получена");
//                    return;
//                }
//            }
//        }

        final int playerLastRewardDay = Integer.parseInt(player.getVar(CURRENT_DAY_VAR));

        String var = player.getVar(REWARD_RECEIVED);
        if (var != null) {
            Calendar calendar = Calendar.getInstance();
            long next = Long.parseLong(var);
            calendar.setTimeInMillis(next);
            sendInfoMessage(getSelf(), "Вы сегодня уже получили Ежедневную награду!");
        } else {

            if (playerLastRewardDay >= itemFromDays.size()){
                player.setVar(CURRENT_DAY_VAR,1, -1);
            }else {
                player.setVar(CURRENT_DAY_VAR, playerLastRewardDay + 1, -1);
            }
            sendInfoMessage(player, "Вы получили ежедневную награду за вход в игру!");
            SchedulingPattern reset = new SchedulingPattern(rewardInterval);
            player.setVar(REWARD_RECEIVED, "true", reset.next(System.currentTimeMillis()));

            final String[] split = itemFromDays.get(playerLastRewardDay - 1).split(":");
            Functions.addItem(player, Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }
        showPage(player);
    }

    public void showPage(Player player){
        NpcHtmlMessage msg = new NpcHtmlMessage(player, null);
        String HTML = "<title>Ежедневная награда</title>";

        String title = "<font color=\"AAAAAA\">Ежедневная награда</font>";

        Table table = new Table(itemFromDays.size() + 1, 1).setParams(border(0), cellpadding(4), cellspacing(4));
        table.row(0).col(0).setParams(height(40), width(280)).insert(title);

        for (int i = 0; i < itemFromDays.size(); i++) {
            final String[] split = itemFromDays.get(i).split(":");
            final int itemId = Integer.parseInt(split[0]);
            final int itemCount = Integer.parseInt(split[1]);
            ItemTemplate item = ItemHolder.getInstance().getTemplate(itemId);
            if (item != null){
                int day  = Integer.parseInt(player.getVar(CURRENT_DAY_VAR)) - 1;
                final String received = player.getVar(REWARD_RECEIVED);
                RewardStatus status;
                if (i == day){
                    if (received != null){
                        status = RewardStatus.NEXTED;
                    }else {
                        status = RewardStatus.ACTIVE;
                    }
                } else if (i < day) {
                    status = RewardStatus.COMPLETE;
                } else {
                    status = RewardStatus.NEXTED;
                }
                table.row(i).col(0).setParams(height(40), width(280)).insert(getItemsTable(item,itemCount, status, day));
            }
        }
        msg.setHtml(HTML + table.build());
        player.sendPacket(msg);
    }

    private String getItemsTable(ItemTemplate item, int count, RewardStatus rewardStatus, int day){
        Button getButton = new Button("Получить", action("bypass -h scripts_events.EveryDayReward.EveryDayReward:getReward " + day + 1), 98, 32, "L2UI_ct1.button_df","L2UI_ct1.button_df");
        Table table = new Table(1, 3).setParams(border(0), background("l2ui_ct1.Windows_DF_TooltipBG"));
        table.row(0).col(0).setParams(height(38), width(32)).insert(new Img(item.getIcon(), 32, 32).build());
        table.row(0).col(1).setParams(width(150)).insert(item.getName() + " - " + count + "шт.");
        switch (rewardStatus){
            case COMPLETE:{
                table.row(0).col(2).setParams(width(98)).insert("<font color=D70000>Получено</font>");
                break;
            }
            case ACTIVE:{
                final String button = getButton.build();
                table.row(0).col(2).setParams(width(98)).insert(button);
                break;
            }
            case NEXTED:{
                table.row(0).col(2).setParams(width(98)).insert("<font color=D70000>Следующее</font>");
                break;
            }
        }
        return table.build();
    }


    /**
     * Запускает эвент
     */
    public void startEvent() {
        Player player = getSelf();
        if(!player.getPlayerAccess().IsEventGm)
            return;

        if(SetActive("EveryDayReward", true)) {
            sendInfoMessage(player, "Event: EveryDayReward started.");
        } else{
            sendInfoMessage(player, "Event: EveryDayReward already started.");
        }


        isActive = true;
        show("admin/events/events.htm", player);
    }

    /**
     * Останавливает эвент
     */
    public void stopEvent()
    {
        Player player = getSelf();
        if(!player.getPlayerAccess().IsEventGm)
            return;
        if(SetActive("EveryDayReward", false)) {
            sendInfoMessage(player, "Event: EveryDayReward stopped.");
        } else{
            sendInfoMessage(player, "Event: EveryDayReward not started.");
        }

        isActive = false;
        show("admin/events/events.htm", player);
    }


    @Override
    public void onReload() {

    }

    @Override
    public void onShutdown() {

    }
}
