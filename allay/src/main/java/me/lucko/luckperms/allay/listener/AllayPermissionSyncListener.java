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

import me.lucko.luckperms.common.event.LuckPermsEventListener;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.allaymc.api.permission.Permission;
import org.allaymc.api.server.Server;

public class AllayPermissionSyncListener implements LuckPermsEventListener {
    @Override
    public void bind(EventBus bus) {
        //noinspection resource
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        var user = event.getUser();
        var player = Server.getInstance().getPlayerService().getPlayers().get(user.getUniqueId());
        if (player != null) {
            var permissions = user.getCachedData().getPermissionData().getPermissionMap();
            for (var entry : permissions.entrySet()) {
                var permission = Permission.get(entry.getKey());
                if (permission == null) {
                    permission = Permission.create(entry.getKey());
                }

                player.setPermission(permission, entry.getValue());
            }
        }
    }
}
