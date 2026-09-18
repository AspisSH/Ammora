package com.ammora.mod.core.events;

import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.util.AmmoraLang;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the generation, lifecycle, and market impact of server economic events.
 */
public class MarketEventManager {

    private final List<MarketEvent> templatePool = new ArrayList<>();
    private MarketEvent activeEvent = null;
    private final Random random = new Random();

    public MarketEventManager() {
        registerTemplates();
    }

    private void registerTemplates() {
        templatePool.add(new MarketEvent(
                "gold_rush",
                "Gold Rush in Nether",
                "event.ammora.gold_rush.title",
                "Massive influx of Nether gold. Supply up, price down 25%.",
                "event.ammora.gold_rush.desc",
                "minecraft:gold_ingot",
                -0.25,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "diamond_collapse",
                "Collapse in Diamond Mines",
                "event.ammora.diamond_collapse.title",
                "A major cave-in caused an acute diamond deficit. Price surged +35%!",
                "event.ammora.diamond_collapse.desc",
                "minecraft:diamond",
                0.35,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "construction_boom",
                "Construction Boom",
                "event.ammora.construction_boom.title",
                "City infrastructure project buys up iron for bridges. Demand and price up +25%!",
                "event.ammora.construction_boom.desc",
                "minecraft:iron_ingot",
                0.25,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "netherite_discovery",
                "Ancient Expedition",
                "event.ammora.netherite_discovery.title",
                "Ancient ruins unearthed in Basalt Deltas. Large shipment of netherite arrived (-20%).",
                "event.ammora.netherite_discovery.desc",
                "minecraft:netherite_ingot",
                -0.20,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "redstone_automation",
                "Technological Breakthrough",
                "event.ammora.redstone_automation.title",
                "Engineers automated machinery assembly. High demand for redstone (+30%)!",
                "event.ammora.redstone_automation.desc",
                "minecraft:redstone",
                0.30,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "emerald_embargo",
                "Villager Trade Embargo",
                "event.ammora.emerald_embargo.title",
                "Villages ceased exporting emeralds. Exchange price jumped +40%!",
                "event.ammora.emerald_embargo.desc",
                "minecraft:emerald",
                0.40,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "lapis_enchantment",
                "Enchantment Season",
                "event.ammora.lapis_enchantment.title",
                "Guild mages are buying up lapis lazuli for rituals. Price rose +25%!",
                "event.ammora.lapis_enchantment.desc",
                "minecraft:lapis_lazuli",
                0.25,
                10,
                2
        ));
        templatePool.add(new MarketEvent(
                "copper_demand",
                "Copper Industrialization",
                "event.ammora.copper_demand.title",
                "Launch of massive factories spurred sharp demand for copper. Price rose +30%!",
                "event.ammora.copper_demand.desc",
                "minecraft:copper_ingot",
                0.30,
                10,
                2
        ));
    }

    public MarketEvent getActiveEvent() {
        return activeEvent;
    }

    public void setActiveEvent(MarketEvent event, MarketManager marketManager) {
        if (this.activeEvent != null && marketManager != null) {
            clearEventModifiers(marketManager);
        }
        this.activeEvent = event;
        if (this.activeEvent != null && marketManager != null) {
            applyEventModifiers(marketManager, this.activeEvent);
        }
    }

    public void applyEventModifiers(MarketManager marketManager, MarketEvent event) {
        if (event == null || marketManager == null) return;
        MarketResource res = marketManager.getResource(event.getAffectedResourceId());
        if (res != null) {
            double prePrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
            res.setEventModifier(event.getPriceMultiplier());
            double postPrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
            try {
                marketManager.recordMarketEventCandle(res, prePrice, postPrice);
            } catch (Exception ignored) {}
        }
    }

    public void clearEventModifiers(MarketManager marketManager) {
        if (activeEvent == null || marketManager == null) return;
        MarketResource res = marketManager.getResource(activeEvent.getAffectedResourceId());
        if (res != null) {
            double prePrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
            res.setEventModifier(0.0);
            double postPrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
            try {
                marketManager.recordMarketEventCandle(res, prePrice, postPrice);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Ticks the daily lifecycle of events.
     * @return Announcement message string to broadcast if an event started or finished, or null.
     */
    public String onDayChanged(MarketManager marketManager, boolean eventsEnabled, double triggerChance, int durationDays) {
        // 1. Process active event expiration
        if (activeEvent != null) {
            activeEvent.setRemainingDays(activeEvent.getRemainingDays() - 1);
            if (activeEvent.isExpired()) {
                String expiredTitle = activeEvent.getTitle();
                clearEventModifiers(marketManager);
                activeEvent = null;
                String msg = AmmoraLang.messageStr("event.expired", expiredTitle);
                return msg.startsWith("message.ammora.") ? "§6[AMMORA] §aMarket stabilized: event §e«" + expiredTitle + "» §ahas ended." : msg;
            }
            return null; // Event still active
        }

        // 2. Check if a new event triggers
        if (!eventsEnabled || templatePool.isEmpty()) {
            return null;
        }

        if (random.nextDouble() <= triggerChance) {
            MarketEvent template;
            int totalWeight = templatePool.stream().mapToInt(MarketEvent::getWeight).sum();
            if (totalWeight > 0) {
                int r = random.nextInt(totalWeight);
                int cumulative = 0;
                MarketEvent chosen = templatePool.get(0);
                for (MarketEvent t : templatePool) {
                    cumulative += t.getWeight();
                    if (r < cumulative) {
                        chosen = t;
                        break;
                    }
                }
                template = chosen;
            } else {
                template = templatePool.get(random.nextInt(templatePool.size()));
            }

            MarketEvent newEvent = new MarketEvent(
                    template.getId(),
                    template.getTitle(),
                    template.getTitleKey(),
                    template.getDescription(),
                    template.getDescriptionKey(),
                    template.getAffectedResourceId(),
                    template.getPriceMultiplier(),
                    template.getWeight(),
                    Math.max(1, durationDays)
            );
            setActiveEvent(newEvent, marketManager);
            String msg = AmmoraLang.messageStr("event.started", newEvent.getTitle(), newEvent.getDescription());
            return msg.startsWith("message.ammora.") ? "§6[AMMORA] §eMARKET NEWS: §6«" + newEvent.getTitle() + "»! §f" + newEvent.getDescription() : msg;
        }

        return null;
    }

    public synchronized void setTemplatePool(List<MarketEvent> templates) {
        templatePool.clear();
        templatePool.addAll(templates);
    }

    public List<MarketEvent> getTemplatePool() {
        return templatePool;
    }
}
