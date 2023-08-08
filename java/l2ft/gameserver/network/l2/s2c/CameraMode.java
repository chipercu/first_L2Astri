package l2ft.gameserver.network.l2.s2c;

public class CameraMode extends L2GameServerPacket
{
	int _mode;

	public CameraMode(int mode)
	{
		_mode = mode;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf7);
		writeD(_mode);
	}
}