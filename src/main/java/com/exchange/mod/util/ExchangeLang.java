package com.exchange.mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Method;

/**
 * Centralized localization and internationalization utility for Ammora.
 * Provides clean access to translation keys with safe client/server and test execution.
 */
public class ExchangeLang {

    private static Method i18nGetMethod = null;
    private static boolean i18nLookupAttempted = false;

    /**
     * Creates a translatable Component (works on both Client and Server).
     */
    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /**
     * Convenience method for GUI translation components.
     * key: "gui.exchange." + subKey
     */
    public static MutableComponent gui(String subKey, Object... args) {
        return Component.translatable("gui.exchange." + subKey, args);
    }

    /**
     * Convenience method for chat and message translation components.
     * key: "message.exchange." + subKey
     */
    public static MutableComponent message(String subKey, Object... args) {
        return Component.translatable("message.exchange." + subKey, args);
    }

    /**
     * Convenience method for item/block tooltips.
     * key: "tooltip.exchange." + subKey
     */
    public static MutableComponent tooltip(String subKey, Object... args) {
        return Component.translatable("tooltip.exchange." + subKey, args);
    }

    /**
     * Returns a localized plain String.
     * Uses client I18n when available on client, falls back to Component.translatable or key.
     */
    public static String str(String key, Object... args) {
        if (!i18nLookupAttempted) {
            i18nLookupAttempted = true;
            try {
                Class<?> i18nClass = Class.forName("net.minecraft.client.resources.language.I18n");
                i18nGetMethod = i18nClass.getMethod("get", String.class, Object[].class);
            } catch (Throwable ignored) {}
        }

        if (i18nGetMethod != null) {
            try {
                return (String) i18nGetMethod.invoke(null, key, args);
            } catch (Throwable ignored) {}
        }

        try {
            return Component.translatable(key, args).getString();
        } catch (Throwable ignored) {}

        return key;
    }

    /**
     * Returns a localized plain String for GUI elements.
     */
    public static String guiStr(String subKey, Object... args) {
        return str("gui.exchange." + subKey, args);
    }

    /**
     * Returns a localized plain String for messages.
     */
    public static String messageStr(String subKey, Object... args) {
        return str("message.exchange." + subKey, args);
    }

    /**
     * Formats a notification payload string that will be translated by translateNotification on client.
     */
    public static String notify(String key, Object... args) {
        if (args == null || args.length == 0) {
            return "key:" + key;
        }
        StringBuilder sb = new StringBuilder("key:").append(key);
        for (Object arg : args) {
            sb.append(';').append(arg != null ? arg.toString() : "");
        }
        return sb.toString();
    }

    /**
     * Parses notification messages sent from server. If prefixed with "key:",
     * decodes the translation key and parameters and returns the localized string.
     */
    public static String translateNotification(String msg) {
        if (msg == null || msg.isEmpty()) return "";
        if (msg.startsWith("key:")) {
            String[] parts = msg.substring(4).split(";", -1);
            String key = parts[0];
            Object[] args = new Object[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, parts.length - 1);

            String res = str(key, args);
            if (res.equals(key) && !key.startsWith("gui.exchange.") && !key.startsWith("message.exchange.")) {
                String candidateMsg = str("message.exchange." + key, args);
                if (!candidateMsg.equals("message.exchange." + key)) {
                    return candidateMsg;
                }
                String candidateGui = str("gui.exchange." + key, args);
                if (!candidateGui.equals("gui.exchange." + key)) {
                    return candidateGui;
                }
            }
            return res;
        }
        return msg;
    }
}
