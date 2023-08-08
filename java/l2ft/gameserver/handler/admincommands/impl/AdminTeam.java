package l2ft.gameserver.handler.admincommands.impl;

import l2ft.gameserver.handler.admincommands.IAdminCommandHandler;
import l2ft.gameserver.model.Creature;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.base.TeamType;
import l2ft.gameserver.network.l2.components.SystemMsg;

public class AdminTeam implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_setteam
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		TeamType team = TeamType.NONE;
		if(wordList.length >= 2)
		{
			for(TeamType t : TeamType.values())
			{
				if(wordList[1].equalsIgnoreCase(t.name()))
					team = t;
			}
		}

		GameObject object = activeChar.getTarget();
		if(object == null || !object.isCreature())
		{
			activeChar.sendPacket(SystemMsg.INVALID_TARGET);
			return false;
		}

		((Creature)object).setTeam(team);
		return true;
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}
