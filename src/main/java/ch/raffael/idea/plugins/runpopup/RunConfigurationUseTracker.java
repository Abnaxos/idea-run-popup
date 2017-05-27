package ch.raffael.idea.plugins.runpopup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Track the use of run configurations. Used to sort run configurations in the
 * popup menu by last used.
 *
 * @author Raffael Herzog
 */
@State(name="ch.raffael.plugins.idea.betterRun.RunConfigurationUseTracker")
public class RunConfigurationUseTracker extends AbstractProjectComponent implements PersistentStateComponent<RunConfigurationUseTracker.State> {

    private final Object stateLock = new Object();
    private State state = new State();

    private final Map<String, Integer> runningConfigurations = new ConcurrentHashMap<>(4, .7f, 1);

    public RunConfigurationUseTracker(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        cleanupObsoleteStateEntries();
        MessageBusConnection conn = myProject.getMessageBus().connect();
        conn.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                if ( env.getRunnerAndConfigurationSettings() != null ) {
                    String confId = env.getRunnerAndConfigurationSettings().getUniqueID();
                    synchronized ( stateLock ) {
                        RunConfInfo rci = state.runConfInfo.computeIfAbsent(confId, (k) -> new RunConfInfo(confId, executorId));
                        rci.timestamp = System.currentTimeMillis();
                        rci.executorId = executorId;
                        cleanupObsoleteStateEntries();
                    }
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
                if ( config == null ) {
                    return;
                }
                runningConfigurations.compute(config.getUniqueID(), (k, v) -> {
                    Integer result;
                    if ( v == null ) {
                        result = delta;
                    }
                    else {
                        result = v + delta;
                    }
                    //noinspection ConstantConditions
                    return result <= 0 ? null : result;
                });
            }

        });
    }

    @Nullable
    String getLastRunExecutorId(String confId) {
        synchronized ( stateLock ) {
            RunConfInfo info = state.runConfInfo.get(confId);
            if ( info == null ) {
                return null;
            }
            else {
                return info.executorId;
            }
        }
    }

    long getLastRunTimestamp(String confId) {
        synchronized ( stateLock ) {
            RunConfInfo info = state.runConfInfo.get(confId);
            return info == null ? Long.MIN_VALUE : info.timestamp;
        }
    }

    boolean isRunning(String confId) {
        return runningConfigurations.containsKey(confId);
    }

    boolean isFavorite(String confId) {
        synchronized ( stateLock ) {
            RunConfInfo info = state.runConfInfo.get(confId);
            return info != null && info.favorite;
        }
    }

    void setFavorite(String confId, boolean favorite) {
        synchronized ( stateLock ) {
            RunConfInfo info = state.runConfInfo.get(confId);
            if ( info == null ) {
                if ( favorite ) {
                    info = new RunConfInfo(confId, null);
                    info.favorite = true;
                    state.runConfInfo.put(confId, info);
                }
            }
            else {
                info.favorite = favorite;
            }
        }
    }

    boolean isOrderFavoritesByLastUsed() {
        synchronized ( stateLock ) {
            return state.orderFavoritesByLastUsed;
        }
    }

    void setOrderFavoritesByLastUsed(boolean enabled) {
        synchronized ( stateLock ) {
            state.orderFavoritesByLastUsed = enabled;
        }     
    }

    boolean isOrderOthersByLastUsed() {
        synchronized ( stateLock ) {
            return state.orderOthersByLastUsed;
        }
    }

    void setOrderOthersByLastUsed(boolean enabled) {
        synchronized ( stateLock ) {
            state.orderOthersByLastUsed = enabled;
        }
    }

    @Nullable
    @Override
    public State getState() {
        synchronized ( stateLock ) {
            return new State(state);
        }
    }

    @Override
    public void loadState(State state) {
        synchronized ( stateLock ) {
            this.state = new State(state);
        }
    }

    private void cleanupObsoleteStateEntries() {
        Set<String> knownConfIds = RunManager.getInstance(myProject).getAllSettings().stream()
                .map(RunnerAndConfigurationSettings::getUniqueID)
                .collect(Collectors.toSet());
        synchronized ( stateLock ) {
            state.runConfInfo.values().removeIf(rci -> !knownConfIds.contains(rci.confId));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static final class State {
        public boolean orderFavoritesByLastUsed = true;
        public boolean orderOthersByLastUsed = true;
        public Map<String, RunConfInfo> runConfInfo = new HashMap<>();

        public State() {
        }
        public State(State that) {
            this.orderFavoritesByLastUsed = that.orderFavoritesByLastUsed;
            this.orderOthersByLastUsed = that.orderOthersByLastUsed;
            that.runConfInfo.values().stream()
                    .map(RunConfInfo::new)
                    .forEach((rci) -> this.runConfInfo.put(rci.confId, rci));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static final class RunConfInfo {
        public String confId;
        public String executorId;
        public long timestamp = System.currentTimeMillis();
        public boolean favorite = false;

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
        }
    }

}
