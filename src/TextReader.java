import java.io.FileReader;
import java.io.IOException;

public class TextReader {
   static final public char chEOT = '\u0000';
   private static FileReader fileReader;

   static void error(String message, int line, int pos) {
      System.out.println(message);
      System.out.printf("Строка ошибки %s, позиция начала неверной лексемы %s\n", line, pos);
      throw new Error(message);
   }

   public static char nextCh() {
      try {
         int charInt = fileReader.read();
         if (charInt == -1) {
            return chEOT;
         } else {
            return (char) charInt;
         }
      } catch (IOException e) {
         throw new Error();
      }
   }

   public static void init(String nameFile) {
      try {
         fileReader = new FileReader(nameFile);
      } catch (Exception e) {
         throw new Error("Файл невозможно считать");
      }
   }
}
