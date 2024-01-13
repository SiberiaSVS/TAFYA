import java.util.ArrayList;

public class Tables {
    public static final String[] tw = {"dim", "if", "else", "begin", "end", "while", "next", "for", "to", "step", "readln", "writeln", "int", "float", "bool", "true", "false"};
    //Таблица разделителей
    public static final String[] tl = {"{", "}", "(", ")", "+", "-", "<>", "=", "<", "<=", ">", ">=", "*", "/", ";", ",", ":=", "#", "and", "not", "or"};
    //Таблица чисел
    public static final ArrayList<String> tn = new ArrayList<>();
    public static final ArrayList<Identifier> ti = new ArrayList<>();

    public static void printTables() {
        Main.ui.printInTableArea("Таблица служебных слов");
        for(int i=0; i<tw.length; i++) {
            Main.ui.printInTableArea(i + ")  " + tw[i]);
        }

        Main.ui.printInTableArea();

        Main.ui.printInTableArea("Таблица ограничителей");
        for(int i=0; i<tl.length; i++) {
            Main.ui.printInTableArea(i + ")  " + tl[i]);
        }

        Main.ui.printInTableArea();

        Main.ui.printInTableArea("Таблица чисел");
        for(int i=0; i<tn.size(); i++) {
            Main.ui.printInTableArea(i + ")  " + tn.get(i));
        }

        Main.ui.printInTableArea();

        Main.ui.printInTableArea("Таблица идентификаторов");
        for(int i=0; i<ti.size(); i++) {
            Main.ui.printInTableArea(i + ")  " + ti.get(i).getName());
        }
    }
}
