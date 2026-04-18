import java.util.HashMap;

public class LexemeScanner {
   private char ch;
   private String identifier = "";
   private long value;

   private int lexStartPos = -1;
   private int lexCurrentPos = 0;
   private int lexLine = 1;

   private final HashMap<String, Lexeme> keyWord;

   LexemeScanner() {
      nextCh();

      keyWord = new HashMap<>();

      //MODULE, IMPORT,CONST, TYPE, VAR, BEGIN, END,
      keyWord.put("MODULE", Lexeme.MODULE);
      keyWord.put("IMPORT", Lexeme.IMPORT);
      keyWord.put("CONST", Lexeme.CONST);
      keyWord.put("TYPE", Lexeme.TYPE);
      keyWord.put("VAR", Lexeme.VAR);
      keyWord.put("BEGIN", Lexeme.BEGIN);
      keyWord.put("END", Lexeme.END);

      //ARRAY, POINTER, PROCEDURE, RECORD
      keyWord.put("ARRAY", Lexeme.ARRAY);
      keyWord.put("POINTER", Lexeme.POINTER);
      keyWord.put("PROCEDURE", Lexeme.PROCEDURE);
      keyWord.put("RECORD", Lexeme.RECORD);

      //INTEGER, BOOLEAN
      keyWord.put("INTEGER", Lexeme.INTEGER);
      keyWord.put("BOOLEAN", Lexeme.BOOLEAN);

      //IF, THEN, ELSE, ELSIF, CASE
      keyWord.put("IF", Lexeme.IF);
      keyWord.put("THEN", Lexeme.THEN);
      keyWord.put("ELSE", Lexeme.ELSE);
      keyWord.put("ELSIF", Lexeme.ELSIF);
      keyWord.put("CASE", Lexeme.CASE);

      //WHILE, DO, LOOP, FOR, REPEAT, UNTIL,
      keyWord.put("WHILE", Lexeme.WHILE);
      keyWord.put("DO", Lexeme.DO);
      keyWord.put("LOOP", Lexeme.LOOP);
      keyWord.put("FOR", Lexeme.FOR);
      keyWord.put("REPEAT", Lexeme.REPEAT);
      keyWord.put("UNTIL", Lexeme.UNTIL);

      //NIL
      keyWord.put("NIL", Lexeme.NIL);

      //IN, IS, BY, WITH, OF, TO
      keyWord.put("IN", Lexeme.IN);
      keyWord.put("IS", Lexeme.IS);
      keyWord.put("BY", Lexeme.BY);
      keyWord.put("WITH", Lexeme.WITH);
      keyWord.put("OF", Lexeme.OF);
      keyWord.put("TO", Lexeme.TO);

      //EXIT, RETURN
      keyWord.put("EXIT", Lexeme.EXIT);
      keyWord.put("RETURN", Lexeme.RETURN);

      //DIV MOD
      keyWord.put("DIV", Lexeme.DIV);
      keyWord.put("MOD", Lexeme.MOD);
   }

   private void nextCh() {
      ch = TextReader.nextCh();
      lexCurrentPos++;
   }

   public void error(String message) {
      TextReader.error(message, lexLine, lexStartPos);
   }

   private Lexeme scannerName() {
      StringBuilder name = new StringBuilder();
      name.append(ch);
      nextCh();

      while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
         name.append(ch);
         nextCh();
      }

      identifier = name.toString();

      if (keyWord.containsKey(identifier)) {
         return keyWord.get(identifier);
      }
      return Lexeme.NAME;
   }

   private Lexeme scannerNumber() {
      value = (int) ch - (int) '0';
      nextCh();

      while (ch >= '0' && ch <= '9') {
         int buf = (int) ch - (int) '0';

         //TODO нужно ли это делать?
         if (value < Integer.MAX_VALUE/10 || value == Integer.MAX_VALUE && buf <= Integer.MAX_VALUE % 10 + 1) {
            value = value*10 + buf;
         } else {
            error("Слишком большое значение");
         }
         nextCh();
      }

      return Lexeme.NUMBER;
   }

   private void skipComment() {
      while (true) {
         if (ch == TextReader.chEOT) {
            error("Ожидается конец комментария");
         } else if (ch == '*') {
            nextCh();
            if (ch == ')') {
               nextCh();
               break;
            }
         } else if (ch == '(') {
            nextCh();
            if (ch == '*') {
               nextCh();
               skipComment();
            }
         } else {
            nextCh();
         }
      }
   }

   public long getValue() {
      return value;
   }

   public String getIdentifier() {
      return identifier;
   }

   public long[] getPosition() {
      return new long[] {lexLine, lexStartPos};
   }

   public Lexeme nextLexeme() {
      while (ch == '\n' || ch == '\t' || ch == ' ' || ch == '\r') {
         if (ch == '\n') {
            lexLine++;
            lexCurrentPos = 0;
         }
         nextCh();
      }
      lexStartPos = lexCurrentPos;

      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
         return scannerName();
      } else if (ch >= '0' && ch <= '9') {
         return scannerNumber();
      } else if (ch == ':') {
         nextCh();
         if (ch == '=') {
            nextCh();
            return Lexeme.ASSIGN;
         }
         return Lexeme.COLON;
      } else if (ch == ';') {
         nextCh();
         return Lexeme.SEMICOLON;
      } else if (ch == '(') {
         nextCh();
         if (ch == '*') {
            nextCh();
            skipComment();
            return nextLexeme();
         }
         return Lexeme.LEFT_PAR;
      } else if (ch == ')') {
         nextCh();
         return Lexeme.RIGHT_PAR;
      } else if (ch == '[') {
         nextCh();
         return Lexeme.LEFT_BRACKET;
      } else if (ch == ']') {
         nextCh();
         return Lexeme.RIGHT_BRACKET;
      } else if (ch == '=') {
         nextCh();
         return Lexeme.EQ;
      } else if (ch == '#') {
         nextCh();
         return Lexeme.NE;
      } else if (ch == '>') {
         nextCh();
         if (ch == '=') {
            nextCh();
            return Lexeme.GE;
         }
         return Lexeme.GT;
      } else if (ch == '<') {
         nextCh();
         if (ch == '=') {
            nextCh();
            return Lexeme.LE;
         }
         return Lexeme.LT;
      } else if (ch == '+') {
         nextCh();
         return Lexeme.PLUS;
      } else if (ch == '-') {
         nextCh();
         return Lexeme.MINUS;
      } else if (ch == '*') {
         nextCh();
         return Lexeme.MULTIPLY;
      } else if (ch == '.') {
         nextCh();
         return Lexeme.DOT;
      } else if (ch == ',') {
         nextCh();
         return Lexeme.COMMA;
      } else if (ch == TextReader.chEOT) {
         return Lexeme.EOT;
      }
      return Lexeme.NONE;
   }
}
