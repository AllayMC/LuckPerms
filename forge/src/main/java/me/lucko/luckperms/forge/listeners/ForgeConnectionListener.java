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

package me.lucko.luckperms.forge.listeners;

import com.mojang.authlib.GameProfile;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.forge.ForgeSenderFactory;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.util.AsyncConfigurationTask;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.util.UUID;

public class ForgeConnectionListener extends AbstractConnectionListener {
    private static final ConfigurationTask.Type USER_LOGIN_TASK_TYPE = new ConfigurationTask.Type("luckperms:user_login");

    private final LPForgePlugin plugin;

    public ForgeConnectionListener(LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onGatherLoginConfigurationTasks(GatherLoginConfigurationTasksEvent event) {
        PacketListener packetListener = event.getConnection().getPacketListener();
        if (!(packetListener instanceof ServerConfigurationPacketListenerImpl)) {
            return;
        }

        GameProfile gameProfile = ((ServerConfigurationPacketListenerImpl) packetListener).getOwner();
        if (gameProfile == null) {
            return;
        }

        String username = gameProfile.getName();
        UUID uniqueId = gameProfile.getId();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login (sync phase) for " + uniqueId + " - " + username);
        }

        AsyncConfigurationTask task = new AsyncConfigurationTask(
                this.plugin,
                USER_LOGIN_TASK_TYPE,
                () -> onPlayerNegotiationAsync(event.getConnection(), uniqueId, username)
        );
        event.addTask(task);
    }

    private void onPlayerNegotiationAsync(Connection connection, UUID uniqueId, String username) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login (async phase) for " + uniqueId + " - " + username);
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(uniqueId, username);
            recordConnection(uniqueId);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + username, ex);

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                Component component = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                connection.send(new ClientboundLoginDisconnectPacket(ForgeSenderFactory.toNativeText(component)));
                connection.disconnect(ForgeSenderFactory.toNativeText(component));
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
            }
        }
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        GameProfile profile = player.getGameProfile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + profile.getId() + " - " + profile.getName());
        }

        User user = this.plugin.getUserManager().getIfLoaded(profile.getId());

        if (user == null) {
            if (!getUniqueConnections().contains(profile.getId())) {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            Component component = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLanguage());
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
                return;
            } else {
                player.sendSystemMessage(ForgeSenderFactory.toNativeText(component));
            }
        }

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    @SubscribeEvent(priority = Priority.LOWEST)
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        handleDisconnect(player.getGameProfile().getId());
    }

}