package l2ft.gameserver.network.l2.s2c;

import l2ft.gameserver.model.Creature;
import l2ft.gameserver.utils.Location;

/**
 * format   dddddd		(player id, target id, distance, startx, starty, startz)<p>
 */
public class ValidateLocation extends L2GameServerPacket
{
	private int _chaObjId;
	private Location _loc;
	private int _heading;

	public ValidateLocation(Creature cha)
	{
		_chaObjId = cha.getObjectId();
		_loc = cha.getLoc();
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x79);

		writeD(_chaObjId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_heading);
	}
}