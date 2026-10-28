// Tela Gestão de Pedidos: histórico vindo de /api/vendas, com filtros e paginação feitos na API.
import { useCallback, useEffect, useState } from "react";
import { errorMessage, usuariosApi, vendasApi, type Pagina, type Usuario, type Venda } from "../../api";
import { Icon } from "../../components/Icon";
import { PersonCell } from "../../components/ui";
import { paymentCodes, paymentLabels, statusFromLabel, type OrderStatus, type PaymentMethod, type SaleRecord, type UserRole } from "../../types";
import { formatCurrency, getInitials } from "../../utils/format";
import { toSaleRecord } from "./mappers";

const PAGE_SIZE = 8;

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`order-status-badge ${status === "Em preparo" ? "preparing" : status === "Concluído" ? "completed" : "cancelled"}`}><i />{status}</span>;
}

/** Números de página exibidos: no máximo 5, centrados na página atual. */
export const visiblePages = (current: number, count: number) => {
  const start = Math.max(1, Math.min(current - 2, count - 4));
  return Array.from({ length: Math.min(5, count) }, (_, index) => start + index);
};

export function Pager({ page, pageCount, onPage }: { page: number; pageCount: number; onPage: (page: number) => void }) {
  return (
    <div>
      <button type="button" aria-label="Página anterior" disabled={page <= 1} onClick={() => onPage(page - 1)}><Icon name="chevron-left" /></button>
      {visiblePages(page, pageCount).map((number) => <button key={number} type="button" className={page === number ? "active" : ""} onClick={() => onPage(number)}>{number}</button>)}
      <button type="button" aria-label="Próxima página" disabled={page >= pageCount} onClick={() => onPage(page + 1)}><Icon name="chevron-right" /></button>
    </div>
  );
}

export function OrderDetailsModal({ order, onClose }: { order: SaleRecord; onClose: () => void }) {
  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="record-modal order-details-modal" role="dialog" aria-modal="true" aria-labelledby="order-details-title">
        <header className="modal-header">
          <div><span className="modal-kicker">Histórico de pedidos</span><h2 id="order-details-title">Detalhes do pedido {order.id}</h2><p>Consulte as informações completas da venda registrada.</p></div>
          <button type="button" aria-label="Fechar detalhes" onClick={onClose}><Icon name="close" /></button>
        </header>
        <div className="modal-body order-details-body">
          <section className="order-detail-section">
            <div className="order-detail-section-title"><h3>Informações gerais</h3><OrderStatusBadge status={order.status} /></div>
            <div className="order-detail-general">
              <div><small>Número do pedido</small><strong>{order.id}</strong></div>
              <div><small>Data e hora</small><strong>{order.confirmedAt.toLocaleDateString("pt-BR")} · {order.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</strong></div>
              <div><small>Vendedor responsável</small><strong>{order.seller}</strong></div>
              <div><small>Cliente</small><strong>{order.clientName}</strong></div>
            </div>
            {order.cancellationReason && <div className="cancellation-reason"><strong>Motivo do cancelamento</strong><p>{order.cancellationReason}</p></div>}
          </section>
          <section className="order-detail-section">
            <div className="order-detail-section-title"><h3>Produtos</h3><span>{order.items.reduce((total, item) => total + item.quantity, 0)} itens</span></div>
            <div className="order-detail-products">
              {order.items.map((item) => {
                const unitPrice = item.unitPrice ?? 0;
                const name = item.productName || "Produto";
                return <div key={item.productId}><PersonCell initials={getInitials(name)} name={name} detail={item.productCode} /><span>{item.quantity} un.</span><span>{formatCurrency(unitPrice)}</span><strong>{formatCurrency(unitPrice * item.quantity)}</strong></div>;
              })}
            </div>
          </section>
          <section className="order-detail-section payment-detail-section">
            <div className="order-detail-section-title"><h3>Pagamento</h3><span>{paymentLabels[order.paymentMethod]}</span></div>
            <div className="order-detail-payment">
              <div><span>Subtotal</span><strong>{formatCurrency(order.subtotal)}</strong></div>
              <div><span>Desconto</span><strong>- {formatCurrency(order.discountValue)}</strong></div>
              {order.paymentMethod === "cash" && <><div><span>Valor recebido</span><strong>{formatCurrency(order.cashReceived)}</strong></div><div><span>Troco</span><strong>{formatCurrency(order.change)}</strong></div></>}
              <div className="order-detail-total"><span>Valor total</span><strong>{formatCurrency(order.total)}</strong></div>
            </div>
          </section>
        </div>
        <footer className="modal-footer"><button className="secondary-modal-button" type="button" onClick={onClose}>Fechar</button></footer>
      </section>
    </div>
  );
}

