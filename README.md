# Visão geral
Projeto Java que implementa **acesso a dados via JDBC puro** (sem ORM) para um domínio simples com **Seller** (vendedor) e **Department** (departamento). O foco é demonstrar **padrão DAO**, organização em camadas, mapeamento `ResultSet → objeto`, uso de `PreparedStatement`, **tratamento de exceções** e **gestão de recursos** (conexão/statement/resultset).

> Stack principal: **Java**, **JDBC**, **MySQL**.

---

## Estrutura do projeto
```
src/
  application/
    Program.java
    Program2.java
  db/
    DB.java
    DbException.java
    DbIntegrityException.java
  model/
    dao/
      DaoFactory.java
      DepartmentDao.java
      SellerDao.java
      impl/
        DepartmentDaoJDBC.java
        SellerDaoJDBC.java
    entities/
      Department.java
      Seller.java
module-info.java

db.properties
```

**Arquivos externos**
- `db.properties`: parâmetros de conexão (usuario/senha/URL/SSL)

---

## Como as camadas se relacionam

- **application**: executáveis de demonstração (**clientes** da camada DAO). Chamam `DaoFactory` para obter instâncias e invocam métodos como `findAll`, `insert`, etc.
- **model.entities**: **modelo de domínio** (objetos de negócio): `Seller`, `Department`.
- **model.dao**: **contratos (interfaces) DAO**. Definem as operações suportadas para cada entidade (CRUD e consultas específicas).
- **model.dao.impl**: **implementações JDBC** dos contratos, responsáveis por acessar o banco via `java.sql.*`.
- **db**: **infraestrutura de banco**: abertura/fechamento de conexão e **exceções** específicas.

Fluxo típico:
1) `application.Program` cria um DAO via `DaoFactory` →
2) a `DaoFactory` usa `DB.getConnection()` e entrega uma implementação JDBC (`SellerDaoJDBC` ou `DepartmentDaoJDBC`) →
3) o DAO executa SQL com `PreparedStatement`, converte o `ResultSet` em `entities` e retorna para a aplicação.

---

## Detalhamento de cada classe

### 1) Camada **db**

#### `DB.java`
Responsável pela **gestão de conexão** e **fechamento seguro** de recursos.
- **Lê** `db.properties` e cria uma `java.sql.Connection` com `DriverManager`.
- Exponibiliza:
  - `getConnection()`: retorna uma única conexão (singleton estático) reaproveitada ao longo da aplicação.
  - `closeConnection()`: encerra a conexão.
  - helpers: `closeStatement(Statement)`, `closeResultSet(ResultSet)`.
- Centraliza o **tratamento de `SQLException`** lançando `DbException` em caso de erro.

#### `DbException.java`
- **Runtime exception** genérica para erros de banco. Envolve `SQLException` e evita poluir a API com `throws` checados.

#### `DbIntegrityException.java`
- Subtipo de `DbException` para **erros de integridade referencial** (ex.: DELETE que viola FK). Útil para diferenciar mensagens e lógicas de fallback.

---

### 2) Camada **model.entities**

#### `Department.java`
- Entidade simples: `id`, `name`.
- Implementa `Serializable`, `equals/hashCode` por `id` e `toString()`.

#### `Seller.java`
- Entidade: `id`, `name`, `email`, `birthDate`, `baseSalary` e associação **`Department department`**.
- Implementa `Serializable`, `equals/hashCode` por `id` e `toString()`.
- A relação **muitos‑para‑um** (Seller → Department) é mapeada manualmente no DAO via `JOIN` e criação/compartilhamento de instâncias de `Department`.

---

### 3) Camada **model.dao (contratos)**

#### `DepartmentDao`
Operações de **CRUD** e consulta geral para `Department`:
- `insert(Department obj)`
- `update(Department obj)`
- `deleteById(Integer id)`
- `findById(Integer id)`
- `findAll()`

#### `SellerDao`
Semelhante ao acima e com uma consulta específica:
- `insert(Seller obj)`, `update`, `deleteById`, `findById`, `findAll()`
- `findByDepartment(Department department)`

> Por serem **interfaces**, a aplicação depende de **abstrações**, permitindo trocar a implementação (JDBC, JPA, mock) sem alterar o código cliente.

#### `DaoFactory`
- Fábrica **centralizada** que recebe a `Connection` de `DB.getConnection()` e cria as implementações JDBC:
  - `createSellerDao()` → `new SellerDaoJDBC(conn)`
  - `createDepartmentDao()` → `new DepartmentDaoJDBC(conn)`

---

### 4) Camada **model.dao.impl (JDBC)**

#### `DepartmentDaoJDBC`
Implementa `DepartmentDao` com **JDBC puro**.
- **SQL parametrizado** via `PreparedStatement` para evitar **SQL Injection**.
- Métodos principais (resumo de responsabilidades):
  - `insert(obj)`: INSERT + `Statement.RETURN_GENERATED_KEYS` para setar `obj.id` a partir do autoincrement.
  - `update(obj)`: UPDATE por `id`.
  - `deleteById(id)`: DELETE por `id`; se violar integridade lança `DbIntegrityException`.
  - `findById(id)`: SELECT por `id`; mapeia linha → `Department`.
  - `findAll()`: SELECT *; itera `ResultSet` → lista de `Department`.
- **Fechamento** de `PreparedStatement`/`ResultSet` sempre em `finally` via helpers da classe `DB`.

