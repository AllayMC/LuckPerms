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

import me.lucko.luckperms.allay.calculator.AllayCalculatorFactory;
import me.lucko.luckperms.allay.context.AllayContextManager;
import me.lucko.luckperms.allay.listener.AllayConnectionListener;
import me.lucko.luckperms.allay.listener.AllayPermissionSyncListener;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.context.manager.ContextManager;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.model.manager.user.UserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Allay API.
 */
public class LPAllayPlugin extends AbstractLuckPermsPlugin {
    private final LPAllayBootstrap bootstrap;

    private AllaySenderFactory senderFactory;
    private AllayConnectionListener connectionListener;
    private CommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private AllayContextManager contextManager;

    public LPAllayPlugin(LPAllayBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new AllaySenderFactory(this);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new AllayConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new AllayConnectionListener(this);
        Server.getInstance().getEventBus().registerListener(this.connectionListener);
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new CommandManager(this);
        Registries.COMMANDS.register(new AllayLPCommand(this, this.commandManager));
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new AllayCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new AllayContextManager(this);
    }

    @Override
    protected void setupPlatformHooks() {
        // NO-OP
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new AllayEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        // NO-OP
    }

    @Override
    protected void performFinalSetup() {
        getApiProvider().getEventBus().subscribe(new AllayPermissionSyncListener());
    }

    @Override
    public LPAllayBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public AllaySenderFactory getSenderFactory() {
        return senderFactory;
    }

    @Override
    public UserManager<? extends User> getUserManager() {
        return this.userManager;
    }

    @Override
    public GroupManager<? extends Group> getGroupManager() {
        return this.groupManager;
    }

    @Override
    public TrackManager<? extends Track> getTrackManager() {
        return this.trackManager;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public ContextManager<?, ?> getContextManager() {
        return this.contextManager;
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        var playerService = Server.getInstance().getPlayerService();
        return Stream.concat(
                Stream.of(getConsoleSender()),
                playerService.getPlayers().values().stream().map(p -> this.senderFactory.wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.senderFactory.wrap(Server.getInstance());
    }
}
