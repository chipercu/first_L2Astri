package l2ft.gameserver.network.l2.s2c;

public class ExServerPrimitive extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeEx(0x11);
		// TODO Sdddddd {[c(Sdddd ddd ddd|)] Sddddddd}
	}
}