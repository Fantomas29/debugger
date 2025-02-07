package dbg.timetravel;

public record ExecutionState(
        String className,
        int lineNumber,
        String threadName,
        String methodName
) {
    @Override
    public String toString() {
        return String.format("%s.%s() line %d [thread: %s]",
                className, methodName, lineNumber, threadName);
    }
}