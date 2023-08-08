package l2ft.gameserver.listener.actor.player;

import l2ft.gameserver.listener.PlayerListener;

public interface OnAnswerListener extends PlayerListener
{
	void sayYes();

	void sayNo();
}
