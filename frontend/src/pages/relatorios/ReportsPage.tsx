// Tela Relatórios Gerenciais (Dono e Gerente): resumo do período e picos de venda vindos da API.
import { useEffect, useState } from "react";
import {
  errorMessage,
  relatoriosApi,
  usuariosApi,
  vendasApi,
  type Pagina,
  type PeriodoPico,
  type Picos,
  type Ponto,
  type Relatorio,
  type Usuario,
  type Venda,
} from "../../api";
import { Icon } from "../../components/Icon";
import { PersonCell } from "../../components/ui";
import { paymentFromCode, paymentLabels } from "../../types";
import { formatCurrency, getInitials } from "../../utils/format";
import { isoDate, toSaleRecord } from "../vendas/mappers";
import { OrderStatusBadge, Pager } from "../vendas/OrdersPage";

export type ReportPeriod = "today" | "7days" | "30days" | "month" | "custom";

/** Intervalo (aaaa-mm-dd) de cada opção de período do protótipo. */
export const reportRange = (period: ReportPeriod, customStart: string, customEnd: string) => {
  const today = new Date();
  const end = period === "custom" && customEnd ? new Date(`${customEnd}T12:00:00`) : today;
  const start = new Date(end);
  if (period === "7days") start.setDate(start.getDate() - 6);
  if (period === "30days") start.setDate(start.getDate() - 29);
  if (period === "month") start.setDate(1);
  if (period === "custom") {
    if (customStart) return { inicio: customStart, fim: isoDate(end) };
    start.setDate(start.getDate() - 29);
  }
  return { inicio: isoDate(start), fim: isoDate(end) };
};

const HISTORY_PAGE_SIZE = 6;

const formatDay = (iso: string) => new Date(`${iso}T12:00:00`).toLocaleDateString("pt-BR");

const formatOrders = (count: number) => `${count} ${count === 1 ? "pedido" : "pedidos"}`;

