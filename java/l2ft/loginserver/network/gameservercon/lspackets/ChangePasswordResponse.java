package l2ft.loginserver.network.gameservercon.lspackets;

import l2ft.loginserver.network.gameservercon.SendablePacket;

public class ChangePasswordResponse extends SendablePacket
{
	
	private String _account;
	boolean _hasChanged;

	public ChangePasswordResponse(String account, boolean hasChanged)
	{
		_account = account;
		_hasChanged = hasChanged;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x06);
		writeS(_account);
		writeD(_hasChanged ? 1 : 0);
	}
}
