package l2ft.gameserver.network.l2.s2c;

import l2ft.gameserver.model.Creature;

public class FinishRotating extends L2GameServerPacket
{
	private int _charId, _degree, _speed;

	public FinishRotating(Creature player, int degree, int speed)
	{
		_charId = player.getObjectId();
		_degree = degree;
		_speed = speed;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x61);
		writeD(_charId);
		writeD(_degree);
		writeD(_speed);
		writeD(0x00); //??
	}
}