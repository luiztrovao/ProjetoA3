import { useEffect, useRef, useState } from "react";
import {
  categoriasApi,
  clientesApi,
  configuracoesApi,
  contaApi,
  errorMessage,
  imageUrl,
  onUnauthorized,
  produtosApi,
  publicoApi,
  tokenStorage,
  vendasApi,
  type Usuario,
} from "./api";
import { BrandMark, Icon, type IconName } from "./components/Icon";
import { PersonCell, StatusBadge } from "./components/ui";
import { RegistrationsPage } from "./pages/cadastros/RegistrationsPage";
import { toClientRecord, toEmployeeRecord, toProductRecord } from "./pages/cadastros/mappers";
import { DashboardPage } from "./pages/dashboard/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { exportReportCsv, reportRange, ReportsPage, type ReportPeriod } from "./pages/relatorios/ReportsPage";
import { defaultCompany, defaultPreferences, toCompanySettings, toSalesPreferences, type CompanySettings, type SalesPreferences } from "./pages/settings/mappers";
import { SettingsPage } from "./pages/settings/SettingsPage";
import { toSaleRecord } from "./pages/vendas/mappers";
import { OrdersPage } from "./pages/vendas/OrdersPage";
import {
  paymentCodes,
  paymentLabels,
  roleKey,
  type CartItem,
  type ClientRecord,
  type EmployeeRecord,
  type PaymentMethod,
  type PreparedOrder,
  type ProductRecord,
  type SaleRecord,
  type TabId,
  type UserRole,
} from "./types";
import { formatCurrency, getInitials, maskCurrency, parseCurrency } from "./utils/format";

const navigation: Array<{
  label: string;
  icon: IconName;
  href: string;
  allowedRoles: Array<"owner" | "manager" | "seller">;
}> = [
  { label: "Dashboard", icon: "dashboard", href: "#dashboard", allowedRoles: ["owner", "manager"] },
  { label: "Vendas", icon: "sales", href: "#vendas", allowedRoles: ["owner", "manager", "seller"] },
  { label: "Pedidos", icon: "orders", href: "#pedidos", allowedRoles: ["owner", "manager", "seller"] },
  { label: "Cadastros", icon: "registers", href: "#cadastros", allowedRoles: ["owner", "manager", "seller"] },
  { label: "Relatórios", icon: "reports", href: "#relatorios", allowedRoles: ["owner", "manager"] },
  { label: "Configurações", icon: "settings", href: "#configuracoes", allowedRoles: ["owner", "manager", "seller"] },
];

function UserIdentity({ compact = false, user }: { compact?: boolean; user?: EmployeeRecord }) {
  const displayName = user?.name || "Matheus Almeida";
  const displayRole = user?.role || "Dono";
  return (
    <div className={`user-identity${compact ? " compact" : ""}`}>
      <span className="avatar">{user?.initials || "MA"}</span>
      <span className="user-copy">
        <strong>{displayName.split(" ")[0]}</strong>
        <small>{displayRole === "Dono" ? "Administrador / Dono" : displayRole}</small>
      </span>
    </div>
  );
}

function Sidebar({
  activePage,
  onNavigate,
  currentRole,
  currentUser,
  businessName,
  businessLogo,
}: {
  activePage: "dashboard" | "sales" | "orders" | "reports" | "registrations" | "settings";
  onNavigate: (page: "dashboard" | "sales" | "orders" | "reports" | "registrations" | "settings") => void;
  currentRole: UserRole;
  currentUser: EmployeeRecord;
  businessName: string;
  businessLogo: string;
}) {
  const visibleItems = navigation.filter((item) =>
    item.allowedRoles.includes(currentRole),
  );

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <BrandMark src={businessLogo} />
        <div className="brand-text">
          <p className="brand-name">{businessName}</p>
          <p className="brand-type">Gestão para lanchonetes</p>
        </div>
      </div>
      <nav className="main-navigation" aria-label="Navegação principal">
        <span className="nav-label">Menu principal</span>
        {visibleItems.map((item) => (
          <a
            key={item.label}
            className={`nav-item${(item.label === "Dashboard" && activePage === "dashboard") || (item.label === "Vendas" && activePage === "sales") || (item.label === "Pedidos" && activePage === "orders") || (item.label === "Relatórios" && activePage === "reports") || (item.label === "Cadastros" && activePage === "registrations") || (item.label === "Configurações" && activePage === "settings") ? " active" : ""}`}
            href={item.href}
            aria-current={(item.label === "Dashboard" && activePage === "dashboard") || (item.label === "Vendas" && activePage === "sales") || (item.label === "Pedidos" && activePage === "orders") || (item.label === "Relatórios" && activePage === "reports") || (item.label === "Cadastros" && activePage === "registrations") || (item.label === "Configurações" && activePage === "settings") ? "page" : undefined}
            title={item.label}
            onClick={(event) => {
              if (item.label === "Dashboard" || item.label === "Vendas" || item.label === "Pedidos" || item.label === "Relatórios" || item.label === "Cadastros" || item.label === "Configurações") {
                event.preventDefault();
                onNavigate(item.label === "Dashboard" ? "dashboard" : item.label === "Vendas" ? "sales" : item.label === "Pedidos" ? "orders" : item.label === "Relatórios" ? "reports" : item.label === "Configurações" ? "settings" : "registrations");
              }
            }}
          >
            <Icon name={item.icon} />
            <span>{item.label}</span>
          </a>
        ))}
      </nav>
      <div className="sidebar-user">
        <UserIdentity compact user={currentUser} />
        <button className="icon-button sidebar-more" type="button" aria-label="Abrir menu da conta">
          <Icon name="more" />
        </button>
      </div>
    </aside>
  );
}

