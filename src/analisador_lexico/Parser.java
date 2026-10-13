package analisador_lexico;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

public final class Parser {
    private final Scanner scanner;
    private final List<ParseError> errors = new ArrayList<>();
    private Token lookahead;
    private long consumed = 0;
    private ParseResult result;

    public Parser(String source) {
        this(new Scanner(source));
    }

    public Parser(Scanner scanner) {
        if (scanner == null) throw new IllegalArgumentException("Scanner não pode ser null.");
        this.scanner = scanner;
        readNext();
    }

    /** Pode ser consultado novamente sem analisar a mesma entrada duas vezes. */
    public ParseResult parse() {
        if (result == null) {
            parsePrograma();
            result = new ParseResult(errors, is("EOF"));
        }
        return result;
    }

    /** verifica a categoria do token atual em lookahead.tipp = token atual.  */
    private String symbol() {
        switch (lookahead.tipo) {
            case IDENTIFICADOR: return "ID";
            case LITERAL_NUMERICO: return "NUM";
            case STRING: return "STR";
            case CHAR_LITERAL: return "CHAR";
            case EOF: return "EOF";
            case PALAVRA_RESERVADA: return lookahead.lexema.toLowerCase(Locale.ROOT);
            default: return lookahead.lexema;
        }
    }

    //symbol retorna o token atual e compara com o esperado, que é a string passada em is
    private boolean is(String expected) {
        return symbol().equals(expected);
    }

    // pede um token pro scanner e coloca em lookahead como token atual. se tiver um erro, parser registra o erro e o token depois incrementa o contador
    //registra os erros e continua ate um token que possa analisar
    private void readNext() {
        do {
            lookahead = scanner.nextToken();
            if (lookahead.tipo == TokenType.ERRO) {
                errors.add(new ParseError("Erro léxico", "scanner", lookahead.lexema, lookahead));
                consumed++;
            }
        } while (lookahead.tipo == TokenType.ERRO);
    }

    //consome o token atual e avanca
    private void advance() {
        if (!is("EOF")) {
            consumed++;
            readNext();
        }
    }

    private void report(String name, String message) {
        errors.add(new ParseError("Erro sintático", name, message, lookahead));
    }

    private void error(String name, String message) {
        report(name, message);
    }

    //descarta token ate encontrar um ponto de parada, que é um token que pertence a FOLLOW do não-terminal atual, ou EOF, ou ; ou }
    //sync e um conjunto de strings que representa os tokens que podem ser usados como ponto de parada
    private void sincronizar(Set<String> sync) {
        while (!sync.contains(symbol()) && !is("EOF") && !is(";") && !is("}")) {
            advance();
        }
    }
    //confere se o token atual e o esperado, se for, consome o token e avanca. Se nao for, registra o erro e tenta sincronizar para continuar a analise.
    private void match(String expected, String owner) {
        if (is(expected)) {
            advance();
        } else {
            error(owner, "Esperava '" + expected + "', achei " + symbol());
            Set<String> sync = new HashSet<>(FirstFollow.follow(owner));
            sync.add(expected);
            if (expected.equals("EOF")) {
                while (!is("EOF")) advance();
            } else {
                sincronizar(sync);
                if (is(expected)) advance();
            }
        }
    }

    // um programa e formado por uma sequencia de elementos e termina com EOF
    private void parsePrograma() {
        parseElementos();
        match("EOF", "programa");
    }

    //reconhecer um elemento e reconhecer os seguintes ou terminar a lista
    private void parseElementos() {
        //verifica se o token atual pode comecar um elemento
        if (is("ID") || is("bool") || is("char") || is("double") || is("int") || is("string")) {
            parseElemento();
            parseElementos();
        } else if (FirstFollow.follow("elementos").contains(symbol())) {
            // uando nao ha mais elementos, o token atual e EOF
        } else {
            //quando aparece um token invalido
            error("elementos", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("elementos"));
            if (!is("EOF")) {
                advance(); //consome a âncora abandonada após o erro
                parseElementos();
            }
        }
    }

    //os elementos sao formados por elementos tipados ou procedimentos
    private void parseElemento() {
        if (is("bool") || is("char") || is("double") || is("int") || is("string")) {
            parseElementoTipado();
        } else if (is("ID")) {
            parseProcedimento();
        } else {
            error("elemento", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("elemento"));
        }
    }

