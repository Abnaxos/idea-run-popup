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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Action group for the run popup.
 *
 * @author Raffael Herzog
 */
class RunPopupActionGroup extends ActionGroup {

    private static final AnAction[] NO_CHILDREN = new AnAction[0];

    @Nullable
    private Integer firstNonFavoriteIndex = null;

    RunPopupActionGroup() {
        super();
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if ( e == null ) {
            return NO_CHILDREN;
        }
        Project project = e.getProject();
        if ( project == null ) {
            return NO_CHILDREN;
        }
        RunConfigurationUseTracker useTracker = project.getComponent(RunConfigurationUseTracker.class);
        List<AnAction> children = new ArrayList<>();
        if ( appendRunConfigurationActions(project, children, true, useTracker.isOrderFavoritesByLastUsed()) ) {
            children.add(new Separator());
            firstNonFavoriteIndex = children.size();
        }
        else {
            firstNonFavoriteIndex = null;
        }
        if ( !appendRunConfigurationActions(project, children, false, useTracker.isOrderOthersByLastUsed()) ) {
            firstNonFavoriteIndex = null;
        }
        children.add(new Separator("Options"));
        children.add(new ToggleAction("Order Favorites by Last Used") {
            @Override
            public boolean isSelected(AnActionEvent e) {
                Project project = e.getProject();
                //noinspection SimplifiableIfStatement
                if ( project != null ) {
                    return project.getComponent(RunConfigurationUseTracker.class).isOrderFavoritesByLastUsed();
                }
                else {
                    return false;
                }
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                Project project = e.getProject();
                if ( project != null ) {
                    project.getComponent(RunConfigurationUseTracker.class).setOrderFavoritesByLastUsed(state);
                }
            }
        });
        children.add(new ToggleAction("Order Others by Last Used") {
            @Override
            public boolean isSelected(AnActionEvent e) {
                Project project = e.getProject();
                //noinspection SimplifiableIfStatement
                if ( project != null ) {
                    return project.getComponent(RunConfigurationUseTracker.class).isOrderOthersByLastUsed();
                }
                else {
                    return false;
                }
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                Project project = e.getProject();
                if ( project != null ) {
                    project.getComponent(RunConfigurationUseTracker.class).setOrderOthersByLastUsed(state);
                }
            }
        });
        return children.toArray(AnAction.EMPTY_ARRAY);
    }

    private boolean appendRunConfigurationActions(Project project, List<AnAction> target, boolean favorites, boolean sort) {
        RunConfigurationUseTracker tracker = project.getComponent(RunConfigurationUseTracker.class);
        Stream<RunnerAndConfigurationSettings> stream = RunManager.getInstance(project).getAllSettings().stream()
                .filter((c) -> tracker.isFavorite(c.getUniqueID()) == favorites);
        if ( sort ) {
            stream = stream.sorted((left, right) -> -Long.compare(
                    tracker.getLastRunTimestamp(left.getUniqueID()),
                    tracker.getLastRunTimestamp(right.getUniqueID())));
        }
        int prevSize = target.size();
        stream.map(RunConfActionGroup::new).forEach(target::add);
        return target.size() > prevSize;
    }

    @SuppressWarnings("unused")
    Optional<Integer> getFirstNonFavoriteIndex() {
        return Optional.ofNullable(firstNonFavoriteIndex);
    }
}
