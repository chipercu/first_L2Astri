package l2ft.gameserver.network.l2.c2s;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.entity.SevenSigns;
import l2ft.gameserver.network.l2.s2c.SSQStatus;

/**
 * Seven Signs Record Update Request
 * packet type id 0xc8
 * format: cc
 */
public class RequestSSQStatus extends L2GameClientPacket
{
	private int _page;

	@Override
	protected void readImpl()
	{
		_page = readC();
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if((SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod()) && _page == 4)
			return;

		activeChar.sendPacket(new SSQStatus(activeChar, _page));
	}
}