const itemUnitPrice = (item: CartItem, product: ProductRecord) => item.unitPrice ?? parseCurrency(product.price);

function SalesPage({
  products,
  clients,
  categories,
  cart,
  productSearch,
  category,
  clientSearch,
  selectedClientId,
  discount,
  sellerName,
  currentRole,
  preferences,
  onProductSearch,
  onCategory,
  onClientSearch,
  onSelectClient,
  onDiscount,
  onAdd,
  onQuantity,
  onRemove,
  onClear,
  onReview,
}: {
  products: ProductRecord[];
  clients: ClientRecord[];
  categories: string[];
  cart: CartItem[];
  productSearch: string;
  category: string;
  clientSearch: string;
  selectedClientId: number | null;
  discount: string;
  sellerName: string;
  currentRole: UserRole;
  preferences: SalesPreferences;
  onProductSearch: (value: string) => void;
  onCategory: (value: string) => void;
  onClientSearch: (value: string) => void;
  onSelectClient: (id: number | null) => void;
  onDiscount: (value: string) => void;
  onAdd: (id: number) => void;
  onQuantity: (id: number, delta: number) => void;
  onRemove: (id: number) => void;
  onClear: () => void;
  onReview: () => void;
}) {
  const [now, setNow] = useState(new Date());

  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 30000);
    return () => window.clearInterval(interval);
  }, []);

  const visibleProducts = products.filter((product) => {
    const matchesSearch = `${product.name} ${product.code}`.toLocaleLowerCase().includes(productSearch.toLocaleLowerCase());
    return matchesSearch && (category === "Todos" || product.category === category);
  });
  const visibleClients = clients.filter((client) =>
    client.status === "Ativo" &&
    (client.id === selectedClientId ||
      `${client.name} ${client.cpf} ${client.phone}`.toLocaleLowerCase().includes(clientSearch.toLocaleLowerCase())),
  );
  const detailedCart = cart.flatMap((item) => {
    const product = products.find((record) => record.id === item.productId);
    return product ? [{ ...item, product }] : [];
  });
  const itemCount = cart.reduce((total, item) => total + item.quantity, 0);
  const subtotal = detailedCart.reduce((total, item) => total + parseCurrency(item.product.price) * item.quantity, 0);
  const discountValue = Math.min(parseCurrency(discount || "R$ 0,00"), subtotal);
  const total = Math.max(0, subtotal - discountValue);

  return (
    <div className="dashboard-content sales-content">
      <section className="sale-context-bar" aria-label="Informações da venda">
        <div><span><Icon name="users" /></span><p><small>Vendedor responsável</small><strong>{sellerName}</strong></p></div>
        <div><span><Icon name="clock" /></span><p><small>Data e hora</small><strong>{now.toLocaleDateString("pt-BR")} · {now.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</strong></p></div>
        <div className="order-identifier"><span><Icon name="orders" /></span><p><small>Identificação</small><strong>Novo pedido</strong></p><i>Em montagem</i></div>
      </section>

      <div className="sales-workspace">
        <section className="product-browser">
          <div className="sales-section-heading">
            <div><h2>Produtos</h2><p>Escolha os itens para adicionar ao pedido.</p></div>
            <span>{visibleProducts.length} produtos</span>
          </div>
          <label className="sale-search-field">
            <span className="sr-only">Pesquisar produtos</span>
            <Icon name="search" />
            <input type="search" value={productSearch} placeholder="Pesquisar produto por nome ou código..." onChange={(event) => onProductSearch(event.target.value)} />
          </label>
          <div className="category-filters" aria-label="Filtrar por categoria">
            {categories.map((item) => (
              <button key={item} type="button" className={category === item ? "active" : ""} onClick={() => onCategory(item)}>{item}</button>
            ))}
          </div>
          {visibleProducts.length > 0 ? (
            <div className="product-grid">
              {visibleProducts.map((product) => {
                const unavailable = product.status === "Inativo" || (preferences.stockControl && preferences.blockWithoutStock && product.stock <= 0);
                const inCart = cart.find((item) => item.productId === product.id)?.quantity || 0;
                const limitReached = preferences.stockControl && preferences.blockWithoutStock && inCart >= product.stock;
                return (
                  <article className={`sale-product-card${unavailable ? " unavailable" : ""}`} key={product.id}>
                    <div className={`sale-product-image${product.image ? " filled" : ""}`}>
                      {product.image ? <img src={product.image} alt="" /> : <Icon name="products" />}
                      <span className={unavailable ? "unavailable" : preferences.stockControl && product.stock <= preferences.lowStockThreshold ? "low" : "available"}>
                        {unavailable ? "Indisponível" : preferences.stockControl && product.stock <= preferences.lowStockThreshold ? "Estoque baixo" : "Disponível"}
                      </span>
                    </div>
                    <div className="sale-product-copy">
                      <small>{product.category} · {product.code}</small>
                      <h3>{product.name}</h3>
                      <div><strong>{product.price}</strong><span>{preferences.stockControl ? `${product.stock} un.` : "Estoque livre"}</span></div>
                    </div>
                    <button type="button" disabled={unavailable || limitReached} onClick={() => onAdd(product.id)}>
                      <Icon name="plus" />{limitReached ? "Limite em estoque" : "Adicionar"}
                    </button>
                  </article>
                );
              })}
            </div>
          ) : (
            <div className="no-products"><Icon name="search" /><strong>Nenhum produto encontrado</strong><p>Tente alterar a busca ou selecionar outra categoria.</p></div>
          )}
        </section>

        <aside className="current-order">
          <header className="current-order-header">
            <div>
              <span><Icon name="cart" /></span>
              <div className="current-order-title">
                <h2>Pedido atual</h2>
                <small>{itemCount === 0 ? "Nenhum item" : `${itemCount} ${itemCount === 1 ? "item" : "itens"}`}</small>
              </div>
            </div>
          </header>
          <div className="customer-selector">
            <label htmlFor="customer-search">Cliente <small>(opcional)</small></label>
            <div className="customer-search-wrap"><Icon name="search" /><input id="customer-search" type="search" value={clientSearch} placeholder="Buscar por nome, CPF ou telefone" onChange={(event) => onClientSearch(event.target.value)} /></div>
            <select aria-label="Selecionar cliente" value={selectedClientId || ""} onChange={(event) => onSelectClient(event.target.value ? Number(event.target.value) : null)}>
              <option value="">Cliente não identificado</option>
              {visibleClients.map((client) => <option key={client.id} value={client.id}>{client.name} · {client.phone}</option>)}
            </select>
          </div>
          <div className={`order-items${detailedCart.length === 0 ? " empty" : ""}`}>
            {detailedCart.length === 0 ? (
              <div className="empty-order"><span><Icon name="cart" /></span><strong>Seu pedido está vazio</strong><p>Nenhum produto adicionado. Selecione um produto para começar.</p></div>
            ) : detailedCart.map(({ product, quantity }) => (
              <article className="order-item" key={product.id}>
                <div className="order-item-main"><span>{product.initials}</span><p><strong>{product.name}</strong><small>{product.price} cada</small></p><button type="button" aria-label={`Remover ${product.name}`} onClick={() => onRemove(product.id)}><Icon name="trash" /></button></div>
                <div className="order-item-controls">
                  <div><button type="button" aria-label={`Diminuir ${product.name}`} onClick={() => onQuantity(product.id, -1)}>−</button><strong>{quantity}</strong><button type="button" aria-label={`Aumentar ${product.name}`} disabled={preferences.stockControl && preferences.blockWithoutStock && quantity >= product.stock} onClick={() => onQuantity(product.id, 1)}>+</button></div>
                  <strong>{formatCurrency(parseCurrency(product.price) * quantity)}</strong>
                </div>
              </article>
            ))}
          </div>
          <footer className="order-summary">
            <div className="summary-line"><span>Quantidade de itens</span><strong>{itemCount}</strong></div>
            <div className="summary-line"><span>Subtotal</span><strong>{formatCurrency(subtotal)}</strong></div>
            <label className="discount-line"><span>Desconto <small>{currentRole === "seller" ? "Somente perfis autorizados" : "Perfil autorizado"}</small></span><input aria-label="Desconto" value={discount} placeholder="R$ 0,00" disabled={currentRole === "seller"} onChange={(event) => onDiscount(maskCurrency(event.target.value))} /></label>
            <div className="order-total-line"><span>Total do pedido</span><strong>{formatCurrency(total)}</strong></div>
            {!preferences.allowAnonymousCustomer && !selectedClientId && cart.length > 0 && <p className="customer-required-note">Selecione um cliente para revisar este pedido.</p>}
            <div className="order-buttons"><button type="button" disabled={cart.length === 0} onClick={onClear}>Limpar pedido</button><button type="button" disabled={cart.length === 0 || (!preferences.allowAnonymousCustomer && !selectedClientId)} onClick={onReview}>Revisar pedido<Icon name="chevron-right" /></button></div>
          </footer>
        </aside>
      </div>
    </div>
  );
}

