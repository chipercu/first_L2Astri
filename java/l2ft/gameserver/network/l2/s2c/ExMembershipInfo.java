package l2ft.gameserver.network.l2.s2c;

public class ExMembershipInfo extends L2GameServerPacket {
	private int i;

	public ExMembershipInfo(int paramInt) {
		i = paramInt;
	}

	protected void writeImpl() {
		writeEx(194);
		writeD(0);
		writeD(0);
		writeD(i);
	}
}