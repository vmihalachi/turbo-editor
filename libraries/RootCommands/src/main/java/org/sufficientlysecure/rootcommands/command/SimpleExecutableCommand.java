

package org.sufficientlysecure.rootcommands.command;

import android.content.Context;

public class SimpleExecutableCommand extends ExecutableCommand {
    private StringBuilder sb = new StringBuilder();

    public SimpleExecutableCommand(Context context, String executableName, String parameters) {
        super(context, executableName, parameters);
    }

    @Override
    public void output(int id, String line) {
        sb.append(line).append('\n');
    }

    @Override
    public void afterExecution(int id, int exitCode) {
    }

    public String getOutput() {
        return sb.toString();
    }

    public int getExitCode() {
        return exitCode;
    }

}