function StepIndicator({ current }: { current: 1 | 2 | 3 }) {
  const steps = ["Seleção de produtos", "Revisão do pedido", "Confirmação"];
  return (
    <ol className="order-steps" aria-label="Etapas da venda">
      {steps.map((step, index) => {
        const number = index + 1;
        return (
          <li key={step} className={number === current ? "active" : number < current ? "complete" : ""}>
            <span>{number < current ? <Icon name="check" /> : number}</span>
            <strong>{step}</strong>
          </li>
        );
      })}
    </ol>
  );
}

function ReviewPage({
  order,
  products,
  clients,
  paymentMethod,
  cashReceived,
  enabledPaymentMethods,
  error,
  confirming,
  onPaymentMethod,
  onCashReceived,
  onEdit,
  onConfirm,
}: {
  order: PreparedOrder;
  products: ProductRecord[];
  clients: ClientRecord[];
  paymentMethod: PaymentMethod | null;
  cashReceived: string;
  enabledPaymentMethods: Record<PaymentMethod, boolean>;
  error: string;
  confirming: boolean;
  onPaymentMethod: (method: PaymentMethod) => void;
  onCashReceived: (value: string) => void;
  onEdit: () => void;
  onConfirm: () => void;
}) {
  const client = clients.find((record) => record.id === order.clientId);
  const items = order.items.flatMap((item) => {
    const product = products.find((record) => record.id === item.productId);
    return product ? [{ ...item, product }] : [];
  });
  const itemCount = items.reduce((total, item) => total + item.quantity, 0);
  const subtotal = items.reduce((total, item) => total + itemUnitPrice(item, item.product) * item.quantity, 0);
  const discountValue = Math.min(parseCurrency(order.discount), subtotal);
  const total = Math.max(0, subtotal - discountValue);
  const received = parseCurrency(cashReceived || "R$ 0,00");
  const change = Math.max(0, received - total);
  const allPaymentOptions: Array<{ id: PaymentMethod; label: string; icon: IconName }> = [
    { id: "cash", label: "Dinheiro", icon: "cash" },
    { id: "credit", label: "Cartão de crédito", icon: "credit-card" },
    { id: "debit", label: "Cartão de débito", icon: "credit-card" },
    { id: "pix", label: "PIX", icon: "pix" },
  ];
  const paymentOptions = allPaymentOptions.filter((option) => enabledPaymentMethods[option.id]);

  return (
    <div className="dashboard-content review-content">
      <StepIndicator current={2} />
      <div className="review-workspace">
        <div className="review-main-column">
          <section className="dashboard-card order-information-card">
            <div className="review-card-heading"><div><span>Informações do pedido</span><h2>{order.id}</h2></div><StatusBadge value="Aguardando confirmação" /></div>
            <div className="order-information-grid">
              <div><small>Data e hora</small><strong>{order.createdAt.toLocaleDateString("pt-BR")} · {order.createdAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</strong></div>
              <div><small>Vendedor responsável</small><strong>{order.seller}</strong></div>
              <div><small>Cliente</small><strong>{client?.name || "Cliente não identificado"}</strong></div>
            </div>
          </section>

          <section className="dashboard-card review-products-card">
            <div className="review-card-heading">
              <div><span>Itens selecionados</span><h2>Produtos do pedido</h2></div>
              <button type="button" onClick={onEdit}><Icon name="edit" />Editar produtos</button>
            </div>
            <div className="review-table-scroll">
              <table className="review-products-table">
                <thead><tr><th>Produto</th><th>Código</th><th>Quantidade</th><th>Preço unitário</th><th>Subtotal</th></tr></thead>
                <tbody>
                  {items.map((item) => {
                    const { product, quantity } = item;
                    const unitPrice = itemUnitPrice(item, product);
                    return (
                    <tr key={product.id}>
                      <td><PersonCell initials={product.initials} name={item.productName || product.name} image={product.image} /></td>
                      <td>{item.productCode || product.code}</td><td><strong>{quantity} un.</strong></td><td>{formatCurrency(unitPrice)}</td><td><strong>{formatCurrency(unitPrice * quantity)}</strong></td>
                    </tr>
                  )})}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside className="review-side-column">
          <section className="financial-review-card">
            <span className="review-card-kicker">Resumo financeiro</span>
            <div><span>Quantidade de itens</span><strong>{itemCount}</strong></div>
            <div><span>Subtotal</span><strong>{formatCurrency(subtotal)}</strong></div>
            <div><span>Desconto aplicado</span><strong>- {formatCurrency(discountValue)}</strong></div>
            <div className="financial-total"><span>Total a pagar</span><strong>{formatCurrency(total)}</strong></div>
          </section>

          <section className="dashboard-card payment-card">
            <div className="payment-heading"><span>Pagamento</span><h2>Forma de pagamento</h2><p>Selecione como o cliente realizará o pagamento.</p></div>
            <div className="payment-options">
              {paymentOptions.map((option) => (
                <button key={option.id} type="button" className={paymentMethod === option.id ? "active" : ""} aria-pressed={paymentMethod === option.id} onClick={() => onPaymentMethod(option.id)}>
                  <span><Icon name={option.icon} /></span>{option.label}{paymentMethod === option.id && <i><Icon name="check" /></i>}
                </button>
              ))}
            </div>
            {paymentMethod === "cash" && (
              <div className="cash-payment">
                <div><span>Valor total</span><strong>{formatCurrency(total)}</strong></div>
                <label htmlFor="cash-received">Valor recebido<input id="cash-received" value={cashReceived} placeholder="R$ 0,00" onChange={(event) => onCashReceived(maskCurrency(event.target.value))} /></label>
                <div className="change-value"><span>Troco</span><strong>{formatCurrency(change)}</strong></div>
                {cashReceived && received < total && <small>O valor recebido é inferior ao total do pedido.</small>}
              </div>
            )}
            {paymentMethod && paymentMethod !== "cash" && (
              <div className="selected-payment-summary"><span><Icon name={paymentMethod === "pix" ? "pix" : "credit-card"} /></span><p><small>Pagamento selecionado</small><strong>{paymentLabels[paymentMethod]}</strong></p><b>{formatCurrency(total)}</b></div>
            )}
          </section>
        </aside>
      </div>
      {error && <div className="review-error" role="alert">{error}</div>}
      <footer className="review-actions">
        <button type="button" onClick={onEdit}><Icon name="chevron-left" />Voltar para vendas</button>
        <button type="button" disabled={confirming} onClick={onConfirm}>{confirming ? "Confirmando..." : "Confirmar venda"}<Icon name="check" /></button>
      </footer>
    </div>
  );
}

function SaleSuccessPage({
  sale,
  products,
  details,
  onNewSale,
  onViewOrder,
  onBack,
}: {
  sale: SaleRecord;
  products: ProductRecord[];
  details: boolean;
  onNewSale: () => void;
  onViewOrder: () => void;
  onBack: () => void;
}) {
  // Nome, código e preço vêm da venda confirmada; o catálogo só fornece a foto, se houver.
  const items = sale.items.map((item) => {
    const name = item.productName || "Produto";
    const product = products.find((record) => record.id === item.productId);
    return { ...item, name, initials: getInitials(name), image: product?.image, unitPrice: item.unitPrice ?? 0 };
  });

  if (details) {
    return (
      <div className="dashboard-content completed-details-content">
        <StepIndicator current={3} />
        <div className="completed-details-heading"><div><span>Venda registrada</span><h2>{sale.id}</h2><p>Detalhes completos do pedido confirmado.</p></div><StatusBadge value={sale.status} /></div>
        <div className="completed-details-grid">
          <section className="dashboard-card review-products-card">
            <div className="review-card-heading"><div><span>Produtos vendidos</span><h2>Itens do pedido</h2></div></div>
            <div className="review-table-scroll">
              <table className="review-products-table">
                <thead><tr><th>Produto</th><th>Código</th><th>Qtd.</th><th>Preço unitário</th><th>Subtotal</th></tr></thead>
                <tbody>{items.map((item) => <tr key={item.productId}><td><PersonCell initials={item.initials} name={item.name} image={item.image} /></td><td>{item.productCode}</td><td>{item.quantity} un.</td><td>{formatCurrency(item.unitPrice)}</td><td><strong>{formatCurrency(item.unitPrice * item.quantity)}</strong></td></tr>)}</tbody>
              </table>
            </div>
          </section>
          <aside className="completed-sale-summary">
            <span>Resumo da venda</span>
            <div><small>Vendedor</small><strong>{sale.seller}</strong></div>
            <div><small>Cliente</small><strong>{sale.clientName}</strong></div>
            <div><small>Data e hora</small><strong>{sale.confirmedAt.toLocaleDateString("pt-BR")} · {sale.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</strong></div>
            <div><small>Forma de pagamento</small><strong>{paymentLabels[sale.paymentMethod]}</strong></div>
            <div className="completed-total"><small>Total pago</small><strong>{formatCurrency(sale.total)}</strong></div>
          </aside>
        </div>
        <div className="completed-details-actions"><button type="button" onClick={onBack}><Icon name="chevron-left" />Voltar</button><button type="button" onClick={onNewSale}><Icon name="plus" />Nova venda</button></div>
      </div>
    );
  }

  return (
    <div className="dashboard-content sale-success-content">
      <StepIndicator current={3} />
      <section className="sale-success-card">
        <span className="success-check"><Icon name="check" /></span>
        <span className="success-kicker">Venda confirmada</span>
        <h2>Venda realizada com sucesso!</h2>
        <p>O pedido foi registrado e o estoque dos produtos foi atualizado.</p>
        <div className="success-order-number"><small>Número do pedido</small><strong>{sale.id}</strong></div>
        <div className="success-details-grid">
          <div><small>Vendedor</small><strong>{sale.seller}</strong></div>
          <div><small>Valor total</small><strong>{formatCurrency(sale.total)}</strong></div>
          <div><small>Forma de pagamento</small><strong>{paymentLabels[sale.paymentMethod]}</strong></div>
          <div><small>Data e hora</small><strong>{sale.confirmedAt.toLocaleDateString("pt-BR")} · {sale.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</strong></div>
        </div>
        <div className="success-actions"><button type="button" onClick={onNewSale}><Icon name="plus" />Nova venda</button><button type="button" onClick={onViewOrder}><Icon name="eye" />Ver pedido</button></div>
      </section>
    </div>
  );
}

/** Funcionário exibido enquanto a sessão ainda não carregou (nunca aparece na tela). */
const emptyUser: EmployeeRecord = { id: 0, initials: "", name: "", cpf: "", email: "", phone: "", username: "", role: "Vendedor", lastAccess: "", status: "Ativo" };

const homePage = (user: Usuario) => user.perfil === "VENDEDOR" ? "sales" as const : "dashboard" as const;

export default function App() {
  const [sessionUser, setSessionUser] = useState<Usuario | null>(null);
  const [checkingSession, setCheckingSession] = useState(() => Boolean(tokenStorage.get()));
  const [loginNotice, setLoginNotice] = useState("");
  const [activePage, setActivePage] = useState<"dashboard" | "sales" | "orders" | "reports" | "settings" | "registrations" | "review" | "success" | "details">("dashboard");
  const [activeTab, setActiveTab] = useState<TabId>("clients");
  // Catálogo da tela de Vendas (produtos, clientes e categorias ativos), carregado da API.
  const [clientRecords, setClientRecords] = useState<ClientRecord[]>([]);
  const [productRecords, setProductRecords] = useState<ProductRecord[]>([]);
  const [saleCategories, setSaleCategories] = useState<string[]>(["Todos"]);
  const [catalogKey, setCatalogKey] = useState(0);
  const [toast, setToast] = useState("");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [productSearch, setProductSearch] = useState("");
  const [saleCategory, setSaleCategory] = useState("Todos");
  const [clientSearch, setClientSearch] = useState("");
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [discount, setDiscount] = useState("R$ 0,00");
  const [clearOrderConfirmation, setClearOrderConfirmation] = useState(false);
  const [preparedOrder, setPreparedOrder] = useState<PreparedOrder | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | null>(null);
  const [cashReceived, setCashReceived] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [lastSale, setLastSale] = useState<SaleRecord | null>(null);
  const [reportPeriod, setReportPeriod] = useState<ReportPeriod>("7days");
  const [reportStartDate, setReportStartDate] = useState("");
  const [reportEndDate, setReportEndDate] = useState("");
  const [reportSeller, setReportSeller] = useState("");
  // Dados da lanchonete vêm sempre da API (nunca do armazenamento local do protótipo).
  const [companySettings, setCompanySettings] = useState<CompanySettings>(defaultCompany);
  const [salesPreferences, setSalesPreferences] = useState<SalesPreferences>(defaultPreferences);
  const confirmationLock = useRef(false);
  const currentUser = sessionUser ? toEmployeeRecord(sessionUser) : emptyUser;
  const currentRole = roleKey(currentUser.role);

  const startSession = (user: Usuario) => {
    setSessionUser(user);
    setLoginNotice("");
    setActiveTab("clients");
    setActivePage(homePage(user));
  };

  const endSession = () => {
    tokenStorage.clear();
    setSessionUser(null);
    setCart([]);
    setPreparedOrder(null);
    setPaymentMethod(null);
    setCashReceived("");
    setDiscount("R$ 0,00");
    setSelectedClientId(null);
    confirmationLock.current = false;
  };

  // 401 em qualquer chamada: o token expirou ou foi invalidado, então volta para o login.
  useEffect(() => {
    onUnauthorized(() => {
      setSessionUser(null);
      setLoginNotice("Sua sessão expirou. Entre novamente para continuar.");
    });
    return () => onUnauthorized(null);
  }, []);

  // Sessão salva ("Lembrar de mim" ou aba ainda aberta): confirma o token buscando a conta.
  useEffect(() => {
    if (!tokenStorage.get()) return;
    contaApi.consultar()
      .then(startSession)
      // Um 401 já apaga o token (onUnauthorized); com a API fora do ar, o token é mantido.
      .catch(() => undefined)
      .finally(() => setCheckingSession(false));
  }, []);

  // Tela de login: nome e logo da lanchonete vêm da rota pública, sem precisar de token.
  useEffect(() => {
    if (sessionUser) return;
    let cancelled = false;
    publicoApi.identidade()
      .then((identidade) => {
        if (cancelled) return;
        setCompanySettings((current) => ({
          ...current,
          tradeName: identidade.nomeFantasia,
          logo: imageUrl(identidade.logoUrl),
        }));
      })
      // Com a API fora do ar, a tela de login mostra o nome e o logo padrão.
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [sessionUser]);

  // Dados da lanchonete e preferências de vendas vêm da API a cada login.
  const sessionUserId = sessionUser?.id;
  useEffect(() => {
    if (!sessionUserId) return;
    let cancelled = false;
    configuracoesApi.consultar()
      .then((configuracao) => {
        if (cancelled) return;
        setCompanySettings(toCompanySettings(configuracao.empresa));
        setSalesPreferences(toSalesPreferences(configuracao.preferenciasVenda));
      })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [sessionUserId]);

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(""), 3200);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  // Catálogo de Vendas: recarregado ao entrar na tela e depois de cada venda (estoque atualizado).
  useEffect(() => {
    if (!sessionUserId) return;
    let cancelled = false;
    Promise.all([
      produtosApi.listar({ ativo: true, size: 500, sort: "nome" }),
      clientesApi.listar({ ativo: true, size: 500, sort: "nome" }),
      categoriasApi.listar(),
    ])
      .then(([produtos, clientes, categorias]) => {
        if (cancelled) return;
        setProductRecords(produtos.conteudo.map(toProductRecord));
        setClientRecords(clientes.conteudo.map(toClientRecord));
        setSaleCategories(["Todos", ...categorias.map((categoria) => categoria.nome)]);
      })
      .catch((error) => { if (!cancelled) setToast(errorMessage(error, "Não foi possível carregar produtos e clientes.")); });
    return () => { cancelled = true; };
  }, [sessionUserId, catalogKey]);

  const addToCart = (productId: number) => {
    const product = productRecords.find((record) => record.id === productId);
    if (!product || product.status !== "Ativo" || (salesPreferences.stockControl && salesPreferences.blockWithoutStock && product.stock <= 0)) return;
    setCart((items) => {
      const existing = items.find((item) => item.productId === productId);
      if (salesPreferences.stockControl && salesPreferences.blockWithoutStock && existing?.quantity === product.stock) return items;
      return existing
        ? items.map((item) => item.productId === productId ? { ...item, quantity: item.quantity + 1 } : item)
        : [...items, { productId, quantity: 1 }];
    });
  };

  const changeCartQuantity = (productId: number, delta: number) => {
    const product = productRecords.find((record) => record.id === productId);
    if (!product) return;
    setCart((items) => items.flatMap((item) => {
      if (item.productId !== productId) return [item];
      const quantity = item.quantity + delta;
      if (quantity <= 0) return [];
      return [{ ...item, quantity: salesPreferences.stockControl && salesPreferences.blockWithoutStock ? Math.min(quantity, product.stock) : quantity }];
    }));
  };

  const clearOrder = () => {
    setCart([]);
    setPreparedOrder(null);
    setPaymentMethod(null);
    setCashReceived("");
    setSelectedClientId(null);
    setClientSearch("");
    setDiscount("R$ 0,00");
    setReviewError("");
    confirmationLock.current = false;
    setClearOrderConfirmation(false);
    setToast("Pedido limpo com sucesso.");
  };

  const prepareOrder = () => {
    if (cart.length === 0) return;
    if (!salesPreferences.allowAnonymousCustomer && !selectedClientId) {
      setToast("Selecione um cliente para continuar.");
      return;
    }
    setPreparedOrder({
      id: "Novo pedido",
      createdAt: preparedOrder?.createdAt || new Date(),
      seller: currentUser.name,
      clientId: selectedClientId,
      items: cart.map((item) => {
        const product = productRecords.find((record) => record.id === item.productId)!;
        return { ...item, unitPrice: parseCurrency(product.price), productName: product.name, productCode: product.code, productCategory: product.category };
      }),
      discount,
    });
    setReviewError("");
    setActivePage("review");
  };

  const confirmSale = async () => {
    if (confirmationLock.current || confirming || !preparedOrder) return;
    if (preparedOrder.items.length === 0) {
      setReviewError("O pedido não possui produtos para confirmar.");
      return;
    }
    if (!paymentMethod) {
      setReviewError("Selecione uma forma de pagamento para continuar.");
      return;
    }
    if (!salesPreferences.paymentMethods[paymentMethod]) {
      setReviewError("Esta forma de pagamento foi desabilitada nas configurações.");
      return;
    }
    const unavailableItem = preparedOrder.items.find((item) => {
      const product = productRecords.find((record) => record.id === item.productId);
      return !product || product.status !== "Ativo" || (salesPreferences.stockControl && salesPreferences.blockWithoutStock && product.stock < item.quantity);
    });
    if (unavailableItem) {
      setReviewError("Um ou mais produtos não possuem estoque suficiente. Volte para vendas e revise as quantidades.");
      return;
    }
    const subtotal = preparedOrder.items.reduce((total, item) => {
      const product = productRecords.find((record) => record.id === item.productId)!;
      return total + itemUnitPrice(item, product) * item.quantity;
    }, 0);
    const discountValue = Math.min(parseCurrency(preparedOrder.discount), subtotal);
    const total = Math.max(0, subtotal - discountValue);
    const received = parseCurrency(cashReceived || "R$ 0,00");
    if (paymentMethod === "cash" && received < total) {
      setReviewError("O valor recebido deve ser igual ou superior ao total do pedido.");
      return;
    }

    confirmationLock.current = true;
    setConfirming(true);
    try {
      const venda = await vendasApi.confirmar({
        clienteId: preparedOrder.clientId,
        itens: preparedOrder.items.map((item) => ({ produtoId: item.productId, quantidade: item.quantity })),
        desconto: discountValue,
        formaPagamento: paymentCodes[paymentMethod],
        valorRecebido: paymentMethod === "cash" ? received : null,
      });
      setLastSale(toSaleRecord(venda));
      setPreparedOrder(null);
      setCart([]);
      setReviewError("");
      setActivePage("success");
      setCatalogKey((key) => key + 1);
    } catch (error) {
      confirmationLock.current = false;
      setReviewError(errorMessage(error, "Não foi possível confirmar a venda. Tente novamente."));
    } finally {
      setConfirming(false);
    }
  };

  const startNewSale = () => {
    setCart([]);
    setPreparedOrder(null);
    setPaymentMethod(null);
    setCashReceived("");
    setDiscount("R$ 0,00");
    setSelectedClientId(null);
    setClientSearch("");
    setReviewError("");
    confirmationLock.current = false;
    setActivePage("sales");
  };

  const exportReport = () => {
    if (currentRole === "seller") {
      setToast("Você não possui permissão para acessar esta funcionalidade.");
      return;
    }
    const { inicio, fim } = reportRange(reportPeriod, reportStartDate, reportEndDate);
    exportReportCsv(inicio, fim, reportSeller ? Number(reportSeller) : undefined)
      .then((total) => setToast(`${total} registros exportados em CSV.`))
      .catch((error) => setToast(errorMessage(error, "Não foi possível exportar o relatório.")));
  };

  const pageCopy = activePage === "registrations"
    ? { title: "Cadastros", description: "Gerencie clientes, funcionários e produtos da sua lanchonete." }
    : activePage === "dashboard"
      ? { title: `Olá, ${currentUser.name.split(" ")[0]}`, description: "Acompanhe os principais resultados da sua lanchonete." }
    : activePage === "sales"
      ? { title: "Nova venda", description: "Selecione os produtos e organize o pedido do cliente." }
      : activePage === "orders"
        ? { title: "Gestão de Pedidos", description: "Consulte e acompanhe os pedidos da sua lanchonete." }
      : activePage === "reports"
        ? { title: "Relatórios Gerenciais", description: "Acompanhe os resultados e o desempenho da sua lanchonete." }
      : activePage === "settings"
        ? { title: "Configurações", description: "Administre dados, preferências e informações da sua conta." }
      : activePage === "review"
        ? { title: "Resumo do pedido", description: "Confira os itens e os valores antes de confirmar a venda." }
        : activePage === "details"
          ? { title: "Detalhes do pedido", description: "Consulte os dados da venda registrada." }
          : { title: "Venda concluída", description: "A venda foi registrada com sucesso." };

  if (checkingSession) return null;

  if (!sessionUser) {
    return <LoginPage key={loginNotice} businessName={companySettings.tradeName || "Ponto & Sabor"} businessLogo={companySettings.logo} notice={loginNotice} onLogin={startSession} />;
  }

  return (
    <div className="dashboard-layout">
      <Sidebar
        activePage={activePage === "dashboard" ? "dashboard" : activePage === "registrations" ? "registrations" : activePage === "orders" ? "orders" : activePage === "reports" ? "reports" : activePage === "settings" ? "settings" : "sales"}
        currentRole={currentRole}
        currentUser={currentUser}
        businessName={companySettings.tradeName || "Ponto & Sabor"}
        businessLogo={companySettings.logo}
        onNavigate={(page) => {
          if ((page === "reports" || page === "dashboard") && currentRole === "seller") {
            setToast("Você não possui permissão para acessar esta funcionalidade.");
            return;
          }
          if (page === "sales" && ((activePage === "success" || activePage === "details") || confirmationLock.current)) startNewSale();
          else setActivePage(page);
        }}
      />
      <main className="dashboard-main">
        <header className="topbar">
          <div className="mobile-dashboard-brand">
            <button className="icon-button" type="button" aria-label="Abrir menu"><Icon name="menu" /></button>
            <BrandMark src={companySettings.logo} />
          </div>
          <div className="welcome-copy">
            <h1>{pageCopy.title}</h1>
            <p>{pageCopy.description}</p>
          </div>
          <div className="topbar-actions">
            {activePage === "reports" && currentRole !== "seller" && <button className="reports-export-button" type="button" onClick={exportReport}><Icon name="download" />Exportar relatório</button>}
            <button className="icon-button notification-button" type="button" aria-label="Notificações">
              <Icon name="bell" /><span aria-hidden="true" />
            </button>
            <button className="user-menu" type="button" aria-label="Abrir menu do usuário">
              <UserIdentity user={currentUser} /><Icon name="chevron" />
            </button>
          </div>
        </header>

        {activePage === "dashboard" ? (
          <DashboardPage currentRole={currentRole} onNavigate={(page) => { if (page === "registrations") setActiveTab("clients"); if (page === "sales") startNewSale(); else setActivePage(page); }} />
        ) : activePage === "sales" ? (
          <SalesPage
            products={productRecords}
            clients={clientRecords}
            categories={saleCategories}
            cart={cart}
            productSearch={productSearch}
            category={saleCategory}
            clientSearch={clientSearch}
            selectedClientId={selectedClientId}
            discount={discount}
            sellerName={currentUser.name}
            currentRole={currentRole}
            preferences={salesPreferences}
            onProductSearch={setProductSearch}
            onCategory={setSaleCategory}
            onClientSearch={setClientSearch}
            onSelectClient={setSelectedClientId}
            onDiscount={setDiscount}
            onAdd={addToCart}
            onQuantity={changeCartQuantity}
            onRemove={(productId) => setCart((items) => items.filter((item) => item.productId !== productId))}
            onClear={() => setClearOrderConfirmation(true)}
            onReview={prepareOrder}
          />
        ) : activePage === "orders" ? (
          <OrdersPage currentRole={currentRole} onToast={setToast} />
        ) : activePage === "reports" ? (
          <ReportsPage
            period={reportPeriod}
            customStart={reportStartDate}
            customEnd={reportEndDate}
            sellerId={reportSeller}
            onPeriod={setReportPeriod}
            onCustomStart={setReportStartDate}
            onCustomEnd={setReportEndDate}
            onSeller={setReportSeller}
          />
        ) : activePage === "settings" ? (
          <SettingsPage
            key={currentUser.id}
            currentUser={currentUser}
            currentRole={currentRole}
            company={companySettings}
            preferences={salesPreferences}
            onCompanySaved={setCompanySettings}
            onPreferencesSaved={setSalesPreferences}
            onAccountSaved={setSessionUser}
            onDenied={() => setToast("Você não possui permissão para acessar esta funcionalidade.")}
            onLogout={endSession}
            onSuccess={setToast}
          />
        ) : activePage === "review" && preparedOrder ? (
          <ReviewPage
            order={preparedOrder}
            products={productRecords}
            clients={clientRecords}
            paymentMethod={paymentMethod}
            cashReceived={cashReceived}
            enabledPaymentMethods={salesPreferences.paymentMethods}
            error={reviewError}
            confirming={confirming}
            onPaymentMethod={(method) => { setPaymentMethod(method); setReviewError(""); }}
            onCashReceived={(value) => { setCashReceived(value); setReviewError(""); }}
            onEdit={() => setActivePage("sales")}
            onConfirm={confirmSale}
          />
        ) : (activePage === "success" || activePage === "details") && lastSale ? (
          <SaleSuccessPage
            sale={lastSale}
            products={productRecords}
            details={activePage === "details"}
            onNewSale={startNewSale}
            onViewOrder={() => setActivePage("details")}
            onBack={() => setActivePage("success")}
          />
        ) : (
          <RegistrationsPage activeTab={activeTab} currentRole={currentRole} onTabChange={setActiveTab} onToast={setToast} />
        )}
      </main>
      {clearOrderConfirmation && (
        <div className="confirm-backdrop" role="presentation">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="clear-order-title">
            <span className="confirm-icon"><Icon name="trash" /></span>
            <h2 id="clear-order-title">Limpar pedido atual?</h2>
            <p>Todos os produtos adicionados, o cliente e o desconto serão removidos do pedido.</p>
            <div><button type="button" onClick={() => setClearOrderConfirmation(false)}>Cancelar</button><button className="danger-button" type="button" onClick={clearOrder}>Limpar pedido</button></div>
          </section>
        </div>
      )}
      {toast && <div className="success-toast" role="status"><span>✓</span>{toast}</div>}
    </div>
  );
}
