# Checkpoint 2 — Gramática, FIRST/FOLLOW e verificação LL(1)

## 1. Status e decisões

Gramática do CP2, compatível com o scanner original e a opção A. Uso de IA registrado em log-uso-ia.md.

- No nível global: variáveis, funções e procedimentos. Comandos executáveis ficam em blocos.
- Funções começam com tipo; procedimentos começam diretamente com identificador, sem void ou function.
- Parâmetros são tipados; argumentos são expressões. Ambas as listas podem ser vazias.
- Uma variável por declaração, com inicialização opcional.
- if, else e while exigem chaves. else é opcional; else if deve ser escrito como else { if (...) { ... } }.
- Blocos aninhados e declarações locais são aceitos; funções e procedimentos locais não.
- Atribuições e chamadas como comandos terminam em ponto e vírgula.
- return aceita valor ou nenhum valor; adequação ao tipo e contexto é semântica.
- Programa vazio e bloco vazio são aceitos; não se exige main.
- Chamadas são permitidas em expressões; atribuições em expressões, arrays, ++ e += não são admitidos.

## 2. Terminais e scanner

| Terminal | Categoria do CP1 |
| --- | --- |
| ID | IDENTIFICADOR |
| NUM | LITERAL_NUMERICO, inteiro ou decimal |
| STR | STRING, literal entre aspas duplas |
| CHAR | CHAR_LITERAL |
| Palavras reservadas entre aspas | PALAVRA_RESERVADA, distinguida pelo lexema |
| Operadores entre aspas | OPERADOR, distinguido pelo lexema |
| Delimitadores entre aspas | DELIMITADOR, distinguido pelo lexema |
| EOF | EOF, lexema vazio |

STR e CHAR são categorias de literais; "string" e "char" são tipos. ε representa ausência de símbolos, não um token. $ é o marcador matemático após o símbolo inicial; EOF é o token real consumido no fim do programa.

Comparar palavras sem distinguir maiúsculas e minúsculas; preservar o conteúdo dos literais. Comentários e espaços são descartados pelo scanner. Token ERRO deve ser reportado com linha e coluna e impede aceitação.

O scanner pode emitir OPERADOR para += e outros lexemas compostos não admitidos pela gramática: ser tokenizado não implica ser permitido sintaticamente. Scanner.java e Token.java permanecem sem alterações.

## 3. Gramática completa em BNF

Símbolo inicial: `<programa>`.

```bnf
<programa> ::= <elementos> EOF
<elementos> ::= <elemento> <elementos> | ε
<elemento> ::= <elemento_tipado> | <procedimento>
<tipo> ::= "int" | "double" | "bool" | "char" | "string"
<elemento_tipado> ::= <tipo> ID <continuacao_tipado>
<continuacao_tipado> ::= ";" | "=" <expressao> ";" | "(" <parametros> ")" <bloco>
<procedimento> ::= ID "(" <parametros> ")" <bloco>
<parametros> ::= <tipo> ID <mais_parametros> | ε
<mais_parametros> ::= "," <tipo> ID <mais_parametros> | ε
<bloco> ::= "{" <comandos> "}"
<comandos> ::= <comando> <comandos> | ε
<comando> ::= <declaracao_local> | <comando_id> | <selecao> | <repeticao> | <retorno> | <bloco>
<declaracao_local> ::= <tipo> ID <inicializacao> ";"
<inicializacao> ::= "=" <expressao> | ε
<comando_id> ::= ID <continuacao_comando_id>
<continuacao_comando_id> ::= "=" <expressao> ";" | "(" <argumentos> ")" ";"
<selecao> ::= "if" "(" <expressao> ")" <bloco> <senao>
<senao> ::= "else" <bloco> | ε
<repeticao> ::= "while" "(" <expressao> ")" <bloco>
<retorno> ::= "return" <valor_retorno> ";"
<valor_retorno> ::= <expressao> | ε
<argumentos> ::= <expressao> <mais_argumentos> | ε
<mais_argumentos> ::= "," <expressao> <mais_argumentos> | ε
<expressao> ::= <disjuncao>
<disjuncao> ::= <conjuncao> <mais_disjuncao>
<mais_disjuncao> ::= "||" <conjuncao> <mais_disjuncao> | ε
<conjuncao> ::= <igualdade> <mais_conjuncao>
<mais_conjuncao> ::= "&&" <igualdade> <mais_conjuncao> | ε
<igualdade> ::= <relacao> <mais_igualdade>
<mais_igualdade> ::= "==" <relacao> <mais_igualdade> | "!=" <relacao> <mais_igualdade> | ε
<relacao> ::= <soma> <mais_relacao>
<mais_relacao> ::= "<" <soma> <mais_relacao> | ">" <soma> <mais_relacao> | "<=" <soma> <mais_relacao> | ">=" <soma> <mais_relacao> | ε
<soma> ::= <produto> <mais_soma>
<mais_soma> ::= "+" <produto> <mais_soma> | "-" <produto> <mais_soma> | ε
<produto> ::= <unario> <mais_produto>
<mais_produto> ::= "*" <unario> <mais_produto> | "/" <unario> <mais_produto> | ε
<unario> ::= "!" <unario> | "-" <unario> | "+" <unario> | <primario>
<primario> ::= NUM | STR | CHAR | "true" | "false" | ID <sufixo_id> | "(" <expressao> ")"
<sufixo_id> ::= "(" <argumentos> ")" | ε
```

