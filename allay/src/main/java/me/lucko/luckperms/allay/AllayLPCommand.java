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
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.tree.CommandNode;
import org.allaymc.api.command.tree.CommandTree;

import java.util.ArrayList;
import java.util.List;

public class AllayLPCommand extends Command {
    private final LPAllayPlugin plugin;
    private final CommandManager commandManager;

    public AllayLPCommand(LPAllayPlugin plugin, CommandManager commandManager) {
        super("luckperms", "LuckPerms commands", null);
        this.aliases.add("lp");
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    // https://luckperms.net/wiki/Command-Usage
    @Override
    public void prepareCommandTree(CommandTree tree) {
        var root = tree.getRoot();

        // General
        root.key("sync").permission("luckperms.sync");
        root.key("info").permission("luckperms.info");
        root.key("editor").permission("luckperms.editor");
        root.key("verbose").permission("luckperms.verbose").enums("verboseToggle", "on", "record", "off", "upload").str("filter").optional();
        root.key("tree").permission("luckperms.tree").str("scope").optional().str("player").optional();
        root.key("search").permission("luckperms.search").str("comparison").optional().str("permission");
        root.key("networksync").permission("luckperms.sync");
        root.key("import").permission("luckperms.import").msg("<file | code --upload> [--replace]");
        root.key("export").permission("luckperms.export").str("file").key("--upload").optional();
        root.key("reloadconfig").permission("luckperms.reloadconfig");
        root.key("bulkupdate").permission("luckperms.bulkupdate");
        root.key("translations").permission("luckperms.translations");
        root.key("creategroup").permission("luckperms.creategroup").str("group").intNum("weight").optional().str("displayname").optional();
        root.key("deletegroup").permission("luckperms.deletegroup").str("group");
        root.key("listgroups").permission("luckperms.listgroups");
        root.key("createtrack").permission("luckperms.createtrack").str("track");
        root.key("deletetrack").permission("luckperms.deletetrack").str("track");
        root.key("listtracks").permission("luckperms.listtracks");

        // User
        var user = root.key("user").str("user");
        user.key("info").permission("luckperms.user.info");
        buildPermissionNode(user.key("permission").permission("luckperms.user.permission"), "luckperms.user.permission");
        buildParentNode(user.key("parent").permission("luckperms.user.parent"), "luckperms.user.parent");
        buildMetaNode(user.key("meta").permission("luckperms.user.meta"), "luckperms.user.meta");
        user.key("editor").permission("luckperms.user.editor");
        user.key("promote").permission("luckperms.user.promote").str("track").msg("context...");
        user.key("demote").permission("luckperms.user.demote").str("track").msg("context...");
        user.key("showtracks").permission("luckperms.user.showtracks");
        user.key("clear").permission("luckperms.user.clear").msg("context...");
        user.key("clone").permission("luckperms.user.clone").str("user");

        // Group
        var group = root.key("group").str("group");
        group.key("info").permission("luckperms.group.info");
        buildPermissionNode(group.key("permission").permission("luckperms.group.permission"), "luckperms.group.permission");
        buildParentNode(group.key("parent").permission("luckperms.group.parent"), "luckperms.group.parent");
        buildMetaNode(group.key("meta").permission("luckperms.group.meta"), "luckperms.group.meta");
        group.key("editor").permission("luckperms.group.editor");
        group.key("listmembers").permission("luckperms.group.listmembers").intNum("page").optional();
        group.key("setweight").permission("luckperms.group.setweight").intNum("weight");
        group.key("setdisplayname").permission("luckperms.group.setdisplayname").str("name");
        group.key("showtracks").permission("luckperms.group.showtracks");
        group.key("clear").permission("luckperms.group.clear").msg("context...");
        group.key("rename").permission("luckperms.group.rename").str("new name");
        group.key("clone").permission("luckperms.group.clone").str("name of clone");

        // Track
        var track = root.key("track").str("track");
        track.key("info").permission("luckperms.track.info");
        track.key("editor").permission("luckperms.track.editor");
        track.key("append").permission("luckperms.track.append").str("group");
        track.key("insert").permission("luckperms.track.insert").str("group").intNum("position");
        track.key("remove").permission("luckperms.track.remove").str("group");
        track.key("clear").permission("luckperms.track.clear");
        track.key("rename").permission("luckperms.track.rename").str("new name");
        track.key("clone").permission("luckperms.track.clone").str("name of clone");

        // Log
        var log = root.key("log");
        log.key("recent").permission("luckperms.log.recent").str("user").optional().intNum("page").optional();
        log.key("search").permission("luckperms.log.search").str("query").optional().intNum("page").optional();
        log.key("notify").permission("luckperms.log.notify").str("on|off");
        log.key("userhistory").permission("luckperms.log.userhistory").str("user").optional().intNum("page").optional();
        log.key("grouphistory").permission("luckperms.log.grouphistory").str("group").optional().intNum("page").optional();
        log.key("trackhistory").permission("luckperms.log.trackhistory").str("track").optional().intNum("page").optional();
    }

    protected void buildPermissionNode(CommandNode node, String permissionPrefix) {
        node.key("info").permission(permissionPrefix + ".info");
        node.key("set").permission(permissionPrefix + ".set").str("node").str("value").msg("context...");
        node.key("unset").permission(permissionPrefix + ".unset").str("node").msg("context...");
        node.key("settemp").permission(permissionPrefix + ".settemp").str("node").str("value").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("unsettemp").permission(permissionPrefix + ".unsettemp").str("node").msg("context...");
        node.key("check").permission(permissionPrefix + ".check").str("node").msg("context...");
        node.key("clear").permission(permissionPrefix + ".clear").msg("context...");
    }

    protected void buildParentNode(CommandNode node, String permissionPrefix) {
        node.key("info").permission(permissionPrefix + ".info");
        node.key("set").permission(permissionPrefix + ".set").str("group").msg("context...");
        node.key("add").permission(permissionPrefix + ".add").str("group").msg("context...");
        node.key("remove").permission(permissionPrefix + ".remove").str("group").msg("context...");
        node.key("settrack").permission(permissionPrefix + ".settrack").str("track").str("group").msg("context...");
        node.key("addtemp").permission(permissionPrefix + ".addtemp").str("group").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("removetemp").permission(permissionPrefix + ".removetemp").str("group").msg("context...");
        node.key("clear").permission(permissionPrefix + ".clear").msg("context...");
        node.key("cleartrack").permission(permissionPrefix + ".cleartrack").str("track").msg("context...");
        node.key("switchprimarygroup").permission(permissionPrefix + ".switchprimarygroup").str("group").msg("context...");
    }

    protected void buildMetaNode(CommandNode node, String permissionPrefix) {
        node.key("info").permission(permissionPrefix + ".info");
        node.key("set").permission(permissionPrefix + ".set").str("key").str("value").msg("context...");
        node.key("unset").permission(permissionPrefix + ".unset").str("key").msg("context...");
        node.key("settemp").permission(permissionPrefix + ".settemp").str("key").str("value").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("unsettemp").permission(permissionPrefix + ".unsettemp").str("key").msg("context...");
        node.key("addprefix").permission(permissionPrefix + ".addprefix").intNum("priority").str("prefix").msg("context...");
        node.key("addsuffix").permission(permissionPrefix + ".addsuffix").intNum("priority").str("suffix").msg("context...");
        node.key("setprefix").permission(permissionPrefix + ".setprefix").intNum("priority").str("prefix").msg("context...");
        node.key("setsuffix").permission(permissionPrefix + ".setsuffix").intNum("priority").str("suffix").msg("context...");
        node.key("removeprefix").permission(permissionPrefix + ".removeprefix").intNum("priority").str("prefix").msg("context...");
        node.key("removesuffix").permission(permissionPrefix + ".removesuffix").intNum("priority").str("suffix").msg("context...");
        node.key("addtempprefix").permission(permissionPrefix + ".addtempprefix").intNum("priority").str("prefix").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("addtempsuffix").permission(permissionPrefix + ".addtempsuffix").intNum("priority").str("suffix").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("settempprefix").permission(permissionPrefix + ".settempprefix").intNum("priority").str("prefix").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("settempsuffix").permission(permissionPrefix + ".settempsuffix").intNum("priority").str("suffix").intNum("duration").enums("temporary modifier", "seconds", "minutes", "hours", "days", "weeks", "months", "years").msg("context...");
        node.key("removetempprefix").permission(permissionPrefix + ".removetempprefix").intNum("priority").str("prefix").msg("context...");
        node.key("removetempsuffix").permission(permissionPrefix + ".removetempsuffix").intNum("priority").str("suffix").msg("context...");
        node.key("clear").permission(permissionPrefix + ".clear").msg("context...");
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        var wrapped = this.plugin.getSenderFactory().wrap(sender);
        this.commandManager.executeCommand(wrapped, "lp", new ArrayList<>(List.of(args)));
        return CommandResult.success(null);
    }
}
