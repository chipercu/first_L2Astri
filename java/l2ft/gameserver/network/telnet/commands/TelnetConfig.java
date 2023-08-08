package l2ft.gameserver.network.telnet.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import l2ft.gameserver.Config;
import l2ft.gameserver.network.telnet.TelnetCommand;
import l2ft.gameserver.network.telnet.TelnetCommandHolder;


public class TelnetConfig implements TelnetCommandHolder
{
	private Set<TelnetCommand> _commands = new LinkedHashSet<TelnetCommand>();

	public TelnetConfig()
	{
		_commands.add(new TelnetCommand("config", "cfg")
		{
			@Override
			public String getUsage()
			{
				return "config parameter[=value]";
			}
			
			@Override
			public String handle(String[] args)
			{
					if(args.length == 0 || args[0].isEmpty())
						return null;

					String[] val = args[0].split("=");
					
					if(val.length == 1)
					{
						String value = Config.getField(args[0]);
						return  value == null ? "Not found.\n\r" : value + "\n\r";
					}
					
					if(Config.setField(val[0], val[1]))
						return "Done.\n\r";
					else
						return "Error!\n\r";
			}
		});
	}

	@Override
	public Set<TelnetCommand> getCommands()
	{
		return _commands;
	}
}
