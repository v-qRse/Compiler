package ovm;

public class OvmCodeGenerator {
   private static int ip = 0;

   public static int getIp() {
      return ip;
   }

   public static void setIp(int i) {
      if (i < 0 || i > OVM.SIZE) {
         //-*-
         throw new Error("Неверное задание ip");
      }
      ip = i;
   }

   public static void command(int value) {
      if (ip >= OVM.SIZE) {
         //-*- ошибка
      } else if (ip < 0) {
         //-*- ошибка
      }
      OVM.memory[ip] = value;
      ip++;
   }

   public static void command(Command command) {
      command(command.getValue());
   }

   public static void constant(long value) {
      if (value >= 0 && value <= Integer.MAX_VALUE) {
         command((int)value);
      } else if (value > Integer.MIN_VALUE) {
         command((int) -value);
         command(Command.NEG.getValue());
      } else {
         command(Integer.MAX_VALUE);
         command(Command.NEG.getValue());
         command(1);
         command(Command.SUB.getValue());
      }
   }

   public static int evaluateExpression (int ipBefore) {
      return evaluateExpression(ipBefore, true);
   }

   public static int evaluateExpression (int ipBefore, boolean gen) {
      int ipAfter = ip;
      int res = OVM.run(ipBefore, ipAfter, false);
      ip = ipBefore;
      if (gen) {
         command(res);
      }
      return res;
   }
}