    //os tipos sao formados por int, double, bool, char e string
    private void parseTipo() {
        if (is("int")) {
            match("int", "tipo");
        } else if (is("double")) {
            match("double", "tipo");
        } else if (is("bool")) {
            match("bool", "tipo");
        } else if (is("char")) {
            match("char", "tipo");
        } else if (is("string")) {
            match("string", "tipo");
        } else {
            error("tipo", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("tipo"));
        }
    }

    // elementos tipados sao formados por tipo, identificador, e continuacao tipada
    private void parseElementoTipado() {
        parseTipo();
        match("ID", "elemento_tipado");
        parseContinuacaoTipado();
    }

    //continuacao tipada pode ser um ponto e virgula, ou um sinal de igual seguido de uma expressao e um ponto e virgula, ou um parenteses seguido de parametros e um bloco
    private void parseContinuacaoTipado() {
        if (is(";")) {
            match(";", "continuacao_tipado");
        } else if (is("=")) {
            match("=", "continuacao_tipado");
            parseExpressao();
            match(";", "continuacao_tipado");
        } else if (is("(")) {
            match("(", "continuacao_tipado");
            parseParametros();
            match(")", "continuacao_tipado");
            parseBloco();
        } else {
            error("continuacao_tipado", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("continuacao_tipado"));
        }
    }

    //formados por um identificador, seguido de parentese com parametros e um bloco
    private void parseProcedimento() {
        match("ID", "procedimento");
        match("(", "procedimento");
        parseParametros();
        match(")", "procedimento");
        parseBloco();
    }

    //parametros sao formados por um tipo, identificador e mais parametros
    private void parseParametros() {
        if (is("bool") || is("char") || is("double") || is("int") || is("string")) {
            parseTipo();
            match("ID", "parametros");
            parseMaisParametros();
        } else if (FirstFollow.follow("parametros").contains(symbol())) {
            //quando nao ha parametros
        } else {
            error("parametros", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("parametros"));
        }
    }

