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
