package ovm;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class OVM {
   public static final int  SIZE = 512*512;
   public static final int[] memory = new int[SIZE + 1];

   public static int run(boolean withException) {
      return run(0, -1, withException);
   }

   public static int run(int ip, boolean withException) {
      return run(ip, -1, withException);
   }

   private static int stackPointer;
   private static Deque<Integer> inputDeque;

   private static void error(String message) {
      System.out.println("Ошибка при исполнении программы: " +  message);
      System.exit(2);
   }

   private static void checkInt(long value) {
      if (!(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE)) {
         //-*-
         error("переполнение целого");
      }
   }

   private static int readMemory(int address) {
      if (!(address >= 0 && address <= SIZE)) {
         error("считывание из несуществующей ячейки памяти" + address);
      }
      return memory[address];
   }

   private static void writeMemory(int address, long value) {
      if (!(address >= 0 && address <= SIZE)) {
         error("запись в несуществующую ячейку памяти" + address);
      }
      checkInt(value);
      memory[address] = (int) value;
   }

   private static int pop() {
      int buf = readMemory(stackPointer);
      stackPointer++;
      return buf;
   }

   private static void push(long value) {
      stackPointer--;
      writeMemory(stackPointer, value);
   }

   public static int run() {
      return run(0, -1, true);
   }

   public static int run(int ip, int endIp, boolean withException) {
      StreamTokenizer in = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));
      stackPointer = SIZE;
      inputDeque = new ArrayDeque<>();

      while (true) {
         if (ip == endIp) {
            return pop();
         }
         int command = readMemory(ip);
         ip++;
         if (command >= 0) {
            push(command);
         } else if (command == Command.LOAD.getValue()) {
            int adr = pop();
            push(readMemory(adr));
         } else if (command == Command.STORE.getValue()) {
            int val = pop();
            int adr = pop();
            writeMemory(adr, val);
         } else if (command == Command.DUPLICATE.getValue()) {
            int val = pop();
            push(val);
            push(val);
         } else if (command == Command.ADD.getValue()) {
            int val2 = pop();
            int val1 = pop();
            push(val1 + val2);
         } else if (command == Command.SUB.getValue()) {
            int val2 = pop();
            int val1 = pop();
            push(val1 - val2);
         } else if (command == Command.MUL.getValue()) {
            int val2 = pop();
            int val1 = pop();
            push((long) val1 * val2);
         } else if (command == Command.DIV.getValue()) {
            int val2 = pop();
            int val1 = pop();
            if (val2 <= 0) {
               error("деление на число <= 0");
            }
            push(val1 / val2);
         } else if (command == Command.MOD.getValue()) {
            int val2 = pop();
            int val1 = pop();
            if (val2 <= 0) {
               error("остаток от деления на число <= 0");
            }
            long buf = val1 % val2;
            push(buf < 0 ? val2 + buf : buf);
         } else if (command == Command.NEG.getValue()) {
            int val = pop();
            push(-val);
         } else if (command == Command.IF_LT.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 < val2) {
               ip = adr;
            }
         } else if (command == Command.IF_LE.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 <= val2) {
               ip = adr;
            }
         } else if (command == Command.IF_GT.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 > val2) {
               ip = adr;
            }
         } else if (command == Command.IF_GE.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 >= val2) {
               ip = adr;
            }
         } else if (command == Command.IF_EQ.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 == val2) {
               ip = adr;
            }
         } else if (command == Command.IF_NE.getValue()) {
            int adr = pop();
            int val2 = pop();
            int val1 = pop();
            if (val1 != val2) {
               ip = adr;
            }
         } else if (command == Command.GOTO.getValue()) {
            int adr = pop();
            ip = adr;
         } else if (command == Command.IN_INT.getValue()) {
            //TODO сделать
            int adr = pop();
            while (inputDeque.isEmpty()) {
               try {
                  in.nextToken();
                  inputDeque.add((int) in.nval);
               } catch (IOException e) {
                  throw new RuntimeException(e);
               }
            }
         } else if (command == Command.OUT_INT.getValue()) {
            int wight = pop();
            int val = pop();
            int buf = String.valueOf(val).length();
            StringBuilder space = new StringBuilder();
            while (buf < wight) {
               space.append(" ");
               buf++;
            }
            System.out.print(space.toString() + val);
         } else if (command == Command.OUT_LN.getValue()) {
            System.out.println();
         } else if (command == Command.HALT.getValue()) {
            int val = pop();
            System.out.printf("%n Программа завершена с кодом ошибки %s.", val);
            break;
         } else if (command == Command.LOAD_SP.getValue()) {
            int shift = pop();
            push(readMemory(stackPointer + shift));
         } else if (command == Command.DROP.getValue()) {
            pop();
         } else {
            error("недопустимая команда: " + command);
         }
      }
      //заглушка
      return -1;
   }
}
