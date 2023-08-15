package fuzzy.fake_players;

import fuzzy.Database_Util.DB_Util.DB_Player;
import fuzzy.Html_Constructor.tags.Button;
import fuzzy.Html_Constructor.tags.Combobox;
import fuzzy.Html_Constructor.tags.Edit;
import fuzzy.Html_Constructor.tags.Table;
import fuzzy.Html_Constructor.tags.parameters.Parameters;
import fuzzy.fake_players.model.Fake_constructor;
import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.ThreadPoolManager;
import l2ft.gameserver.data.xml.holder.SkillAcquireHolder;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.SkillLearn;
import l2ft.gameserver.model.actor.instances.player.Bonus;
import l2ft.gameserver.model.actor.instances.player.ShortCut;
import l2ft.gameserver.model.base.AcquireType;
import l2ft.gameserver.model.base.ClassId;
import l2ft.gameserver.model.base.Experience;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.network.l2.s2c.NpcHtmlMessage;
import l2ft.gameserver.scripts.Functions;
import l2ft.gameserver.scripts.ScriptFile;
import l2ft.gameserver.tables.SkillTable;
import l2ft.gameserver.templates.PlayerTemplate;
import l2ft.gameserver.templates.item.CreateItem;
import l2ft.gameserver.templates.item.ItemTemplate;
import l2ft.gameserver.utils.ItemFunctions;
import l2ft.gameserver.utils.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static l2ft.loginserver.network.gameservercon.gspackets.ChangeAccessLevel._log;

/**
 * Created by a.kiperku
 * Date: 15.08.2023
 */

public class FakeManager extends Functions implements ScriptFile, Parameters {


    public void newFake(String [] params){
        final Fake_constructor fake = new Fake_constructor(params[0], params[1], Integer.parseInt(params[2]), ClassId.valueOf(params[3]), params[4], params[5], params[6]);
        fake.initNewChar(getSelf());
        System.out.println(fake);
    }

    public void startSpawn(){
        final List<Integer> fake = DB_Player.loadPlayersIdByAccountName("fake");
        int count = 0;
        for (Integer playerId: fake){
            Player player = Player.restore(playerId);
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
        _log.info("Загружено " + count + " фантомов");


    }

    @Override
    public void onLoad() {
        _log.info("Запуск спавна фантомов");
        ThreadPoolManager.getInstance().schedule(this::startSpawn, 60 * 1000);
    }

    @Override
    public void onReload() {

    }

    @Override
    public void onShutdown() {

    }
}
