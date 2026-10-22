# Visão Geral

Eu e minha namorada começamos a vender bolo e sanduíche na faculdade e queremos ter um controle de vendas e ter também uma gestão de gastos. Atualmente estamos fazendo toda essa gestão através de planilha do Excel e conversa no WhatsApp e queremos automatizar esse processo de gestão de vendas e de gastos, além de fornecer um meio dos clientes fazerem o pedido de bolo e sanduiche através de um site.

## Problema

|   S#   | Problema | Porque |
| :---: | :---: | :---: |
| P1 | O cálculo das vendas é feito manualmente | É feito conforme as vendas ou visualizando pessoalmente, colocando na planilha os resultados |
| P2 | Cliente tem que chamar no WhatsApp para realizar um pedido | Porque ou o cliente encontra o vendedor na faculdade e faz o pedido ou ele manda mensagem no WhatsApp |
| P3 | Quantificar o número de bolo e sanduiche é feito manualmente | O bolo fica armazenado na geladeira do container, logo o vendedor tem que ficar indo lá pra olhar quantos tem ou gravar mentalmente com base nas vendas diárias e possivelmente anotar em algum lugar (papel, bloco de notas) |
| P4 | Realizar métricas diárias, semanais e mensais é feito na planilha | Tivemos que fazer uma planilha para ter todas essas métricas, mas ainda temos que colocar manualmente as vendas do dia, as compras/gastos feitos, e configurar as equações para gerar os cálculos |
| P5 | Colocar cada item (ingrediente) comprado com nome e preço é feito manualmente | Se encaixa no motivo da P4 |

## Solução Proposta

| S# | P#... | Solução |
| :---: | :---: | :---: |
| S1 | P1 | O sistema fará o cálculo automático das vendas a partir da quantidade de produtos lançada no início do dia e de quantos produtos foram vendidos até o final do dia |
| S2 | P3 | Com base nas vendas realizadas o sistema irá calcular e manter um histórico de vendas de cada dia |
| S3 | P4 | Com base no histórico gerado na S2 o sistema irá calcular métricas automáticas |
| S4 | P5 | Registro de ingredientes será feito manualmente, com o banco de dados já modelado para permitir automação futura (sem precisar de reforma de schema) |
| S5 | P2 | O sistema terá uma página web para o cliente realizar o pedido, sem necessidade de login, eliminando a dependência do WhatsApp para esse fluxo |

### Decisões que fecham o desenho da solução

- O lançamento do estoque inicial do dia (bolos/sanduíches) será feito manualmente por Carlos e a namorada toda manhã, contando os produtos.
- O cadastro de ingredientes fica manual por enquanto; a modelagem separa `ingrediente` e `compra_ingrediente` (com campo de origem) para permitir automação futura (ex: leitura de QR code de nota fiscal) sem reformar o banco.
- O pedido no site é feito sem login/conta — o cliente informa apenas nome e contato (ex: WhatsApp) junto do pedido.
- A divulgação/marketing do produto continua como é feito hoje, na comunidade da própria faculdade; o sistema não cuida disso, apenas recebe o link do site que será compartilhado.

## Stack

- **Frontend do cliente**: página web simples (sem necessidade de login) para consulta do cardápio e realização de pedidos
- **Backend**: Spring Boot
- **Banco de dados**: SQLite
- **Infraestrutura**: servidor pessoal com Debian, responsável por rodar o backend e armazenar os dados
- **Backup**: pendrive (backup local) + repositório privado no GitHub (versionamento do código)

## Requisitos

| RF# | P#... | Requisito |
| :---: | :---: | :---: |
| RF1 | P1 | O sistema deverá ter uma função para calcular as métricas das vendas |
| RF2 | P2 | O sistema deverá fornecer uma página web para o cliente realizar pedidos, sem necessidade de login |
| RF2.1 | P2 | O sistema deverá coletar nome e contato do cliente junto do pedido, já que não há login |
| RF2.2 | P2 | O sistema deverá fornecer um cardápio para o cliente |
| RF2.3 | P2 | O sistema deverá permitir a seleção de 1 ou mais itens do cardápio |
| RF3 | P3 | O sistema deverá calcular automaticamente o número de produtos (bolo e sanduiche) disponíveis, a partir do estoque inicial e das vendas do dia |
| RF3.1 | P3 | O sistema deverá permitir o lançamento manual do estoque inicial diário de cada produto |
| RF4 | P4 | O sistema deverá fornecer cálculo de lucro diário/semanal/mensal |
| RF5 | P4 | O sistema deverá fornecer cálculo de gasto diário/semanal/mensal |
| RF5.1 | P5 | O sistema deverá permitir o cadastro manual de ingredientes, com nome, preço e unidade de medida |

### Requisitos futuros (fora do escopo inicial)

| RF# | P#... | Requisito |
| :---: | :---: | :---: |
| RF6 | P5 | O sistema deverá escanear o QR code da notinha do mercado |
| RF7 | P5 | O sistema deverá extrair os valores de cada ingrediente do site redirecionado do QR code |

RF6 e RF7 ficam registrados como evolução futura, já que a decisão atual (S4) é manter o cadastro de ingredientes manual — mas o banco já é modelado para acomodar essa automação sem retrabalho quando ela for priorizada.
