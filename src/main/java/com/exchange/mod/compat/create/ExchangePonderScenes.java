package com.exchange.mod.compat.create;

import com.exchange.mod.ExchangeMod;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ponder scene definitions for Ammora blocks and integrations.
 */
public class ExchangePonderScenes {

    public static void terminalRedstone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("terminal_redstone", Component.translatable("exchange.ponder.terminal_redstone.header").getString());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos terminalPos = util.grid().at(2, 1, 2);
        BlockPos comparatorPos = util.grid().at(2, 1, 3);
        BlockPos wirePos = util.grid().at(2, 1, 4);
        BlockPos lampPos = util.grid().at(1, 1, 4);

        // Place terminal
        scene.world().setBlock(terminalPos, ExchangeMod.EXCHANGE_TERMINAL.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(terminalPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .text(Component.translatable("exchange.ponder.terminal_redstone.text_1").getString())
                .pointAt(util.vector().topOf(terminalPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        // Place comparator
        scene.world().setBlock(comparatorPos,
                Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(util.select().position(comparatorPos), Direction.DOWN);
        scene.idle(15);

        // Place wire & lamp
        scene.world().setBlock(wirePos, Blocks.REDSTONE_WIRE.defaultBlockState(), false);
        scene.world().setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState(), false);
        scene.world().showSection(util.select().position(wirePos), Direction.DOWN);
        scene.world().showSection(util.select().position(lampPos), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .colored(PonderPalette.OUTPUT)
                .text(Component.translatable("exchange.ponder.terminal_redstone.text_2").getString())
                .pointAt(util.vector().topOf(comparatorPos))
                .placeNearTarget()
                .attachKeyFrame();

        // Activate redstone
        scene.world().setBlock(comparatorPos, Blocks.COMPARATOR.defaultBlockState()
                .setValue(ComparatorBlock.FACING, Direction.SOUTH).setValue(ComparatorBlock.POWERED, true), false);
        scene.world().setBlock(wirePos, Blocks.REDSTONE_WIRE.defaultBlockState()
                .setValue(RedStoneWireBlock.POWER, 12), false);
        scene.world().setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState()
                .setValue(RedstoneLampBlock.LIT, true), false);
        scene.idle(85);

        scene.overlay().showText(70)
                .colored(PonderPalette.BLUE)
                .text(Component.translatable("exchange.ponder.terminal_redstone.text_3").getString())
                .pointAt(util.vector().topOf(terminalPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void terminalDisplay(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("terminal_display", Component.translatable("exchange.ponder.terminal_display.header").getString());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos terminalPos = util.grid().at(1, 1, 2);
        BlockPos linkPos = util.grid().at(2, 1, 2);
        BlockPos boardPos = util.grid().at(3, 1, 2);

        scene.world().setBlock(terminalPos, ExchangeMod.EXCHANGE_TERMINAL.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(terminalPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(60)
                .text(Component.translatable("exchange.ponder.terminal_display.text_1").getString())
                .pointAt(util.vector().topOf(terminalPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(65);

        Block linkBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "display_link"));
        if (linkBlock != null && linkBlock != Blocks.AIR) {
            scene.world().setBlock(linkPos, linkBlock.defaultBlockState(), false);
            scene.world().showSection(util.select().position(linkPos), Direction.DOWN);
            scene.idle(20);
        }

        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text(Component.translatable("exchange.ponder.terminal_display.text_2").getString())
                .pointAt(util.vector().topOf(linkPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        Block boardBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "display_board"));
        if (boardBlock != null && boardBlock != Blocks.AIR) {
            scene.world().setBlock(boardPos, boardBlock.defaultBlockState(), false);
            scene.world().showSection(util.select().position(boardPos), Direction.DOWN);
            scene.idle(20);
        }

        scene.overlay().showText(70)
                .colored(PonderPalette.MEDIUM)
                .text(Component.translatable("exchange.ponder.terminal_display.text_3").getString())
                .pointAt(util.vector().topOf(boardPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);
        scene.markAsFinished();
    }

    public static void tradeDock(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("trade_dock", Component.translatable("exchange.ponder.trade_dock.header").getString());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos dockPos = util.grid().at(2, 1, 2);
        BlockPos chestPos = util.grid().at(2, 2, 2);
        BlockPos shaftPos = util.grid().at(1, 1, 2);

        scene.world().setBlock(dockPos, ExchangeMod.TRADE_DOCK.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(dockPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(60)
                .text(Component.translatable("exchange.ponder.trade_dock.text_1").getString())
                .pointAt(util.vector().topOf(dockPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(65);

        scene.world().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), false);
        scene.world().showSection(util.select().position(chestPos), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text(Component.translatable("exchange.ponder.trade_dock.text_2").getString())
                .pointAt(util.vector().topOf(dockPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        Block shaftBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "shaft"));
        if (shaftBlock != null && shaftBlock != Blocks.AIR) {
            BlockState shaftState = shaftBlock.defaultBlockState();
            if (shaftState.hasProperty(RotatedPillarBlock.AXIS)) {
                shaftState = shaftState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            }
            scene.world().setBlock(shaftPos, shaftState, false);
            scene.world().showSection(util.select().position(shaftPos), Direction.EAST);
            scene.idle(20);
        }

        scene.overlay().showText(80)
                .colored(PonderPalette.FAST)
                .text(Component.translatable("exchange.ponder.trade_dock.text_3").getString())
                .pointAt(util.vector().topOf(shaftPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(85);
        scene.markAsFinished();
    }

    public static void purchaseDock(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("purchase_dock", Component.translatable("exchange.ponder.purchase_dock.header").getString());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos dockPos = util.grid().at(2, 1, 2);
        BlockPos chestPos = util.grid().at(2, 1, 1);
        BlockPos leverPos = util.grid().at(2, 1, 3);

        scene.world().setBlock(dockPos, ExchangeMod.PURCHASE_DOCK.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(dockPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(65)
                .text(Component.translatable("exchange.ponder.purchase_dock.text_1").getString())
                .pointAt(util.vector().topOf(dockPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.world().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), false);
        scene.world().showSection(util.select().position(chestPos), Direction.SOUTH);
        scene.idle(15);

        scene.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState(), false);
        scene.world().showSection(util.select().position(leverPos), Direction.NORTH);
        scene.idle(20);

        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text(Component.translatable("exchange.ponder.purchase_dock.text_2").getString())
                .pointAt(util.vector().topOf(leverPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);

        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text(Component.translatable("exchange.ponder.purchase_dock.text_3").getString())
                .pointAt(util.vector().topOf(dockPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(75);
        scene.markAsFinished();
    }
}
