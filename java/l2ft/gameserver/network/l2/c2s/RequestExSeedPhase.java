package l2ft.gameserver.network.l2.c2s;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.network.l2.s2c.ExShowSeedMapInfo;

public class RequestExSeedPhase extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		sendPacket(new ExShowSeedMapInfo());
	}
}