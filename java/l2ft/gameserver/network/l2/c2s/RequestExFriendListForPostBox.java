package l2ft.gameserver.network.l2.c2s;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.network.l2.s2c.FriendList;

/**
 * @author VISTALL
 * @date 23:36/22.03.2011
 */
public class RequestExFriendListForPostBox extends L2GameClientPacket
{
	@Override
	protected void readImpl() throws Exception
	{

	}

	@Override
	protected void runImpl() throws Exception
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		player.sendPacket(new FriendList(player));
	}
}