#### `SellerDaoJDBC`
Implementa `SellerDao`. Além do CRUD, demonstra **mapeamento com JOIN** e **cache de departamentos** para evitar instâncias duplicadas:
- `insert(obj)`: INSERT em `seller`; define `id` retornado.
- `update(obj)`: UPDATE dos campos, incluindo `departmentId`.
- `deleteById(id)`: DELETE; converte erros de integridade em `DbIntegrityException`.
- `findById(id)`: SELECT com **JOIN** em `department` para popular `Seller.department`.
- `findAll()`: SELECT com JOIN; usa um **Map<Integer, Department>** como cache para reusar a mesma instância de `Department` por id durante a leitura do `ResultSet` (otimiza memória e igualdade referencial).
- `findByDepartment(dep)`: SELECT filtrando por `departmentId`; reaproveita o mesmo padrão de mapeamento com cache.
- **Auxiliares privados**:
  - `instantiateDepartment(rs)`: cria `Department` a partir do `ResultSet`.
  - `instantiateSeller(rs, dep)`: cria `Seller` e associa o `Department` informado.

> **Boas práticas explícitas na classe**: uso de `PreparedStatement`, tratamento de `SQLException` como `DbException`, e **finally** garantindo `close` dos recursos.

---

### 5) Camada **application**

#### `Program.java` / `Program2.java`
- São **clientes de demonstração**. Mostram o uso das DAOs (ex.: `findById`, `findAll`, `findByDepartment`, `insert`, `update`, `deleteById`).
- Servem de guia para a ordem das operações e o contrato exposto pela camada DAO.

---

## Banco de dados esperado

`db.properties` (exemplo do projeto):
```
user=Developer
password=123456789
dburl=jdbc:mysql://localhost:3306/coursejdbc
useSSL=false
```

> Ajuste usuário/senha/URL conforme seu ambiente.

**Schema e tabelas** (modelo típico usado com esse domínio):

```sql
CREATE DATABASE IF NOT EXISTS coursejdbc DEFAULT CHARACTER SET utf8mb4;
USE coursejdbc;

CREATE TABLE department (
  Id INT PRIMARY KEY AUTO_INCREMENT,
  Name VARCHAR(60) NOT NULL
);

CREATE TABLE seller (
  Id INT PRIMARY KEY AUTO_INCREMENT,
  Name VARCHAR(60) NOT NULL,
  Email VARCHAR(100) NOT NULL,
  BirthDate DATE NOT NULL,
  BaseSalary DOUBLE NOT NULL,
  DepartmentId INT NOT NULL,
  CONSTRAINT fk_seller_department FOREIGN KEY (DepartmentId)
    REFERENCES department(Id)
    ON DELETE RESTRICT
);
```

> A coluna `DepartmentId` materializa a relação **Seller → Department**. Ajuste `ON DELETE` conforme a política desejada (RESTRICT, CASCADE, SET NULL...).

---

## Execução local

1) **Crie o schema** e tabelas conforme o SQL acima; insira alguns registros.
2) Ajuste `db.properties` com suas credenciais/URL.
3) Compile e rode `Program.java`/`Program2.java` (IDE ou `javac/java`).
4) Verifique saídas no console para cada operação (CRUD/consultas).

> Dica: se usar timezone/charset, prefira uma URL do tipo:
> `jdbc:mysql://localhost:3306/coursejdbc?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC`.

---

## Erros e integridade

- **Falhas de conexão** (`SQLException`) → convertidas em `DbException` (runtime).
- **Violação de integridade** (FK ao deletar, por ex.) → `DbIntegrityException`.
- Sempre **feche recursos**: mesmo com exceção, os helpers em `DB` garantem `close` em `finally`.

---

## Transações

- As operações mostradas são **simples** e independentes. Em cenários com múltiplos statements atômicos, configure `conn.setAutoCommit(false)` e faça `commit()`/`rollback()` manual (centralizando na camada DAO/serviço). Este projeto mantém o `autoCommit` padrão do JDBC.

---

## Extensões recomendadas

- **Pooling** de conexões (ex.: HikariCP) para produção.
- **Camada de serviço** (regras de negócio/coordenação transacional) entre `application` e `dao`.
- **Paginação/ordenação/filtros** nos métodos de consulta.
- **Migrations** (Flyway/Liquibase) para versionar o schema.
- **Validação** de dados antes do DAO.
- **Tests**: integração com DB real (ou Testcontainers) e mocks para validar mapeamentos/contratos.

---

## Resumo do papel de cada arquivo

- **application/Program*.java**: exemplos de uso da API DAO.
- **db/DB.java**: abre/fecha conexões e recursos; carrega propriedades.
- **db/DbException.java**: exceção runtime genérica de banco.
- **db/DbIntegrityException.java**: exceção para violação de integridade.
- **model/entities/Department.java**: entidade Department.
- **model/entities/Seller.java**: entidade Seller (com `Department`).
- **model/dao/DepartmentDao.java**: contrato DAO para Department.
- **model/dao/SellerDao.java**: contrato DAO para Seller.
- **model/dao/impl/DepartmentDaoJDBC.java**: implementação JDBC de DepartmentDao.
- **model/dao/impl/SellerDaoJDBC.java**: implementação JDBC de SellerDao.
- **model/dao/DaoFactory.java**: fábrica das implementações DAO com a Connection compartilhada.
