package ai.fake;

import l2ft.commons.threading.RunnableImpl;
import l2ft.commons.util.Rnd;
import l2ft.gameserver.ThreadPoolManager;
import l2ft.gameserver.ai.PlayerAI;
import l2ft.gameserver.model.Creature;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.Skill;
import l2ft.gameserver.model.WorldRegion;
import l2ft.gameserver.utils.Location;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class FakeTownAi extends PlayerAI {

    enum Task{
        MOVE, USE_SELF, RANDOM_MOVE
    }

    private ScheduledFuture<?> ai;

    public FakeTownAi(Player actor) {
        super(actor);
        actor.setRunning();
        ai = ThreadPoolManager.getInstance().scheduleAtFixedDelay(new TOWN(getActor()), 30000, 30000);
    }




    class TOWN extends RunnableImpl {

        private Player player;



        public TOWN(Creature actor) {
            this.player = (Player) actor;
        }

        @Override
        public void runImpl() throws Exception {
            if (player != null){
                final Task[] values = Task.values();
                final Task value = values[Rnd.get(values.length)];
                switch (value){
                    case MOVE:{
                        final List<Creature> aroundCharacters = player.getAroundCharacters(3000, 300);
                        if (!aroundCharacters.isEmpty()){
                            final Creature creature = aroundCharacters.get(Rnd.get(aroundCharacters.size()));
                            if (!player.isMoving && creature.isNpc()){
                                final Location pointToStay = Location.findPointToStay(creature.getLoc(), 100, creature.getGeoIndex());
                                player.moveToLocation(pointToStay, 30, true);
                            }
                        }
                        break;
                    }
                    case USE_SELF:{
                        player.getAllSkills().stream()
                                .filter(Skill::isBuff)
                                .filter(s -> !s.isToggle())
                                .filter(s -> !s.isPassive())
                                .limit(3)
                                .forEach(s -> ThreadPoolManager.getInstance().schedule(() -> player.altUseSkill(s, player), Rnd.get(1000, 3000)));
                        break;
                    }
                    case RANDOM_MOVE:{
                        final Location aroundPosition = Location.findPointToStay(player, 500);
                        player.moveToLocation(aroundPosition, 70, true);
                        break;
                    }
                }
            }else {
                ai.cancel(true);
                ai = null;
            }
        }
    }






}
