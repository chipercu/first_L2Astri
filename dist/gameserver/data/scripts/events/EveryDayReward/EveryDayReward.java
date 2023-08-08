package events.EveryDayReward;

import l2ft.commons.configuration.ExProperties;
import l2ft.gameserver.Announcements;
import l2ft.gameserver.Config;
import l2ft.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.actor.listener.CharListenerList;
import l2ft.gameserver.network.l2.components.ChatType;
import l2ft.gameserver.network.l2.s2c.Say2;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.scripts.ScriptFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EveryDayReward extends Functions implements ScriptFile, OnPlayerEnterListener {

    private static final Logger _log = LoggerFactory.getLogger(EveryDayReward.class);
    private static final String configPath = "config/events/EveryDayReward.ini";
    private static boolean isActive = false;
    private static final String EVENT_NAME = "EveryDayReward";

    private static Map<Integer, Integer> rewardListItems = new HashMap<Integer, Integer>();
    private static int rewardInterval;


    @Override
    public void onPlayerEnter(Player player) {
        player.sendMessage("евент шаг 1");

        if (!IsActive(EVENT_NAME)){
            if (player.isGM()){
                Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Награда за вход отключена");
                player.sendPacket(cs);
            }
            return;
        }

        String var = player.getVar("nextEveryDayReward");
        if (var != null && Long.parseLong(var) > System.currentTimeMillis()) {
            Calendar calendar = Calendar.getInstance();
            long next = Long.parseLong(var);
            calendar.setTimeInMillis(next);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            System.out.println(dateFormat.format(calendar.getTime()));
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Ежедневную награду получите в " + dateFormat.format(calendar.getTime()));
            player.sendPacket(cs);
        } else {
            player.setVar("nextEveryDayReward", String.valueOf(System.currentTimeMillis() + rewardInterval * 60 * 1000L), -1);
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Вы получили ежедневную награду за вход в игру!");
            player.sendPacket(cs);
            for (Map.Entry<Integer, Integer> reward: rewardListItems.entrySet()){
                Functions.addItem(player, reward.getKey(), reward.getValue());
            }
        }
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
        isActive = everyDayReward.getProperty("isActive", true);
        rewardInterval = everyDayReward.getProperty("rewardInterval", Integer.MAX_VALUE);

        if (isActive) {
            _log.info("Loaded Event: Every Day Reward [state: activated]");
        } else
            _log.info("Loaded Event: Every Day Reward [state: deactivated]");
    }


    /**
     * Запускает эвент
     */
    public void startEvent() {
        Player player = getSelf();
        if(!player.getPlayerAccess().IsEventGm)
            return;

        if(SetActive("EveryDayReward", true)) {
            System.out.println("Event: EveryDayReward started.");
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward started.");
            player.sendPacket(cs);
        } else{
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward already started.");
            player.sendPacket(cs);
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
            System.out.println("Event: EveryDayReward stopped.");
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward stopped.");
            player.sendPacket(cs);
        } else{
            Say2 cs = new Say2(0, ChatType.SCREEN_ANNOUNCE, "", "Event: EveryDayReward not started.");
            player.sendPacket(cs);
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
