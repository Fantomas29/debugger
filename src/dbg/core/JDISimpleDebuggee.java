package dbg.core; // 1

import java.util.ArrayList; // 3
import java.util.List; // 4

public class JDISimpleDebuggee { // 6

    public static void main(String[] args) { // 8
        JDISimpleDebuggee demo = new JDISimpleDebuggee(); // 9
        List<Integer>tableau = new ArrayList<>(); // 10
        tableau.add(1); // 11
        tableau.add(2); // 12
        tableau.add(3); // 13
        tableau.add(4); // 14
        tableau.add(5); // 15
        tableau.add(6); // 16
        tableau.add(7); // 17
        System.out.println(tableau); // 18
    } // 19
} // 20