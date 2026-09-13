package krys.itemimport.image;

/** Neutralny opis jednej strukturalnej linii poziomej w finalnym obrazie itemu. */
public final class HorizontalDivider {
    private final int y;
    private final int bandStartY;
    private final int bandEndY;
    private final double strength;

    public HorizontalDivider(int y, int bandStartY, int bandEndY, double strength) {
        if (bandStartY < 0 || bandEndY <= bandStartY) {
            throw new IllegalArgumentException("Pasmo separatora musi miec dodatni zakres Y.");
        }
        if (y < bandStartY || y >= bandEndY) {
            throw new IllegalArgumentException("Pozycja separatora musi lezec wewnatrz jego pasma.");
        }
        if (Double.isNaN(strength) || strength < 0.0d || strength > 1.0d) {
            throw new IllegalArgumentException("Sila separatora musi byc w zakresie 0..1.");
        }
        this.y = y;
        this.bandStartY = bandStartY;
        this.bandEndY = bandEndY;
        this.strength = strength;
    }

    public int getY() {
        return y;
    }

    public int getBandStartY() {
        return bandStartY;
    }

    public int getBandEndY() {
        return bandEndY;
    }

    public double getStrength() {
        return strength;
    }
}
