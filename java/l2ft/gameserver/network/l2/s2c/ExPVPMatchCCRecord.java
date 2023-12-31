package l2ft.gameserver.network.l2.s2c;

import l2ft.gameserver.model.entity.events.impl.KrateisCubeEvent;
import l2ft.gameserver.model.entity.events.objects.KrateisCubePlayerObject;

/**
 * @author VISTALL
 */
public class ExPVPMatchCCRecord extends L2GameServerPacket
{
	private final KrateisCubePlayerObject[] _players;

	public ExPVPMatchCCRecord(KrateisCubeEvent cube)
	{
		_players = cube.getSortedPlayers();
	}

	@Override
	public void writeImpl()
	{
		writeEx(0x89);
		writeD(0x00); // Open/Dont Open
		writeD(_players.length);
		for(KrateisCubePlayerObject p : _players)
		{
			writeS(p.getName());
			writeD(p.getPoints());
		}
	}
}