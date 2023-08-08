package l2ft.gameserver.network.l2.s2c;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.entity.boat.Boat;
import l2ft.gameserver.utils.Location;

public class ExGetOffAirShip extends L2GameServerPacket
{
	private int _playerObjectId, _boatObjectId;
	private Location _loc;

	public ExGetOffAirShip(Player cha, Boat boat, Location loc)
	{
		_playerObjectId = cha.getObjectId();
		_boatObjectId = boat.getObjectId();
		_loc = loc;
	}

	@Override
	protected final void writeImpl()
	{
		writeEx(0x64);
		writeD(_playerObjectId);
		writeD(_boatObjectId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
	}
}