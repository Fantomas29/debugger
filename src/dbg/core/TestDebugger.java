package dbg.core; // 1

public class TestDebugger { // 3
    private int counter = 0; // 4

    public void methodWithLoop() { // 6
        System.out.println("Debut de methodWithLoop"); // 7
        for(int i = 0; i < 3; i++) { // 8
            counter++; // 9
            processIteration(i); // 10
        } // 11
        for(int i = 0; i < 5; i++) { // 12
            counter++; // 13
            processIteration(i); // 14
        } // 15
        System.out.println("Fin de methodWithLoop"); // 16
    } // 17

    private void processIteration(int value) { // 19
        String message = "Traitement de l'iteration " + value; // 20
        System.out.println(message); // 21
        helperMethod(value); // 22
    } // 23

    private void helperMethod(int value) { // 25
        value = counter + value; // 26
        System.out.println("value = " + value); // 27
    } // 28

    public static void main(String[] args) { // 30
        TestDebugger test = new TestDebugger(); // 31
        test.methodWithLoop(); // 32
    } // 33
} // 34