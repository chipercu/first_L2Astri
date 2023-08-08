package l2ft.gameserver.skills.skillclasses;

import java.util.List;

import l2ft.commons.util.Rnd;
import l2ft.gameserver.model.Creature;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.Skill;
import l2ft.gameserver.templates.StatsSet;

public class PcBangPointsAddrnd extends Skill
{
	private final int _minCount;
	private final int _maxCount;
	
	public PcBangPointsAddrnd(StatsSet set)
	{
		super(set);
		_minCount = set.getInteger("PCMinCount");
		_maxCount = set.getInteger("PCMaxCount", _minCount);
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		int points = Rnd.get(_minCount, _maxCount);

		for(Creature target : targets)
		{
			if(target.isPlayer())
			{
				Player player = target.getPlayer();
				player.addPcBangPoints(points, false);
			}
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}