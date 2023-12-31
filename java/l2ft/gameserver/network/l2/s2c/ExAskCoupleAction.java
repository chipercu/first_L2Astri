package l2ft.gameserver.network.l2.s2c;

public class ExAskCoupleAction extends L2GameServerPacket
{
	private int _objectId, _socialId;

	public ExAskCoupleAction(int objectId, int socialId)
	{
		_objectId = objectId;
		_socialId = socialId;
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0xBB);
		writeD(_socialId);
		writeD(_objectId);
	}
}
