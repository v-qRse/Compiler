package table;

import data.container.KindObject;

import java.util.HashMap;
import java.util.Stack;

public class Table {
   private final Stack<HashMap<String, KindObject>> table;

   public Table() {
      table = new Stack<>();
   }

   public void openScore() {
      table.add(new HashMap<>());
   }

   public void closeScore() {
      table.pop();
   }

   public void add(String name, KindObject obj) {
      if (table.peek().put(name, obj) != null) {
         throw new Error("Попытка добавления объявленого елемента в таблицу"); //-*-
      }
   }

   public KindObject find(String name) {
      for (HashMap<String, KindObject> score: table) {
         if (score.containsKey(name)) {
            return score.get(name);
         }
      }
      //-*- вывод ошибки
      return null;
   }
}
