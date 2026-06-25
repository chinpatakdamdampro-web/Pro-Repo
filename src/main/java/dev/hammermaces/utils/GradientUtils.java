package dev.hammermaces.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.awt.*;

/**
 * Utility for building animated gradient name components and parsing lore strings.
 */
public class GradientUtils {

    /**
     * Builds an animated gradient component with a sine-wave shimmer effect.
     * The offset parameter (0.0 - 1.0) shifts the phase of the wave each tick,
     * creating a living, moving gradient across each character of the text.
     *
     * @param text       The display name text (supports small caps unicode directly)
     * @param hexStart   Start color e.g. "#1a1a6e"
     * @param hexEnd     End color e.g. "#00e5ff"
     * @param offset     Animation phase offset, incremented per tick by AnimationManager
     */
    public static Component buildAnimatedGradient(String text, String hexStart, String hexEnd, float offset) {
        Color start = Color.decode(hexStart);
        Color end   = Color.decode(hexEnd);

        Component result = Component.empty();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // Skip spaces — don't color them, just append
            if (c == ' ') {
                result = result.append(Component.text(" "));
                continue;
            }

            float position = (float) i / Math.max(length - 1, 1);
            // Sine wave across the text + shifting phase = shimmer
            float wave = (float) ((Math.sin((position + offset) * Math.PI * 2.0) + 1.0) / 2.0);

            int r = clamp((int) (start.getRed()   + wave * (end.getRed()   - start.getRed())));
            int g = clamp((int) (start.getGreen() + wave * (end.getGreen() - start.getGreen())));
            int b = clamp((int) (start.getBlue()  + wave * (end.getBlue()  - start.getBlue())));

            result = result.append(
                Component.text(String.valueOf(c))
                    .color(TextColor.color(r, g, b))
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, false)
            );
        }

        return result;
    }

    /**
     * Parse a lore string with & color codes into an Adventure Component.
     * Always removes italic decoration so lore doesn't look bad in-game.
     */
    public static Component parseLore(String line) {
        return LegacyComponentSerializer.legacyAmpersand()
            .deserialize(line)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Parse a hex color string like "#00e5ff" into a TextColor.
     */
    public static TextColor hex(String hex) {
        return TextColor.fromHexString(hex);
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
