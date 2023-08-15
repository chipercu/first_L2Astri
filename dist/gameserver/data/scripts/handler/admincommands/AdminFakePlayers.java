package handler.admincommands;

import fuzzy.Html_Constructor.tags.Button;
import fuzzy.Html_Constructor.tags.Combobox;
import fuzzy.Html_Constructor.tags.Edit;
import fuzzy.Html_Constructor.tags.Table;
import fuzzy.Html_Constructor.tags.parameters.Parameters;
import fuzzy.fake_players.model.Fake_constructor;
import l2ft.commons.dbutils.DbUtils;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.database.mysql;
import l2ft.gameserver.handler.admincommands.AdminCommandHandler;
import l2ft.gameserver.handler.admincommands.IAdminCommandHandler;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.base.ClassId;
import l2ft.gameserver.network.l2.s2c.ExShowScreenMessage;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.scripts.ScriptFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static l2ft.loginserver.network.gameservercon.gspackets.ChangeAccessLevel._log;

public class AdminFakePlayers extends Functions implements IAdminCommandHandler, ScriptFile, Parameters {

    public static final String bypass = "bypass -h scripts_handler.admincommands.AdminFakePlayers:";
    private enum Commands {

        admin_spawn_old_players,
        admin_spawn_fake,
        admin_set_fake,
        admin_set_old_fake,
        admin_unset_old_fake,
        admin_create_fake,
        admin_init_fake,
        admin_fake
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
            case admin_fake:
                getBotPanel(activeChar.getObjectId());
                break;
            case admin_init_fake:
                newFake(wordList);
                break;
            case admin_create_fake:
                createFake();
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

    public void getBotPanel(int playerId) {
        Player activeChar = GameObjectsStorage.getPlayer(playerId);
        if (activeChar == null){
            activeChar = getSelf();
        }
        NpcHtmlMessage msg = new NpcHtmlMessage(activeChar, null);
        String HTML = "<title>Управление ботами</title>";
        Table table = new Table(4, 1).setParams(border(0), background("l2ui_ct1.Windows_DF_TooltipBG"), cellpadding(0), cellspacing(0));
        final Table row1 = new Table(1, 3).setParams(width(280), height(60));
        row1.row(0).col(0).insert(new Button("Info", action(bypass + "info"), 80, 32).build());
        row1.row(0).col(1).setParams(width(100)).insert(new Button("Unspawn all", action(bypass + "unspawnAll"), 80, 32).build());
        row1.row(0).col(2).setParams(width(80)).insert(new Button("Создать", action(bypass + "createFake"), 70, 32).build());

        final Table row2 = new Table(2, 4).setParams(width(280), height(60));
        row2.row(0).col(0).setParams(width(70)).insert("Мин.Уровень");
        row2.row(0).col(1).setParams(width(70)).insert("Макс.Уровень");
        row2.row(0).col(2).setParams(width(70)).insert("Количество");
        row2.row(0).col(3).setParams(width(60)).insert("");
        row2.row(1).col(0).setParams(width(70)).insert(new Edit("minlvl").setParams(width(70)).build());
        row2.row(1).col(1).setParams(width(70)).insert(new Edit("maxlvl").setParams(width(70)).build());
        row2.row(1).col(2).setParams(width(70)).insert(new Edit("count").setParams(width(70)).build());
        row2.row(1).col(3).setParams(width(60)).insert(new Button("spawn", action(bypass + "spawn $minlvl $maxlvl $count"), 60, 32).build());
        table.row(0).col(0).insert(row1.build());
        table.row(0).col(0).insert(row2.build());
        msg.setHtml(HTML + table.build());
        activeChar.sendPacket(msg);
    }

    public void info(){
        System.out.println("show Info");
    }
    public void unspawnAll(){
        System.out.println("unspawn all");
    }
    public void spawn(String [] params){
        System.out.println(Arrays.toString(params));
    }

    public void createFake() {
        Player player = getSelf();

        NpcHtmlMessage msg = new NpcHtmlMessage(player, null);
        String HTML = "<title>Создание ботов</title>";

        Table table = new Table(4, 1).setParams(border(0), background("l2ui_ct1.Windows_DF_TooltipBG"), cellpadding(4), cellspacing(4));

        final Table row1 = new Table(2, 3).setParams(width(280), height(60));
        row1.row(0).col(0).setParams(width(100)).insert("Имя");
        row1.row(0).col(1).setParams(width(100)).insert("Титул");
        row1.row(0).col(2).setParams(width(80)).insert("Уровень");
        row1.row(1).col(0).setParams(width(100)).insert(new Edit("name").setParams(width(100)).build());
        row1.row(1).col(1).setParams(width(100)).insert(new Edit("title").setParams(width(100)).build());
        row1.row(1).col(2).setParams(width(80)).insert(new Edit("level").setParams(width(80)).build());

        final Table row2 = new Table(2, 2).setParams(width(280), height(60));
        row2.row(0).col(0).setParams(width(100)).insert("Профессия");
        row2.row(0).col(1).setParams(width(100)).insert("Пол");
        final List<String> classList = Arrays.stream(ClassId.values())
                .filter(classId -> classId.level() > 1)
                .map(Enum::name).filter(name -> !name.contains("dummy")).collect(Collectors.toList());
        row2.row(1).col(0).setParams(width(100)).insert(new Combobox("class", classList).setParams(width(100)).build());
        row2.row(1).col(1).setParams(width(100)).insert(new Combobox("sex", new ArrayList<String>(){{add("мужской"); add("женский");}}).setParams(width(100)).build());

        final Table row3 = new Table(1, 2).setParams(width(280), height(60));
        row3.row(0).col(1).insert(new Button("Создать", action(bypass + "newFake $name $title $level $class $sex"), 70, 32).build());

        table.row(1).col(0).insert(new Button("Панель", action(bypass + "getBotPanel"), 60, 32).build());
        table.row(1).col(0).insert(row1.build());
        table.row(2).col(0).insert(row2.build());
        table.row(3).col(0).insert(row3.build());
        msg.setHtml(HTML + table.build());
        player.sendPacket(msg);
    }

    public void newFake(String [] params){
        final Fake_constructor fake = new Fake_constructor(params[0], params[1], Integer.parseInt(params[2]), ClassId.valueOf(params[3]), params[4]);
        fake.initNewChar(getSelf());


        System.out.println(fake);
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
