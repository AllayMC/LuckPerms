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

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.SenderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.util.Tristate;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.i18n.LangCode;
import org.allaymc.api.permission.Permission;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import java.util.Objects;
import java.util.UUID;

public class AllaySenderFactory extends SenderFactory<LPAllayPlugin, CommandSender> {
    private static final UUID SERVER_UUID = new UUID(0, 0);

    public AllaySenderFactory(LPAllayPlugin plugin) {
        super(plugin);
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        if (sender instanceof EntityPlayer player) {
            return player.getUUID();
        }

        return SERVER_UUID;
    }

    @Override
    protected String getName(CommandSender sender) {
        return sender.getCommandSenderName();
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        LangCode locale = LangCode.en_US;
        if (sender instanceof EntityPlayer player) {
            locale = player.getLangCode();
        }
        var rendered = TranslationManager.render(message, Objects.requireNonNull(locale, "locale").name());
        sender.sendText(LegacyComponentSerializer.legacySection().serialize(rendered));
    }

    @Override
    protected Tristate getPermissionValue(CommandSender sender, String node) {
        return sender.hasPermission(getPermissionOrCreate(node)) ? Tristate.TRUE : Tristate.FALSE;
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(getPermissionOrCreate(node));
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        Registries.COMMANDS.execute(sender, command);
    }

    private Permission getPermissionOrCreate(String name) {
        var permission = Permission.get(name);
        if (permission == null) {
            permission = Permission.create(name);
        }

        return permission;
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof Server;
    }
}
