package l2ft.gameserver.skills.skillclasses;

import java.util.List;

import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.ai.CtrlEvent;
import l2ft.gameserver.ai.CtrlIntention;
import l2ft.gameserver.geodata.GeoEngine;
import l2ft.gameserver.model.Creature;
import l2ft.gameserver.model.Skill;
import l2ft.gameserver.model.instances.NpcInstance;
import l2ft.gameserver.network.l2.s2c.FlyToLocation;
import l2ft.gameserver.network.l2.s2c.SystemMessage;
import l2ft.gameserver.network.l2.s2c.ValidateLocation;
import l2ft.gameserver.stats.Stats;
import l2ft.gameserver.templates.StatsSet;
import l2ft.gameserver.utils.Location;
import l2ft.gameserver.utils.PositionUtils;

/**
 * @author Ro0TT
 * @date 4.2.2012
 **/

public class InstantJump extends Skill
{
	public InstantJump(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		if (targets.size()==0)
			return;

		Creature target = targets.get(0);
		if(Rnd.chance(target.calcStat(Stats.PSKILL_EVASION, 0, activeChar, this)))
		{
			if(activeChar.isPlayer())
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1_DODGES_THE_ATTACK).addName(target));
			if(target.isPlayer())
				target.sendPacket(new SystemMessage(SystemMessage.C1_HAS_EVADED_C2S_ATTACK).addName(target).addName(activeChar));
			return;
		}
		int x, y, z;

		int px = target.getX();
		int py = target.getY();
		double ph = PositionUtils.convertHeadingToDegree(target.getHeading());

		ph += 180;

		if (ph > 360)
			ph -= 360;

		ph = (Math.PI * ph) / 180;

		x = (int) (px + (25 * Math.cos(ph)));
		y = (int) (py + (25 * Math.sin(ph)));
		z = target.getZ();

		Location loc = new Location(x, y, z);

		if (Config.ALLOW_GEODATA)
			loc = GeoEngine.moveCheck(activeChar.getX(), activeChar.getY(), activeChar.getZ(), x, y, activeChar.getReflection().getGeoIndex());


		if(target.isNpc())
		{
			NpcInstance npc = (NpcInstance) target;
			npc.abortAttack(true, true);
			npc.abortCast(true, true);
			npc.getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		else target.setTarget(null);

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.broadcastPacket(new FlyToLocation(activeChar, loc, FlyToLocation.FlyType.DUMMY));
		activeChar.abortAttack(true, true);
		activeChar.abortCast(true, true);
		activeChar.setXYZ(loc.x, loc.y, loc.z);
		activeChar.setHeading(target.getHeading());
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
	}
}
