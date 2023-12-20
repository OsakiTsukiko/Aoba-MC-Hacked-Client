package net.aoba.cmd.commands;

import net.aoba.Aoba;
import net.aoba.cmd.Command;
import net.aoba.cmd.InvalidSyntaxException;
import net.minecraft.client.util.InputUtil;

public class CmdClickgui extends Command {

	public CmdClickgui() {
		super("clickgui", "Allows the player to see chest locations through ESP", "set [value]");
	}

	@Override
	public void runCommand(String[] parameters) throws InvalidSyntaxException {
		if (parameters.length != 2)
			throw new InvalidSyntaxException(this);

		switch (parameters[0]) {
		case "set":
			char keybind = Character.toUpperCase(parameters[1].charAt(0));
			Aoba.getInstance().hudManager.clickGuiButton.setValue(InputUtil.fromKeyCode(keybind, 0));
			break;
		default:
			throw new InvalidSyntaxException(this);
		}
	}

	@Override
	public String[] getAutocorrect(String previousParameter) {
		switch (previousParameter) {
		default:
			return new String[] { "set" };
		}
	}
}
