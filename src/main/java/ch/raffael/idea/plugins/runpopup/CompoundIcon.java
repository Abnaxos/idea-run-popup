/*
 * Copyright 2018 Raffael Herzog
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

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
