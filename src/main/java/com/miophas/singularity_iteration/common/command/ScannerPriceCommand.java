// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.processing.scanner.ScannedPrices;
import com.miophas.singularity_iteration.common.processing.scanner.ScannerPricing;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /mio_icif uu scanner} (0.1.10.36, permission level 2): the Scanner's own prices of items without a published price.
 * <ul>
 *   <li>{@code price <item>}: the published price, the saved Scanner price, or what a scan would give now (and from where);</li>
 *   <li>{@code forget <item>}: drops the saved Scanner price, so the next scan works it out again (patterns of the item
 *       cannot be copied until then).</li>
 * </ul>
 */
public final class ScannerPriceCommand {
    private ScannerPriceCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("uu").then(Commands.literal("scanner")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("price").then(Commands.argument("item", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                .executes(context -> price(context.getSource(), ResourceLocationArgument.getId(context, "item")))))
            .then(Commands.literal("forget").then(Commands.argument("item", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                .executes(context -> forget(context.getSource(), ResourceLocationArgument.getId(context, "item"))))))));
    }

    private static ItemStack stack(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).map(item -> item.getDefaultInstance()).orElse(ItemStack.EMPTY);
    }

    private static int price(CommandSourceStack source, ResourceLocation id) {
        var stack = stack(id);
        if (stack.isEmpty()) { source.sendFailure(Component.translatable("command.mio_icif.uu_scanner.unknown", id.toString())); return 0; }
        var server = source.getServer();
        var published = UuQuoteBook.publishedQuote(server, stack);
        if (published != null) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.uu_scanner.published", id.toString(), fmt(published.buckets())), false);
            return 1;
        }
        var saved = ScannedPrices.get(server).get(stack);
        if (saved != null) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.uu_scanner.saved", id.toString(), fmt(saved.buckets()),
                source(saved.source())), false);
            return 1;
        }
        var price = ScannerPricing.price(server, stack);
        if (price == null) {
            source.sendSuccess(() -> Component.translatable(ScannerPricing.excluded(server, stack)
                ? "command.mio_icif.uu_scanner.excluded" : "command.mio_icif.uu_scanner.none", id.toString()), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.mio_icif.uu_scanner.would", id.toString(), fmt(price.buckets()),
            source(price.source())), false);
        return 1;
    }

    private static int forget(CommandSourceStack source, ResourceLocation id) {
        var stack = stack(id);
        if (stack.isEmpty()) { source.sendFailure(Component.translatable("command.mio_icif.uu_scanner.unknown", id.toString())); return 0; }
        boolean removed = ScannedPrices.get(source.getServer()).remove(stack);
        removed |= UuQuoteBook.forgetScanned(source.getServer(), stack);
        boolean done = removed;
        source.sendSuccess(() -> Component.translatable(done ? "command.mio_icif.uu_scanner.forgot" : "command.mio_icif.uu_scanner.not_saved",
            id.toString()), true);
        return done ? 1 : 0;
    }

    private static Component source(ScannerPricing.Source source) {
        return Component.translatable(source == ScannerPricing.Source.RECIPE ? "gui.mio_icif.scanner.source.recipe" : "gui.mio_icif.scanner.source.estimate");
    }

    private static String fmt(double buckets) {
        return String.format(java.util.Locale.ROOT, "%.6f", buckets);
    }
}
