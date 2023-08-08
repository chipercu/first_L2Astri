package l2ft.gameserver.scripts;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.Skill;

public interface ScriptFile
{
	public void onLoad();
	public void onReload();
	public void onShutdown();
}