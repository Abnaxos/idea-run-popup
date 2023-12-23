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
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.Icon;

import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static ch.raffael.idea.plugins.runpopup.RunConfigurationUseTracker.runConfigurationUseTracker;


/**
 * Action group for the submenu of each run configuration entry.
 *
 * @author Raffael Herzog
 */
class RunConfActionGroup extends ActionGroup {

    private final RunnerAndConfigurationSettings runConfiguration;

    RunConfActionGroup(RunnerAndConfigurationSettings runConfiguration) {
        super(runConfiguration.getName(), null,
                new CompoundIcon(runConfiguration.getType().getIcon(),
                        Executor.EXECUTOR_EXTENSION_NAME.getExtensionList().get(0).getIcon()));
        this.runConfiguration = runConfiguration;
        getTemplatePresentation().setPopupGroup(true);
        getTemplatePresentation().setPerformGroup(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if ( project != null ) {
            RunConfigurationUseTracker useTracker = runConfigurationUseTracker(project);
            Executor executor = findExecutor(useTracker.getLastRunExecutorId(runConfiguration.getUniqueID()));
            if (executor != null) {
                useTracker.touchRunConfiguration(runConfiguration.getUniqueID(), executor.getId());
                ExecutionUtil.runConfiguration(runConfiguration, executor);
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        var project = e.getProject();
        if ( project != null ) {
            var useTracker = runConfigurationUseTracker(project);
            var executor = findExecutor(useTracker.getLastRunExecutorId(runConfiguration.getUniqueID()));
            if ( executor == null ) {
                e.getPresentation().setVisible(false);
                e.getPresentation().setEnabled(false);
            }
            else {
                var confIcon = runConfiguration.getConfiguration().getIcon();
                var executorIcon = executor.getIcon();
                try {
                    runConfiguration.checkSettings();
                }
                catch ( RuntimeConfigurationException e1 ) {
                    confIcon = (LayeredIcon.create(confIcon,
                            AllIcons.RunConfigurations.InvalidConfigurationLayer));
                }
                if ( useTracker.isRunning(runConfiguration.getUniqueID()) ) {
                    executorIcon = ExecutionUtil.getLiveIndicator(executorIcon);
                }
                e.getPresentation().setIcon(
                        confIcon == null ? executorIcon : new CompoundIcon(confIcon, executorIcon));
                if ( runConfiguration.isTemporary() ) {
                    e.getPresentation().setIcon(getTemporaryIcon(e.getPresentation().getIcon()));
                }
                e.getPresentation().setText(runConfiguration.getName(), false);
            }
        }
    }

    private static Icon getTemporaryIcon(Icon icon) {
        return IconLoader.getTransparentIcon(icon, 0.3f);
    }

    @Nullable
    private Executor findExecutor(@Nullable String id) {
        return IdeaExecutors.findExecutor(runConfiguration, id);
    }

    private boolean canRunWith(String executorId) {
        return IdeaExecutors.canRunWith(runConfiguration, executorId);
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        var executors = Executor.EXECUTOR_EXTENSION_NAME.getExtensionList();
        List<AnAction> children = new ArrayList<>();
        for ( Executor executor : executors ) {
            children.add(new AnAction(executor.getStartActionText(), null, executor.getIcon()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    Project project = e.getProject();
                    if (project != null) {
                        RunConfigurationUseTracker useTracker = runConfigurationUseTracker(project);
                        useTracker.touchRunConfiguration(runConfiguration.getUniqueID(), executor.getId());
                        ExecutionUtil.runConfiguration(runConfiguration, executor);
                    }
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }

                @Override
                public void update(AnActionEvent e) {
                    e.getPresentation().setEnabled(canRunWith(executor.getId()));
                }
            });
        }
        children.add(new Separator());
        children.add(new FlagAction("Favorite",
                t -> t.isFavorite(runConfiguration.getUniqueID()),
                (t, s) -> t.setFavorite(runConfiguration.getUniqueID(), s)));
        children.add(new FlagAction("Helper",
                t -> t.isHelper(runConfiguration.getUniqueID()),
                (t, s) -> t.setHelper(runConfiguration.getUniqueID(), s)));
        return children.toArray(AnAction.EMPTY_ARRAY);
    }

    private static class FlagAction extends ToggleAction {
        private final Function<? super RunConfigurationUseTracker, Boolean> getter;
        private final BiConsumer<? super RunConfigurationUseTracker, Boolean> setter;

        private FlagAction(String text,
                           Function<? super RunConfigurationUseTracker, Boolean> getter,
                           BiConsumer<? super RunConfigurationUseTracker, Boolean> setter) {
            super(text);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            Project project = e.getProject();
            //noinspection SimplifiableIfStatement
            if ( project == null ) {
                return false;
            }
            return getter.apply(runConfigurationUseTracker(project));
        }
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            Project project = e.getProject();
            if ( project != null ) {
                setter.accept(runConfigurationUseTracker(project), state);
            }
        }
    }

}
