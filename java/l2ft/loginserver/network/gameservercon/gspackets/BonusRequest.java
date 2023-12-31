package l2ft.loginserver.network.gameservercon.gspackets;


import l2ft.loginserver.accounts.Account;
import l2ft.loginserver.network.gameservercon.ReceivablePacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BonusRequest extends ReceivablePacket
{
	private static final Logger log = LoggerFactory.getLogger(BonusRequest.class);

	private String account;
	private double bonus;
	private int bonusExpire;

	@Override
	protected void readImpl()
	{
		account = readS();
		bonus = readF();
		bonusExpire = readD();
	}

	@Override
	protected void runImpl()
	{
		Account acc = new Account(account);
		acc.restore();
		acc.setBonus(bonus);
		acc.setBonusExpire(bonusExpire);
		acc.update();
	}
}
