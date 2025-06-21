/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.allay;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.SimpleCommand;
import org.allaymc.api.command.tree.CommandTree;

public class AllayLPCommand extends SimpleCommand {
    private final LPAllayPlugin plugin;
    private final CommandManager commandManager;

    public AllayLPCommand(LPAllayPlugin plugin, CommandManager commandManager) {
        super("luckperms", "LuckPerms commands");
        aliases.add("lp");

        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                // General overloads
                .key("sync").root()
                .key("info").root()
                .key("editor").root()
                .key("verbose").enums("verboseToggle", "on", "record", "off", "upload").str("filter").optional().root()
                .key("verbose").key("command").enums("verboseSelect", "me", "player").str("command").root()
                .key("tree").str("scope").optional().str("player").optional().root()

                .key("createtrack").str("track").root()
                .key("deletetrack").str("track").root()
                .key("listtracks");
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        var wrapped = this.plugin.getSenderFactory().wrap(sender);
        var arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        this.commandManager.executeCommand(wrapped, "lp", arguments);
        return CommandResult.success(null);
    }
}