    //mais parametros sao formados por uma virgula, um tipo, um identificador e mais parametros
    private void parseMaisParametros() {
        if (is(",")) {
            match(",", "mais_parametros");
            parseTipo();
            match("ID", "mais_parametros");
            parseMaisParametros();
        } else if (FirstFollow.follow("mais_parametros").contains(symbol())) {
            //quando nao ha mais parametros
        } else {
            // token invalido
            error("mais_parametros", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_parametros"));
        }
    }

    //blocos sao formados por chaves e comandos dentro delas
    private void parseBloco() {
        match("{", "bloco");
        parseComandos();
        match("}", "bloco");
    }

    //comandos sao formados por um comando seguido de mais comandos ou podem ser vazios
    private void parseComandos() {
        if (is("ID") || is("bool") || is("char") || is("double") || is("if") || is("int") || is("return") || is("string") || is("while") || is("{")) {
            parseComando();
            parseComandos();
        } else if (FirstFollow.follow("comandos").contains(symbol())) {
            // quando nao ha mais comandos
        } else {
            error("comandos", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("comandos"));
            if (!is("EOF") && !is("}")) {
                advance(); // consome a âncora abandonada após o erro -> token atual nao pertence a FOLLOW e nem EOF, entao consome o token e tenta continuar a analise
                parseComandos();
            }
        }
    }

    //comandos sao formados por declaracoes, comandos com id, selecoes, repeticos, retornos e blocos
    private void parseComando() {
        if (is("bool") || is("char") || is("double") || is("int") || is("string")) {
            parseDeclaracaoLocal();
        } else if (is("ID")) {
            parseComandoId();
        } else if (is("if")) {
            parseSelecao();
        } else if (is("while")) {
            parseRepeticao();
        } else if (is("return")) {
            parseRetorno();
        } else if (is("{")) {
            parseBloco();
        } else {
            error("comando", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("comando"));
        }
    }

    //declaracoes locais sao formados por tipo, identificador, inicializacao e ponto e virgula
    private void parseDeclaracaoLocal() {
        parseTipo();
        match("ID", "declaracao_local");
        parseInicializacao();
        match(";", "declaracao_local");
    }

    //inicializacao pode ser um sinal de igual seguido de uma expressao ou pode ser vazio
    private void parseInicializacao() {
        if (is("=")) {
            match("=", "inicializacao");
            parseExpressao();
        } else if (FirstFollow.follow("inicializacao").contains(symbol())) {
            //quando nao tem inicializacao
        } else {
            error("inicializacao", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("inicializacao"));
        }
    }

    //omandos com id sao formados por um identificador seguido de uma continuacao de comando com id
    private void parseComandoId() {
        match("ID", "comando_id");
        parseContinuacaoComandoId();
    }

    //<continuacao_comando_id> ::= = expressao ; | ( argumentos ) ;
    //analisa os tokens depois de receber um identificador, se o token atual for =, consome o token e chama parseExpressao e exige ;
    //se for ( reconhece uma chamaada, consome (, analisa os argumentos e exige )
    //caso contrario, registra um erro e sincroniza ate encontrar um token que possa ser analisado
    private void parseContinuacaoComandoId() {
        if (is("=")) {
            match("=", "continuacao_comando_id");
            parseExpressao();
            match(";", "continuacao_comando_id");
        } else if (is("(")) {
            match("(", "continuacao_comando_id");
            parseArgumentos();
            match(")", "continuacao_comando_id");
            match(";", "continuacao_comando_id");
        } else {
            error("continuacao_comando_id", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("continuacao_comando_id"));
        }
    }

    //o if deve ser escrito na ordem if, (, <expressao>, ), <bloco>, else ou nada
    private void parseSelecao() {
        match("if", "selecao");
        match("(", "selecao");
        parseExpressao();
        match(")", "selecao");
        parseBloco();
        parseSenao();
    }

    //verifica se existe um else depois do bloco ou vazio.
    private void parseSenao() {
        if (is("else")) {
            match("else", "senao");
            parseBloco();
        } else if (FirstFollow.follow("senao").contains(symbol())) {
            // ε: o lookahead pertence a FOLLOW(senao).
        } else {
            error("senao", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("senao"));
        }
    }

    //reconhece um comando while.
    //espera While, "(", <expressao>, ")"
    private void parseRepeticao() {
        match("while", "repeticao");
        match("(", "repeticao");
        parseExpressao();
        match(")", "repeticao");
        parseBloco();
    }

    // <retorno> ::= return valor_retorno ;
    // reconhece e consome return, reconhece <expressao>, exige e consome ;
    private void parseRetorno() {
        match("return", "retorno");
        parseValorRetorno();
        match(";", "retorno");
    }

    //decide se o return tem uma expressao ou esta sem valor
    private void parseValorRetorno() {
        if (is("!") || is("(") || is("+") || is("-") || is("CHAR") || is("ID") || is("NUM") || is("STR") || is("false") || is("true")) {
            parseExpressao();
        } else if (FirstFollow.follow("valor_retorno").contains(symbol())) {
            //o return nao tem valor, o token atual e ;
        } else {
            error("valor_retorno", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("valor_retorno"));
        }
    }

    // reconhece os argumenots passados em uma chamada
    // expressao seguida de outros argumento ou nenhum argumento 
    private void parseArgumentos() {
        if (is("!") || is("(") || is("+") || is("-") || is("CHAR") || is("ID") || is("NUM") || is("STR") || is("false") || is("true")) {
            parseExpressao();
            parseMaisArgumentos();
        } else if (FirstFollow.follow("argumentos").contains(symbol())) {
            // ε: o lookahead pertence a FOLLOW(argumentos).
            //podemos considerar a lista de argumentos vazia porque o token que vem depois e valido
        } else {
            error("argumentos", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("argumentos"));
        }
    }

    //reconhece os argumentos que vem depois do primeiro. separados por virgulas.
    // uma virgula, outra expressao e possiveis argumentos seguintes ou nao.
    private void parseMaisArgumentos() {
        if (is(",")) {
            match(",", "mais_argumentos");
            parseExpressao();
            parseMaisArgumentos();
        } else if (FirstFollow.follow("mais_argumentos").contains(symbol())) {
            // ε: o lookahead pertence a FOLLOW(mais_argumentos). -> a lista de argumentos terminou porque o token atual e ).
        } else {
            error("mais_argumentos", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_argumentos"));
        }
    }

    //expressao e formada por uma disjuncao
    private void parseExpressao() {
        parseDisjuncao();
    }

    // disjuncao e formada por uma conjuncao seguida de possiveis disjuncao adicionais
    private void parseDisjuncao() {
        parseConjuncao();
        parseMaisDisjuncao();
    }

    // reconhece a continuacao de uma disjuncao com o operador ou
    private void parseMaisDisjuncao() {
        if (is("||")) {
            match("||", "mais_disjuncao");
            parseConjuncao();
            parseMaisDisjuncao();
        } else if (FirstFollow.follow("mais_disjuncao").contains(symbol())) {
            // nao ha mais || na expressao, o proximo token e ;
        } else {
            error("mais_disjuncao", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_disjuncao"));
        }
    }

    //uma conjuncao e formada por uma igualdade, uma continuacao que pode conter && ou ser vazia.
    private void parseConjuncao() {
        parseIgualdade();
        parseMaisConjuncao();
    }

    // reconhece a continuacao de uma expressao com &&.
    private void parseMaisConjuncao() {
        if (is("&&")) {
            match("&&", "mais_conjuncao");
            parseIgualdade();
            parseMaisConjuncao();
        } else if (FirstFollow.follow("mais_conjuncao").contains(symbol())) {
            // a lista de conjunções terminou porque o token atual é ;.
        } else {
            error("mais_conjuncao", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_conjuncao"));
        }
    }

    // uma igualdade e formada por uma relacao seguida de possiveis continuacoes com == ou !=
    private void parseIgualdade() {
        parseRelacao();
        parseMaisIgualdade();
    }

    //reconhece a continuacao de uma igualdade com == ou !=, ou pode ser vazia
    private void parseMaisIgualdade() {
        if (is("==")) {
            match("==", "mais_igualdade");
            parseRelacao();
            parseMaisIgualdade();
        } else if (is("!=")) {
            match("!=", "mais_igualdade");
            parseRelacao();
            parseMaisIgualdade();
        } else if (FirstFollow.follow("mais_igualdade").contains(symbol())) {
            // a lista de igualdades terminou porque o token atual é ;
        } else {
            error("mais_igualdade", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_igualdade"));
        }
    }

    // <relacao> ::= soma mais_relacao
    private void parseRelacao() {
        parseSoma();
        parseMaisRelacao();
    }

    // econhece a continuacao de uma relacao com <, >, <= ou >=, ou pode ser vazia
    private void parseMaisRelacao() {
        if (is("<")) {
            match("<", "mais_relacao");
            parseSoma();
            parseMaisRelacao();
        } else if (is(">")) {
            match(">", "mais_relacao");
            parseSoma();
            parseMaisRelacao();
        } else if (is("<=")) {
            match("<=", "mais_relacao");
            parseSoma();
            parseMaisRelacao();
        } else if (is(">=")) {
            match(">=", "mais_relacao");
            parseSoma();
            parseMaisRelacao();
        } else if (FirstFollow.follow("mais_relacao").contains(symbol())) {
            // a lista de relacoes terminou porque o token atual é ;
        } else {
            error("mais_relacao", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_relacao"));
        }
    }

    //uma soma e formada por um produto seguido de possiveis continuacoes com + ou - ou pode ser vazia
    private void parseSoma() {
        parseProduto();
        parseMaisSoma();
    }

    //reconhece a continuacao de uma soma com + ou - ou pode ser vazia
    private void parseMaisSoma() {
        if (is("+")) {
            match("+", "mais_soma");
            parseProduto();
            parseMaisSoma();
        } else if (is("-")) {
            match("-", "mais_soma");
            parseProduto();
            parseMaisSoma();
        } else if (FirstFollow.follow("mais_soma").contains(symbol())) {
            //a lista de somas terminou porque o token atual é ;
        } else {
            error("mais_soma", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_soma"));
        }
    }

    //um produto e formado por um termo unario seguido de possiveis continuacoes com * ou /
    private void parseProduto() {
        parseUnario();
        parseMaisProduto();
    }

    //econhece a continuacao de uma expressao com * ou /, ou pode ser vazia
    private void parseMaisProduto() {
        if (is("*")) {
            match("*", "mais_produto");
            parseUnario();
            parseMaisProduto();
        } else if (is("/")) {
            match("/", "mais_produto");
            parseUnario();
            parseMaisProduto();
        } else if (FirstFollow.follow("mais_produto").contains(symbol())) {
            //a lista de produtos terminou porque o token atual é ;.
        } else {
            error("mais_produto", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("mais_produto"));
        }
    }

    // reconhece um operador unario seguido de outro unario ou um primario
    private void parseUnario() {
        if (is("!")) {
            match("!", "unario");
            parseUnario();
        } else if (is("-")) {
            match("-", "unario");
            parseUnario();
        } else if (is("+")) {
            match("+", "unario");
            parseUnario();
        } else if (is("(") || is("CHAR") || is("ID") || is("NUM") || is("STR") || is("false") || is("true")) {
            parsePrimario();
        } else {
            error("unario", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("unario"));
        }
    }

    //primario pode ser um numero, uma string, um char, true, false, um identificador seguido de possiveis argumentos ou uma expressao entre parenteses
    private void parsePrimario() {
        if (is("NUM")) {
            match("NUM", "primario");
        } else if (is("STR")) {
            match("STR", "primario");
        } else if (is("CHAR")) {
            match("CHAR", "primario");
        } else if (is("true")) {
            match("true", "primario");
        } else if (is("false")) {
            match("false", "primario");
        } else if (is("ID")) {
            match("ID", "primario");
            parseSufixoId();
        } else if (is("(")) {
            match("(", "primario");
            parseExpressao();
            match(")", "primario");
        } else {
            error("primario", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("primario"));
        }
    }

    //reconhece os argumentos passados em uma chamada de função, ou pode ser vazio
    private void parseSufixoId() {
        if (is("(")) {
            match("(", "sufixo_id");
            parseArgumentos();
            match(")", "sufixo_id");
        } else if (FirstFollow.follow("sufixo_id").contains(symbol())) {
            // ε: o lookahead pertence a FOLLOW(sufixo_id).
        } else {
            error("sufixo_id", "Token inesperado: " + symbol());
            sincronizar(FirstFollow.follow("sufixo_id"));
        }
    }

    //classe que representa um erro de análise
    public static final class ParseError {
        public final String natureza;
        public final String contexto;
        public final String mensagem;
        public final int linha;
        public final int coluna;
        public final String encontrado;
    
        ParseError(String natureza, String contexto, String mensagem, Token token) {
            this.natureza = natureza;
            this.contexto = contexto;
            this.mensagem = mensagem;
            this.linha = token.linha;
            this.coluna = token.coluna;
            this.encontrado = token.tipo == TokenType.EOF ? "EOF" : token.lexema;
        }
    
        @Override
        public String toString() {
            return String.format("%s em <%s>, linha %d, coluna %d: %s (encontrado: '%s')",
                    natureza, contexto, linha, coluna, mensagem, encontrado);
        }
    }
    //classe que representa o resultado da análise, contendo a lista de erros e se chegou ao fim do arquivo
    public static final class ParseResult {
        private final List<ParseError> erros;
        private final boolean chegouAoFim;
    
        ParseResult(List<ParseError> erros, boolean chegouAoFim) {
            this.erros = Collections.unmodifiableList(new ArrayList<>(erros));
            this.chegouAoFim = chegouAoFim;
        }
    
        public boolean isAccepted() {
            return chegouAoFim && erros.isEmpty();
        }
    
        public boolean reachedEnd() {
            return chegouAoFim;
        }
    
        public List<ParseError> getErrors() {
            return erros;
        }
    }
    //classe que representa o conjunto de FIRST e FOLLOW para cada não-terminal da gramática
    private static final class FirstFollow {
        private static final Map<String, Set<String>> FIRST = new LinkedHashMap<>();
        private static final Map<String, Set<String>> FOLLOW = new LinkedHashMap<>();
        private FirstFollow() {}
        static {
            FIRST.put("programa", Set.of("EOF", "ID", "bool", "char", "double", "int", "string"));
            FOLLOW.put("programa", Set.of("$"));
            FIRST.put("elementos", Set.of("ID", "bool", "char", "double", "int", "string", "ε"));
            FOLLOW.put("elementos", Set.of("EOF"));
            FIRST.put("elemento", Set.of("ID", "bool", "char", "double", "int", "string"));
            FOLLOW.put("elemento", Set.of("EOF", "ID", "bool", "char", "double", "int", "string"));
            FIRST.put("tipo", Set.of("bool", "char", "double", "int", "string"));
            FOLLOW.put("tipo", Set.of("ID"));
            FIRST.put("elemento_tipado", Set.of("bool", "char", "double", "int", "string"));
            FOLLOW.put("elemento_tipado", Set.of("EOF", "ID", "bool", "char", "double", "int", "string"));
            FIRST.put("continuacao_tipado", Set.of("(", ";", "="));
            FOLLOW.put("continuacao_tipado", Set.of("EOF", "ID", "bool", "char", "double", "int", "string"));
            FIRST.put("procedimento", Set.of("ID"));
            FOLLOW.put("procedimento", Set.of("EOF", "ID", "bool", "char", "double", "int", "string"));
            FIRST.put("parametros", Set.of("bool", "char", "double", "int", "string", "ε"));
            FOLLOW.put("parametros", Set.of(")"));
            FIRST.put("mais_parametros", Set.of(",", "ε"));
            FOLLOW.put("mais_parametros", Set.of(")"));
            FIRST.put("bloco", Set.of("{"));
            FOLLOW.put("bloco", Set.of("EOF", "ID", "bool", "char", "double", "else", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("comandos", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "ε"));
            FOLLOW.put("comandos", Set.of("}"));
            FIRST.put("comando", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{"));
            FOLLOW.put("comando", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("declaracao_local", Set.of("bool", "char", "double", "int", "string"));
            FOLLOW.put("declaracao_local", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("inicializacao", Set.of("=", "ε"));
            FOLLOW.put("inicializacao", Set.of(";"));
            FIRST.put("comando_id", Set.of("ID"));
            FOLLOW.put("comando_id", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("continuacao_comando_id", Set.of("(", "="));
            FOLLOW.put("continuacao_comando_id", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("selecao", Set.of("if"));
            FOLLOW.put("selecao", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("senao", Set.of("else", "ε"));
            FOLLOW.put("senao", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("repeticao", Set.of("while"));
            FOLLOW.put("repeticao", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("retorno", Set.of("return"));
            FOLLOW.put("retorno", Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"));
            FIRST.put("valor_retorno", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true", "ε"));
            FOLLOW.put("valor_retorno", Set.of(";"));
            FIRST.put("argumentos", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true", "ε"));
            FOLLOW.put("argumentos", Set.of(")"));
            FIRST.put("mais_argumentos", Set.of(",", "ε"));
            FOLLOW.put("mais_argumentos", Set.of(")"));
            FIRST.put("expressao", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("expressao", Set.of(")", ",", ";"));
            FIRST.put("disjuncao", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("disjuncao", Set.of(")", ",", ";"));
            FIRST.put("mais_disjuncao", Set.of("||", "ε"));
            FOLLOW.put("mais_disjuncao", Set.of(")", ",", ";"));
            FIRST.put("conjuncao", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("conjuncao", Set.of(")", ",", ";", "||"));
            FIRST.put("mais_conjuncao", Set.of("&&", "ε"));
            FOLLOW.put("mais_conjuncao", Set.of(")", ",", ";", "||"));
            FIRST.put("igualdade", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("igualdade", Set.of("&&", ")", ",", ";", "||"));
            FIRST.put("mais_igualdade", Set.of("!=", "==", "ε"));
            FOLLOW.put("mais_igualdade", Set.of("&&", ")", ",", ";", "||"));
            FIRST.put("relacao", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("relacao", Set.of("!=", "&&", ")", ",", ";", "==", "||"));
            FIRST.put("mais_relacao", Set.of("<", "<=", ">", ">=", "ε"));
            FOLLOW.put("mais_relacao", Set.of("!=", "&&", ")", ",", ";", "==", "||"));
            FIRST.put("soma", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("soma", Set.of("!=", "&&", ")", ",", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("mais_soma", Set.of("+", "-", "ε"));
            FOLLOW.put("mais_soma", Set.of("!=", "&&", ")", ",", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("produto", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("produto", Set.of("!=", "&&", ")", "+", ",", "-", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("mais_produto", Set.of("*", "/", "ε"));
            FOLLOW.put("mais_produto", Set.of("!=", "&&", ")", "+", ",", "-", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("unario", Set.of("!", "(", "+", "-", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("unario", Set.of("!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("primario", Set.of("(", "CHAR", "ID", "NUM", "STR", "false", "true"));
            FOLLOW.put("primario", Set.of("!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||"));
            FIRST.put("sufixo_id", Set.of("(", "ε"));
            FOLLOW.put("sufixo_id", Set.of("!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||"));
        }
        static Set<String> first(String name) { return FIRST.get(name); }
        static Set<String> follow(String name) { return FOLLOW.get(name); }
        static boolean nullable(String name) { return first(name).contains("ε"); }
        static boolean begins(String name, String symbol) { return first(name).contains(symbol); }
    }
}
