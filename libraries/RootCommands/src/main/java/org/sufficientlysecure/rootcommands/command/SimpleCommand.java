

package org.sufficientlysecure.rootcommands.command;

public class SimpleCommand extends Command {
    private StringBuilder sb = new StringBuilder();

    public SimpleCommand(String... command) {
        super(command);
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