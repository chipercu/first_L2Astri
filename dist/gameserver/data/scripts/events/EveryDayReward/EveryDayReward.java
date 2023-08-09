package events.EveryDayReward;

import fuzzy.Html_Constructor.tags.Button;
import fuzzy.Html_Constructor.tags.Img;
import fuzzy.Html_Constructor.tags.Table;
import fuzzy.Html_Constructor.tags.parameters.Parameters;
import l2ft.commons.configuration.ExProperties;
import l2ft.gameserver.Announcements;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.ItemHolder;
import l2ft.gameserver.instancemanager.ServerVariables;
import l2ft.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.actor.listener.CharListenerList;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.network.l2.components.ChatType;
import l2ft.gameserver.network.l2.s2c.ExShowScreenMessage;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.network.l2.s2c.Say2;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.scripts.ScriptFile;
import l2ft.gameserver.templates.item.ItemTemplate;
import l2ft.gameserver.utils.HWID;
import l2ft.gameserver.utils.ItemFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class EveryDayReward extends Functions implements ScriptFile, OnPlayerEnterListener, Parameters {

    private static final Logger _log = LoggerFactory.getLogger(EveryDayReward.class);
    private static final String configPath = "config/events/EveryDayReward.ini";
    private static boolean isActive = false;
    private static final String EVENT_NAME = "EveryDayReward";
    private static final String CURRENT_DAY_VAR = "EveryDayReward_CurrentDay";
    private static boolean checkIP = false;
    private static boolean checkHWID = false;
    private static boolean checkAccount = false;

    private static final Map<Integer, Integer> itemFromDays = new HashMap<>();
    private static Map<Integer, Integer> rewardListItems = new HashMap<Integer, Integer>();
    private static int rewardInterval;

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
        showPage();
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
                final String[] split = everyDayReward.getProperty("rewardDay" + i, "57:1").split(":");
                itemFromDays.put(Integer.parseInt(split[0]),Integer.parseInt(split[1]));
            }
        }
        checkIP = everyDayReward.getProperty("checkIP", false);
        checkHWID = everyDayReward.getProperty("checkHWID", false);
        isActive = everyDayReward.getProperty("isActive", true);
        checkAccount = everyDayReward.getProperty("checkAccount", true);
        rewardInterval = everyDayReward.getProperty("rewardInterval", Integer.MAX_VALUE);

        if (IsActive(EVENT_NAME)) {
            _log.info("Loaded Event: Every Day Reward [state: activated]");
        } else
            _log.info("Loaded Event: Every Day Reward [state: deactivated]");
    }

    private void sendInfoMessage(Player player, String text){
        int time = 3000 + text.length() * 100; // 3 секунды + 100мс на символ
        ExShowScreenMessage sm = new ExShowScreenMessage(text, time, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, text.length() < 64);
        player.sendPacket(sm);
    }

    public void getReward(String[] param){
        if (param == null || param.length < 1){
            showPage();
            return;
        }
        //реализовать ограничение по аккаунту, по ИП;
        if (checkIP) {
            for (Player player : GameObjectsStorage.getAllPlayers()) {
                if (player.getIP().equals(getSelf().getIP()) && player.getObjectId() != getSelf().getObjectId()){
                    player.sendMessage("С данного IP адреса ежедневная награда уже получена");
                    sendInfoMessage(player,"С данного IP адреса ежедневная награда уже получена");
                    return;
                }
            }
        }
        if (checkHWID) {
            for (Player player : GameObjectsStorage.getAllPlayers()) {
                if (player.getHWID().equals(getSelf().getHWID()) && player.getObjectId() != getSelf().getObjectId()){
                    player.sendMessage("С данного компьютера ежедневная награда уже получена");
                    sendInfoMessage(player, "С данного компьютера ежедневная награда уже получена");
                    return;
                }
            }
        }
        if (checkAccount) {
            for (Player player : GameObjectsStorage.getAllPlayers()) {
                if (player.getAccountName().equals(getSelf().getAccountName()) && player.getObjectId() != getSelf().getObjectId()){
                    player.sendMessage("С данного аккаунта ежедневная награда уже получена");
                    sendInfoMessage(player, "С данного аккаунта ежедневная награда уже получена");
                    return;
                }
            }
        }



        final int playerLastRewardDay = Integer.parseInt(getSelf().getVar(CURRENT_DAY_VAR));
        final int requestDay = Integer.parseInt(param[0]);

        if (playerLastRewardDay == requestDay){
            sendInfoMessage(getSelf(),"Вы уже получили сегодня ежедневную награду!" );
//            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Вы уже получили сегодня ежедневную награду!");
//            getSelf().sendPacket(cs);
        }
        String var = getSelf().getVar("nextEveryDayReward");
        if (var != null && Long.parseLong(var) > System.currentTimeMillis()) {
            Calendar calendar = Calendar.getInstance();
            long next = Long.parseLong(var);
            calendar.setTimeInMillis(next);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            sendInfoMessage(getSelf(), "Ежедневную награду получите в " + dateFormat.format(calendar.getTime()));
        } else {
            getSelf().setVar("nextEveryDayReward", String.valueOf(System.currentTimeMillis() + rewardInterval * 60 * 1000L), -1);
            getSelf().setVar(CURRENT_DAY_VAR, requestDay, -1);
            sendInfoMessage(getSelf(), "Вы получили ежедневную награду за вход в игру!");

            for (Map.Entry<Integer, Integer> reward: rewardListItems.entrySet()){
                Functions.addItem(getSelf(), reward.getKey(), reward.getValue());
            }
        }
        showPage();
    }

    public void showPage(){
        NpcHtmlMessage msg = new NpcHtmlMessage(getSelf(), getNpc());
        String title = "<font color=\"AAAAAA\">Ежедневная награда</font>";

        Table table = new Table(itemFromDays.size() + 1, 1).setParams(border(1)).setParams(cellpadding(1));
        table.row(0).col(0).setParams(height(32), width(280)).insert(title);

        final List<Integer> collect = new ArrayList<Integer>(itemFromDays.keySet());
        for (int i = 0; i < itemFromDays.size(); i++) {
            ItemTemplate item = ItemHolder.getInstance().getTemplate(collect.get(i));
            if (item != null){
                int day  = Integer.parseInt(getSelf().getVar(CURRENT_DAY_VAR));
                if (day == i){
                    if (Long.parseLong(getSelf().getVar("nextEveryDayReward")) < System.currentTimeMillis()){
                        table.row(i).col(0).setParams(height(32), width(280)).insert(getItemsTable(item, RewardStatus.COMPLETE, day));
                    }else {
                        table.row(i).col(0).setParams(height(32), width(280)).insert(getItemsTable(item, RewardStatus.ACTIVE, day));
                    }
                }else if (day < i){
                    table.row(i).col(0).setParams(height(32), width(280)).insert(getItemsTable(item, RewardStatus.COMPLETE, day));
                }else {
                    table.row(i).col(0).setParams(height(32), width(280)).insert(getItemsTable(item, RewardStatus.NEXTED, day));
                }
            }
        }
        msg.setHtml(table.build());
        getSelf().sendPacket(msg);
    }

    private String getItemsTable(ItemTemplate item, RewardStatus rewardStatus, int day){
        Button getButton = new Button("Получить", action("scripts_events.EveryDayReward.EveryDayReward:getReward " + day), 32, 96, "L2UI_ct1.button_df","L2UI_ct1.button_df");
        Table table = new Table(1, 3).setParams(border(1));
        table.row(0).col(0).setParams(height(32), width(32)).insert(new Img(item.getIcon(), 32, 32).build());
        table.row(0).col(1).setParams(width(150)).insert(item.getName());
        switch (rewardStatus){
            case COMPLETE:{
                table.row(0).col(2).setParams(width(98)).insert("<font color=D70000>Получено</font>");
                break;
            }
            case ACTIVE:{
                table.row(0).col(2).setParams(width(98)).insert(getButton.build());
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
//            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward started.");
//            player.sendPacket(cs);
        } else{
            sendInfoMessage(player, "Event: EveryDayReward already started.");
//            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward already started.");
//            player.sendPacket(cs);
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
//            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward stopped.");
//            player.sendPacket(cs);
            sendInfoMessage(player, "Event: EveryDayReward stopped.");
        } else{
            sendInfoMessage(player, "Event: EveryDayReward not started.");
//            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward not started.");
//            player.sendPacket(cs);
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
