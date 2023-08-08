package l2ft.gameserver.network.l2.s2c;

import l2ft.gameserver.model.entity.boat.Boat;
import l2ft.gameserver.utils.Location;

public class VehicleInfo extends L2GameServerPacket
{
	private int _boatObjectId;
	private Location _loc;

	public VehicleInfo(Boat boat)
	{
		_boatObjectId = boat.getObjectId();
		_loc = boat.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x60);
		writeD(_boatObjectId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
	}
}