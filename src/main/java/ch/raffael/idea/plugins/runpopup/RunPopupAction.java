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

import javax.swing.JComponent;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;


/**
 * The main Run popup action.
 *
 * @author Raffael Herzog
 */
public class RunPopupAction extends AnAction implements CustomComponentAction{

    public RunPopupAction() {
        super("Run Popup", "Show a run popup menu",
                new LayeredIcon(AllIcons.Actions.Execute, AllIcons.General.Dropdown));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if ( project == null ) {
            return;
        }
        JComponent actionComponent = e.getPresentation().getClientProperty(COMPONENT_KEY);
        RunPopupActionGroup group = new RunPopupActionGroup();
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        ListPopup popup = popupFactory.createActionGroupPopup(
                actionComponent == null ? "Run" : null,
                group,
                dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        if ( actionComponent == null ) {
            popup.showCenteredInCurrentWindow(project);
        }
        else {
            popup.show(RelativePoint.getSouthWestOf(actionComponent));
        }
    }

    @Override
    public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        // TODO (2020-12-03) now we can handle places :)
        return new ActionButton(this, presentation, ActionPlaces.TOOLBAR, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    }
}
