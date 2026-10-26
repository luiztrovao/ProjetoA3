// Tela Dashboard (Dono e Gerente): indicadores de hoje e dos últimos 7 dias vindos de /api/dashboard.
import { useEffect, useState } from "react";
import { errorMessage, relatoriosApi, type Dashboard } from "../../api";
import { Icon } from "../../components/Icon";
import { statusLabels, type UserRole } from "../../types";
import { formatCurrency } from "../../utils/format";

type Page = "sales" | "orders" | "reports" | "registrations";

const weekdayLabel = (isoDay: string) =>
  new Date(`${isoDay}T12:00:00`).toLocaleDateString("pt-BR", { weekday: "short" }).replace(".", "");

export function DashboardPage({ currentRole, onNavigate }: { currentRole: UserRole; onNavigate: (page: Page) => void }) {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    relatoriosApi.dashboard()
      .then((dashboard) => { if (!cancelled) setData(dashboard); })
      .catch((loadError) => { if (!cancelled) setError(errorMessage(loadError, "Não foi possível carregar o dashboard.")); });
    return () => { cancelled = true; };
  }, []);

  if (error) return <div className="dashboard-content"><div className="form-alert" role="alert">{error}</div></div>;
  if (!data) return <div className="dashboard-content" aria-busy="true" />;

  const chartDays = data.ultimos7Dias;
  const chartMax = Math.max(...chartDays.map((day) => day.faturamento), 1);
  const step = chartDays.length > 1 ? 600 / (chartDays.length - 1) : 600;
  const y = (value: number) => 200 - value / chartMax * 170;
  const points = chartDays.map((day, index) => `${index * step},${y(day.faturamento)}`).join(" ");
  const areaPath = `M0 200 L${chartDays.map((day, index) => `${index * step} ${y(day.faturamento)}`).join(" L")} L600 200 Z`;

  return (
    <div className="dashboard-content">
      <section className="metrics-grid">
        <article className="metric-card"><span className="metric-icon terracotta"><Icon name="sales" /></span><div className="metric-copy"><span>Vendas de hoje</span><strong>{formatCurrency(data.hoje.faturamento)}</strong><small>{data.hoje.pedidos} vendas confirmadas hoje</small></div></article>
        <article className="metric-card"><span className="metric-icon green"><Icon name="orders" /></span><div className="metric-copy"><span>Pedidos de hoje</span><strong>{data.hoje.pedidos}</strong><small>Cancelados não contabilizados</small></div></article>
        <article className="metric-card"><span className="metric-icon gold"><Icon name="cash" /></span><div className="metric-copy"><span>Ticket médio</span><strong>{formatCurrency(data.hoje.ticketMedio)}</strong><small>Calculado com as vendas de hoje</small></div></article>
        <article className="metric-card"><span className="metric-icon slate"><Icon name="products" /></span><div className="metric-copy"><span>Produtos cadastrados</span><strong>{data.produtos.cadastrados}</strong><small>{data.produtos.ativos} ativos · {data.produtos.estoqueBaixo} com estoque baixo</small></div></article>
      </section>

      <section className="dashboard-card sales-summary">
        <div className="section-heading"><div><h2>Evolução das vendas</h2><p>Faturamento confirmado nos últimos sete dias.</p></div><button className="period-control" type="button" onClick={() => onNavigate("reports")}>Ver relatórios <Icon name="chevron" /></button></div>
        <div className="sales-card-body">
          <div className="chart">
            <div className="chart-y-labels">{[1, 0.75, 0.5, 0.25, 0].map((ratio) => <span key={ratio}>{formatCurrency(chartMax * ratio)}</span>)}</div>
            <div className="chart-plot">
              <div className="chart-grid">{Array.from({ length: 5 }, (_, index) => <span key={index} />)}</div>
              <svg className="chart-svg" viewBox="0 0 600 220" preserveAspectRatio="none">
                <defs><linearGradient id="dashboard-sales-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#c75a3b" stopOpacity=".18" /><stop offset="100%" stopColor="#c75a3b" stopOpacity="0" /></linearGradient></defs>
                <path d={areaPath} fill="url(#dashboard-sales-fill)" />
                <polyline className="chart-line" points={points} />
                {chartDays.map((day, index) => <circle key={day.chave} className="chart-point" cx={index * step} cy={y(day.faturamento)} r="4"><title>{`${day.rotulo}: ${formatCurrency(day.faturamento)}`}</title></circle>)}
              </svg>
              <div className="chart-x-labels">{chartDays.map((day) => <span key={day.chave}>{weekdayLabel(day.chave)}</span>)}</div>
            </div>
          </div>
          <aside className="sales-totals"><span className="totals-kicker">Resumo do período</span><div className="total-featured"><span>Faturamento</span><strong>{formatCurrency(data.resumo7Dias.faturamento)}</strong><small>Dados dos pedidos válidos</small></div><div className="total-row"><span>Pedidos</span><strong>{data.resumo7Dias.pedidos}</strong></div><div className="total-row"><span>Ticket médio</span><strong>{formatCurrency(data.resumo7Dias.ticketMedio)}</strong></div></aside>
        </div>
      </section>

      <div className="dashboard-bottom-grid">
        <section className="dashboard-card orders-card">
          <div className="section-heading"><div><h2>Pedidos recentes</h2><p>Últimos registros do histórico compartilhado.</p></div><button className="dashboard-text-action" type="button" onClick={() => onNavigate("orders")}>Ver todos</button></div>
          <div className="table-scroll">
            <table><thead><tr><th>Pedido</th><th>Cliente</th><th>Valor</th><th>Status</th></tr></thead><tbody>{data.pedidosRecentes.length ? data.pedidosRecentes.map((order) => { const status = statusLabels[order.status]; return <tr key={order.id}><td><strong>{order.numero}</strong></td><td>{order.cliente?.nome || "Cliente não identificado"}</td><td><strong>{formatCurrency(order.total)}</strong></td><td><span className={`status-badge ${status === "Concluído" ? "success" : status === "Cancelado" ? "cancelled" : "preparing"}`}><i />{status}</span></td></tr>; }) : <tr><td colSpan={4}>Nenhum pedido registrado.</td></tr>}</tbody></table>
          </div>
        </section>
        <section className="dashboard-card quick-actions">
          <div className="section-heading"><div><h2>Ações rápidas</h2><p>Acesse as tarefas mais utilizadas.</p></div></div>
          <div className="action-list">
            <button className="quick-action primary" type="button" onClick={() => onNavigate("sales")}><span><Icon name="plus" /></span><div><strong>Nova venda</strong><small>Iniciar um novo pedido</small></div><b>›</b></button>
            <button className="quick-action" type="button" onClick={() => onNavigate("registrations")}><span><Icon name="users" /></span><div><strong>Novo cliente</strong><small>Abrir cadastros</small></div><b>›</b></button>
            {currentRole !== "seller" && <button className="quick-action" type="button" onClick={() => onNavigate("reports")}><span><Icon name="reports" /></span><div><strong>Ver relatórios</strong><small>Acompanhar resultados e picos de venda</small></div><b>›</b></button>}
          </div>
        </section>
      </div>
    </div>
  );
}
