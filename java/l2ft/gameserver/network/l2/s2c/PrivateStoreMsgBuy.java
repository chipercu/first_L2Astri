package l2ft.gameserver.network.l2.s2c;


import org.apache.commons.lang3.StringUtils;
import l2ft.gameserver.model.Player;

public class PrivateStoreMsgBuy extends L2GameServerPacket
{
	private int _objId;
	private String _name;

	/**
	 * Название личного магазина покупки
	 * @param player
	 */
	public PrivateStoreMsgBuy(Player player)
	{
		_objId = player.getObjectId();
		_name = StringUtils.defaultString(player.getBuyStoreName());
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xBF);
		writeD(_objId);
		writeS(_name);
	}
}