### 3.1 Precedência

Da menor para a maior: ||, &&, igualdade, relações, soma/subtração, multiplicação/divisão, unários, primários e chamadas. Parênteses alteram o agrupamento. Os operadores binários devem ser acumulados da esquerda para a direita em uma futura avaliação/AST: a - b - c significa (a - b) - c. O reconhecedor atual não constrói AST. Unários agrupam da direita para a esquerda.

Comparações encadeadas são sintaticamente aceitas; sua validade de tipos é semântica.

## 4. Transformações

### 4.1 Fatoração de variáveis e funções

Antes:

```bnf
<elemento_tipado> ::= <tipo> ID ";"
                   | <tipo> ID "=" <expressao> ";"
                   | <tipo> ID "(" <parametros> ")" <bloco>
```

Depois:

```bnf
<elemento_tipado> ::= <tipo> ID <continuacao_tipado>
<continuacao_tipado> ::= ";" | "=" <expressao> ";" | "(" <parametros> ")" <bloco>
```

O prefixo comum é `<tipo>` ID; a decisão é adiada até ;, = ou (. Isso é fatoração, não recursão à esquerda.

### 4.2 Fatoração de comandos e primários

Antes: `<comando_id>` ::= ID "=" `<expressao>` ";" | ID "(" `<argumentos>` ")" ";".
Depois: ID `<continuacao_comando_id>`, que distingue = de (.

Nos primários, ID e ID "(" `<argumentos>` ")" são fatorados com `<sufixo_id>`. No nível global, ID inicia procedimento; dentro de bloco, inicia atribuição ou chamada. Separar os contextos evita conflito entre definição de procedimento e chamada.

### 4.3 Recursão à esquerda nas expressões

Antes:

```bnf
<soma> ::= <soma> "+" <produto> | <soma> "-" <produto> | <produto>
```

Depois:

```bnf
<soma> ::= <produto> <mais_soma>
<mais_soma> ::= "+" <produto> <mais_soma> | "-" <produto> <mais_soma> | ε
```

O mesmo padrão vale para os demais níveis binários. Não há recursão à esquerda direta ou indireta na versão final: listas consomem elementos não anuláveis antes de repetir; caudas consomem operadores; blocos e agrupamentos consomem delimitadores; a cadeia de precedências termina em primários.

### 4.4 else pendente

Comandos sem chaves como corpos de condicionais aninhadas criariam o conflito do else pendente. Exigir blocos elimina esse conflito. "else" não pertence a FOLLOW(`<senao>`); portanto não compete com ε.

## 5. FIRST e FOLLOW de todos os não-terminais

FIRST contém os terminais que podem iniciar uma derivação e ε se ela pode ser vazia. FOLLOW contém os terminais que podem ocorrer imediatamente depois do não-terminal.

Os conjuntos foram calculados por iteração até ponto fixo sobre exatamente a BNF acima, com EOF explícito, sem abreviações.

| Não-terminal | FIRST | FOLLOW |
| --- | --- | --- |
| `<programa>` | {EOF, ID, "bool", "char", "double", "int", "string"} | {$} |
| `<elementos>` | {ID, "bool", "char", "double", "int", "string", ε} | {EOF} |
| `<elemento>` | {ID, "bool", "char", "double", "int", "string"} | {EOF, ID, "bool", "char", "double", "int", "string"} |
| `<tipo>` | {"bool", "char", "double", "int", "string"} | {ID} |
| `<elemento_tipado>` | {"bool", "char", "double", "int", "string"} | {EOF, ID, "bool", "char", "double", "int", "string"} |
| `<continuacao_tipado>` | {"(", ";", "="} | {EOF, ID, "bool", "char", "double", "int", "string"} |
| `<procedimento>` | {ID} | {EOF, ID, "bool", "char", "double", "int", "string"} |
| `<parametros>` | {"bool", "char", "double", "int", "string", ε} | {")"} |
| `<mais_parametros>` | {",", ε} | {")"} |
| `<bloco>` | {"{"} | {EOF, ID, "bool", "char", "double", "else", "if", "int", "return", "string", "while", "{", "}"} |
| `<comandos>` | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", ε} | {"}"} |
| `<comando>` | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{"} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<declaracao_local>` | {"bool", "char", "double", "int", "string"} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<inicializacao>` | {"=", ε} | {";"} |
| `<comando_id>` | {ID} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<continuacao_comando_id>` | {"(", "="} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<selecao>` | {"if"} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<senao>` | {"else", ε} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<repeticao>` | {"while"} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<retorno>` | {"return"} | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}"} |
| `<valor_retorno>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true", ε} | {";"} |
| `<argumentos>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true", ε} | {")"} |
| `<mais_argumentos>` | {",", ε} | {")"} |
| `<expressao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {")", ",", ";"} |
| `<disjuncao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {")", ",", ";"} |
| `<mais_disjuncao>` | {"&#124;&#124;", ε} | {")", ",", ";"} |
| `<conjuncao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {")", ",", ";", "&#124;&#124;"} |
| `<mais_conjuncao>` | {"&&", ε} | {")", ",", ";", "&#124;&#124;"} |
| `<igualdade>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {"&&", ")", ",", ";", "&#124;&#124;"} |
| `<mais_igualdade>` | {"!=", "==", ε} | {"&&", ")", ",", ";", "&#124;&#124;"} |
| `<relacao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {"!=", "&&", ")", ",", ";", "==", "&#124;&#124;"} |
| `<mais_relacao>` | {"<", "<=", ">", ">=", ε} | {"!=", "&&", ")", ",", ";", "==", "&#124;&#124;"} |
| `<soma>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {"!=", "&&", ")", ",", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<mais_soma>` | {"+", "-", ε} | {"!=", "&&", ")", ",", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<produto>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {"!=", "&&", ")", "+", ",", "-", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<mais_produto>` | {"*", "/", ε} | {"!=", "&&", ")", "+", ",", "-", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<unario>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | {"!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<primario>` | {"(", CHAR, ID, NUM, STR, "false", "true"} | {"!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |
| `<sufixo_id>` | {"(", ε} | {"!=", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "&#124;&#124;"} |

### 5.1 Como justificar algumas decisões

- `<elemento_tipado>` inicia com um tipo, enquanto `<procedimento>` inicia com ID.
- `<parametros>` e `<argumentos>` aceitam ε somente diante de ")".
- `<inicializacao>` e `<valor_retorno>` aceitam ε somente diante de ";".
- `<elementos>` termina diante de EOF; `<comandos>` termina diante de "}".
- Caudas aritméticas devolvem controle ao chamador diante de operadores de menor precedência presentes em FOLLOW.

## 6. Verificação LL(1) por não-terminal

(a) FIRST das alternativas sem ε são disjuntos dois a dois.
(b) Há no máximo uma alternativa anulável.
(c) Quando existe alternativa anulável, FIRST das demais alternativas é disjunto de FOLLOW.

Cada conjunto da segunda coluna corresponde a uma alternativa na ordem da BNF. Produções com alternativa única satisfazem (a) trivialmente.

| Não-terminal | FIRST de cada alternativa | (a) | (b): quantidade anulável | (c) |
| --- | --- | --- | --- | --- |
| `<programa>` | {EOF, ID, "bool", "char", "double", "int", "string"} | Disjuntos | 0 (OK) | Não se aplica |
| `<elementos>` | {ID, "bool", "char", "double", "int", "string"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<elemento>` | {"bool", "char", "double", "int", "string"} ; {ID} | Disjuntos | 0 (OK) | Não se aplica |
| `<tipo>` | {"int"} ; {"double"} ; {"bool"} ; {"char"} ; {"string"} | Disjuntos | 0 (OK) | Não se aplica |
| `<elemento_tipado>` | {"bool", "char", "double", "int", "string"} | Disjuntos | 0 (OK) | Não se aplica |
| `<continuacao_tipado>` | {";"} ; {"="} ; {"("} | Disjuntos | 0 (OK) | Não se aplica |
| `<procedimento>` | {ID} | Disjuntos | 0 (OK) | Não se aplica |
| `<parametros>` | {"bool", "char", "double", "int", "string"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<mais_parametros>` | {","} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<bloco>` | {"{"} | Disjuntos | 0 (OK) | Não se aplica |
| `<comandos>` | {ID, "bool", "char", "double", "if", "int", "return", "string", "while", "{"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<comando>` | {"bool", "char", "double", "int", "string"} ; {ID} ; {"if"} ; {"while"} ; {"return"} ; {"{"} | Disjuntos | 0 (OK) | Não se aplica |
| `<declaracao_local>` | {"bool", "char", "double", "int", "string"} | Disjuntos | 0 (OK) | Não se aplica |
| `<inicializacao>` | {"="} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<comando_id>` | {ID} | Disjuntos | 0 (OK) | Não se aplica |
| `<continuacao_comando_id>` | {"="} ; {"("} | Disjuntos | 0 (OK) | Não se aplica |
| `<selecao>` | {"if"} | Disjuntos | 0 (OK) | Não se aplica |
| `<senao>` | {"else"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<repeticao>` | {"while"} | Disjuntos | 0 (OK) | Não se aplica |
| `<retorno>` | {"return"} | Disjuntos | 0 (OK) | Não se aplica |
| `<valor_retorno>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<argumentos>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<mais_argumentos>` | {","} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<expressao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<disjuncao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_disjuncao>` | {"&#124;&#124;"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<conjuncao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_conjuncao>` | {"&&"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<igualdade>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_igualdade>` | {"=="} ; {"!="} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<relacao>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_relacao>` | {"<"} ; {">"} ; {"<="} ; {">="} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<soma>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_soma>` | {"+"} ; {"-"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<produto>` | {"!", "(", "+", "-", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<mais_produto>` | {"*"} ; {"/"} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |
| `<unario>` | {"!"} ; {"-"} ; {"+"} ; {"(", CHAR, ID, NUM, STR, "false", "true"} | Disjuntos | 0 (OK) | Não se aplica |
| `<primario>` | {NUM} ; {STR} ; {CHAR} ; {"true"} ; {"false"} ; {ID} ; {"("} | Disjuntos | 0 (OK) | Não se aplica |
| `<sufixo_id>` | {"("} ; {ε} | Disjuntos | 1 (OK) | Interseção = ∅ |

Todas as condições foram verificadas. SELECT(A → α) é FIRST(α) sem ε, acrescido de FOLLOW(A) quando α é anulável. Os SELECT das alternativas de cada não-terminal são disjuntos; nenhuma célula da tabela preditiva possui duas alternativas. A conclusão depende de distinguir terminais por categoria e lexema, não apenas pelas categorias genéricas do scanner.
