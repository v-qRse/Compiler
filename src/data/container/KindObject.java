package data.container;

import table.BuildId;
import table.Kind;
import table.Table;

public class KindObject {
   public Kind kind;
   public BuildId id; //для стандартных процедур и функций
   public Type type;

   public int value;
   public int address; //переменная

   public long[] namePos; //для вызова ошибки при повторном объявлении
   public Table table; //для импортированных модулей

   public KindObject(Kind kind) {
      this.kind = kind;
   }

   public KindObject(Kind kind, Type type) {
      this(kind);
      if (type == null) {
         this.type = null;
      } else {
         this.type = new Type(type);
      }
   }

   public KindObject(Kind kind, BuildId id) {
      this(kind);
      this.id = id;
   }

   public KindObject(Kind kind, Table table) {
      this(kind);
      this.table = table;
   }

   public KindObject(Kind kind, long[] namePos) {
      this(kind);
      this.namePos = namePos.clone();
   }

   public KindObject(KindObject kindObject) {
      kind = kindObject.kind;
      id = kindObject.id;
      if (kindObject.type == null) {
         type = null;
      } else {
         type = new Type(kindObject.type);
      }

      value = kindObject.value;
      address = kindObject.address;

      namePos = kindObject.namePos;
      table = kindObject.table;//TODO проверить на ошибку при копировании
   }

   @Override
   public boolean equals(Object o) {
      if (o == null) {
         return false;
      }
      if (o instanceof KindObject) {
         KindObject kindObject = (KindObject) o;

         return kind == kindObject.kind && id == kindObject.id && type.equals(kindObject.type) &&
               value == kindObject.value && address == kindObject.address;
      }
      return false;
   }
}
