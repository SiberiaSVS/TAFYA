import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class LexicalAnalysis {
    //Таблица служебных слов
    private final String[] tw = Tables.tw;
    //Таблица разделителей
    private final String[] tl = Tables.tl;
    //Таблица чисел
    private final ArrayList<String> tn = Tables.tn;
    private final ArrayList<Identifier> ti = Tables.ti;
    private FileReader reader;
    private FileWriter writer;
    private boolean endOfFile = false;
    //буфер для накопления лексемы
    private final StringBuilder s = new StringBuilder();
    private char c;
    private String state = "H";
    private boolean stop = false;

    public LexicalAnalysis() {
        Arrays.sort(tw);
        Arrays.sort(tl);
        Tables.ti.clear();
        Tables.tn.clear();
    }

    private void writeInFile(String string) {
        try {
            writer.write(string);
            Main.ui.addLexemeInLexemesArea(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void analysis() throws IOException {
        try {
            File file = new File("lexeme.txt");
            if (file.createNewFile()) {
                Main.ui.log("Файл лексем создан");
            }
        } catch (IOException e) {
            Main.ui.log("Ошибка при создании файла лексем");
            System.exit(1);
        }
        writer = new FileWriter("lexeme.txt");
        reader = new FileReader("program.txt");

        Main.ui.log("Запущен лексический анализ");

        c = gc();

        while(!endOfFile) {
            if(stop)
                return;

            //Начало программы
            if(c == '{') {
                state = "H";
                s.append(c);
                writeInFile(search(s));
            }
            //Конец программы
            else if(c == '}') {
                state = "V";
                s.append(c);
                writeInFile(search(s));
            }
            //Комментарии
            else if(c == '#') {
                state = "C";
                s.append(c);
                //writeInFile(search(s));
                c = gc();
                s.delete(0, s.length());
                while (true) {
                    if (c == '#') {
                        state = "#";
                        s.append(c);
                        //writeInFile(search(s));
                        break;
                    }
                    if (endOfFile) {
                        state = "ER";
                        er("Не найден символ конца комментария");
                        if(stop)
                            return;
                        break;
                    }
                    c = gc();
                }
            }
            //Присваивание
            else if(c == ':') {
                state = "DV";
                s.append(c);
                c = gc();
                if(c == '=') {
                    state = ":=";
                    s.append(c);
                    writeInFile(search(s));
                }
                else {
                    state = "ER";
                    er("Найден символ ':' вместо ':='");
                    if(stop)
                        return;
                    break;
                }
            }
            //Сравнение
            else if(c == '<') {
                state = "<";
                s.append(c);
                c = gc();
                continue;
            }
            else if(c == '=' && state.equals("<")) {
                state = "<=";
                s.append(c);
                writeInFile(search(s));
            }
            else if(c == '>' && state.equals("<")) {
                state = "<>";
                s.append(c);
                writeInFile(search(s));
            }
            else if(state.equals("<")) {
                state = "";
                writeInFile(search(s));
            }
            else if (c == '>') {
                state = ">";
                s.append(c);
                c = gc();
                continue;
            }
            else if (c == '=' && state.equals(">")) {
                state = ">=";
                s.append(c);
                writeInFile(search(s));
            }
            else if (state.equals(">")) {
                state = "";
                writeInFile(search(s));
            }
            //Числа
            else if(c == '0' || c == '1') {
                bin();
                s.delete(0, s.length());
                continue;
            }
            else if(c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7') {
                oct();
                s.delete(0, s.length());
                continue;
            }
            else if(c == '8' || c == '9') {
                dec();
                s.delete(0, s.length());
                continue;
            }
            else if(c == '.') {
                rn();
                s.delete(0, s.length());
                continue;
            }
            //Идентификаторы
            else if(Character.toString(c).matches("^[a-zA-Z]+$")) {
                state = "I";
                s.append(c);
                c = gc();
                while (Character.toString(c).matches("^[a-zA-Z0-9]+$")) {
                    s.append(c);
                    c = gc();
                }
                writeInFile(search(s));

                s.delete(0, s.length());
                continue;
            }
            //Остальное
            else if(matchesFirstCharInTL(c)){
                state = "L";
                s.append(c);
                if(BinarySearch.getLexemeIndex(tl, s.toString()) >= 0)
                    writeInFile(search(s));
            }
            else if(c == ' ' || c == '\n' || c == '\t') {
                c = gc();
                continue;
            }
            else {
                er("Неопознанный символ '" + c + "'");
            }
            c = gc();
            s.delete(0, s.length());
        }

        reader.close();
        writer.close();

        Main.ui.log("Лексический анализ успешно завершен");

        new SyntaxAnalysis().analysis();
    }

    //Чтение следующего символа
    private char gc() {
        try {
            int c = reader.read();
            if(c == -1) {
                endOfFile = true;
            }
            return (char) c;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String search(StringBuilder s) {
        int table;
        int index = BinarySearch.getLexemeIndex(tw, s.toString());

        if(index == -1) {
            index = BinarySearch.getLexemeIndex(tl, s.toString());
            if(index == -1) {
                if(s.toString().matches("^[a-zA-Z]+[a-zA-Z0-9]*$")) {
                    table = 4;
                    for(int i = 0; i<ti.size(); i++) {
                        if(s.toString().matches(ti.get(i).getName())) {
                            index = i;
                        }
                    }
                    if(index == -1) {
                        ti.add(new Identifier(s.toString()));
                        index = ti.size() - 1;
                    }
                }
                else {
                    table = 3;
                    tn.add(s.toString());
                    index = tn.size() - 1;
                }
            }
            else table = 2;
        }
        else {
            table = 1;
        }

        return "[" + table + "," + index + "]";
    }

    private void bin() {
        state = "2C";
        while(c == '0' || c == '1') {
            s.append(c);
            c = gc();
        }
        if(c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7') {
            oct();
        }
        else if(c == '8' || c == '9') {
            dec();
        }
        else if(c == 'O' || c == 'o') {
            oct1();
        }
        else if(c == 'D' || c == 'd') {
            dec1();
        }
        else if(c == '.') {
            rn();
        }
        else if(c == 'E' || c == 'e') {
            ef1();
        }
        else if(c == 'A' || c == 'C' || c == 'F' || c == 'a' || c == 'c' || c == 'f') {
            hex();
        }
        else if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == 'B' || c == 'b') {
            bin1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void bin1() {
        state = "2C*";
        s.append(c);
        c = gc();
        if(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
                c == '5' || c == '6' || c == '7' || c == '8' || c == '9' ||
                c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' ||
                c == 'F' || c == 'a' || c == 'b' || c == 'c' || c == 'd' ||
                c == 'e' || c == 'f') {
            hex();
        }
        else if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void oct() {
        state = "8C";
        while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7') {
            s.append(c);
            c = gc();
        }
        if(c == '8' || c == '9') {
            dec();
        }
        else if(c == 'D' || c == 'd') {
            dec1();
        }
        else if(c == '.') {
            rn();
        }
        else if(c == 'E' || c == 'e') {
            ef1();
        }
        else if(c == 'A' || c == 'B' || c == 'C' || c == 'F' ||
                c == 'a' || c == 'b' || c == 'c' || c == 'f') {
            hex();
        }
        else if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == 'O' || c == 'o') {
            oct1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void oct1() {
        state = "8C*";
        s.append(c);
        c = gc();
        if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void dec() {
        state = "10C";
        while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
            s.append(c);
            c = gc();
        }
        if(c == '.') {
            rn();
        }
        else if(c == 'E' || c == 'e') {
            ef1();
        }
        else if(c == 'A' || c == 'B' || c == 'C' || c == 'F' ||
                c == 'a' || c == 'b' || c == 'c' || c == 'f') {
            hex();
        }
        else if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == 'D' || c == 'd') {
            dec1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void dec1() {
        state = "10C*";
        s.append(c);
        c = gc();
        if(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
                c == '5' || c == '6' || c == '7' || c == '8' || c == '9' ||
                c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' ||
                c == 'F' || c == 'a' || c == 'b' || c == 'c' || c == 'd' ||
                c == 'e' || c == 'f') {
            hex();
        }
        else if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void hex() {
        state = "16C";
        while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
                c == '5' || c == '6' || c == '7' || c == '8' || c == '9' ||
                c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' ||
                c == 'F' || c == 'a' || c == 'b' || c == 'c' || c == 'd' ||
                c == 'e' || c == 'f') {
            s.append(c);
            c = gc();
        }
        if(c == 'H' || c == 'h') {
            hex1();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void hex1() {
        state = "16C*";
        s.append(c);
        c = gc();
        if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void rn() {
        state = "RN";
        s.append(c);
        c = gc();
        while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
                c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
            s.append(c);
            c = gc();
        }
        if(c == 'E' || c == 'e') {
            ef2();
        }
        else if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
            writeInFile(search(s));
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void ef1() {
        state = "EF1";
        s.append(c);
        c = gc();
        if(c == '+') {
            s.append(c);
            c = gc();
            while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
                s.append(c);
                c = gc();
            }
            if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
                writeInFile(search(s));
            }
            else {
                er("Ошибка в чтении числа");
            }
        }
        else if(c == '-') {
            s.append(c);
            c = gc();
            while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
                s.append(c);
                c = gc();
            }
            if(c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' ||
                    c == 'F' || c == 'a' || c == 'b' || c == 'c' || c == 'd' ||
                    c == 'e' || c == 'f') {
                hex();
            }
            else if(c == 'H' || c == 'h') {
                hex1();
            }
            if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
                writeInFile(search(s));
            }
            else {
                er("Ошибка в чтении числа");
            }
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void ef2() {
        state = "EF2";
        s.append(c);
        c = gc();
        if(c == '+' || c == '-') {
            s.append(c);
            c = gc();
            while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
                s.append(c);
                c = gc();
            }
            if(c == ' ' || c == '\n' || c == '\t' || matchesFirstCharInTL(c)) {
                writeInFile(search(s));
            }
            else {
                er("Ошибка в чтении числа");
            }
        }
        else {
            er("Ошибка в чтении числа");
        }
    }

    private void er(String message) {
        state = "ER";
        Main.ui.log("Лексическая ошибка: " + message);
        stop = true;
    }

    public boolean matchesFirstCharInTL(char c) {
        for (String string : tl) {
            if (c == string.charAt(0)) {
                return true;
            }
        }
        return false;
    }
}