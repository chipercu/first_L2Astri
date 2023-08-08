package l2ft.gameserver.handler.bypass;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.instances.NpcInstance;

public interface IBypassHandler
{
	String[] getBypasses();

	void onBypassFeedback(NpcInstance npc, Player player, String command);
}
