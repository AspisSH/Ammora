package com.ammora.mod.command;

import com.ammora.mod.network.AdminPacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command registry for the Ammora financial mod.
 * Registers operator commands such as `/ammora admin`.
 */
public class AmmoraCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ammora")
                        .then(Commands.literal("admin")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> executeOpenAdmin(ctx.getSource()))
                        )
        );
    }

    private static int executeOpenAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            AdminPacketHandler.openAdminScreen(player);
            return 1;
        }
        source.sendFailure(Component.literal("§cДанную команду может вызывать только игрок."));
        return 0;
    }
}
