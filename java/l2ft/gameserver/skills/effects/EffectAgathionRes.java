package l2ft.gameserver.skills.effects;

import l2ft.gameserver.model.Effect;
import l2ft.gameserver.stats.Env;

public final class EffectAgathionRes extends Effect
{
	public EffectAgathionRes(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setIsBlessedByNoblesse(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setIsBlessedByNoblesse(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}