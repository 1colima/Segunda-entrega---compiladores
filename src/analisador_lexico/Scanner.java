package analisador_lexico;

import java.util.Set;
import java.util.HashSet;

public class Scanner {
    private final String codigoFonte;
    private int ponteiro = 0;
    private int linha = 1;
    private int coluna = 1;

    // Tabela de palavras reservadas exigidas no escopo da linguagem, Set<String> é uma coleção que não permite elementos duplicados
    private static final Set<String> PALAVRAS_RESERVADAS = new HashSet<>();
 

static {
    String[] palavras = {
        "int",
        "double",
        "bool",
        "char",
        "string",
        "if",
        "else",
        "while",
        "return",
        "true",
        "false"
    };

    for (String p : palavras) {
        PALAVRAS_RESERVADAS.add(p);
    }
}

    public Scanner(String codigoFonte) {
        this.codigoFonte = codigoFonte;
    }

    // Retorna o caractere atual sem avançar o cursor (peek)
    private char peek() {
        if (isAtEnd()) return '\0';
        return codigoFonte.charAt(ponteiro);
    }

    // Retorna o caractere atual e avança o cursor (advance)
    private char advance() {
        char atual = peek();
        ponteiro++;
        if (atual == '\n') {
            linha++;
            coluna = 1;
        } else {
            coluna++;
        }
        return atual;
    }

    private boolean isAtEnd() {
        return ponteiro >= codigoFonte.length();
    }

    private boolean ehDelimitador(char c) {
    return c == '(' ||
           c == ')' ||
           c == '{' ||
           c == '}' ||
           c == ',' ||
           c == ';';
}

    // Método principal que retorna o próximo token (aplicando Maximal Munch)
    public Token nextToken() {
        while (!isAtEnd()) {
            char c = peek();

            // 1. Ignorar espaços em branco, tabs e quebras de linha fora de strings
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                advance();
                continue;
            }

            // 2. Ignorar Comentários (linha // e bloco /* */)
            if (c == '/') {
                if (ponteiro + 1 < codigoFonte.length() && codigoFonte.charAt(ponteiro + 1) == '/') {
                    // Comentário de linha
                    while (!isAtEnd() && peek() != '\n') {
                        advance();
                    }
                    continue;
                } else if (ponteiro + 1 < codigoFonte.length() && codigoFonte.charAt(ponteiro + 1) == '*') {
                    // Comentário de bloco
                    int linhaInicio = linha;
                    int colunaInicio = coluna;
                    advance(); advance(); // consome "/*"
                    boolean fechou = false;
                    while (!isAtEnd()) {
                        if (peek() == '*' && ponteiro + 1 < codigoFonte.length() && codigoFonte.charAt(ponteiro + 1) == '/') {
                            advance(); advance(); // consome "*/"
                            fechou = true;
                            break;
                        }
                        advance();
                    }
                    if (!fechou) {
                        return new Token(TokenType.ERRO, "Comentário de bloco não fechado até o EOF", linhaInicio, colunaInicio);
                    }
                    continue;
                }
            }

            int inicioLinha = linha;
            int inicioColuna = coluna;

            // 3. Identificadores e Palavras Reservadas (Começam com letra ou '_')
            if (Character.isLetter(c) || c == '_') {
                StringBuilder lexema = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    lexema.append(advance());
                }
                
                String texto = lexema.toString();
                // Como a linguagem não é case-sensitive, podemos checar em minúsculo na tabela
                if (PALAVRAS_RESERVADAS.contains(texto.toLowerCase())) {
                    return new Token(TokenType.PALAVRA_RESERVADA, texto, inicioLinha, inicioColuna);
                } else {
                    return new Token(TokenType.IDENTIFICADOR, texto, inicioLinha, inicioColuna);
                }
            }

           // 4. Literais Numéricos (Inteiros ou Decimais)
if (Character.isDigit(c)) {
    StringBuilder lexema = new StringBuilder();

    // Parte inteira
    while (!isAtEnd() && Character.isDigit(peek())) {
        lexema.append(advance());
    }

    // Parte decimal opcional:
    // só consome o ponto se houver um dígito depois dele
    if (!isAtEnd()
            && peek() == '.'
            && ponteiro + 1 < codigoFonte.length()
            && Character.isDigit(codigoFonte.charAt(ponteiro + 1))) {

        lexema.append(advance()); // consome '.'

        while (!isAtEnd() && Character.isDigit(peek())) {
            lexema.append(advance());
        }
    }

    return new Token(
        TokenType.LITERAL_NUMERICO,
        lexema.toString(),
        inicioLinha,
        inicioColuna
    );
}
 // 5. Strings (Delimitadas por aspas duplas)
if (c == '"') {
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance()); // consome a aspa inicial
    boolean fechou = false;

