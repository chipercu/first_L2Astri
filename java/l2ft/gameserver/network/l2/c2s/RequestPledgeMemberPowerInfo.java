package l2ft.gameserver.network.l2.c2s;

import l2ft.gameserver.model.pledge.Clan;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.pledge.UnitMember;
import l2ft.gameserver.network.l2.s2c.PledgeReceivePowerInfo;

public class RequestPledgeMemberPowerInfo extends L2GameClientPacket
{
	// format: chdS
	@SuppressWarnings("unused")
	private int _not_known;
	private String _target;

	@Override
	protected void readImpl()
	{
		_not_known = readD();
		_target = readS(16);
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		Clan clan = activeChar.getClan();
		if(clan != null)
		{
			UnitMember cm = clan.getAnyMember(_target);
			if(cm != null)
				activeChar.sendPacket(new PledgeReceivePowerInfo(cm));
		}
	}
}