package l2ft.gameserver.network.l2.c2s;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import l2ft.gameserver.instancemanager.RaidBossSpawnManager;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.network.l2.s2c.ExGetBossRecord;
import l2ft.gameserver.network.l2.s2c.ExGetBossRecord.BossRecordInfo;

/**
 * Format: (ch) d
 */
public class RequestGetBossRecord extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _bossID;

	@Override
	protected void readImpl()
	{
		_bossID = readD(); // always 0?
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		int totalPoints = 0;
		int ranking = 0;

		if(activeChar == null)
			return;

		List<BossRecordInfo> list = new ArrayList<BossRecordInfo>();
		Map<Integer, Integer> points = RaidBossSpawnManager.getInstance().getPointsForOwnerId(activeChar.getObjectId());
		if(points != null && !points.isEmpty())
			for(Map.Entry<Integer, Integer> e : points.entrySet())
				switch(e.getKey())
				{
					case -1: // RaidBossSpawnManager.KEY_RANK
						ranking = e.getValue();
						break;
					case 0: //  RaidBossSpawnManager.KEY_TOTAL_POINTS
						totalPoints = e.getValue();
						break;
					default:
						list.add(new BossRecordInfo(e.getKey(), e.getValue(), 0));
				}

		activeChar.sendPacket(new ExGetBossRecord(ranking, totalPoints, list));
	}
}