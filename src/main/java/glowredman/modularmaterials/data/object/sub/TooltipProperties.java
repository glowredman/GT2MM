package glowredman.modularmaterials.data.object.sub;

import java.util.Arrays;

public class TooltipProperties {

    public String background = Integer.toHexString(0xF0100010)
        .toUpperCase();
    public String borderStart = Integer.toHexString(0x505000FF)
        .toUpperCase();
    public String borderEnd = Integer.toHexString(0x5028007F)
        .toUpperCase();
    public String[] text = new String[0];

    @Override
    public String toString() {
        return String.format(
            "[background: %s, borderStart: %s, borderEnd: %s, text: %s]",
            background,
            borderStart,
            borderEnd,
            Arrays.toString(text));
    }

}
