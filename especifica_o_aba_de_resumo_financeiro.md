# Especificação Funcional: Aba de Resumo Financeiro

Este documento descreve os requisitos funcionais e de interface (UI/UX) para a implementação da tela de Resumo Financeiro no aplicativo de controle de vendas.

## 1. Filtro de Período (Cabeçalho)

Controle de recorte temporal fixo no topo da tela para atualização dinâmica de todos os indicadores e gráficos abaixo.

### Opções de Seleção Rápida (Pills/Abas):
- **Hoje** (selecionado por padrão)
- **Esta Semana**
- **Este Mês**
- **Personalizado** (aciona seletor de data inicial e final)

**Comportamento:** Ao alternar o filtro, todos os cards, gráficos e rankings da tela devem ser recalculados automaticamente.

---

## 2. KPIs Principais (Big Numbers)

Bloco superior exibindo os 4 indicadores-chave de desempenho do período selecionado em formato de cards de destaque:

- **Faturamento Bruto:** Soma total do valor das vendas concluídas no período (R$).
- **Lucro Líquido (Estimado):** Faturamento bruto subtraído dos custos dos produtos vendidos (R$). Deve possuir destaque visual superior.
- **Ticket Médio:** Relação entre faturamento e volume de vendas:
  $$\text{Ticket Médio} = \frac{\text{Faturamento Bruto}}{\text{Total de Vendas}}$$
- **Total de Vendas:** Contagem absoluta de pedidos/transações concluídas.

**Requisito Adicional (Contexto):** Cada card deve exibir um indicador percentual de variação em relação ao período anterior equivalente (ex.: +12% em verde ou -5% em vermelho).

---

## 3. Quebra por Forma de Pagamento

Componente de conciliação financeira detalhando a origem das receitas no período filtrado.

### Categorias:
- Pix
- Cartão (Crédito / Débito)
- Dinheiro
- Pendente / A Receber (exibir com alerta visual caso o valor seja maior que zero)

### Dados Exibidos por Categoria:
- Valor monetário total (R$)
- Representatividade percentual (% sobre o faturamento total)
- **Formato Visual:** Lista compacta com barras de progresso horizontais ou gráfico de rosca (donut chart).

---

## 4. Gráfico de Evolução de Vendas (Barras Verticais)

Card dedicado à análise visual de tendência e volume de vendas ao longo do tempo.

### Controles do Card:
- **Alternador de Granularidade:** [ Semanal | Mensal ]
  - **Visão Semanal:** Exibe 7 barras correspondentes aos dias da semana (Seg a Dom).
  - **Visão Mensal:** Exibe barras agrupadas por semanas do mês (Sem 1 a Sem 4/5) ou visão comparativa dos últimos meses.
- **Alternador de Modo de Visualização:** [ Padrão | Empilhado ]
  - **Barras Verticais Padrão:** Exibe o faturamento total por coluna.
  - **Barras Empilhadas:** Divide cada coluna por composição (ex.: Lucro Líquido na base + Custo no topo, ou divisão por principais produtos/categorias).

### Detalhes de Interface (UI/UX):
- **Linha de Média:** Linha horizontal tracejada indicando a média de vendas do período exibido.
- **Eixo Y Simplificado:** Valores abreviados (ex.: R$ 0, R$ 500, R$ 1k).
- **Interatividade (Tooltip):** Ao tocar ou passar o cursor sobre uma barra, exibir card flutuante com data/período exato, faturamento total, lucro e quantidade de pedidos.

---

## 5. Ranking de Desempenho (Top Produtos)

Lista ordenada dos itens com melhor performance no período selecionado para apoio à decisão de estoque e produção.

- **Critério de Ordenação:** Mais vendidos (Curva A), exibindo o Top 5 produtos.
- **Informações por Item:**
  - Posição no ranking (#1, #2, etc.)
  - Nome do produto
  - Quantidade total de unidades vendidas
  - Receita total gerada pelo item (R$)

---

## Critérios de Aceite Gerais

- [ ] A tela deve ser responsiva, priorizando a leitura rápida sem poluição visual.
- [ ] Valores monetários devem seguir a formatação padrão brasileira (R$ 0.000,00).
- [ ] Estados vazios (Empty States) devem ser tratados com mensagens claras caso não existam vendas no período filtrado.
