package l2ft.gameserver.network.authcomm.gspackets;

import l2ft.gameserver.network.authcomm.SendablePacket;


public class ChangePassword extends SendablePacket
{
	private String account;
	private String oldPass;
	private String newPass;
	private String hwid;
	
	public ChangePassword(String account, String oldPass, String newPass, String hwid)
	{
		this.account = account;
		this.oldPass = oldPass;
		this.newPass = newPass;
		this.hwid = hwid;
	}
	
    @Override
	protected void writeImpl()
	{
		writeC(0x08);
		writeS(account);
		writeS(oldPass);
		writeS(newPass);
		writeS(hwid);
	}
}
