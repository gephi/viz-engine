package org.gephi.viz.engine.lwjgl.util.text;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public class GraphicsRenderer {

    public static void draw(final Graphics2D g2d,
        final String str,
        final int x,
        final int y) {

        Check.notNull(g2d, "Graphics cannot be null");
        Check.notNull(str, "String cannot be null");

        g2d.drawString(str, x, y);
    }

    public static void drawGlyphVector(final Graphics2D g2d,
        final GlyphVector gv,
        final int x,
        final int y) {

        Check.notNull(g2d, "Graphics cannot be null");
        Check.notNull(gv, "Glyph vector cannot be null");

        g2d.drawGlyphVector(gv, x, y);
    }

    public static Rectangle2D getBounds(final CharSequence text,
        final Font font,
        final FontRenderContext frc) {

        Check.notNull(text, "Text cannot be null");
        Check.notNull(font, "Font cannot be null");
        Check.notNull(frc, "Font render context cannot be null");

        return getBounds(text.toString(), font, frc);
    }

    public static Rectangle2D getBounds(final GlyphVector gv,
        final FontRenderContext frc) {

        Check.notNull(gv, "Glyph vector cannot be null");
        Check.notNull(frc, "Font render context cannot be null");

        return gv.getVisualBounds();
    }

    public static Rectangle2D getBounds(final String text,
        final Font font,
        final FontRenderContext frc) {

        Check.notNull(text, "Text cannot be null");
        Check.notNull(font, "Font cannot be null");
        Check.notNull(frc, "Font render context cannot be null");

        return getBounds(font.createGlyphVector(frc, text), frc);
    }
}
