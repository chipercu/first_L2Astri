package l2ft.gameserver.network.l2.s2c;

public class ExRaidCharSelected extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeEx(0xB5);
		// just a trigger
	}
}