    while (!isAtEnd()) {
        char atual = peek();

        // Fecha a string
        if (atual == '"') {
            lexema.append(advance());
            fechou = true;
            break;
        }

        // Trata sequências de escape
        if (atual == '\\') {
            lexema.append(advance()); // consome '\'

            if (isAtEnd()) {
                return new Token(
                    TokenType.ERRO,
                    "String não fechada",
                    inicioLinha,
                    inicioColuna
                );
            }

            char escape = peek();

            if (escape == '"' || escape == 'n' || escape == '\\') {
                lexema.append(advance());
            } else {
                lexema.append(advance());

                return new Token(
                    TokenType.ERRO,
                    "Sequência de escape inválida: \\" + escape,
                    inicioLinha,
                    inicioColuna
                );
            }

            continue;
        }

        // Qualquer outro caractere é permitido,
        // inclusive quebra de linha
        lexema.append(advance());
    }

    if (!fechou) {
        return new Token(
            TokenType.ERRO,
            "String não fechada até o EOF",
            inicioLinha,
            inicioColuna
        );
    }

    return new Token(
        TokenType.STRING,
        lexema.toString(),
        inicioLinha,
        inicioColuna
    );
}

// 6. Literais de caractere
if (c == '\'') {
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance()); // consome a aspa simples inicial

    if (isAtEnd()) {
        return new Token(
            TokenType.ERRO,
            "Literal char não fechado",
            inicioLinha,
            inicioColuna
        );
    }

    char atual = peek();

    // Caso seja sequência de escape
    if (atual == '\\') {
        lexema.append(advance()); // consome '\'

        if (isAtEnd()) {
            return new Token(
                TokenType.ERRO,
                "Literal char não fechado",
                inicioLinha,
                inicioColuna
            );
        }

        char escape = peek();

        if (escape == 'n' || escape == '\\' || escape == '\'') {
            lexema.append(advance());
        } else {
            lexema.append(advance());

            return new Token(
                TokenType.ERRO,
                "Escape inválido em char: \\" + escape,
                inicioLinha,
                inicioColuna
            );
        }

    } else {
        // caractere comum
        lexema.append(advance());
    }

    // Depois do caractere deve haver a aspa final
    if (isAtEnd() || peek() != '\'') {
        return new Token(
            TokenType.ERRO,
            "Literal char inválido ou não fechado",
            inicioLinha,
            inicioColuna
        );
    }

    lexema.append(advance()); // consome a aspa final

    return new Token(
        TokenType.CHAR_LITERAL,
        lexema.toString(),
        inicioLinha,
        inicioColuna
    );
}

// 7. Delimitadores
if (ehDelimitador(c)) {
    char delimitador = advance();

    return new Token(
        TokenType.DELIMITADOR,
        String.valueOf(delimitador),
        inicioLinha,
        inicioColuna
    );
}



// 8. Operadores
if (c == '=' || c == '<' || c == '>' || c == '!' ||
    c == '+' || c == '-' || c == '*' || c == '/' ||
    c == '&' || c == '|') {

    char primeiro = advance();

    // Operadores && e ||
    if (primeiro == '&' || primeiro == '|') {

        if (!isAtEnd() && peek() == primeiro) {
            char segundo = advance();

            return new Token(
                TokenType.OPERADOR,
                "" + primeiro + segundo,
                inicioLinha,
                inicioColuna
            );
        }

        // & e | sozinhos não são operadores válidos
        return new Token(
            TokenType.ERRO,
            "Operador inválido: " + primeiro,
            inicioLinha,
            inicioColuna
        );
    }

    // Operadores compostos: ==, <=, >= e !=
    if (!isAtEnd() && peek() == '=') {
        char segundo = advance();

        return new Token(
            TokenType.OPERADOR,
            "" + primeiro + segundo,
            inicioLinha,
            inicioColuna
        );
    }

    // Operadores simples: =, <, >, !, +, -, *, /
    return new Token(
        TokenType.OPERADOR,
        String.valueOf(primeiro),
        inicioLinha,
        inicioColuna
    );
}

// 9. Erro Léxico
char invalido = advance();

return new Token(
    TokenType.ERRO,
    "Caractere inválido: " + invalido,
    inicioLinha,
    inicioColuna
);

} // fecha o while

return new Token(
    TokenType.EOF,
    "",
    linha,
    coluna
);

} // fecha nextToken()

} // fecha Scanner