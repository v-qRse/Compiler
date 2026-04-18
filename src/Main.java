import ovm.OVM;

public class Main {
   public static void main(String[] args) {
      Parser.parse("test.o");
      OVM.run();
   }
}