const formatPercent = (value: number) => `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;

/** Gráfico de barras no estilo do protótipo; destaca a barra de pico. */
function Bars({ points, peakKey, emptyText }: { points: Ponto[]; peakKey?: string; emptyText: string }) {
  const max = Math.max(...points.map((point) => point.faturamento), 1);
  if (!points.some((point) => point.pedidos > 0)) {
    return <div className="report-empty-chart"><Icon name="sales" /><p>{emptyText}</p></div>;
  }
  return (
    <div className="revenue-bars">
      {points.map((point, index) => (
        <div className="revenue-bar-column" key={point.chave} title={`${point.rotulo}: ${formatCurrency(point.faturamento)} · ${formatOrders(point.pedidos)}`}>
          <span className={`${point.faturamento ? "filled" : ""}${point.chave === peakKey ? " peak" : ""}`} style={{ height: `${Math.max(point.faturamento / max * 100, point.faturamento ? 5 : 1)}%` }}>
            <i>{point.faturamento ? formatCurrency(point.faturamento) : ""}</i>
          </span>
          {(points.length <= 12 || index % Math.ceil(points.length / 12) === 0) && <small>{point.rotulo}</small>}
        </div>
      ))}
    </div>
  );
}

const peakPeriods: Array<{ id: PeriodoPico; label: string; serieTitle: string; peakLabel: string }> = [
  { id: "SEMANA", label: "Semana", serieTitle: "Faturamento dia a dia", peakLabel: "Dia de maior venda" },
  { id: "MES", label: "Mês", serieTitle: "Faturamento por dia do mês", peakLabel: "Dia de maior venda" },
  { id: "SEMESTRE", label: "Semestre", serieTitle: "Faturamento por mês", peakLabel: "Mês de maior venda" },
  { id: "ANO", label: "Ano", serieTitle: "Faturamento por mês", peakLabel: "Mês de maior venda" },
];

/** Dashboards estratégicos: maior pico de vendas na semana, no mês, no semestre e no ano. */
function PeaksSection({ sellerId }: { sellerId: string }) {
  const [period, setPeriod] = useState<PeriodoPico>("SEMANA");
  const [reference, setReference] = useState(() => isoDate(new Date()));
  const [data, setData] = useState<Picos | null>(null);
  const [error, setError] = useState("");
  const config = peakPeriods.find((item) => item.id === period)!;

  useEffect(() => {
    let cancelled = false;
    relatoriosApi.picos({ periodo: period, referencia: reference || undefined, vendedorId: sellerId ? Number(sellerId) : undefined })
      .then((picos) => { if (!cancelled) { setData(picos); setError(""); } })
      .catch((loadError) => { if (!cancelled) setError(errorMessage(loadError, "Não foi possível carregar os picos de venda.")); });
    return () => { cancelled = true; };
  }, [period, reference, sellerId]);

  const variation = data?.variacaoFaturamento;
  return (
    <section className="dashboard-card peaks-card">
      <div className="report-section-heading">
        <div><h2>Picos de venda</h2><p>Quando a lanchonete mais vende na semana, no mês, no semestre e no ano.</p></div>
        <div className="seller-sort-buttons">{peakPeriods.map((item) => <button key={item.id} type="button" className={period === item.id ? "active" : ""} onClick={() => setPeriod(item.id)}>{item.label}</button>)}</div>
      </div>
      <div className="peaks-toolbar">
        <label><span>Data de referência</span><input type="date" value={reference} onChange={(event) => setReference(event.target.value)} /></label>
        {data && <p>Período analisado: <strong>{formatDay(data.inicio)}</strong> a <strong>{formatDay(data.fim)}</strong>{data.fim >= isoDate(new Date()) && " · em andamento (a comparação considera o período anterior completo)"}</p>}
      </div>
      {error && <div className="form-alert" role="alert">{error}</div>}
      {data && (
        <>
          <div className="peaks-highlights">
            <article>
              <small>Faturamento no período</small>
              <strong>{formatCurrency(data.indicadores.faturamento)}</strong>
              <span className={variation == null ? "" : variation >= 0 ? "up" : "down"}>
                {variation == null ? "Sem vendas no período anterior" : `${variation >= 0 ? "+" : ""}${formatPercent(variation)} em relação ao período anterior`}
              </span>
            </article>
            <article>
              <small>{config.peakLabel}</small>
              <strong>{data.pico ? data.pico.rotulo : "—"}</strong>
              <span>{data.pico ? `${formatCurrency(data.pico.faturamento)} · ${formatOrders(data.pico.pedidos)}` : "Nenhuma venda no período"}</span>
            </article>
            <article>
              <small>Dia da semana mais forte</small>
              <strong>{data.diaDaSemanaPico ? data.diaDaSemanaPico.rotulo : "—"}</strong>
              <span>{data.diaDaSemanaPico ? `${formatCurrency(data.diaDaSemanaPico.faturamento)} · ${formatOrders(data.diaDaSemanaPico.pedidos)}` : "Nenhuma venda no período"}</span>
            </article>
            <article>
              <small>Horário de pico</small>
              <strong>{data.horaPico ? data.horaPico.rotulo : "—"}</strong>
              <span>{data.horaPico ? `${formatCurrency(data.horaPico.faturamento)} · ${formatOrders(data.horaPico.pedidos)}` : "Nenhuma venda no período"}</span>
            </article>
          </div>
          <div className="peaks-charts">
            <div><h3>{config.serieTitle}</h3><Bars points={data.serie} peakKey={data.pico?.chave} emptyText="Nenhuma venda no período." /></div>
            <div><h3>Por dia da semana</h3><Bars points={data.porDiaDaSemana} peakKey={data.diaDaSemanaPico?.chave} emptyText="Nenhuma venda no período." /></div>
            <div className="wide"><h3>Por horário</h3><Bars points={data.porHora} peakKey={data.horaPico?.chave} emptyText="Nenhuma venda no período." /></div>
          </div>
        </>
      )}
    </section>
  );
}

export function ReportsPage({
  period,
  customStart,
  customEnd,
  sellerId,
  onPeriod,
  onCustomStart,
  onCustomEnd,
  onSeller,
}: {
  period: ReportPeriod;
  customStart: string;
  customEnd: string;
  sellerId: string;
  onPeriod: (period: ReportPeriod) => void;
  onCustomStart: (value: string) => void;
  onCustomEnd: (value: string) => void;
  onSeller: (value: string) => void;
}) {
  const [sellerSort, setSellerSort] = useState<"revenue" | "sales">("revenue");
  const [historySort, setHistorySort] = useState<"date" | "value">("date");
  const [historyDescending, setHistoryDescending] = useState(true);
  const [historyPage, setHistoryPage] = useState(1);
  const [report, setReport] = useState<Relatorio | null>(null);
  const [history, setHistory] = useState<Pagina<Venda> | null>(null);
  const [sellers, setSellers] = useState<Usuario[]>([]);
  const [error, setError] = useState("");
  const { inicio, fim } = reportRange(period, customStart, customEnd);
  const vendedorId = sellerId ? Number(sellerId) : undefined;

  useEffect(() => {
    usuariosApi.listar({ size: 200, sort: "nome" }).then((pagina) => setSellers(pagina.conteudo)).catch(() => undefined);
  }, []);

  useEffect(() => {
    let cancelled = false;
    relatoriosApi.resumo({ inicio, fim, vendedorId })
      .then((relatorio) => { if (!cancelled) { setReport(relatorio); setError(""); } })
      .catch((loadError) => { if (!cancelled) setError(errorMessage(loadError, "Não foi possível carregar o relatório.")); });
    return () => { cancelled = true; };
  }, [inicio, fim, vendedorId]);

  useEffect(() => {
    let cancelled = false;
    const sort = `${historySort === "date" ? "dataHora" : "total"},${historyDescending ? "desc" : "asc"}`;
    vendasApi.listar({ inicio, fim, vendedorId, page: historyPage - 1, size: HISTORY_PAGE_SIZE, sort })
      .then((pagina) => { if (!cancelled) setHistory(pagina); })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [inicio, fim, vendedorId, historySort, historyDescending, historyPage]);

  const indicators = report?.indicadores;
  const hasSales = Boolean(indicators && indicators.pedidos > 0);
  const ranking = report?.maisVendidos || [];
  const rankingMax = Math.max(...ranking.map((item) => item.quantidade), 1);
  const sellerPerformance = [...(report?.vendedores || [])]
    .sort((a, b) => sellerSort === "revenue" ? b.faturamento - a.faturamento : b.vendas - a.vendas);
  const historyRows = (history?.conteudo || []).map(toSaleRecord);
  const historyTotal = history?.totalElementos || 0;
  const historyPageCount = Math.max(1, history?.totalPaginas || 1);
  const periodOptions: Array<{ id: ReportPeriod; label: string }> = [
    { id: "today", label: "Hoje" }, { id: "7days", label: "Últimos 7 dias" }, { id: "30days", label: "Últimos 30 dias" }, { id: "month", label: "Este mês" }, { id: "custom", label: "Período personalizado" },
  ];

  const changeHistorySort = (sort: "date" | "value") => {
    if (historySort === sort) setHistoryDescending((value) => !value);
    else { setHistorySort(sort); setHistoryDescending(true); }
    setHistoryPage(1);
  };

  return (
    <div className="dashboard-content reports-content">
      <section className="dashboard-card report-filters">
        <div className="period-options">{periodOptions.map((option) => <button key={option.id} type="button" className={period === option.id ? "active" : ""} onClick={() => { onPeriod(option.id); setHistoryPage(1); }}>{option.label}</button>)}</div>
        <div className="report-filter-fields">
          {period === "custom" && <><label><span>Data inicial</span><input type="date" value={customStart} onChange={(event) => { onCustomStart(event.target.value); setHistoryPage(1); }} /></label><label><span>Data final</span><input type="date" value={customEnd} onChange={(event) => { onCustomEnd(event.target.value); setHistoryPage(1); }} /></label></>}
          <label className="report-seller-filter"><span>Vendedor</span><select value={sellerId} onChange={(event) => { onSeller(event.target.value); setHistoryPage(1); }}><option value="">Todos os vendedores</option>{sellers.map((seller) => <option key={seller.id} value={seller.id}>{seller.nome}</option>)}</select></label>
        </div>
      </section>

      {error && <div className="form-alert" role="alert">{error}</div>}

      <section className="report-metrics-grid">
        <article><span className="revenue"><Icon name="cash" /></span><p><small>Faturamento total</small><strong>{formatCurrency(indicators?.faturamento || 0)}</strong></p></article>
        <article><span className="sales"><Icon name="orders" /></span><p><small>Total de vendas</small><strong>{indicators?.pedidos || 0}</strong></p></article>
        <article><span className="ticket"><Icon name="sales" /></span><p><small>Ticket médio</small><strong>{formatCurrency(indicators?.ticketMedio || 0)}</strong></p></article>
        <article><span className="items"><Icon name="products" /></span><p><small>Produtos vendidos</small><strong>{report?.produtosVendidos || 0}</strong></p></article>
      </section>

      <div className="report-top-grid">
        <section className="dashboard-card revenue-chart-card">
          <div className="report-section-heading"><div><h2>Evolução das vendas</h2><p>Faturamento {report?.granularidade === "HORA" ? "por hora" : report?.granularidade === "MES" ? "por mês" : "por dia"} no período selecionado.</p></div><strong>{formatCurrency(indicators?.faturamento || 0)}</strong></div>
          <Bars points={report?.evolucao || []} emptyText="Nenhuma venda encontrada para o período selecionado." />
        </section>
        <section className="dashboard-card payment-report-card">
          <div className="report-section-heading"><div><h2>Formas de pagamento</h2><p>Participação no faturamento.</p></div></div>
          {hasSales ? <div className="payment-report-list">{(report?.formasPagamento || []).map((row) => { const method = paymentFromCode(row.formaPagamento); return <div key={row.formaPagamento}><span className={`payment-report-icon ${method}`}><Icon name={method === "cash" ? "cash" : method === "pix" ? "pix" : "credit-card"} /></span><p><strong>{paymentLabels[method]}</strong><small>{formatCurrency(row.valor)}</small></p><b>{formatPercent(row.percentual)}</b><i><span style={{ width: `${row.percentual}%` }} /></i></div>; })}</div> : <div className="report-compact-empty">Nenhum pagamento no período.</div>}
        </section>
      </div>

      <PeaksSection sellerId={sellerId} />

      <div className="report-analysis-grid">
        <section className="dashboard-card top-products-card">
          <div className="report-section-heading"><div><h2>Produtos mais vendidos</h2><p>Classificação por quantidade vendida.</p></div></div>
          {ranking.length ? <div className="product-ranking">{ranking.map((item, index) => <div key={item.produtoId}><span className={`ranking-position position-${index + 1}`}>{index + 1}</span><p><strong>{item.nome}</strong><small>{item.codigo}</small><i><span style={{ width: `${item.quantidade / rankingMax * 100}%` }} /></i></p><div><strong>{item.quantidade} un.</strong><small>{formatCurrency(item.faturamento)}</small></div></div>)}</div> : <div className="report-compact-empty">Nenhum produto vendido no período.</div>}
        </section>
        <section className="dashboard-card seller-performance-card">
          <div className="report-section-heading"><div><h2>Desempenho por vendedor</h2><p>Resultados da equipe no período.</p></div><div className="seller-sort-buttons"><button type="button" className={sellerSort === "revenue" ? "active" : ""} onClick={() => setSellerSort("revenue")}>Faturamento</button><button type="button" className={sellerSort === "sales" ? "active" : ""} onClick={() => setSellerSort("sales")}>Vendas</button></div></div>
          {sellerPerformance.length ? <div className="seller-table-scroll"><table className="seller-report-table"><thead><tr><th>Vendedor</th><th>Vendas</th><th>Faturamento</th><th>Ticket médio</th><th>Participação</th></tr></thead><tbody>{sellerPerformance.map((row) => <tr key={row.vendedorId}><td><PersonCell initials={getInitials(row.nome)} name={row.nome} /></td><td>{row.vendas}</td><td><strong>{formatCurrency(row.faturamento)}</strong></td><td>{formatCurrency(row.ticketMedio)}</td><td><span className="seller-share"><i><span style={{ width: `${row.participacao}%` }} /></i>{formatPercent(row.participacao)}</span></td></tr>)}</tbody></table></div> : <div className="report-compact-empty">Nenhum resultado por vendedor no período.</div>}
        </section>
      </div>

      <section className="dashboard-card financial-history-card">
        <div className="report-section-heading"><div><h2>Histórico financeiro</h2><p>Vendas e cancelamentos registrados no período.</p></div><span>{historyTotal} registros</span></div>
        {historyRows.length ? <div className="financial-history-scroll"><table className="financial-history-table"><thead><tr><th><button type="button" onClick={() => changeHistorySort("date")}>Data {historySort === "date" && (historyDescending ? "↓" : "↑")}</button></th><th>Nº do pedido</th><th>Vendedor</th><th>Pagamento</th><th><button type="button" onClick={() => changeHistorySort("value")}>Valor {historySort === "value" && (historyDescending ? "↓" : "↑")}</button></th><th>Status</th></tr></thead><tbody>{historyRows.map((order) => <tr key={order.apiId}><td><span className="order-date">{order.confirmedAt.toLocaleDateString("pt-BR")}<small>{order.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</small></span></td><td><strong>{order.id}</strong></td><td>{order.seller}</td><td>{paymentLabels[order.paymentMethod]}</td><td><strong>{formatCurrency(order.total)}</strong></td><td><OrderStatusBadge status={order.status} /></td></tr>)}</tbody></table></div> : <div className="reports-history-empty">Nenhuma venda encontrada para o período selecionado.</div>}
        <footer className="orders-pagination"><p>Mostrando <strong>{historyRows.length}</strong> de {historyTotal} registros</p><Pager page={Math.min(historyPage, historyPageCount)} pageCount={historyPageCount} onPage={setHistoryPage} /></footer>
      </section>
    </div>
  );
}

/** Exporta em CSV todos os pedidos do período (busca a API página por página). */
export async function exportReportCsv(inicio: string, fim: string, vendedorId?: number) {
  const vendas: Venda[] = [];
  for (let page = 0; ; page++) {
    const pagina = await vendasApi.listar({ inicio, fim, vendedorId, page, size: 200, sort: "dataHora,desc" });
    vendas.push(...pagina.conteudo);
    if (page + 1 >= pagina.totalPaginas) break;
  }
  const escapeCsv = (value: string | number) => `"${String(value).replace(/"/g, "\"\"")}"`;
  const header = ["Data", "Número do pedido", "Vendedor", "Cliente", "Forma de pagamento", "Valor", "Status"];
  const lines = vendas.map(toSaleRecord).map((order) => [
    order.confirmedAt.toLocaleString("pt-BR"),
    order.id,
    order.seller,
    order.clientName,
    paymentLabels[order.paymentMethod],
    order.total.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    order.status,
  ].map(escapeCsv).join(";"));
  const csv = `﻿${header.map(escapeCsv).join(";")}\n${lines.join("\n")}`;
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `relatorio-ponto-e-sabor-${inicio}-a-${fim}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return vendas.length;
}
