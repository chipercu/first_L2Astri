package l2ft.gameserver.network.l2.s2c;

public class ExShowTerritory extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeEx(0x89);
		// TODO ddd[dd]
	}
}