package decorator;

import com.google.common.base.Preconditions;

import java.awt.*;

import static java.util.Objects.nonNull;

/**
 * Created by mtumilowicz on 2018-11-30.
 */
public class ColorTransformers {
    static Color brighten(Color color, int modifier) {
        Preconditions.checkArgument(nonNull(color));
        Preconditions.checkArgument(modifier >= 0);

        return new Color(color.getRed());
    }

    static Color negate(Color color) {
        Preconditions.checkArgument(nonNull(color));

        return new Color(negate(red(color)), negate(green(color)), negate(blue(color)));

    }

    static private int negate(int color) {
        Preconditions.checkArgument(color <= 255);
        Preconditions.checkArgument(color >= 0);

        return 255 - color;
    }

    static private int red(Color color) {
        return color.getRed();
    }

    static private int green(Color color) {
        return color.getGreen();
    }

    static private int blue(Color color) {
        return color.getBlue();
    }
}