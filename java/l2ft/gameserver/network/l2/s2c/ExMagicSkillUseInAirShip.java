package l2ft.gameserver.network.l2.s2c;

public class ExMagicSkillUseInAirShip extends L2GameServerPacket
{
	/**
	 * заготовка!!!
	 * Format: ddddddddddh[h]h[ddd]
	 */

	@Override
	protected final void writeImpl()
	{
		writeEx(0x73);
	}
}