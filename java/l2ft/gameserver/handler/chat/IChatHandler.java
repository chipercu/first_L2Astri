package l2ft.gameserver.handler.chat;

import l2ft.gameserver.network.l2.components.ChatType;

public interface IChatHandler
{
	void say();

	ChatType getType();
}
