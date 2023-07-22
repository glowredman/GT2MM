package glowredman.modularmaterials.data.object.sub;

public class ColorProperties {

    public int red = 255;
    public int green = 255;
    public int blue = 255;
    public int alpha = 255;
    public PulseMode pulseModeRed = PulseMode.NONE;
    public PulseMode pulseModeGreen = PulseMode.NONE;
    public PulseMode pulseModeBlue = PulseMode.NONE;
    public PulseMode pulseModeAlpha = PulseMode.NONE;
    public PulseMode pulseModeMoltenRed = PulseMode.NONE;
    public PulseMode pulseModeMoltenGreen = PulseMode.NONE;
    public PulseMode pulseModeMoltenBlue = PulseMode.NONE;
    public PulseMode pulseModeMoltenAlpha = PulseMode.NONE;

    @Override
    public String toString() {
        return String.format("{red: %d, green: %d, blue: %d, alpha: %d}", red, green, blue, alpha);
    }

}
