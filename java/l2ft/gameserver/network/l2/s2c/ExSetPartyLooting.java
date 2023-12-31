package l2ft.gameserver.network.l2.s2c;

public class ExSetPartyLooting extends L2GameServerPacket
{
	private int _result;
	private int _mode;
	
	public ExSetPartyLooting(int result, int mode)
	{
		_result = result;
		_mode = mode;
	}
	
	@Override
	protected void writeImpl()
	{
		writeEx(0xC0);
		writeD(_result);
		writeD(_mode);
	}
}
