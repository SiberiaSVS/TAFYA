import java.io.*;
import java.util.ArrayList;
import java.util.Stack;

public class SyntaxAnalysis {
    private final ArrayList<Identifier> ti = Tables.ti;
    private final BufferedInputStream bufferedInputStream;

    {
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream("lexeme.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final Lex lex = new Lex();

    private final ArrayList<Integer> stack = new ArrayList<>();
    private final Stack<String> opStack = new Stack<>();
    private final ArrayList<TOP> top = new ArrayList<>();

    public void analysis(){
        top.add(new TOP(":=",  "int", "int", "int"));
        top.add(new TOP("+",  "int", "int", "int"));
        top.add(new TOP("-",  "int", "int", "int"));
        top.add(new TOP("*",  "int", "int", "int"));
        top.add(new TOP("/",  "int", "int", "float"));

        top.add(new TOP(":=",  "float", "float", "float"));
        top.add(new TOP("+",  "float", "float", "float"));
        top.add(new TOP("-",  "float", "float", "float"));
        top.add(new TOP("*",  "float", "float", "float"));
        top.add(new TOP("/",  "float", "float", "float"));

        top.add(new TOP("+",  "int", "float", "float"));
        top.add(new TOP("-",  "int", "float", "float"));
        top.add(new TOP("*",  "int", "float", "float"));
        top.add(new TOP("/",  "int", "float", "float"));

        top.add(new TOP(":=",  "float", "int", "float"));
        top.add(new TOP("+",  "float", "int", "float"));
        top.add(new TOP("-",  "float", "int", "float"));
        top.add(new TOP("*",  "float", "int", "float"));
        top.add(new TOP("/",  "float", "int", "float"));

        top.add(new TOP(">",  "int", "int", "bool"));
        top.add(new TOP(">=", "int", "int", "bool"));
        top.add(new TOP("<",  "int", "int", "bool"));
        top.add(new TOP("<>", "int", "int", "bool"));
        top.add(new TOP("<=", "int", "int", "bool"));
        top.add(new TOP("=",  "int", "int", "bool"));

        top.add(new TOP(">",  "float", "float", "bool"));
        top.add(new TOP(">=", "float", "float", "bool"));
        top.add(new TOP("<",  "float", "float", "bool"));
        top.add(new TOP("<>", "float", "float", "bool"));
        top.add(new TOP("<=", "float", "float", "bool"));
        top.add(new TOP("=",  "float", "float", "bool"));

        top.add(new TOP(":=",  "bool", "bool", "bool"));
        top.add(new TOP("=",  "bool", "bool", "bool"));
        top.add(new TOP("<>",  "bool", "bool", "bool"));
        top.add(new TOP("and",  "bool", "bool", "bool"));
        top.add(new TOP("or",  "bool", "bool", "bool"));


        gl();

        if(lex.EQ("{")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден символ входа в программу");
        }

        if(description() || operator()) {
            if(lex.EQ(";")) {
                gl();
            }
            else {
                er("Синтаксическая ошибка: не найден символ ';' после описания или оператора");
            }
            while(description() || operator()) {
                if(lex.EQ(";")) {
                    gl();
                }
                else {
                    er("Синтаксическая ошибка: не найден символ ';' после описания или оператора");
                }
            }
        }
        else {
            er("Синтаксическая ошибка: не найдено описание или оператор");
        }

        if(lex.EQ("}")) {
            System.out.println("Ошибок не обнаружено");
        }
        else {
            er("Синтаксическая ошибка: не найден символ конца программы");
        }
    }

    private boolean description(){//Описание
        if(lex.EQ("dim")) {
            gl();
        }
        else {
            return false;
        }

        stack.add(lex.getIndex());//Добавление в стек индекса идентификатора

        if(!lex.ID()) {
            er("Синтаксическая ошибка: не найден идентификатор при описании");
            return false;
        }

        gl();

        while(lex.EQ(",")) {
            gl();
            stack.add(lex.getIndex());//Добавление в стек индекса идентификатора
            if(!lex.ID()) {
                er("Синтаксическая ошибка: не найден идентификатор при описании после ','");
                return false;
            }
            gl();
        }

        for(int a : stack) {
            Identifier id = ti.get(a);
            if(id.isDescribed()) {
                er("Семантическая ошибка: переменная уже была описана");
            }
            id.setDescribed(true);
            if(lex.EQ("int"))
                id.setType("int");
            else if(lex.EQ("float"))
                id.setType("float");
            else if(lex.EQ("bool"))
                id.setType("bool");
        }

        stack.clear();

        if(!type()) {
            er("Синтаксическая ошибка: не определен тип переменной при описании");
            return false;
        }

        return true;
    }

    private boolean operator() {
        return composite()/*составной*/ || assignments() /*присваивания*/ || conditional() /*условный*/ || fixedLoop() /*фиксированного цикла*/ ||
                conditionalLoop() /*условного цикла*/ || input() /*ввода*/ || output(); /*вывода*/
    }


    private boolean composite() {//составной
        if(lex.EQ("begin")) {
            gl();
        }
        else {
            return false;
        }
        if(!operator()) {
            er("Синтаксическая ошибка: не найден оператор в составном операторе");
            return false;
        }
        while (lex.EQ(";")) {
            gl();
            if(!operator()) {
                er("Синтаксическая ошибка: не найден оператор в составном операторе после ';'");
                return false;
            }
        }

        if(lex.EQ("end")) {
            gl();
            return true;
        }
        else {
            er("Синтаксическая ошибка: не найдено ключевое слово 'end'");
            return false;
        }
    }

    private boolean assignments() {//присваивания
        if (!identifier()) {
            return false;
        }
        if(lex.EQ(":=")) {
            opStack.push(":=");
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден ':=' при присваивании");
            return false;
        }

        if(expression()) {
            checkOp();
            return true;
        }

        return false;
    }

    private boolean conditional() {//условный
        if(lex.EQ("if")) {
            gl();
        }
        else {
            return false;
        }
        if(lex.EQ("(")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден '(' в условном операторе");
            return false;
        }
        if(!expression()) {
            er("Синтаксическая ошибка: не найдено выражение в условном операторе");
            return false;
        }
        if(lex.EQ(")")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден ')' в условном операторе");
            return false;
        }
        if(!operator()) {
            er("Синтаксическая ошибка: не найден оператор в условном операторе");
            return false;
        }
        if(lex.EQ("else")) {
            gl();
            if(!operator()) {
                er("Синтаксическая ошибка: не найден оператор в условном операторе после 'else'");
                return false;
            }
        }

        return true;
    }

    private boolean fixedLoop() {//фиксированного цикла
        if (lex.EQ("for")) {
            gl();
        }
        else {
            return false;
        }
        if (!assignments()) {
            er("Синтаксическая ошибка: не найден оператор присваивания после ключевого слова 'for'");
            return false;
        }
        if (lex.EQ("to")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найдено ключевое слово 'to'");
            return false;
        }
        if (!expression()) {
            return false;
        }
        if(lex.EQ("step")) {
            gl();
            if(!expression()) {
                er("Синтаксическая ошибка: не найдено выражение после ключевого слова 'step'");
                return false;
            }
        }
        if(!operator()) {
            er("Синтаксическая ошибка: не найден оператор в фиксированном цикле");
            return false;
        }
        if(lex.EQ("next")) {
            gl();
            return true;
        }
        else {
            er("Синтаксическая ошибка: не найдено ключевое слово 'next'");
            return false;
        }
    }

    private boolean conditionalLoop() {//условного цикла
        if(lex.EQ("while")) {
            gl();
        }
        else {
            return false;
        }
        if(lex.EQ("(")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден символ '(' после ключевого слова 'while'");
            return false;
        }
        if(!expression()) {
            er("Синтаксическая ошибка: не найдено выражение в условном цикле");
            return false;
        }
        if(lex.EQ(")")) {
            gl();
        }
        else {
            er("Синтаксическая ошибка: не найден символ ')' в условном цикле");
            return false;
        }
        if(!operator()) {
            er("Синтаксическая ошибка: не найден оператор в условном цикле");
            return false;
        }
        else {
            return true;
        }
    }

    private boolean input() {//ввода
        if(lex.EQ("readln")) {
            gl();
        }
        else {
            return false;
        }
        if(!identifier()) {
            return false;
        }
        opStack.pop();
        while(lex.EQ(",")) {
            gl();
            if(!identifier()) {
                er("Синтаксическая ошибка: не найден идентификатор после ',' в операторе ввода");
                return false;
            }
            opStack.pop();
        }

        return true;
    }

    private boolean output() {//вывода
        if(lex.EQ("writeln")) {
            gl();
        }
        else {
            return false;
        }
        if(!expression()) {
            return false;
        }
        opStack.pop();
        while(lex.EQ(",")) {
            gl();
            if(!expression()) {
                er("Синтаксическая ошибка: не найдено выражение после ',' в операторе вывода");
                return false;
            }
            opStack.pop();
        }

        return true;
    }

    private boolean expression() {
        if(notOperand()) {
            return false;
        }
        while (relationGroupOperations()) {
            if(notOperand()) {
                er("Синтаксическая ошибка: не найден операнд в выражении");
                return false;
            }
            checkOp();
        }

        return true;
    }

    private boolean relationGroupOperations() {
        if (lex.EQ("<>") || lex.EQ("=") || lex.EQ("<") || lex.EQ("<=") || lex.EQ(">") || lex.EQ(">=")){
            opStack.push(Tables.tl[lex.getIndex()]);
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean identifier() {
        if (lex.ID()) {
            checkId();
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean number() {
        if (lex.NUM()) {
            if(Tables.tn.get(lex.getIndex()).matches(".*\\..+")) {
                opStack.push("float");
            }
            else {
                opStack.push("int");
            }
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean type() {
        if (lex.EQ("int") || lex.EQ("float") || lex.EQ("bool")) {
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean notOperand() {
        if(notSummand()) {
            return true;
        }
        while (additionGroupOperations()) {
            if(notSummand()) {
                er("Синтаксическая ошибка: не найдено слагаемое");
                return true;
            }
            checkOp();
        }

        return false;
    }

    private boolean notSummand() {//слагаемое
        if(!multiplier()) {
            return true;
        }
        while (multiplicationGroupOperations()) {
            if(!multiplier()) {
                er("Синтаксическая ошибка: не найден множитель");
                return true;
            }
            checkOp();
        }

        return false;
    }

    private boolean multiplicationGroupOperations() {//операции группы умножения
        if (lex.EQ("*") || lex.EQ("/") || lex.EQ("and")) {
            opStack.push(Tables.tl[lex.getIndex()]);
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean additionGroupOperations() {//операции группы сложения
        if (lex.EQ("+") || lex.EQ("-") || lex.EQ("or")) {
            opStack.push(Tables.tl[lex.getIndex()]);
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean multiplier() {//множитель
        if(identifier() || number() || booleanConstant()) {
            return true;
        }
        else if(identifier()) {
            return true;
        }
        else if(unaryOperator()) {
            if(multiplier()) {
                checkNot();
                return true;
            }
            else {
                er("Синтаксическая ошибка: не найден множитель после унарного оператора");
                return false;
            }
        }
        else if(lex.EQ("(")) {
            gl();
            if(!expression()) {
                er("Синтаксическая ошибка: не найдено выражение после '('");
                return false;
            }
            if(lex.EQ(")")) {
                gl();
                return true;
            }
            else {
                er("Синтаксическая ошибка: не найден символ ')'");
                return false;
            }
        }
        else {
            er("Синтаксическая ошибка: не найден множитель");
            return false;
        }
    }

    private boolean unaryOperator() {
        if (lex.EQ("not")) {
            opStack.push("not");
            gl();
            return true;
        }
        else
            return false;
    }

    private boolean booleanConstant() {
        if (lex.EQ("true") || lex.EQ("false")) {
            opStack.push("bool");
            gl();
            return true;
        }
        else
            return false;
    }

    //Считывание следующей лексемы
    private void gl(){
        try {
            int c;
            c = bufferedInputStream.read();

            StringBuilder table = new StringBuilder();
            StringBuilder index = new StringBuilder();
            if(c == -1) {
                return;
            }
            if ((char) c == '[') {
                c = bufferedInputStream.read();
                while ((char) c != ',') {
                    table.append((char) c);
                    c = bufferedInputStream.read();
                }
                c = bufferedInputStream.read();
                while ((char) c != ']') {
                    index.append((char) c);
                    c = bufferedInputStream.read();
                }
            }
            lex.set(Integer.parseInt(table.toString()), Integer.parseInt(index.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void er(String message) {
        System.out.println(message);
        System.exit(0);
    }

    private void checkId() {
        for(int i = 0; i<ti.size(); i++) {
            if(lex.getIndex() == i) {
                if ((ti.get(i).isDescribed())) {
                    opStack.push(ti.get(i).getType());
                }
                else er("Семантическая ошибка: идентификатор не описан");
            }
        }
    }

    private void checkOp() {
        String type2 = opStack.pop();
        String op = opStack.pop();
        String type1 = opStack.pop();
        getType(op, type1, type2);
    }

    private void getType(String op, String t1, String t2) {
        for(TOP a : top) {
            if(a.getOperation().equals(op)) {
                if(a.getType1().equals(t1)) {
                    if(a.getType2().equals(t2)) {
                        opStack.push(a.getResultType());
                        return;
                    }
                }
            }
        }
        er("Семантическая ошибка: использован неверный тип в выражении");
    }

    private void checkNot() {
        String type = opStack.pop();
        String op = opStack.pop();

        if(!op.equals("not") && !type.equals("bool")) {
            er("Семантическая ошибка: ошибка при вызове унарного оператора");
        }

        opStack.push("bool");
    }
}
