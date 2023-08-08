package l2ft.gameserver.handler.petition;

import l2ft.gameserver.model.Player;

public interface IPetitionHandler
{
	void handle(Player player, int id, String txt);
}
