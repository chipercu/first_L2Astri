package l2ft.gameserver.network.authcomm.lspackets;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.network.authcomm.AuthServerCommunication;
import l2ft.gameserver.network.authcomm.ReceivablePacket;
import l2ft.gameserver.network.l2.GameClient;
import l2ft.gameserver.network.l2.components.CustomMessage;
import l2ft.gameserver.scripts.Functions;


public class ChangePasswordResponse extends ReceivablePacket
{
	String account;
	boolean changed;

	@Override
	public void readImpl()
	{
		account = readS();
		changed = readD() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		GameClient client = AuthServerCommunication.getInstance().removeWaitingClient(account);
		if(client == null)
			return;
		
		Player activeChar = client.getActiveChar();

		if(activeChar == null)
			return;
		
		if(changed)
			Functions.show(new CustomMessage("scripts.commands.user.password.ResultTrue", activeChar), activeChar);
		else
			Functions.show(new CustomMessage("scripts.commands.user.password.ResultFalse", activeChar), activeChar);
	}
}