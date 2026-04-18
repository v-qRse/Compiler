package data.container;

public class Type {
   public String type;
   public int size;
   public int length;
   public Type t;

   public Type(String type, int size) {
      this.type = type;
      this.size = size;
   }

   public Type(String type, int size, int length, Type t) {
      this(type, size);
      this.length = length;
      this.t = t;
   }

   public Type(Type copyType) {
      type = copyType.type;
      size = copyType.size;
      length = copyType.length;
      if (copyType.t == null) {
         t = null;
      } else {
         t = new Type(copyType.t);
      }
   }

   @Override
   public boolean equals (Object o) {
      if (o == null) {
         return false;
      }
      if (o instanceof Type) {
         Type typeO = (Type) o;

         return type.equals(typeO.type) && size == typeO.size &&
               length == typeO.length && ((t == null && typeO.t == null) || (t != null && t.equals(typeO.t)));
      }
      return false;
   }
}
