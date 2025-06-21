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

package me.lucko.luckperms.allay.listener;

import me.lucko.luckperms.allay.LPAllayPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.player.PlayerJoinEvent;
import org.allaymc.api.eventbus.event.player.PlayerLoginEvent;
import org.allaymc.api.eventbus.event.player.PlayerQuitEvent;

public class AllayConnectionListener extends AbstractConnectionListener {
    private final LPAllayPlugin plugin;

    public AllayConnectionListener(LPAllayPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = 1)
    public void onPlayerLogin(PlayerLoginEvent event) {
        var player = event.getPlayer();
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + player.getUUID() + " - " + player.getOriginName());
        }

        if (event.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + player.getUUID() + " - " + player.getOriginName() + ". No permissions data will be loaded.");
            return;
        }

        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            /* Actually process the login for the connection.
               We do this here to delay the login until the data is ready.
               If the login gets cancelled later on, then this will be cleaned up.

               This includes:
               - loading uuid data
               - loading permissions
               - creating a user instance in the UserManager for this connection.
               - setting up cached data. */
            try {
                var user = loadUser(player.getUUID(), player.getOriginName());
                this.recordConnection(player.getUUID());
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUUID(), player.getOriginName(), user);
            } catch (Exception exception) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.getUUID() + " - " + player.getOriginName(), exception);

                // there was some error loading
                if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                    // cancel the login attempt
                    var reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                    event.setDisconnectReason(LegacyComponentSerializer.legacySection().serialize(reason));
                    event.cancel();
                }

                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUUID(), player.getOriginName(), null);
            }
        });
    }

    @EventHandler(priority = 1)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUUID() + " - " + player.getOriginName());
        }

        var user = this.plugin.getUserManager().getIfLoaded(player.getUUID());
        if (user != null) {
            return;
        }

        if (!getUniqueConnections().contains(player.getUUID())) {
            this.plugin.getLogger().warn("User " + player.getUUID() + " - " + player.getOriginName() +
                                         " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
        } else {
            this.plugin.getLogger().warn("User " + player.getUUID() + " - " + player.getOriginName() +
                                         " doesn't currently have data pre-loaded, but they have been processed before in this session.");
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
            // disconnect the user
            var reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            event.getPlayer().disconnect(LegacyComponentSerializer.legacySection().serialize(reason));
        } else {
            // just send a message
            if (player.isInitialized()) {
                Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
            }
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = Integer.MAX_VALUE)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer().getUUID());
    }
}
