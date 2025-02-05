package dbg.core;

public class TestDebugger {
    private int counter = 0;

    public void methodWithLoop() {
        System.out.println("Debut de methodWithLoop");
        for(int i = 0; i < 3; i++) {
            counter++;
            processIteration(i);
        }
        for(int i = 0; i < 5; i++) {
            counter++;
            processIteration(i);
        }
        System.out.println("Fin de methodWithLoop");
    }

    private void processIteration(int value) {
        String message = "Traitement de l'iteration " + value;
        System.out.println(message);
        helperMethod(value);
    }

    private void helperMethod(int value) {
        value = counter + value;
        System.out.println("value = " + value);
    }

    public static void main(String[] args) {
        TestDebugger test = new TestDebugger();
        test.methodWithLoop();
    }
}