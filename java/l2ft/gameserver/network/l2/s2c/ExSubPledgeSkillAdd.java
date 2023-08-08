package l2ft.gameserver.network.l2.s2c;

/**
 * Author: VISTALL
 */
public class ExSubPledgeSkillAdd extends L2GameServerPacket
{
	private int _type, _id, _level;

	public ExSubPledgeSkillAdd(int type, int id, int level)
	{
		_type = type;
		_id = id;
		_level = level;
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0x76);
		writeD(_type);
		writeD(_id);
		writeD(_level);
	}
}