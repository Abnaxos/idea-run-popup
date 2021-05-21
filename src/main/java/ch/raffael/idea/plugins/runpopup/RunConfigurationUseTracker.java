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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Track the use of run configurations. Used to sort run configurations in the
 * popup menu by last used.
 *
 * @author Raffael Herzog
 */
@Service
@State(name = "ch.raffael.plugins.idea.runpopup.RunConfigurationUseTracker",
        storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public final class RunConfigurationUseTracker implements PersistentStateComponent<RunConfigurationUseTracker.State> {

    private final Object stateLock = new Object();
    private final Project project;
    private State state = new State();

    private final Map<String, Integer> runningConfigurations = new ConcurrentHashMap<>(4, .7f, 1);

    public RunConfigurationUseTracker(Project project) {
        this.project = project;
    }

    @NotNull
    static RunConfigurationUseTracker runConfigurationUseTracker(Project project) {
        return Objects.requireNonNull(project.getService(RunConfigurationUseTracker.class),
                "project.getService(RunConfigurationUseTracker.class)");
    }

    void touchRunConfiguration(@NotNull String confId, @NotNull String executorId) {
        synchronized (stateLock) {
            RunConfInfo rci = state.runConfInfo.computeIfAbsent(confId, (k) -> new RunConfInfo(confId, executorId));
            rci.timestamp = System.currentTimeMillis();
            rci.executorId = executorId;
            cleanupObsoleteStateEntries();
        }
    }

    @Nullable
    String getLastRunExecutorId(String confId) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            if (info == null) {
                return null;
            } else {
                return info.executorId;
            }
        }
    }

    long getLastRunTimestamp(String confId) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            return info == null ? Long.MIN_VALUE : info.timestamp;
        }
    }

    boolean isRunning(String confId) {
        return runningConfigurations.containsKey(confId);
    }

    boolean isFavorite(String confId) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            return info != null && info.favorite && !info.helper;
        }
    }

    void setFavorite(String confId, boolean favorite) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            if (info == null) {
                if (favorite) {
                    info = new RunConfInfo(confId, null);
                    info.favorite = true;
                    state.runConfInfo.put(confId, info);
                }
            } else {
                info.favorite = favorite;
                if (favorite) {
                    info.helper = false;
                }
            }
        }
    }

    boolean isHelper(String confId) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            return info != null && info.helper && !info.favorite;
        }
    }

    void setHelper(String confId, boolean helper) {
        synchronized (stateLock) {
            RunConfInfo info = state.runConfInfo.get(confId);
            if (info == null) {
                if (helper) {
                    info = new RunConfInfo(confId, null);
                    info.helper = false;
                    state.runConfInfo.put(confId, info);
                }
            } else {
                info.helper = helper;
                if (info.helper) {
                    info.favorite = false;
                }
            }
        }
    }

    boolean isOrderFavoritesByLastUsed() {
        synchronized (stateLock) {
            return state.orderFavoritesByLastUsed;
        }
    }

    void setOrderFavoritesByLastUsed(boolean enabled) {
        synchronized (stateLock) {
            state.orderFavoritesByLastUsed = enabled;
        }
    }

    boolean isOrderOthersByLastUsed() {
        synchronized (stateLock) {
            return state.orderOthersByLastUsed;
        }
    }

    void setOrderOthersByLastUsed(boolean enabled) {
        synchronized (stateLock) {
            state.orderOthersByLastUsed = enabled;
        }
    }

    boolean isLastUsedOnTop() {
        synchronized (stateLock) {
            return state.lastUsedOnTop;
        }
    }

    void setLastUsedOnTop(boolean enabled) {
        synchronized (stateLock) {
            state.lastUsedOnTop = enabled;
        }
    }

    @Override
    public State getState() {
        synchronized (stateLock) {
            return new State(state);
        }
    }

    @Override
    public void loadState(State state) {
        synchronized (stateLock) {
            this.state = new State(state);
        }
    }

    private void cleanupObsoleteStateEntries() {
        Set<String> knownConfIds = RunManager.getInstance(project).getAllSettings().stream()
                .map(RunnerAndConfigurationSettings::getUniqueID)
                .collect(Collectors.toSet());
        synchronized (stateLock) {
            state.runConfInfo.values().removeIf(rci -> !knownConfIds.contains(rci.confId));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static final class State {

        public boolean orderFavoritesByLastUsed = true;
        public boolean orderOthersByLastUsed = true;
        public boolean lastUsedOnTop = true;
        public Map<String, RunConfInfo> runConfInfo = new HashMap<>();

        public State() {
        }

        public State(State that) {
            this.orderFavoritesByLastUsed = that.orderFavoritesByLastUsed;
            this.orderOthersByLastUsed = that.orderOthersByLastUsed;
            this.lastUsedOnTop = that.lastUsedOnTop;
            that.runConfInfo.values().stream()
                    .map(RunConfInfo::new)
                    .forEach((rci) -> this.runConfInfo.put(rci.confId, rci));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static final class RunConfInfo {

        public String confId;
        @Nullable
        public String executorId;
        public long timestamp = System.currentTimeMillis();
        public boolean favorite = false;
        public boolean helper = false;

        @SuppressWarnings("unused")
        public RunConfInfo() {
        }

        public RunConfInfo(String confId, @Nullable String executorId) {
            this.confId = confId;
            this.executorId = executorId;
        }

        public RunConfInfo(RunConfInfo that) {
            this.confId = that.confId;
            this.executorId = that.executorId;
            this.timestamp = that.timestamp;
            this.favorite = that.favorite;
            this.helper = that.helper;
        }
    }

    public static class MyExecutionListener implements ExecutionListener {
        @Override
        public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
            if (env.getRunnerAndConfigurationSettings() != null) {
                String confId = env.getRunnerAndConfigurationSettings().getUniqueID();
                runConfigurationUseTracker(env.getProject()).touchRunConfiguration(confId, executorId);
            }
        }

        @Override
        public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
            updateRunConfCount(env.getRunnerAndConfigurationSettings(), 1);
        }

        @Override
        public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
            updateRunConfCount(env.getRunnerAndConfigurationSettings(), -1);
        }

        private void updateRunConfCount(@Nullable RunnerAndConfigurationSettings config, int delta) {
            if (config == null) {
                return;
            }
            runConfigurationUseTracker(config.getConfiguration().getProject()).runningConfigurations
                    .compute(config.getUniqueID(), (k, v) -> {
                        int result = (v == null ? delta : v + delta);
                        return result <= 0 ? null : result;
                    });
        }
    }
}
