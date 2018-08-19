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

import javax.swing.Icon;

import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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
                        Extensions.getExtensions(Executor.EXECUTOR_EXTENSION_NAME)[0].getIcon()));
        this.runConfiguration = runConfiguration;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if ( project != null ) {
            RunConfigurationUseTracker useTracker = project.getComponent(RunConfigurationUseTracker.class);
            Executor executor = findExecutor(useTracker.getLastRunExecutorId(runConfiguration.getUniqueID()));
            ExecutionUtil.runConfiguration(runConfiguration, executor);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if ( project != null ) {
            RunConfigurationUseTracker useTracker = project.getComponent(RunConfigurationUseTracker.class);
            Executor executor = findExecutor(useTracker.getLastRunExecutorId(runConfiguration.getUniqueID()));
            if ( executor == null ) {
                e.getPresentation().setVisible(false);
                e.getPresentation().setEnabled(false);
            }
            else {
                Icon confIcon = runConfiguration.getConfiguration().getIcon();
                Icon executorIcon = executor.getIcon();
                try {
                    runConfiguration.checkSettings();
                }
                catch ( RuntimeConfigurationException e1 ) {
                    confIcon = (new LayeredIcon(confIcon,
                            AllIcons.RunConfigurations.InvalidConfigurationLayer));
                }
                //if ( runConfiguration.isTemporary() ) {
                //    confIcon = getTemporaryIcon(confIcon);
                //}
                if ( useTracker.isRunning(runConfiguration.getUniqueID()) ) {
                    //confIcon = ExecutionUtil.getLiveIndicator(confIcon);
                    executorIcon = ExecutionUtil.getLiveIndicator(executorIcon);
                }
                e.getPresentation().setIcon(new CompoundIcon(confIcon, executorIcon));
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
        Executor[] executors = Extensions.getExtensions(Executor.EXECUTOR_EXTENSION_NAME);
        if ( id != null ) {
            for ( Executor executor : executors ) {
                if ( executor.getId().equals(id) ) {
                    return executor;
                }
            }
        }
        else {
            for ( Executor executor : executors ) {
                if ( canRunWith(executor.getId()) ) {
                    return executor;
                }
            }
        }
        return null;
    }

    private boolean canRunWith(String executorId) {
        ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, runConfiguration);
        return runner != null && runner.canRun(executorId, runConfiguration.getConfiguration());
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Executor[] executors = Extensions.getExtensions(Executor.EXECUTOR_EXTENSION_NAME);
        List<AnAction> children = new ArrayList<>();
        for ( Executor executor : executors ) {
            children.add(new AnAction(executor.getStartActionText(), null, executor.getIcon()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    ExecutionUtil.runConfiguration(runConfiguration, executor);
                }

                @Override
                public void update(AnActionEvent e) {
                    e.getPresentation().setEnabled(canRunWith(executor.getId()));
                }
            });
        }
        children.add(new Separator());
        children.add(new ToggleAction("Favorite") {
            @Override
            public boolean isSelected(AnActionEvent e) {
                Project project = e.getProject();
                //noinspection SimplifiableIfStatement
                if ( project == null ) {
                    return false;
                }
                return project.getComponent(RunConfigurationUseTracker.class).isFavorite(runConfiguration.getUniqueID());
            }
            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                Project project = e.getProject();
                if ( project != null ) {
                    project.getComponent(RunConfigurationUseTracker.class).setFavorite(runConfiguration.getUniqueID(), state);
                }
            }
        });
        return children.toArray(new AnAction[children.size()]);
    }

    @Override
    public boolean canBePerformed(DataContext context) {
        return true;
    }

    @Override
    public boolean isPopup() {
        return true;
    }
}
