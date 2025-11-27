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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.luckperms.api.query.QueryOptions;
import org.allaymc.api.permission.PermissionCalculator;
import org.allaymc.api.permission.Tristate;
import org.allaymc.api.player.Player;

/**
 * @author daoge_cmd
 */
public record AllayPermissionCalculator(Player player, User user, Function<Player, QueryOptions> queryOptionsSupplier) implements PermissionCalculator {
    @Override
    public Tristate calculatePermission(String permission) {
        QueryOptions queryOptions = Preconditions.checkNotNull(this.queryOptionsSupplier.apply(this.player));
        return convertTristate(this.user.getCachedData().getPermissionData(queryOptions)
                .checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result());
    }

    private static Tristate convertTristate(net.luckperms.api.util.Tristate tristate) {
        return switch (tristate) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case UNDEFINED -> Tristate.UNDEFINED;
        };
    }
}
