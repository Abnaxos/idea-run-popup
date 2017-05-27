package ch.raffael.idea.plugins.runpopup;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;


/**
 * An icon that paints two icons side by side.
 *
 * @author Raffael Herzog
 */
class CompoundIcon implements Icon {

    private static final int GAP = 2;

    private final Icon left;
    private final Icon right;

    CompoundIcon(Icon left, Icon right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        left.paintIcon(c, g, x, y);
        right.paintIcon(c, g, x + GAP + left.getIconWidth(), y);
    }

    @Override
    public int getIconWidth() {
        return left.getIconWidth() + GAP + right.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return Math.max(left.getIconHeight(), right.getIconHeight());
    }
}
