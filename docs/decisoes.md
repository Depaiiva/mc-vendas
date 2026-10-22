Como o seu dashboard já possui os filtros de Dia, Semana e Mês funcionando via backend (DashboardController), a integração de um gráfico interativo se encaixa com  
  muita naturalidade.                                                                                                                                                 
  ──────                                                                                                                                                              
  ### Como funcionaria a visualização em cada período:
                                                                                                                                                                      
  1. Visão Diária ("Dia"):                                                                                                                                            
      • Gráfico de barras ou linha mostrando as vendas agrupadas por faixa de horário do dia (ex.: 08h, 10h, 12h, 14h, 16h, 18h), permitindo ver os horários de pico  
      de movimento no campus.                                                                                                                                         
      • (Alternativa): Gráfico comparativo dos últimos 7 dias até a data selecionada.                                                                                 
  2. Visão Semanal ("Semana"):                                                                                                                                        
      • Gráfico de barras com os 7 dias da semana (Seg, Ter, Qua, Qui, Sex, Sáb, Dom), exibindo o faturamento de cada dia e destacando os dias de maior venda.        
  3. Visão Mensal ("Mês"):                                                                                                                                            
      • Gráfico de linha/área ou barras com a evolução do faturamento dia a dia ao longo do mês (dias 1 a 31), com a opção de exibir também as despesas de compras de 
      ingredientes para comparar receita vs. custos.                                                                                                                  
                                                                                                                                                                      
  ──────                                                                                                                                                              
  ### Como é a implementação técnica:
                                                                                                                                                                      
  • Backend (MetricsService.java):
  Adicionamos uma lista de pontos temporais (TimelinePoint(label, valorVendas, valorDespesas)) que agrupa as vendas do período já consultadas no banco SQLite.        
  • Frontend (dashboard.html):
  Utilizamos o Chart.js (biblioteca leve, responsiva e moderna), com as cores da identidade visual do projeto (verde petróleo #173f42, verde menta e laranja #cf704c),
  com tooltips automáticos formatados em R$ 0,00.
  ──────
  Deseja que eu implemente esse gráfico no dashboard agora mesmo?

────────────────────────────────────────────────────────────
> gostei desse modelo que você indicou, e eu queria que a visualização dos gráficos sejam controlados por um botão (que serão os produtos cadastrados) e quando fosse
  selecionado cada item aparecesse os três gráficos (diario, semanal, mensal)
