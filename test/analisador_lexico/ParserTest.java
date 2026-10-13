package analisador_lexico;

import analisador_lexico.Parser.ParseError;
import analisador_lexico.Parser.ParseResult;


//arquivo de testes minimo para o checkpoint 2, com 3 casos validos, 3 invalidos e 1 com multiplos erros
public class ParserTest {
    private static int testes = 0;

    private static void verificarErro(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }

    private static ParseResult testar(String nome, String codigo, boolean valido) {
        ParseResult resultado = new Parser(codigo).parse();
        verificarErro(resultado.reachedEnd(), nome + ": não chegou ao EOF");
        verificarErro(resultado.isAccepted() == valido, nome + ": aceitação incorreta");
        if (!valido) {
            verificarErro(!resultado.getErrors().isEmpty(), nome + ": não reportou erro");
            for (ParseError erro : resultado.getErrors()) {
                verificarErro(erro.linha > 0 && erro.coluna > 0, nome + ": erro sem localização");
                System.out.println(erro);
            }
        }
        testes++;
        System.out.println("PASSOU: " + nome);
        return resultado;
    }

    public static void main(String[] args) {
        testar("Válido 1 — tipos e procedimento",
                "int x = 0; double d = 1.5; bool b = true; char c = 'a'; " +
                "string s = \"teste\"; executar() { x = x + 1; return; }", true);

        testar("Válido 2 — função, parâmetros, chamada e retorno",
                "int somar(int a, int b) { return a + b; } " +
                "executar() { int r = somar(10, 20); somar(r, 1); return; }", true);

        testar("Válido 3 — if/else e while",
                "executar(int limite) { int i = 0; while (i < limite) { " +
                "if (i == 2) { i = i + 2; } else { i = i + 1; } } return; }", true);

        testar("Inválido 1 — falta de ponto e vírgula",
                "executar() { int x = 1 return; }", false);

        testar("Inválido 2 — falta de operando",
                "executar() { x = 1 + ; return; }", false);

        testar("Inválido 3 — falta de parêntese",
                "executar() { while (true { return; } }", false);

        ParseResult multiplo = testar("Inválido 4 — recuperação de múltiplos erros",
                "executar() {\n" +
                "    int x = ;\n" +
                "    x = 1 + ;\n" +
                "    return;\n" +
                "}\n", false);
        verificarErro(multiplo.getErrors().stream().anyMatch(e -> e.linha == 2),
                "Não detectou o primeiro erro, na linha 2");
        verificarErro(multiplo.getErrors().stream().anyMatch(e -> e.linha == 3),
                "Não recuperou para detectar o segundo erro, na linha 3");
        verificarErro(multiplo.getErrors().stream().noneMatch(e -> e.linha == 4),
                "Erro indevido no retorno válido após a recuperação");

        System.out.println("TOTAL: " + testes + " testes passaram.");
    }
}
