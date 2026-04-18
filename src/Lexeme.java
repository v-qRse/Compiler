public enum Lexeme {
   NAME,
   NUMBER,

   MODULE, IMPORT,
   CONST, TYPE, VAR,
   BEGIN, END,

   ARRAY,
   POINTER,
   PROCEDURE,
   RECORD,

   INTEGER, BOOLEAN,

   IF, THEN, ELSE, ELSIF,
   CASE,

   WHILE, DO,
   LOOP, FOR,
   REPEAT, UNTIL,

   NIL, NONE, EOT,
   IN, IS, BY, WITH, OF, TO,

   EXIT,
   RETURN,

   SEMICOLON,     // ;
   ASSIGN,        // :=
   COLON,         // :
   DOT, COMMA,    // . ,
   LEFT_PAR, RIGHT_PAR,             // ( )
   LEFT_BRACKET, RIGHT_BRACKET,     // [ ]

   OR, AND,

   PLUS, MINUS,         // + -
   MULTIPLY, DIV, MOD,  // * DIV MOD

   EQ, NE,  // = #
   LT, LE,  // < <=
   GT, GE,  // > >=

   /*NAME, NUMBER, SEMICOLON, ASSIGN, COLON, COMMA, LEFT_PAR, RIGHT_PAR, LEFT_BRACKET, RIGHT_BRACKET, DOT, PLUS, MINUS,
   MULTIPLY, EQ, NE, LE, LT, GE, GT, ARRAY, BEGIN, BY, CONST, DIV, DO, ELSE, ELSIF,
   END, FOR, IF, IMPORT, MOD, MODULE, OF, THEN, TO, TYPE, VAR, WHILE, NONE, EOT*/
}
