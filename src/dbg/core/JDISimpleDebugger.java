package dbg.core;

public class JDISimpleDebugger {
    public static void main(String[] args) throws Exception {

        ScriptableDebugger debuggerInstance = new ScriptableDebugger();
        debuggerInstance.attachTo(TestDebugger.class);

    }
}

