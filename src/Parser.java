import data.container.KindObject;
import data.container.Type;
import ovm.Command;
import ovm.OVM;
import ovm.OvmCodeGenerator;
import table.BuildId;
import table.Kind;
import table.Table;

import java.util.ArrayList;

public class Parser {
   private static final Type INTEGER = new Type("INTEGER", 1);
   private static final Type BOOLEAN = new Type("BOOLEAN", 1);

   private static LexemeScanner lexemeScanner;
   private static Lexeme lexeme;
   private static Table table = new Table();

   private static int lastVarAdr = OVM.SIZE;

   private static void error(String message) {
      lexemeScanner.error(message);
   }

   private static void nextLex() {
      lexeme = lexemeScanner.nextLexeme();
   }

   //TODO отладить skipLexeme, в разных местах используется по разному
   private static void skipLexeme(Lexeme skipLexeme) {
      if (lexeme != skipLexeme) {
         error("Ожидается: " + skipLexeme + ", а встечена: " + lexeme);
      }
      nextLex();
   }

   private static String getName() {
      return lexemeScanner.getIdentifier();
   }

   private static void importList() {
      while (true) {
         if (getName().equals("In")) {
            Table inTable = new Table();
            inTable.openScore();
            inTable.add("Open", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.IN_OPEN));
            inTable.add("Int", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.IN_INT));
            table.add("In", new KindObject(Kind.IMPORTED_MODULE, inTable));
         } else if (getName().equals("Out")) {
            Table outTable = new Table();
            outTable.openScore();
            outTable.add("Int", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.OUT_INT));
            outTable.add("Ln", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.OUT_LN));
            table.add("Out", new KindObject(Kind.IMPORTED_MODULE, outTable));
         } else {
            error("Данный модуль не поддерживается");
            break;
         }
         nextLex();

         if (lexeme == Lexeme.COMMA) {
            nextLex();
         } else {
            break;
         }
      }

      skipLexeme(Lexeme.SEMICOLON);
   }

   static private KindObject factor() {
      if (lexeme == Lexeme.NAME) {
         String name = getName();
         nextLex();

         KindObject nameKindObject = new KindObject(table.find(name));
         Type nameType;

         if (nameKindObject.kind == Kind.VAR) {
            OvmCodeGenerator.constant(nameKindObject.address);
            nameType = new Type(nameKindObject.type);
         } else {
            nameType = null;
         }

         while (lexeme == Lexeme.LEFT_BRACKET) {
            if (nameType == null || !nameType.type.equals("array")) {
               error("индексация применима только к массивам");
            }

            nextLex();
            intExpression();
            skipLexeme(Lexeme.RIGHT_BRACKET);
            OvmCodeGenerator.constant(nameType.t.size);
            OvmCodeGenerator.command(Command.MUL.getValue());
            OvmCodeGenerator.command(Command.ADD.getValue());

            nameType = new Type(nameType.t);
         }

         nameKindObject.type = nameType;//-*-

         if (lexeme == Lexeme.LEFT_PAR) {
            if (nameKindObject.kind != Kind.STANDARD_FUNCTION) {
               error("имя " + name + " не принадлежит функции");
            }
            nextLex();

            int ipBefore = OvmCodeGenerator.getIp();
            KindObject argKindObject = expression();

            int dimension;
            if (nameKindObject.id == BuildId.LEN && lexeme == Lexeme.COMMA) {
               nextLex();
               dimension = constIntExpression();
            } else {
               dimension = 0;
            }

            skipLexeme(Lexeme.RIGHT_PAR);

            switch (nameKindObject.id) {
               case ABS -> {
                  if (argKindObject.kind == Kind.CONST_EXPRESSION && argKindObject.type.equals(INTEGER)) {
                     //
                  } else if (argKindObject.kind == Kind.VAR && argKindObject.type.equals(INTEGER)) {
                     OvmCodeGenerator.command(Command.LOAD);
                     argKindObject.kind = Kind.GENERAL_EXPRESSION;
                  } else if (argKindObject.kind == Kind.GENERAL_EXPRESSION && argKindObject.type.equals(INTEGER)) {
                     //
                  } else {
                     error("недопустимый аргумент функции ABS");
                  }

                  OvmCodeGenerator.command(Command.DUPLICATE);
                  OvmCodeGenerator.constant(0);
                  OvmCodeGenerator.constant(OvmCodeGenerator.getIp());
                  OvmCodeGenerator.command(Command.IF_GE);
                  OvmCodeGenerator.command(Command.NEG);

                  if (argKindObject.kind == Kind.CONST_EXPRESSION && argKindObject.type.equals(INTEGER)) {
                     OvmCodeGenerator.evaluateExpression(ipBefore);
                     //-*-
                  }
                  return argKindObject;
               } case LEN -> {
                  if (!(argKindObject.kind == Kind.VAR && argKindObject.type.type.equals("array"))) {
                     error("Функция LEN применима только к переменым типа массив");
                  } else if (dimension < 0) {
                     error("второй агрумент функции LEN должен быть положительным");
                  }

                  Type argType = new Type(argKindObject.type);

                  while (dimension > 0 && argType.type.equals("array")) {
                     dimension--;
                     argType = new Type(argType.t);
                  }

                  if (dimension > 0 || !argType.type.equals("array")) {
                     error("слишком большая размерность (слишком большое значение второго агрумента)");
                  }

                  OvmCodeGenerator.setIp(ipBefore);
                  OvmCodeGenerator.constant(argType.length);

                  return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
               } case MAX -> {
                  if (argKindObject.kind == Kind.TYPE_NAME && argKindObject.type.equals(INTEGER)) {
                     OvmCodeGenerator.constant(Integer.MAX_VALUE);
                     return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
                  } else {
                     error("недопустимый аргумент функции MAX");
                  }
               } case MIN -> {
                  if (argKindObject.kind == Kind.TYPE_NAME && argKindObject.type.equals(INTEGER)) {
                     OvmCodeGenerator.constant(Integer.MIN_VALUE);
                     return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
                  } else {
                     error("недопустимый аргумент функции MIN");
                  }
               } case ODD -> {
                  if (argKindObject.kind == Kind.CONST_EXPRESSION && argKindObject.type.equals(INTEGER)) {
                     //
                  } else if (argKindObject.kind == Kind.VAR && argKindObject.type.equals(INTEGER)) {
                     OvmCodeGenerator.command(Command.LOAD);
                     //argKindObject.kind = Kind.GENERAL_EXPRESSION;
                  } else if (argKindObject.kind == Kind.GENERAL_EXPRESSION && argKindObject.type.equals(INTEGER)) {
                     //
                  } else {
                     error("недопустимый аргумент функции ODD");
                  }

                  OvmCodeGenerator.command(Command.DUPLICATE);
                  OvmCodeGenerator.constant(OvmCodeGenerator.getIp() + 6);
                  OvmCodeGenerator.command(Command.IF_LT);
                  OvmCodeGenerator.constant(2);
                  OvmCodeGenerator.command(Command.MOD);
                  OvmCodeGenerator.constant(OvmCodeGenerator.getIp() + 13);
                  OvmCodeGenerator.command(Command.GOTO);
                  //OvmCodeGenerator.getIp() + 6
                  OvmCodeGenerator.command(Command.DUPLICATE);
                  OvmCodeGenerator.constant(Integer.MIN_VALUE);
                  OvmCodeGenerator.constant(OvmCodeGenerator.getIp() + 7);
                  OvmCodeGenerator.command(Command.IF_EQ);
                  OvmCodeGenerator.command(Command.NEG);
                  OvmCodeGenerator.constant(2);
                  OvmCodeGenerator.command(Command.MOD);
                  OvmCodeGenerator.constant(OvmCodeGenerator.getIp() + 4);
                  OvmCodeGenerator.command(Command.GOTO);
                  //OvmCodeGenerator.getIp() + 7
                  OvmCodeGenerator.command(Command.DROP);
                  OvmCodeGenerator.constant(0);
                  //
                  OvmCodeGenerator.constant(0);
                  OvmCodeGenerator.constant(0);
                  OvmCodeGenerator.command(Command.IF_EQ);

                  return new KindObject(Kind.GENERAL_EXPRESSION, BOOLEAN);
               } case SIZE -> {
                  if (argKindObject.kind == Kind.TYPE_NAME) {
                     OvmCodeGenerator.constant(argKindObject.type.size);
                     return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
                  } else {
                     error("недопустимый тип аргумента");
                  }
               } default -> {
                  error("недостимая функция");
               }
            }

         } else {
            //имя без круглых скобок
            if (nameKindObject.kind == Kind.TYPE_NAME) {
               return nameKindObject;
            } else if (nameKindObject.kind == Kind.VAR) {
               return nameKindObject;
            } else if (nameKindObject.kind == Kind.CONST) {
               if (nameKindObject.type != INTEGER) {
                  //-*-
               }
               OvmCodeGenerator.constant(nameKindObject.value);
               return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
            } else if (nameKindObject.kind == Kind.UNDEFINED_TYPE_NAME) {
               error("тип ещё не определен");
            } else if (nameKindObject.kind == Kind.UNDEFINED_CONST) {
               error("константа ещё не определена");
            } else {
               error("ожидается имя переменной, константы или типа");
            }
         }
      } else if (lexeme == Lexeme.NUMBER) {
         OvmCodeGenerator.constant(lexemeScanner.getValue());
         nextLex();
         return new KindObject(Kind.CONST_EXPRESSION, INTEGER);
      } else {
         skipLexeme(Lexeme.LEFT_PAR);

         KindObject exp = expression();
         if (exp.kind == Kind.TYPE_NAME) {
            error("имя типа нельзя заключать в дополнительные скобки");
         }

         skipLexeme(Lexeme.RIGHT_PAR);

         if (exp.kind == Kind.VAR) {
            OvmCodeGenerator.command(Command.LOAD);
            return new KindObject(Kind.GENERAL_EXPRESSION, exp.type);
         }
         return new KindObject(exp.kind, exp.type);
      }
      return null;
   }

   static private KindObject term() {
      int ipBefore = OvmCodeGenerator.getIp();
      KindObject kindObject1 = factor();

      while (lexeme == Lexeme.MOD || lexeme == Lexeme.DIV || lexeme == Lexeme.MULTIPLY) {
         KindObject kindObject2;
         Lexeme binaryLex = lexeme;
         nextLex();

         if (kindObject1.kind == Kind.VAR && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         }

         kindObject2 = factor();

         if (kindObject2.kind == Kind.VAR && kindObject2.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject2.kind = Kind.GENERAL_EXPRESSION;
         }

         if (kindObject1.kind == Kind.CONST_EXPRESSION && kindObject1.type.equals(INTEGER) &&
            kindObject2.kind == Kind.CONST_EXPRESSION && kindObject2.type.equals(INTEGER))
         {
            //
         } else if ((kindObject1.kind == Kind.VAR || kindObject1.kind == Kind.CONST_EXPRESSION || kindObject1.kind == Kind.GENERAL_EXPRESSION) &&
               (kindObject2.kind == Kind.VAR || kindObject2.kind == Kind.CONST_EXPRESSION || kindObject2.kind == Kind.GENERAL_EXPRESSION) &&
               kindObject1.type.equals(INTEGER) && kindObject2.type.equals(INTEGER))
         {
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         } else {
            error("надопустимые типы операндов");
         }

         switch (binaryLex) {
            case MOD -> {
               OvmCodeGenerator.command(Command.MOD);
            } case DIV -> {
               OvmCodeGenerator.command(Command.DIV);
            } case MULTIPLY -> {
               OvmCodeGenerator.command(Command.MUL);
            } default -> error("ошибка в factor");
         }

         if (kindObject1.kind == Kind.CONST_EXPRESSION && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.evaluateExpression(ipBefore);
         }
      }

      return kindObject1;
   }

   static private KindObject simpleExpression() {
      boolean wasUnary = false;
      Lexeme unaryLexeme = null;
      if (lexeme == Lexeme.MINUS || lexeme == Lexeme.PLUS) {
         wasUnary = true;
         unaryLexeme = lexeme;
         nextLex();
      }

      int ipBefore = OvmCodeGenerator.getIp();

      KindObject kindObject1 = term();

      if (wasUnary) {
         if (kindObject1.kind == Kind.VAR && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         } else if ((kindObject1.kind == Kind.CONST_EXPRESSION || kindObject1.kind == Kind.GENERAL_EXPRESSION) &&
               kindObject1.type.equals(INTEGER))
         {
            //
         } else {
            error("унарный плюс или минус применим только к численным типам");
         }

         if (unaryLexeme == Lexeme.MINUS) {
            OvmCodeGenerator.command(Command.NEG);
            if (kindObject1.kind == Kind.CONST_EXPRESSION && kindObject1.type.equals(INTEGER)) {
               OvmCodeGenerator.evaluateExpression(ipBefore);
            }//-*-
         }
      }

      while (lexeme == Lexeme.MINUS || lexeme == Lexeme.PLUS) {
         Lexeme binLex = lexeme;
         nextLex();

         if (kindObject1.kind == Kind.VAR && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         }

         KindObject kindObject2 = term();
         if (kindObject2.kind == Kind.VAR && kindObject2.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject2.kind = Kind.GENERAL_EXPRESSION;
         }

         if (kindObject1.kind == Kind.CONST_EXPRESSION && kindObject1.type.equals(INTEGER) &&
            kindObject2.kind == Kind.CONST_EXPRESSION && kindObject2.type.equals(INTEGER))
         {
            //
         } else if ((kindObject1.kind == Kind.VAR || kindObject1.kind == Kind.CONST_EXPRESSION || kindObject1.kind == Kind.GENERAL_EXPRESSION) &&
               (kindObject2.kind == Kind.VAR || kindObject2.kind == Kind.CONST_EXPRESSION || kindObject2.kind == Kind.GENERAL_EXPRESSION) &&
               kindObject1.type.equals(INTEGER) && kindObject2.type.equals(INTEGER))
         {
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         } else {
            error("надопустимые типы операндов");
         }

         switch (binLex) {
            case PLUS -> {
               OvmCodeGenerator.command(Command.ADD);
            } case MINUS -> {
               OvmCodeGenerator.command(Command.SUB);//TODO проверить
            } default -> {
               error("ошибка в simpleExpression");
            }
         }

         if (kindObject1.kind == Kind.CONST_EXPRESSION && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.evaluateExpression(ipBefore);
            //-*-
         }
      }

      return kindObject1;
   }

   static private KindObject expression() {
      KindObject kindObject1 = simpleExpression();

      if (lexeme == Lexeme.LT || lexeme == Lexeme.LE || lexeme == Lexeme.GT || lexeme == Lexeme.GE ||
            lexeme == Lexeme.NE || lexeme == Lexeme.EQ)
      {
         Lexeme comLex = lexeme;
         nextLex();

         if (kindObject1.kind == Kind.VAR && kindObject1.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject1.kind = Kind.GENERAL_EXPRESSION;
         }

         KindObject kindObject2 = simpleExpression();

         if (kindObject2.kind == Kind.VAR && kindObject2.type.equals(INTEGER)) {
            OvmCodeGenerator.command(Command.LOAD);
            kindObject2.kind = Kind.GENERAL_EXPRESSION;
         }

         if ((kindObject1.kind == Kind.VAR || kindObject1.kind == Kind.CONST_EXPRESSION || kindObject1.kind == Kind.GENERAL_EXPRESSION) &&
               (kindObject2.kind == Kind.VAR || kindObject2.kind == Kind.CONST_EXPRESSION || kindObject2.kind == Kind.GENERAL_EXPRESSION) &&
               kindObject1.type.equals(INTEGER) && kindObject2.type.equals(INTEGER))
         {
            //
         } else {
            error("надопустимые типы операндов");
         }

         OvmCodeGenerator.constant(0);
         switch (comLex) {
            case EQ -> {
               OvmCodeGenerator.command(Command.IF_NE);
            } case NE -> {
               OvmCodeGenerator.command(Command.IF_EQ);
            } case GT -> {
               OvmCodeGenerator.command(Command.IF_LE);
            } case GE -> {
               OvmCodeGenerator.command(Command.IF_LT);
            } case LT -> {
               OvmCodeGenerator.command(Command.IF_GE);
            } case LE -> {
               OvmCodeGenerator.command(Command.IF_GT);
            }
         }

         return new KindObject(Kind.GENERAL_EXPRESSION, BOOLEAN);
      }

      return kindObject1;
   }

   static private void intExpression() {
      KindObject kindObject = expression();

      if (!((kindObject.kind == Kind.VAR || kindObject.kind == Kind.CONST_EXPRESSION || kindObject.kind == Kind.GENERAL_EXPRESSION) &&
         kindObject.type.equals(INTEGER)))
      {
         error("ожидается целочисленное выражение");
      }

      if (kindObject.kind == Kind.VAR) {
         OvmCodeGenerator.command(Command.LOAD);
      }
   }

   static private void booleanExpression() {
      KindObject kindObject = expression();

      if (!((kindObject.kind == Kind.VAR || kindObject.kind == Kind.CONST_EXPRESSION || kindObject.kind == Kind.GENERAL_EXPRESSION) &&
            kindObject.type.equals(BOOLEAN)))
      {
         error("ожидается логическое выражение");
      }
   }

   static private int constIntExpression() {
      int ipBefore = OvmCodeGenerator.getIp();

      KindObject kindObject = expression();

      if (kindObject.kind == Kind.CONST_EXPRESSION && kindObject.type.equals(INTEGER)) {
         return OvmCodeGenerator.evaluateExpression(ipBefore, false);
      } else {
         error("ожидается целочисленная константа");
      }
      return 0;
   }

   static private void constDeclaration() {
      String nameConst = getName();
      nextLex();
      skipLexeme(Lexeme.EQ);

      KindObject constKingObject = new KindObject(Kind.UNDEFINED_CONST);
      table.add(nameConst, constKingObject);

      int valueConst = constIntExpression();
      constKingObject.value = valueConst;
      constKingObject.type = INTEGER;
      constKingObject.kind = Kind.CONST;
   }

   static private Type type() {
      if (lexeme == Lexeme.ARRAY) {
         nextLex();

         int len = constIntExpression();
         if (len <= 0) {
            error("недопустимая длина массива");
         }
         skipLexeme(Lexeme.OF);

         Type t = new Type(type());
         int size = t.size * len;
         return new Type("array", size, len, t);
      } else {
         String typeName = getName();
         nextLex();

         KindObject kindObject = table.find(typeName);

         if (kindObject.kind == Kind.UNDEFINED_TYPE_NAME) {
            error("тип ещё не определен");
         } else if (kindObject.kind != Kind.TYPE_NAME) {
            error("имя не принадлежит типу");
         }
         return new Type(kindObject.type);
      }
   }

   static private void typeDeclaration() {
      String typeName = getName();
      nextLex();

      KindObject typeKindObject = new KindObject(Kind.UNDEFINED_TYPE_NAME);
      table.add(typeName, typeKindObject);

      skipLexeme(Lexeme.EQ);

      Type type = type();

      typeKindObject.kind = Kind.TYPE_NAME;
      typeKindObject.type = type;
   }

   static private void varDeclaration() {
      ArrayList<KindObject> kindObjectArrayList = new ArrayList<>();
      while (true) {
         String name = getName();
         KindObject kindObject = new KindObject(Kind.VAR, lexemeScanner.getPosition());
         kindObjectArrayList.add(kindObject);
         table.add(name, kindObject);

         nextLex();
         if (lexeme == Lexeme.COMMA) {
            nextLex();
         } else {
            break;
         }
      }

      skipLexeme(Lexeme.COLON);

      Type type = type();
      int size = type.size;
      for (KindObject kindObj: kindObjectArrayList) {
         kindObj.type = new Type(type);
         lastVarAdr -= size;
         kindObj.address = lastVarAdr;
         for (int i = 0; i < size; i++) {
            OvmCodeGenerator.constant(0);
            if (OvmCodeGenerator.getIp() > lastVarAdr) {
               error("превышен размер кода и переменных виртуальной машины");
            }
         }
      }
   }

   static private void declarationSequence() {
      while (true) {
         if (lexeme == Lexeme.CONST) {
            nextLex();
            while (lexeme == Lexeme.NAME) {
               constDeclaration();
               skipLexeme(Lexeme.SEMICOLON);
            }
         } else if (lexeme == Lexeme.TYPE) {
            nextLex();
            while (lexeme == Lexeme.NAME) {
               typeDeclaration();
               skipLexeme(Lexeme.SEMICOLON);
            }
         } else if (lexeme == Lexeme.VAR) {
            nextLex();
            while (lexeme == Lexeme.NAME) {
               varDeclaration();
               skipLexeme(Lexeme.SEMICOLON);
            }
         } else {
            break;
         }
      }
   }

   private static void intVariable() {
      KindObject kindObjectArg = expression();

      if (kindObjectArg.kind == Kind.VAR && kindObjectArg.type.equals(INTEGER)) {
         //
      } else {
         error("ожидается целочисленная переменная");
      }
   }

   private static void procedureCallArgument(BuildId buildId) {
      switch (buildId) {
         case DEC -> {
            skipLexeme(Lexeme.LEFT_PAR);

            intVariable();
            OvmCodeGenerator.command(Command.DUPLICATE);
            OvmCodeGenerator.command(Command.LOAD);
            if (lexeme == Lexeme.COMMA) {
               nextLex();
               intExpression();
            } else {
               OvmCodeGenerator.constant(1);
            }
            OvmCodeGenerator.command(Command.SUB);
            OvmCodeGenerator.command(Command.STORE);

            skipLexeme(Lexeme.RIGHT_PAR);
         } case INC -> {
            skipLexeme(Lexeme.LEFT_PAR);

            intVariable();
            OvmCodeGenerator.command(Command.DUPLICATE);
            OvmCodeGenerator.command(Command.LOAD);
            if (lexeme == Lexeme.COMMA) {
               nextLex();
               intExpression();
            } else {
               OvmCodeGenerator.constant(1);
            }
            OvmCodeGenerator.command(Command.ADD);
            OvmCodeGenerator.command(Command.STORE);

            skipLexeme(Lexeme.RIGHT_PAR);
         } case IN_INT -> {
            skipLexeme(Lexeme.LEFT_PAR);

            intVariable();
            OvmCodeGenerator.command(Command.IN_INT);

            skipLexeme(Lexeme.RIGHT_PAR);
         } case IN_OPEN -> {
            skipLexeme(Lexeme.LEFT_PAR);
            skipLexeme(Lexeme.RIGHT_PAR);
         } case OUT_INT -> {
            skipLexeme(Lexeme.LEFT_PAR);

            intExpression();
            skipLexeme(Lexeme.COMMA);
            intExpression();
            OvmCodeGenerator.command(Command.OUT_INT);

            skipLexeme(Lexeme.RIGHT_PAR);
         } case OUT_LN -> {
            skipLexeme(Lexeme.LEFT_PAR);
            skipLexeme(Lexeme.RIGHT_PAR);

            OvmCodeGenerator.command(Command.OUT_LN);
         } case HALT -> {
            skipLexeme(Lexeme.LEFT_PAR);

            intExpression();

            skipLexeme(Lexeme.RIGHT_PAR);
            OvmCodeGenerator.command(Command.HALT);
         } default -> {
            error("процедура не распознана");
         }
      }
   }

   private static void statement() {
      if (lexeme == Lexeme.NAME) {
         String name = getName();
         nextLex();

         KindObject kindObjectName = new KindObject(table.find(name));
         Type typeName = null;

         if (kindObjectName.kind == Kind.VAR) {
            OvmCodeGenerator.constant(kindObjectName.address);
            typeName = new Type(kindObjectName.type);
         }

         while (lexeme == Lexeme.LEFT_BRACKET) {
            if (typeName == null || !typeName.type.equals("array")) {
               error("индексация возможна только для массивов");
            }
            nextLex();

            intExpression();
            skipLexeme(Lexeme.RIGHT_BRACKET);
            OvmCodeGenerator.constant(typeName.t.size);
            OvmCodeGenerator.command(Command.MUL);
            OvmCodeGenerator.command(Command.ADD);

            typeName = new Type(typeName.t);
         }

         kindObjectName.type = typeName;

         if (lexeme == Lexeme.ASSIGN) {
            nextLex();

            if (kindObjectName.kind != Kind.VAR) {
               error("ожидается имя переменной");
            }

            KindObject kindObj = expression();

            if (kindObj.kind == Kind.VAR && kindObj.type.equals(INTEGER)) {
               OvmCodeGenerator.command(Command.LOAD);
               kindObj.kind = Kind.GENERAL_EXPRESSION;
            }

            if (kindObjectName.type.equals(INTEGER) && kindObj.type.equals(INTEGER)) {
               OvmCodeGenerator.command(Command.STORE);
            } else if (kindObjectName.type.equals(kindObj.type)) { //массивы
               OvmCodeGenerator.constant(kindObjectName.type.size);
               int repeatId = OvmCodeGenerator.getIp();
               OvmCodeGenerator.constant(1);
               OvmCodeGenerator.command(Command.SUB);
               OvmCodeGenerator.constant(2);
               OvmCodeGenerator.command(Command.LOAD_SP);
               OvmCodeGenerator.constant(1);
               OvmCodeGenerator.command(Command.LOAD_SP);
               OvmCodeGenerator.command(Command.ADD);//a+i
               OvmCodeGenerator.constant(2);
               OvmCodeGenerator.command(Command.LOAD_SP);
               OvmCodeGenerator.constant(2);
               OvmCodeGenerator.command(Command.LOAD_SP);
               OvmCodeGenerator.command(Command.ADD);//b+i
               OvmCodeGenerator.command(Command.LOAD);//val(b+i)
               OvmCodeGenerator.command(Command.STORE);
               OvmCodeGenerator.command(Command.DUPLICATE);
               OvmCodeGenerator.constant(0);
               OvmCodeGenerator.constant(repeatId);
               OvmCodeGenerator.command(Command.IF_GT);
               OvmCodeGenerator.command(Command.DROP);
               OvmCodeGenerator.command(Command.DROP);
               OvmCodeGenerator.command(Command.DROP);
            } else {
               error("переменная и выражения несовместимы по присваиванию");
            }
         } else if (lexeme == Lexeme.DOT) {
            nextLex();
            String procName = "";
            if (lexeme == Lexeme.NAME) {
               procName = getName();
            } else {
               error("ожидается имя");
            }
            nextLex();

            if (kindObjectName.kind != Kind.IMPORTED_MODULE) {
               error("ожидается имя импортированного модуля");
            }

            KindObject kindObjectProc = new KindObject(kindObjectName.table.find(procName));
            if (kindObjectProc.kind != Kind.STANDARD_PROCEDURE) {
               error("ожидается процедура импортированного модуля");
            }
            procedureCallArgument(kindObjectProc.id);
         } else if (lexeme == Lexeme.LEFT_PAR) {
            if (kindObjectName.kind != Kind.STANDARD_PROCEDURE) {
               error("ожидается втроенная процедура");
            }
            procedureCallArgument(kindObjectName.id);
         } else {
            error("и что прикажешь мне делать с этим именем?");
         }
      } else if (lexeme == Lexeme.IF) {
         ArrayList<Integer> jumpEndIp = new ArrayList<>();

         nextLex();
         booleanExpression();
         int nextIp = OvmCodeGenerator.getIp() - 2;
         skipLexeme(Lexeme.THEN);

         statementSequence();
         jumpEndIp.add(OvmCodeGenerator.getIp());
         OvmCodeGenerator.constant(0);
         OvmCodeGenerator.command(Command.GOTO);
         while (lexeme == Lexeme.ELSIF) {
            nextLex();
            OVM.memory[nextIp] = OvmCodeGenerator.getIp();
            booleanExpression();
            nextIp = OvmCodeGenerator.getIp() - 2;
            skipLexeme(Lexeme.THEN);

            statementSequence();
            jumpEndIp.add(OvmCodeGenerator.getIp());
            OvmCodeGenerator.constant(0);
            OvmCodeGenerator.command(Command.GOTO);
         }
         OVM.memory[nextIp] = OvmCodeGenerator.getIp();
         if (lexeme == Lexeme.ELSE) {
            nextLex();
            statementSequence();
         }
         skipLexeme(Lexeme.END);

         for (Integer ip: jumpEndIp) {
            OVM.memory[ip] = OvmCodeGenerator.getIp();
         }

      } else if (lexeme == Lexeme.WHILE) {
         nextLex();
         int beforeAdr = OvmCodeGenerator.getIp();
         booleanExpression();
         int jumpAdr = OvmCodeGenerator.getIp() - 2;

         skipLexeme(Lexeme.DO);
         statementSequence();

         skipLexeme(Lexeme.END);
         OvmCodeGenerator.constant(beforeAdr);
         OvmCodeGenerator.command(Command.GOTO);
         OVM.memory[jumpAdr] = OvmCodeGenerator.getIp();
      } else if (lexeme == Lexeme.FOR) {
         nextLex();
         int step;

         String name = "";
         if (lexeme == Lexeme.NAME) {
            name = getName();
         } else {
            error("ожидается имя переменной");
         }
         nextLex();

         int beforeAddr = OvmCodeGenerator.getIp();
         OvmCodeGenerator.constant(0);
         OvmCodeGenerator.command(Command.GOTO);

         KindObject kindObjectName = new KindObject(table.find(name));
         if (kindObjectName.kind == Kind.VAR && kindObjectName.type.equals(INTEGER)) {
            OvmCodeGenerator.constant(kindObjectName.address);
         } else {
            error("ожидается переменная");
         }
         skipLexeme(Lexeme.ASSIGN);
         intExpression();

         OvmCodeGenerator.command(Command.STORE);
         int stepIp = OvmCodeGenerator.getIp();
         OvmCodeGenerator.constant(0);
         OvmCodeGenerator.command(Command.GOTO);

         skipLexeme(Lexeme.TO);

         OVM.memory[beforeAddr] = OvmCodeGenerator.getIp(); //?
         intExpression();
         OvmCodeGenerator.constant(beforeAddr + 2);
         OvmCodeGenerator.command(Command.GOTO);

         if (lexeme == Lexeme.BY) {
            nextLex();
            step = constIntExpression();
            if (step == 0) {
               error("шаг не должен быть равен 0");
            }
         } else {
            step = 1;
         }
         skipLexeme(Lexeme.DO);

         int whileBegin =  OvmCodeGenerator.getIp();
         OVM.memory[stepIp] = whileBegin;
         OvmCodeGenerator.command(Command.DUPLICATE); //dup(to)
         OvmCodeGenerator.constant(kindObjectName.address);
         OvmCodeGenerator.command(Command.LOAD);
         int whileEndAddr = OvmCodeGenerator.getIp();
         OvmCodeGenerator.constant(0);
         if (step > 0) {
            OvmCodeGenerator.command(Command.IF_LT);
         } else {
            OvmCodeGenerator.command(Command.IF_GT);
         }

         statementSequence();
         skipLexeme(Lexeme.END);

         OvmCodeGenerator.constant(kindObjectName.address);
         OvmCodeGenerator.command(Command.DUPLICATE);
         OvmCodeGenerator.command(Command.LOAD);
         OvmCodeGenerator.constant(step);
         OvmCodeGenerator.command(Command.ADD);
         OvmCodeGenerator.command(Command.STORE);

         OvmCodeGenerator.constant(whileBegin);
         OvmCodeGenerator.command(Command.GOTO);
         OVM.memory[whileEndAddr] = OvmCodeGenerator.getIp();
         OvmCodeGenerator.command(Command.DROP);
      }
   }

   private static void statementSequence() {
      statement();
      while (lexeme == Lexeme.SEMICOLON) {
         nextLex();
         statement();
      }
   }

   private static void module() {
      skipLexeme(Lexeme.MODULE);
      String nameModule = getName();
      nextLex();
      table.openScore();
      table.add(nameModule, new KindObject(Kind.CURRENT_MODULE_NAME));
      skipLexeme(Lexeme.SEMICOLON);

      table.openScore();
      if (lexeme == Lexeme.IMPORT) {
         nextLex();
         importList();
      }

      declarationSequence();

      if (lexeme == Lexeme.BEGIN) {
         nextLex();
         statementSequence();
      }

      OvmCodeGenerator.constant(0);
      OvmCodeGenerator.command(Command.HALT);
      table.closeScore();

      skipLexeme(Lexeme.END);

      String endModuleName = lexemeScanner.getIdentifier();
      nextLex();
      if (!endModuleName.equals(nameModule)) {
         error("имя модуля в начале и в конце не совпадают");
      } else {
         skipLexeme(Lexeme.DOT);
      }
      table.closeScore();

   }

   public static void parse(String file) {
      TextReader.init(file);
      lexemeScanner = new LexemeScanner();

      table.openScore();

      table.add("ABS", new KindObject(Kind.STANDARD_FUNCTION, BuildId.ABS));
      table.add("MIN", new KindObject(Kind.STANDARD_FUNCTION, BuildId.MIN));
      table.add("MAX", new KindObject(Kind.STANDARD_FUNCTION, BuildId.MAX));
      table.add("ODD", new KindObject(Kind.STANDARD_FUNCTION, BuildId.ODD));
      table.add("SIZE", new KindObject(Kind.STANDARD_FUNCTION, BuildId.SIZE));
      table.add("LEN", new KindObject(Kind.STANDARD_FUNCTION, BuildId.LEN));

      table.add("HALT", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.HALT));
      table.add("INC", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.INC));
      table.add("DEC", new KindObject(Kind.STANDARD_PROCEDURE, BuildId.DEC));

      table.add("INTEGER", new KindObject(Kind.TYPE_NAME, INTEGER));

      //-*-
      nextLex();
      module();
      skipLexeme(Lexeme.EOT);
      table.closeScore();
   }
}
