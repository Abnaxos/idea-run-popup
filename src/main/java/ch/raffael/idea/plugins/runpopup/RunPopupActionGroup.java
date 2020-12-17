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
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static ch.raffael.idea.plugins.runpopup.RunConfigurationUseTracker.runConfigurationUseTracker;


/**
 * Action group for the run popup.
 *
 * @author Raffael Herzog
 */
class RunPopupActionGroup extends ActionGroup {

    private static final Logger LOG = Logger.getInstance(RunPopupActionGroup.class);

    private static final AnAction[] NO_CHILDREN = new AnAction[0];
    private static final String EDIT_RUN_CONFIGURATIONS_ACTION_ID = "editRunConfigurations";

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
        RunConfigurationUseTracker useTracker = runConfigurationUseTracker(project);
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
        children.add(new ActionGroup("Options", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return new AnAction[] {
                        new ToggleAction("Order Favorites by Last Used") {
                            @Override
                            public boolean isSelected(AnActionEvent e) {
                                Project project = e.getProject();
                                //noinspection SimplifiableIfStatement
                                if (project != null) {
                                    return runConfigurationUseTracker(project).isOrderFavoritesByLastUsed();
                                } else {
                                    return false;
                                }
                            }

                            @Override
                            public void setSelected(AnActionEvent e, boolean state) {
                                Project project = e.getProject();
                                if (project != null) {
                                    runConfigurationUseTracker(project).setOrderFavoritesByLastUsed(state);
                                }
                            }
                        },
                        new ToggleAction("Order Others by Last Used") {
                            @Override
                            public boolean isSelected(AnActionEvent e) {
                                Project project = e.getProject();
                                //noinspection SimplifiableIfStatement
                                if (project != null) {
                                    return runConfigurationUseTracker(project).isOrderOthersByLastUsed();
                                } else {
                                    return false;
                                }
                            }

                            @Override
                            public void setSelected(AnActionEvent e, boolean state) {
                                Project project = e.getProject();
                                if (project != null) {
                                    runConfigurationUseTracker(project).setOrderOthersByLastUsed(state);
                                }
                            }
                        }};
            }
        });
        Optional.ofNullable(ActionManager.getInstance().getAction(EDIT_RUN_CONFIGURATIONS_ACTION_ID))
                .ifPresentOrElse(
                        children::add,
                        () -> LOG.warn("Action not found: " + EDIT_RUN_CONFIGURATIONS_ACTION_ID));
        return children.toArray(AnAction.EMPTY_ARRAY);
    }

    private boolean appendRunConfigurationActions(Project project, List<AnAction> target, boolean favorites, boolean sort) {
        RunConfigurationUseTracker tracker = runConfigurationUseTracker(project);
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