type Metrics = { total: number; preparing: number; completed: number; cancelled: number };

/** Motivo gravado quando a preferência "Confirmar antes de cancelar" está desligada. */
const MOTIVO_SEM_CONFIRMACAO = "Não informado (confirmação desativada nas configurações)";

export function OrdersPage({
  currentRole,
  confirmCancellation = true,
  initialSearch = "",
  onToast,
}: {
  currentRole: UserRole;
  /** Preferência de vendas: pede confirmação e motivo antes de cancelar. */
  confirmCancellation?: boolean;
  /** Busca já preenchida (ex.: número do pedido vindo de uma notificação). */
  initialSearch?: string;
  onToast: (message: string) => void;
}) {
  const [search, setSearch] = useState(initialSearch);
  const [debouncedSearch, setDebouncedSearch] = useState(initialSearch);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [status, setStatus] = useState("");
  const [sellerId, setSellerId] = useState("");
  const [payment, setPayment] = useState("");
  const [newestFirst, setNewestFirst] = useState(true);
  const [page, setPage] = useState(1);
  const [result, setResult] = useState<Pagina<Venda> | null>(null);
  const [metrics, setMetrics] = useState<Metrics>({ total: 0, preparing: 0, completed: 0, cancelled: 0 });
  const [sellers, setSellers] = useState<Usuario[]>([]);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [menuId, setMenuId] = useState<number | null>(null);
  const [viewing, setViewing] = useState<SaleRecord | null>(null);
  const [completeConfirmation, setCompleteConfirmation] = useState<SaleRecord | null>(null);
  const [cancelOrder, setCancelOrder] = useState<SaleRecord | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelError, setCancelError] = useState("");
  const [working, setWorking] = useState(false);
  const canManage = currentRole === "owner" || currentRole === "manager";
  const orders = (result?.conteudo || []).map(toSaleRecord);
  const totalRecords = result?.totalElementos || 0;
  const pageCount = Math.max(1, result?.totalPaginas || 1);
  const hasFilters = Boolean(search || startDate || endDate || status || sellerId || payment);
  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  // A busca vai para a API 300 ms depois que o usuário para de digitar.
  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [search]);

  // Vendedores para o filtro (o Vendedor só enxerga os próprios pedidos e não usa este filtro).
  useEffect(() => {
    if (!canManage) return;
    usuariosApi.listar({ size: 200, sort: "nome" }).then((pagina) => setSellers(pagina.conteudo)).catch(() => undefined);
  }, [canManage]);

  useEffect(() => {
    let cancelled = false;
    const filters = {
      termo: debouncedSearch || undefined,
      inicio: startDate || undefined,
      fim: endDate || undefined,
      vendedorId: sellerId ? Number(sellerId) : undefined,
      formaPagamento: payment ? paymentCodes[payment as PaymentMethod] : undefined,
    };
    const count = (statusFilter?: "EM_PREPARO" | "CONCLUIDA" | "CANCELADA") =>
      vendasApi.listar({ ...filters, status: statusFilter, size: 1 }).then((pagina) => pagina.totalElementos);
    Promise.all([
      vendasApi.listar({ ...filters, status: statusFromLabel(status), page: page - 1, size: PAGE_SIZE, sort: `dataHora,${newestFirst ? "desc" : "asc"}` }),
      count(), count("EM_PREPARO"), count("CONCLUIDA"), count("CANCELADA"),
    ])
      .then(([pagina, total, preparing, completed, cancelledCount]) => {
        if (cancelled) return;
        setResult(pagina);
        setMetrics({ total, preparing, completed, cancelled: cancelledCount });
        setLoadError("");
      })
      .catch((error) => { if (!cancelled) setLoadError(errorMessage(error, "Não foi possível carregar os pedidos.")); });
    return () => { cancelled = true; };
  }, [debouncedSearch, startDate, endDate, status, sellerId, payment, newestFirst, page, reloadKey]);

  const clearFilters = () => {
    setSearch(""); setStartDate(""); setEndDate(""); setStatus(""); setSellerId(""); setPayment(""); setPage(1);
  };

  const completeOrder = async (order: SaleRecord) => {
    if (working) return;
    setWorking(true);
    try {
      await vendasApi.concluir(order.apiId);
      onToast("Pedido concluído com sucesso.");
      reload();
    } catch (error) {
      onToast(errorMessage(error));
    } finally {
      setWorking(false);
      setCompleteConfirmation(null);
    }
  };

  const startCancellation = (order: SaleRecord) => {
    if (confirmCancellation) setCancelOrder(order);
    else void cancel(order, MOTIVO_SEM_CONFIRMACAO);
  };

  const submitCancellation = async () => {
    if (!cancelOrder) return;
    if (!cancelReason.trim()) {
      setCancelError("Informe o motivo do cancelamento.");
      return;
    }
    await cancel(cancelOrder, cancelReason.trim());
  };

  const cancel = async (order: SaleRecord, reason: string) => {
    if (working) return;
    setWorking(true);
    try {
      await vendasApi.cancelar(order.apiId, reason);
      onToast("Pedido cancelado e mantido no histórico.");
      setCancelOrder(null);
      setCancelReason("");
      setCancelError("");
      reload();
    } catch (error) {
      if (cancelOrder) setCancelError(errorMessage(error));
      else onToast(errorMessage(error));
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="dashboard-content orders-content">
      <section className="order-metrics-grid">
        <article><span className="all"><Icon name="orders" /></span><p><small>Total de pedidos</small><strong>{metrics.total}</strong></p></article>
        <article><span className="preparing"><Icon name="clock" /></span><p><small>Pedidos em preparo</small><strong>{metrics.preparing}</strong></p></article>
        <article><span className="completed"><Icon name="check" /></span><p><small>Pedidos concluídos</small><strong>{metrics.completed}</strong></p></article>
        <article><span className="cancelled"><Icon name="close" /></span><p><small>Pedidos cancelados</small><strong>{metrics.cancelled}</strong></p></article>
      </section>

      <section className="dashboard-card orders-card-main">
        <div className="orders-heading"><div><h2>Histórico de pedidos</h2><p>{canManage ? "Acompanhe as vendas registradas e o andamento dos pedidos." : "Acompanhe os pedidos que você registrou."}</p></div><span>{totalRecords} registros</span></div>
        <div className="orders-filter-panel">
          <label className="orders-search"><Icon name="search" /><input type="search" value={search} placeholder="Pesquisar por número, cliente ou vendedor..." onChange={(event) => { setSearch(event.target.value); setPage(1); }} /></label>
          <div className="orders-filters-row">
            <label><span>Data inicial</span><input type="date" value={startDate} onChange={(event) => { setStartDate(event.target.value); setPage(1); }} /></label>
            <label><span>Data final</span><input type="date" value={endDate} onChange={(event) => { setEndDate(event.target.value); setPage(1); }} /></label>
            <label><span>Status</span><select value={status} onChange={(event) => { setStatus(event.target.value); setPage(1); }}><option value="">Todos</option><option>Em preparo</option><option>Concluído</option><option>Cancelado</option></select></label>
            <label><span>Vendedor</span><select value={sellerId} disabled={!canManage} onChange={(event) => { setSellerId(event.target.value); setPage(1); }}><option value="">Todos</option>{sellers.map((seller) => <option key={seller.id} value={seller.id}>{seller.nome}</option>)}</select></label>
            <label><span>Pagamento</span><select value={payment} onChange={(event) => { setPayment(event.target.value); setPage(1); }}><option value="">Todos</option>{Object.entries(paymentLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <button type="button" disabled={!hasFilters} onClick={clearFilters}><Icon name="close" />Limpar filtros</button>
          </div>
        </div>
        {loadError && <div className="form-alert" role="alert">{loadError}</div>}

        {orders.length > 0 ? (
          <div className="orders-table-scroll">
            <table className="orders-table">
              <thead><tr><th>Nº do pedido</th><th><button type="button" onClick={() => { setNewestFirst((value) => !value); setPage(1); }}>Data e hora <span>{newestFirst ? "↓" : "↑"}</span></button></th><th>Cliente</th><th>Vendedor</th><th>Itens</th><th>Valor total</th><th>Pagamento</th><th>Status</th><th>Ações</th></tr></thead>
              <tbody>{orders.map((order) => {
                const itemCount = order.items.reduce((total, item) => total + item.quantity, 0);
                return (
                  <tr key={order.apiId}>
                    <td><div className="order-number-cell"><strong>{order.id}</strong></div></td>
                    <td><span className="order-date">{order.confirmedAt.toLocaleDateString("pt-BR")}<small>{order.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</small></span></td>
                    <td>{order.clientName}</td><td>{order.seller}</td><td>{itemCount} un.</td><td><strong>{formatCurrency(order.total)}</strong></td><td>{paymentLabels[order.paymentMethod]}</td><td><OrderStatusBadge status={order.status} /></td>
                    <td><div className="order-row-actions"><button type="button" title="Visualizar" aria-label={`Visualizar ${order.id}`} onClick={() => setViewing(order)}><Icon name="eye" /></button>{canManage && order.status === "Em preparo" && <><button type="button" title="Mais opções" aria-label={`Mais opções para ${order.id}`} aria-expanded={menuId === order.apiId} onClick={() => setMenuId((current) => current === order.apiId ? null : order.apiId)}><Icon name="more" /></button>{menuId === order.apiId && <div className="order-actions-menu"><button type="button" onClick={() => { setCompleteConfirmation(order); setMenuId(null); }}><Icon name="check" />Marcar como concluído</button><button type="button" className="cancel" onClick={() => { startCancellation(order); setMenuId(null); }}><Icon name="close" />Cancelar pedido</button></div>}</>}</div></td>
                  </tr>
                );
              })}</tbody>
            </table>
          </div>
        ) : (
          <div className="orders-empty"><span><Icon name="orders" /></span><strong>Nenhum pedido encontrado</strong><p>{hasFilters ? "Ajuste ou limpe os filtros para visualizar outros pedidos." : "As vendas confirmadas aparecerão aqui automaticamente."}</p>{hasFilters && <button type="button" onClick={clearFilters}>Limpar filtros</button>}</div>
        )}
        <footer className="orders-pagination"><p>Mostrando <strong>{orders.length}</strong> de {totalRecords} pedidos</p><Pager page={Math.min(page, pageCount)} pageCount={pageCount} onPage={setPage} /></footer>
      </section>

      {viewing && <OrderDetailsModal order={viewing} onClose={() => setViewing(null)} />}
      {completeConfirmation && <div className="confirm-backdrop" role="presentation"><section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="complete-order-title"><span className="confirm-icon complete"><Icon name="check" /></span><h2 id="complete-order-title">Concluir {completeConfirmation.id}?</h2><p>O pedido será marcado como concluído. Esta alteração não poderá ser desfeita automaticamente.</p><div><button type="button" onClick={() => setCompleteConfirmation(null)}>Voltar</button><button className="confirm-complete-button" type="button" disabled={working} onClick={() => completeOrder(completeConfirmation)}>Concluir pedido</button></div></section></div>}
      {cancelOrder && <div className="confirm-backdrop" role="presentation"><section className="cancel-order-dialog" role="alertdialog" aria-modal="true" aria-labelledby="cancel-order-title"><span className="confirm-icon"><Icon name="close" /></span><h2 id="cancel-order-title">Cancelar pedido {cancelOrder.id}?</h2><p>O pedido continuará no histórico e os produtos voltarão ao estoque.</p><label htmlFor="cancellation-reason">Motivo do cancelamento<textarea id="cancellation-reason" value={cancelReason} placeholder="Descreva o motivo do cancelamento..." onChange={(event) => { setCancelReason(event.target.value); setCancelError(""); }} /></label>{cancelError && <small role="alert">{cancelError}</small>}<div><button type="button" onClick={() => { setCancelOrder(null); setCancelReason(""); setCancelError(""); }}>Voltar</button><button type="button" disabled={working} onClick={submitCancellation}>Confirmar cancelamento</button></div></section></div>}
    </div>
  );
}
