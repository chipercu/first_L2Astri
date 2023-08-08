package l2ft.gameserver.handler.admincommands.impl;

import l2ft.gameserver.handler.admincommands.IAdminCommandHandler;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.entity.events.GlobalEvent;
import l2ft.gameserver.network.l2.components.SystemMsg;

public class AdminGlobalEvent implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_list_events
	}
	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		Commands c = (Commands)comm;
		switch(c)
		{
			case admin_list_events:
				GameObject object = activeChar.getTarget();
				if(object == null)
					activeChar.sendPacket(SystemMsg.INVALID_TARGET);
				else
				{
					for(GlobalEvent e : object.getEvents())
						activeChar.sendMessage("- " + e.toString());
				}
				break;
		}
		return false;
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}
