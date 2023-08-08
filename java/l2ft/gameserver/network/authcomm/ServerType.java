package l2ft.gameserver.network.authcomm;

public enum ServerType
{
	NORMAL,
	RELAX,
	TEST,
	NO_LABEL,
	RESTRICTED,
	EVENT,
	FREE;

	private int _mask;

	ServerType()
	{
		_mask = 1 << ordinal();
	}

	public int getMask()
	{
		return _mask;
	}
}
