/*
 * Copyright 2020 Raffael Herzog
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

import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.Nullable;


final class IdeaExecutors {

    private IdeaExecutors() {
    }

    @Nullable
    static Executor findExecutor(RunnerAndConfigurationSettings runConfiguration, @Nullable String id) {
        var executors = Executor.EXECUTOR_EXTENSION_NAME.getExtensionList();
        if ( id != null ) {
            for ( Executor executor : executors ) {
                if ( executor.getId().equals(id) ) {
                    return executor;
                }
            }
        }
        else {
            for ( Executor executor : executors ) {
                if ( canRunWith(runConfiguration, executor.getId()) ) {
                    return executor;
                }
            }
        }
        return null;
    }

    static boolean canRunWith(RunnerAndConfigurationSettings runConfiguration, String executorId) {
        var runner = ProgramRunner.getRunner(executorId, runConfiguration.getConfiguration());
        return runner != null && runner.canRun(executorId, runConfiguration.getConfiguration());
    }
}
