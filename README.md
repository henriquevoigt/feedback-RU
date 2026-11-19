# Sistema de Avaliação dos Cardápios do RU (UFPel)

Trabalho prático da disciplina de **Projeto de Banco de Dados (2026/2)** do curso de Ciência da Computação da UFPel (**Grupo 10**).

## Sobre o Projeto

A ideia deste projeto é substituir as avaliações em papel dos Restaurantes Universitários (RU) por um sistema integrado ao registro de acesso nas catracas.

No banco de dados, nós relacionamos os cardápios do dia (pratos, ingredientes, calorias e unidades do RU) com o registro individual de entrada dos alunos e servidores. A partir disso, cada usuário pode avaliar a refeição que realmente consumiu (nota e comentário), permitindo cruzar a satisfação com os pratos servidos, o turno e os horários de pico.

## Tecnologias Utilizadas

- **Banco de Dados:** PostgreSQL (hospedado em homelab e acessado pela equipe via Tailscale)
- **Gerenciamento do BD:** DBeaver
- **Interface Gráfica:** Java (Swing + JDBC + Maven)

## Estrutura do Repositório

- `/sql`: Scripts do banco de dados divididos por etapa:
  - `01_ddl_schema.sql`: Criação das 13 tabelas do modelo lógico e restrições de integridade.
  - `02_triggers.sql`: Funções e gatilhos (triggers) do banco.
  - `03_seed_dados.sql`: Carga inicial de dados para testes.
  - `04_consultas.sql`: Consultas e relatórios (mínimo 6 consultas, sendo ao menos 4 com `JOIN`).
- `/src`: Código-fonte da aplicação em Java Swing para interação com o banco:
  - `PainelCardapio.java` (Aba 1 - Henrique): Gestão de pratos, ingredientes e montagem de cardápios.
  - `PainelConsumoFeedback.java` (Aba 2 - Samuel): Registro de consumo na catraca e envio/edição de avaliações.
  - `PainelRelatorios.java` (Aba 3 - Pedro): Consultas analíticas e relatórios gerenciais.
- `db.properties`: Arquivo de configuração de conexão com o PostgreSQL (IP do Tailscale, porta, banco, usuário e senha).
- `/docs`: Documentação do projeto, DER e modelo lógico.

## Como Executar

1. Certifique-se de estar conectado à rede do **Tailscale** do grupo para alcançar o servidor PostgreSQL (ou rode os scripts da pasta `/sql` em uma instância local do Postgres).
2. Edite o arquivo `db.properties` na raiz do projeto com o IP do servidor, usuário e senha do banco (ou configure diretamente pelo botão **"Configurar Conexão"** dentro da interface gráfica).
3. Execute o projeto via sua IDE (rodando a classe `br.edu.ufpel.ru.Main`) ou via terminal com Maven:
   ```bash
   mvn compile exec:java
   ```

## Integrantes (Grupo 10)

- Henrique Voigt
- Pedro de Freitas
- Samuel Flores
