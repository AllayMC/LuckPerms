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

import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.luckperms.api.platform.Platform;
import org.allaymc.api.entity.component.player.EntityPlayerNetworkComponent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Allay.
 */
public class LPAllayBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    private final Plugin loader;
    /**
     * A scheduler adapter for the platform
     */
    private final AllaySchedulerAdapter schedulerAdapter;
    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;
    /**
     * The plugin instance
     */
    private final LPAllayPlugin plugin;
    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);
    /**
     * The plugin logger
     */
    private PluginLogger logger;
    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    public LPAllayBootstrap(Plugin loader) {
        this.loader = loader;

        this.schedulerAdapter = new AllaySchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPAllayPlugin(this);
    }

    @Override
    public void onLoad() {
        try {
            this.logger = new Slf4jPluginLogger(loader.getPluginLogger());
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    @Override
    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
    public void onDisable() {
        this.plugin.disable();
    }

    @Override
    public Plugin getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public String getVersion() {
        return this.loader.getPluginContainer().descriptor().getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    @Override
    public Platform.Type getType() {
        return Platform.Type.ALLAY;
    }

    @Override
    public String getServerBrand() {
        return "Allay";
    }

    @Override
    public String getServerVersion() {
        try {
            var gitProperties = Class.forName("org.allaymc.server.utils.GitProperties");
            var getBuildApiVersion = gitProperties.getMethod("getBuildApiVersion");
            return String.valueOf(getBuildApiVersion.invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getDataDirectory() {
        return this.loader.getPluginContainer().dataFolder().toAbsolutePath();
    }

    @Override
    public Optional<EntityPlayer> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(Server.getInstance().getPlayerService().getPlayers().get(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.ofNullable(Server.getInstance().getPlayerService().getOnlinePlayerByName(username)).map(EntityPlayer::getUUID);
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getPlayer(uniqueId).map(EntityPlayer::getOriginName);
    }

    @Override
    public int getPlayerCount() {
        return Server.getInstance().getPlayerService().getPlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        return Server.getInstance().getPlayerService().getPlayers().values().stream()
                .map(EntityPlayerNetworkComponent::getOriginName)
                .toList();
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return new ArrayList<>(Server.getInstance().getPlayerService().getPlayers().keySet());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        var player = Server.getInstance().getPlayerService().getPlayers().get(uniqueId);
        return player != null;
    }
}
