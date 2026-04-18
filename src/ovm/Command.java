package ovm;

public enum Command {
   LOAD(-1),
   STORE(-2),
   DUPLICATE(-3),
   ADD(-4),
   SUB(-5),
   MUL(-6),
   DIV(-7),
   MOD(-8),
   NEG(-9),
   IF_LT(-10),
   IF_LE(-11),
   IF_GT(-12),
   IF_GE(-13),
   IF_EQ(-14),
   IF_NE(-15),
   GOTO(-16),
   IN_INT(-17),
   OUT_INT(-18),
   OUT_LN(-19),
   HALT(-20),
   LOAD_SP(-21),
   DROP(-22);

   private final int value;

   Command(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
