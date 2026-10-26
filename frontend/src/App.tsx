import { useEffect, useRef, useState } from "react";
import { configuracoesApi, contaApi, onUnauthorized, tokenStorage, type Usuario } from "./api";
import { BrandMark, Icon, type IconName } from "./components/Icon";
import { PersonCell, StatusBadge } from "./components/ui";
import { RegistrationsPage } from "./pages/cadastros/RegistrationsPage";
import { toEmployeeRecord } from "./pages/cadastros/mappers";
import { LoginPage } from "./pages/LoginPage";
import { defaultCompany, defaultPreferences, toCompanySettings, toSalesPreferences, type CompanySettings, type SalesPreferences } from "./pages/settings/mappers";
import { SettingsPage } from "./pages/settings/SettingsPage";
import { paymentLabels, roleKey, type ClientRecord, type EmployeeRecord, type PaymentMethod, type ProductRecord, type TabId, type UserRole } from "./types";
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

const initialClients: ClientRecord[] = [
  { id: 1, initials: "JS", name: "João Silva", cpf: "123.456.789-00", phone: "(81) 99999-1234", email: "joao@email.com", date: "12/05/2024", status: "Ativo", cep: "50030-230", street: "Rua do Sol", number: "120", complement: "Apto. 302", district: "Santo Antônio", city: "Recife", state: "PE", notes: "Prefere contato pelo WhatsApp." },
  { id: 2, initials: "AS", name: "Ana Souza", cpf: "987.654.321-00", phone: "(81) 98888-5678", email: "ana@email.com", date: "08/05/2024", status: "Ativo", cep: "52050-020", street: "Rua das Graças", number: "45", complement: "", district: "Graças", city: "Recife", state: "PE", notes: "" },
  { id: 3, initials: "PL", name: "Pedro Lima", cpf: "456.789.123-00", phone: "(81) 97777-4321", email: "pedro@email.com", date: "02/05/2024", status: "Inativo", cep: "51020-030", street: "Av. Boa Viagem", number: "980", complement: "", district: "Boa Viagem", city: "Recife", state: "PE", notes: "" },
  { id: 4, initials: "MS", name: "Maria Santos", cpf: "321.654.987-00", phone: "(81) 96666-8910", email: "maria@email.com", date: "28/04/2024", status: "Ativo", cep: "", street: "", number: "", complement: "", district: "", city: "Recife", state: "PE", notes: "" },
  { id: 5, initials: "RC", name: "Rafael Costa", cpf: "741.852.963-00", phone: "(81) 95555-1122", email: "rafael@email.com", date: "22/04/2024", status: "Ativo", cep: "", street: "", number: "", complement: "", district: "", city: "Olinda", state: "PE", notes: "" },
];

const initialProducts: ProductRecord[] = [
  { id: 1, initials: "XB", name: "X-Burger", category: "Hambúrgueres", code: "XBG001", description: "Pão, carne, queijo e molho da casa.", price: "R$ 18,00", stock: 24, minimumStock: 6, image: "", status: "Ativo" },
  { id: 2, initials: "XS", name: "X-Salada", category: "Hambúrgueres", code: "XSL001", description: "Pão, carne, queijo, salada e molho da casa.", price: "R$ 21,00", stock: 15, minimumStock: 6, image: "", status: "Ativo" },
  { id: 3, initials: "SN", name: "Suco Natural", category: "Bebidas", code: "SUC001", description: "Suco natural de fruta, 400 ml.", price: "R$ 9,00", stock: 5, minimumStock: 6, image: "", status: "Ativo" },
  { id: 4, initials: "BF", name: "Batata Frita", category: "Acompanhamentos", code: "BTF001", description: "Porção individual de batatas fritas.", price: "R$ 12,00", stock: 18, minimumStock: 6, image: "", status: "Ativo" },
  { id: 5, initials: "RF", name: "Refrigerante Lata", category: "Bebidas", code: "REF001", description: "Refrigerante em lata, 350 ml.", price: "R$ 6,00", stock: 4, minimumStock: 6, image: "", status: "Ativo" },
];

type CartItem = {
  productId: number;
  quantity: number;
  unitPrice?: number;
  productName?: string;
  productCode?: string;
  productCategory?: string;
};

const categories = ["Todos", "Hambúrgueres", "Bebidas", "Acompanhamentos", "Combos", "Sobremesas"];
const itemUnitPrice = (item: CartItem, product: ProductRecord) => item.unitPrice ?? parseCurrency(product.price);

function SalesPage({
  products,
  clients,
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

type OrderStatus = "Em preparo" | "Concluído" | "Cancelado";

type PreparedOrder = {
  id: string;
  createdAt: Date;
  seller: string;
  clientId: number | null;
  items: CartItem[];
  discount: string;
};

type SaleRecord = PreparedOrder & {
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountValue: number;
  total: number;
  cashReceived: number;
  change: number;
  confirmedAt: Date;
  status: OrderStatus;
  source: "real" | "demo";
  cancellationReason?: string;
};

const demoDate = (daysAgo: number, hour: number, minute: number) => {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  date.setHours(hour, minute, 0, 0);
  return date;
};

const initialSales: SaleRecord[] = [
  { id: "#PV-240518", createdAt: demoDate(0, 12, 42), confirmedAt: demoDate(0, 12, 46), seller: "Fernanda Souza", clientId: 2, items: [{ productId: 2, quantity: 2 }, { productId: 5, quantity: 2 }], discount: "R$ 2,00", paymentMethod: "pix", subtotal: 54, discountValue: 2, total: 52, cashReceived: 52, change: 0, status: "Em preparo", source: "demo" },
  { id: "#PV-240517", createdAt: demoDate(0, 11, 15), confirmedAt: demoDate(0, 11, 19), seller: "Matheus Almeida", clientId: null, items: [{ productId: 1, quantity: 1 }, { productId: 3, quantity: 1 }], discount: "R$ 0,00", paymentMethod: "cash", subtotal: 27, discountValue: 0, total: 27, cashReceived: 30, change: 3, status: "Concluído", source: "demo" },
  { id: "#PV-240516", createdAt: demoDate(1, 20, 8), confirmedAt: demoDate(1, 20, 11), seller: "Carlos Lima", clientId: 4, items: [{ productId: 1, quantity: 2 }, { productId: 4, quantity: 1 }], discount: "R$ 3,00", paymentMethod: "credit", subtotal: 48, discountValue: 3, total: 45, cashReceived: 45, change: 0, status: "Concluído", source: "demo" },
  { id: "#PV-240515", createdAt: demoDate(2, 18, 34), confirmedAt: demoDate(2, 18, 38), seller: "Fernanda Souza", clientId: 1, items: [{ productId: 2, quantity: 1 }, { productId: 4, quantity: 1 }, { productId: 5, quantity: 2 }], discount: "R$ 0,00", paymentMethod: "debit", subtotal: 45, discountValue: 0, total: 45, cashReceived: 45, change: 0, status: "Cancelado", source: "demo", cancellationReason: "Cliente solicitou o cancelamento antes do preparo." },
  { id: "#PV-240514", createdAt: demoDate(3, 13, 22), confirmedAt: demoDate(3, 13, 25), seller: "Matheus Almeida", clientId: 5, items: [{ productId: 1, quantity: 1 }, { productId: 4, quantity: 1 }], discount: "R$ 0,00", paymentMethod: "pix", subtotal: 30, discountValue: 0, total: 30, cashReceived: 30, change: 0, status: "Concluído", source: "demo" },
  { id: "#PV-240513", createdAt: demoDate(4, 19, 5), confirmedAt: demoDate(4, 19, 9), seller: "Carlos Lima", clientId: 3, items: [{ productId: 2, quantity: 2 }, { productId: 3, quantity: 2 }], discount: "R$ 5,00", paymentMethod: "cash", subtotal: 60, discountValue: 5, total: 55, cashReceived: 60, change: 5, status: "Em preparo", source: "demo" },
];

function DashboardPage({
  orders,
  products,
  clients,
  currentRole,
  onNavigate,
}: {
  orders: SaleRecord[];
  products: ProductRecord[];
  clients: ClientRecord[];
  currentRole: UserRole;
  onNavigate: (page: "sales" | "orders" | "reports" | "registrations") => void;
}) {
  const today = new Date();
  const isToday = (date: Date) => date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth() && date.getDate() === today.getDate();
  const validToday = orders.filter((order) => order.status !== "Cancelado" && isToday(order.confirmedAt));
  const todayRevenue = validToday.reduce((total, order) => total + order.total, 0);
  const todayTicket = validToday.length ? todayRevenue / validToday.length : 0;
  const activeProducts = products.filter((product) => product.status === "Ativo").length;
  const recentOrders = [...orders].sort((a, b) => b.confirmedAt.getTime() - a.confirmedAt.getTime()).slice(0, 5);
  const chartDays = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    date.setHours(0, 0, 0, 0);
    const value = orders.filter((order) => order.status !== "Cancelado" && isTodayFor(order.confirmedAt, date)).reduce((sum, order) => sum + order.total, 0);
    return { date, value };
  });
  const chartMax = Math.max(...chartDays.map((day) => day.value), 1);
  const points = chartDays.map((day, index) => `${index * 100},${200 - day.value / chartMax * 170}`).join(" ");
  const areaPath = `M0 200 L${chartDays.map((day, index) => `${index * 100} ${200 - day.value / chartMax * 170}`).join(" L")} L600 200 Z`;
  const weekRevenue = chartDays.reduce((total, day) => total + day.value, 0);
  const weekOrders = orders.filter((order) => order.status !== "Cancelado" && order.confirmedAt >= chartDays[0].date).length;
  const weekTicket = weekOrders ? weekRevenue / weekOrders : 0;

  return (
    <div className="dashboard-content">
      <section className="metrics-grid">
        <article className="metric-card"><span className="metric-icon terracotta"><Icon name="sales" /></span><div className="metric-copy"><span>Vendas de hoje</span><strong>{formatCurrency(todayRevenue)}</strong><small>{validToday.length} vendas confirmadas hoje</small></div></article>
        <article className="metric-card"><span className="metric-icon green"><Icon name="orders" /></span><div className="metric-copy"><span>Pedidos de hoje</span><strong>{validToday.length}</strong><small>Cancelados não contabilizados</small></div></article>
        <article className="metric-card"><span className="metric-icon gold"><Icon name="cash" /></span><div className="metric-copy"><span>Ticket médio</span><strong>{formatCurrency(todayTicket)}</strong><small>Calculado com as vendas de hoje</small></div></article>
        <article className="metric-card"><span className="metric-icon slate"><Icon name="products" /></span><div className="metric-copy"><span>Produtos cadastrados</span><strong>{products.length}</strong><small>{activeProducts} produtos ativos</small></div></article>
      </section>

      <section className="dashboard-card sales-summary">
        <div className="section-heading"><div><h2>Evolução das vendas</h2><p>Faturamento confirmado nos últimos sete dias.</p></div><button className="period-control" type="button">Últimos 7 dias <Icon name="chevron" /></button></div>
        <div className="sales-card-body">
          <div className="chart">
            <div className="chart-y-labels">{[1, 0.75, 0.5, 0.25, 0].map((ratio) => <span key={ratio}>{formatCurrency(chartMax * ratio)}</span>)}</div>
            <div className="chart-plot">
              <div className="chart-grid">{Array.from({ length: 5 }, (_, index) => <span key={index} />)}</div>
              <svg className="chart-svg" viewBox="0 0 600 220" preserveAspectRatio="none"><defs><linearGradient id="dashboard-sales-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#c75a3b" stopOpacity=".18" /><stop offset="100%" stopColor="#c75a3b" stopOpacity="0" /></linearGradient></defs><path d={areaPath} fill="url(#dashboard-sales-fill)" /><polyline className="chart-line" points={points} />{chartDays.map((day, index) => <circle key={day.date.toISOString()} className="chart-point" cx={index * 100} cy={200 - day.value / chartMax * 170} r="4" />)}</svg>
              <div className="chart-x-labels">{chartDays.map((day) => <span key={day.date.toISOString()}>{day.date.toLocaleDateString("pt-BR", { weekday: "short" }).replace(".", "")}</span>)}</div>
            </div>
          </div>
          <aside className="sales-totals"><span className="totals-kicker">Resumo do período</span><div className="total-featured"><span>Faturamento</span><strong>{formatCurrency(weekRevenue)}</strong><small>Dados dos pedidos válidos</small></div><div className="total-row"><span>Pedidos</span><strong>{weekOrders}</strong></div><div className="total-row"><span>Ticket médio</span><strong>{formatCurrency(weekTicket)}</strong></div></aside>
        </div>
      </section>

      <div className="dashboard-bottom-grid">
        <section className="dashboard-card orders-card">
          <div className="section-heading"><div><h2>Pedidos recentes</h2><p>Últimos registros do histórico compartilhado.</p></div><button className="dashboard-text-action" type="button" onClick={() => onNavigate("orders")}>Ver todos</button></div>
          <div className="table-scroll">
            <table><thead><tr><th>Pedido</th><th>Cliente</th><th>Valor</th><th>Status</th></tr></thead><tbody>{recentOrders.length ? recentOrders.map((order) => { const client = clients.find((record) => record.id === order.clientId); return <tr key={order.id}><td><strong>{order.id}</strong></td><td>{client?.name || "Cliente não identificado"}</td><td><strong>{formatCurrency(order.total)}</strong></td><td><span className={`status-badge ${order.status === "Concluído" ? "success" : order.status === "Cancelado" ? "cancelled" : "preparing"}`}><i />{order.status}</span></td></tr>; }) : <tr><td colSpan={4}>Nenhum pedido registrado.</td></tr>}</tbody></table>
          </div>
        </section>
        <section className="dashboard-card quick-actions">
          <div className="section-heading"><div><h2>Ações rápidas</h2><p>Acesse as tarefas mais utilizadas.</p></div></div>
          <div className="action-list">
            <button className="quick-action primary" type="button" onClick={() => onNavigate("sales")}><span><Icon name="plus" /></span><div><strong>Nova venda</strong><small>Iniciar um novo pedido</small></div><b>›</b></button>
            <button className="quick-action" type="button" onClick={() => onNavigate("registrations")}><span><Icon name="users" /></span><div><strong>Novo cliente</strong><small>Abrir cadastros</small></div><b>›</b></button>
            {currentRole !== "seller" && <button className="quick-action" type="button" onClick={() => onNavigate("reports")}><span><Icon name="reports" /></span><div><strong>Ver relatórios</strong><small>Acompanhar resultados</small></div><b>›</b></button>}
          </div>
        </section>
      </div>
    </div>
  );
}

const isTodayFor = (date: Date, target: Date) =>
  date.getFullYear() === target.getFullYear() && date.getMonth() === target.getMonth() && date.getDate() === target.getDate();

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
  client,
  details,
  onNewSale,
  onViewOrder,
  onBack,
}: {
  sale: SaleRecord;
  products: ProductRecord[];
  client?: ClientRecord;
  details: boolean;
  onNewSale: () => void;
  onViewOrder: () => void;
  onBack: () => void;
}) {
  const items = sale.items.flatMap((item) => {
    const product = products.find((record) => record.id === item.productId);
    return product ? [{ ...item, product }] : [];
  });

  if (details) {
    return (
      <div className="dashboard-content completed-details-content">
        <StepIndicator current={3} />
        <div className="completed-details-heading"><div><span>Venda registrada</span><h2>{sale.id}</h2><p>Detalhes completos do pedido confirmado.</p></div><StatusBadge value="Concluído" /></div>
        <div className="completed-details-grid">
          <section className="dashboard-card review-products-card">
            <div className="review-card-heading"><div><span>Produtos vendidos</span><h2>Itens do pedido</h2></div></div>
            <div className="review-table-scroll">
              <table className="review-products-table">
                <thead><tr><th>Produto</th><th>Código</th><th>Qtd.</th><th>Preço unitário</th><th>Subtotal</th></tr></thead>
                <tbody>{items.map((item) => { const unitPrice = itemUnitPrice(item, item.product); return <tr key={item.product.id}><td><PersonCell initials={item.product.initials} name={item.productName || item.product.name} image={item.product.image} /></td><td>{item.productCode || item.product.code}</td><td>{item.quantity} un.</td><td>{formatCurrency(unitPrice)}</td><td><strong>{formatCurrency(unitPrice * item.quantity)}</strong></td></tr>; })}</tbody>
              </table>
            </div>
          </section>
          <aside className="completed-sale-summary">
            <span>Resumo da venda</span>
            <div><small>Vendedor</small><strong>{sale.seller}</strong></div>
            <div><small>Cliente</small><strong>{client?.name || "Cliente não identificado"}</strong></div>
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

function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`order-status-badge ${status === "Em preparo" ? "preparing" : status === "Concluído" ? "completed" : "cancelled"}`}><i />{status}</span>;
}

function OrderDetailsModal({
  order,
  products,
  client,
  onClose,
}: {
  order: SaleRecord;
  products: ProductRecord[];
  client?: ClientRecord;
  onClose: () => void;
}) {
  const items = order.items.flatMap((item) => {
    const product = products.find((record) => record.id === item.productId);
    return product ? [{ ...item, product }] : [];
  });

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
              <div><small>Cliente</small><strong>{client?.name || "Cliente não identificado"}</strong></div>
            </div>
            {order.cancellationReason && <div className="cancellation-reason"><strong>Motivo do cancelamento</strong><p>{order.cancellationReason}</p></div>}
          </section>
          <section className="order-detail-section">
            <div className="order-detail-section-title"><h3>Produtos</h3><span>{order.items.reduce((total, item) => total + item.quantity, 0)} itens</span></div>
            <div className="order-detail-products">
              {items.map((item) => {
                const unitPrice = itemUnitPrice(item, item.product);
                return <div key={item.product.id}><PersonCell initials={item.product.initials} name={item.productName || item.product.name} image={item.product.image} /><span>{item.quantity} un.</span><span>{formatCurrency(unitPrice)}</span><strong>{formatCurrency(unitPrice * item.quantity)}</strong></div>;
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

function OrdersPage({
  orders,
  clients,
  currentRole,
  currentSeller,
  onView,
  onComplete,
  onCancel,
}: {
  orders: SaleRecord[];
  clients: ClientRecord[];
  currentRole: "owner" | "manager" | "seller";
  currentSeller: string;
  onView: (order: SaleRecord) => void;
  onComplete: (id: string) => void;
  onCancel: (id: string, reason: string) => void;
}) {
  const [search, setSearch] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [status, setStatus] = useState("");
  const [seller, setSeller] = useState("");
  const [payment, setPayment] = useState("");
  const [newestFirst, setNewestFirst] = useState(true);
  const [page, setPage] = useState(1);
  const [menuId, setMenuId] = useState<string | null>(null);
  const [completeConfirmation, setCompleteConfirmation] = useState<SaleRecord | null>(null);
  const [cancelOrder, setCancelOrder] = useState<SaleRecord | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelError, setCancelError] = useState("");
  const canManage = currentRole === "owner" || currentRole === "manager";
  const roleOrders = currentRole === "seller" ? orders.filter((order) => order.seller === currentSeller) : orders;
  const sellers = [...new Set(roleOrders.map((order) => order.seller))].sort();
  const metrics = {
    total: roleOrders.length,
    preparing: roleOrders.filter((order) => order.status === "Em preparo").length,
    completed: roleOrders.filter((order) => order.status === "Concluído").length,
    cancelled: roleOrders.filter((order) => order.status === "Cancelado").length,
  };
  const filteredOrders = roleOrders
    .filter((order) => {
      const client = clients.find((record) => record.id === order.clientId);
      const searchMatch = `${order.id} ${client?.name || "Cliente não identificado"} ${order.seller}`.toLocaleLowerCase().includes(search.toLocaleLowerCase());
      const orderDay = new Date(order.confirmedAt);
      orderDay.setHours(0, 0, 0, 0);
      const afterStart = !startDate || orderDay >= new Date(`${startDate}T00:00:00`);
      const beforeEnd = !endDate || orderDay <= new Date(`${endDate}T23:59:59`);
      return searchMatch && afterStart && beforeEnd && (!status || order.status === status) && (!seller || order.seller === seller) && (!payment || order.paymentMethod === payment);
    })
    .sort((a, b) => newestFirst ? b.confirmedAt.getTime() - a.confirmedAt.getTime() : a.confirmedAt.getTime() - b.confirmedAt.getTime());
  const pageSize = 5;
  const pageCount = Math.max(1, Math.ceil(filteredOrders.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const paginatedOrders = filteredOrders.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const hasFilters = Boolean(search || startDate || endDate || status || seller || payment);

  const clearFilters = () => {
    setSearch(""); setStartDate(""); setEndDate(""); setStatus(""); setSeller(""); setPayment(""); setPage(1);
  };
  const submitCancellation = () => {
    if (!cancelOrder || !cancelReason.trim()) {
      setCancelError("Informe o motivo do cancelamento.");
      return;
    }
    onCancel(cancelOrder.id, cancelReason.trim());
    setCancelOrder(null);
    setCancelReason("");
    setCancelError("");
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
        <div className="orders-heading"><div><h2>Histórico de pedidos</h2><p>Acompanhe as vendas registradas e o andamento dos pedidos.</p></div><span>{filteredOrders.length} registros</span></div>
        {orders.every((order) => order.source === "demo") && <div className="orders-demo-note"><Icon name="shield" /><p><strong>Dados demonstrativos</strong>Estes pedidos servem para visualizar a gestão enquanto nenhuma venda real foi registrada.</p></div>}
        <div className="orders-filter-panel">
          <label className="orders-search"><Icon name="search" /><input type="search" value={search} placeholder="Pesquisar por número, cliente ou vendedor..." onChange={(event) => { setSearch(event.target.value); setPage(1); }} /></label>
          <div className="orders-filters-row">
            <label><span>Data inicial</span><input type="date" value={startDate} onChange={(event) => { setStartDate(event.target.value); setPage(1); }} /></label>
            <label><span>Data final</span><input type="date" value={endDate} onChange={(event) => { setEndDate(event.target.value); setPage(1); }} /></label>
            <label><span>Status</span><select value={status} onChange={(event) => { setStatus(event.target.value); setPage(1); }}><option value="">Todos</option><option>Em preparo</option><option>Concluído</option><option>Cancelado</option></select></label>
            <label><span>Vendedor</span><select value={seller} disabled={currentRole === "seller"} onChange={(event) => { setSeller(event.target.value); setPage(1); }}><option value="">Todos</option>{sellers.map((name) => <option key={name}>{name}</option>)}</select></label>
            <label><span>Pagamento</span><select value={payment} onChange={(event) => { setPayment(event.target.value); setPage(1); }}><option value="">Todos</option>{Object.entries(paymentLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <button type="button" disabled={!hasFilters} onClick={clearFilters}><Icon name="close" />Limpar filtros</button>
          </div>
        </div>

        {paginatedOrders.length > 0 ? (
          <div className="orders-table-scroll">
            <table className="orders-table">
              <thead><tr><th>Nº do pedido</th><th><button type="button" onClick={() => setNewestFirst((value) => !value)}>Data e hora <span>{newestFirst ? "↓" : "↑"}</span></button></th><th>Cliente</th><th>Vendedor</th><th>Itens</th><th>Valor total</th><th>Pagamento</th><th>Status</th><th>Ações</th></tr></thead>
              <tbody>{paginatedOrders.map((order) => {
                const client = clients.find((record) => record.id === order.clientId);
                const itemCount = order.items.reduce((total, item) => total + item.quantity, 0);
                return (
                  <tr key={order.id}>
                    <td><div className="order-number-cell"><strong>{order.id}</strong>{order.source === "demo" && <small>Demo</small>}</div></td>
                    <td><span className="order-date">{order.confirmedAt.toLocaleDateString("pt-BR")}<small>{order.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</small></span></td>
                    <td>{client?.name || "Cliente não identificado"}</td><td>{order.seller}</td><td>{itemCount} un.</td><td><strong>{formatCurrency(order.total)}</strong></td><td>{paymentLabels[order.paymentMethod]}</td><td><OrderStatusBadge status={order.status} /></td>
                    <td><div className="order-row-actions"><button type="button" title="Visualizar" aria-label={`Visualizar ${order.id}`} onClick={() => onView(order)}><Icon name="eye" /></button>{canManage && order.status === "Em preparo" && <><button type="button" title="Mais opções" aria-label={`Mais opções para ${order.id}`} aria-expanded={menuId === order.id} onClick={() => setMenuId((current) => current === order.id ? null : order.id)}><Icon name="more" /></button>{menuId === order.id && <div className="order-actions-menu"><button type="button" onClick={() => { setCompleteConfirmation(order); setMenuId(null); }}><Icon name="check" />Marcar como concluído</button><button type="button" className="cancel" onClick={() => { setCancelOrder(order); setMenuId(null); }}><Icon name="close" />Cancelar pedido</button></div>}</>}</div></td>
                  </tr>
                );
              })}</tbody>
            </table>
          </div>
        ) : (
          <div className="orders-empty"><span><Icon name="orders" /></span><strong>Nenhum pedido encontrado</strong><p>{hasFilters ? "Ajuste ou limpe os filtros para visualizar outros pedidos." : "As vendas confirmadas aparecerão aqui automaticamente."}</p>{hasFilters && <button type="button" onClick={clearFilters}>Limpar filtros</button>}</div>
        )}
        <footer className="orders-pagination"><p>Mostrando <strong>{paginatedOrders.length}</strong> de {filteredOrders.length} pedidos</p><div><button type="button" disabled={currentPage === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}><Icon name="chevron-left" /></button>{Array.from({ length: pageCount }, (_, index) => <button key={index + 1} type="button" className={currentPage === index + 1 ? "active" : ""} onClick={() => setPage(index + 1)}>{index + 1}</button>)}<button type="button" disabled={currentPage === pageCount} onClick={() => setPage((value) => Math.min(pageCount, value + 1))}><Icon name="chevron-right" /></button></div></footer>
      </section>

      {completeConfirmation && <div className="confirm-backdrop" role="presentation"><section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="complete-order-title"><span className="confirm-icon complete"><Icon name="check" /></span><h2 id="complete-order-title">Concluir {completeConfirmation.id}?</h2><p>O pedido será marcado como concluído. Esta alteração não poderá ser desfeita automaticamente.</p><div><button type="button" onClick={() => setCompleteConfirmation(null)}>Voltar</button><button className="confirm-complete-button" type="button" onClick={() => { onComplete(completeConfirmation.id); setCompleteConfirmation(null); }}>Concluir pedido</button></div></section></div>}
      {cancelOrder && <div className="confirm-backdrop" role="presentation"><section className="cancel-order-dialog" role="alertdialog" aria-modal="true" aria-labelledby="cancel-order-title"><span className="confirm-icon"><Icon name="close" /></span><h2 id="cancel-order-title">Cancelar pedido {cancelOrder.id}?</h2><p>O pedido continuará no histórico e não poderá voltar automaticamente para outro status.</p><label htmlFor="cancellation-reason">Motivo do cancelamento<textarea id="cancellation-reason" value={cancelReason} placeholder="Descreva o motivo do cancelamento..." onChange={(event) => { setCancelReason(event.target.value); setCancelError(""); }} /></label>{cancelError && <small role="alert">{cancelError}</small>}<div><button type="button" onClick={() => { setCancelOrder(null); setCancelReason(""); setCancelError(""); }}>Voltar</button><button type="button" onClick={submitCancellation}>Confirmar cancelamento</button></div></section></div>}
    </div>
  );
}

type ReportPeriod = "today" | "7days" | "30days" | "month" | "custom";

const getReportRange = (period: ReportPeriod, customStart: string, customEnd: string) => {
  const end = period === "custom" && customEnd ? new Date(`${customEnd}T23:59:59`) : new Date();
  const start = new Date(end);
  if (period === "today") start.setHours(0, 0, 0, 0);
  if (period === "7days") { start.setDate(start.getDate() - 6); start.setHours(0, 0, 0, 0); }
  if (period === "30days") { start.setDate(start.getDate() - 29); start.setHours(0, 0, 0, 0); }
  if (period === "month") { start.setDate(1); start.setHours(0, 0, 0, 0); }
  if (period === "custom") {
    const parsedStart = customStart ? new Date(`${customStart}T00:00:00`) : new Date(end);
    if (!customStart) parsedStart.setDate(parsedStart.getDate() - 29);
    return { start: parsedStart, end };
  }
  return { start, end };
};

const filterReportOrders = (orders: SaleRecord[], period: ReportPeriod, customStart: string, customEnd: string, seller: string) => {
  const { start, end } = getReportRange(period, customStart, customEnd);
  return orders.filter((order) => order.confirmedAt >= start && order.confirmedAt <= end && (!seller || order.seller === seller));
};

function ReportsPage({
  orders,
  products,
  period,
  customStart,
  customEnd,
  seller,
  onPeriod,
  onCustomStart,
  onCustomEnd,
  onSeller,
}: {
  orders: SaleRecord[];
  products: ProductRecord[];
  period: ReportPeriod;
  customStart: string;
  customEnd: string;
  seller: string;
  onPeriod: (period: ReportPeriod) => void;
  onCustomStart: (value: string) => void;
  onCustomEnd: (value: string) => void;
  onSeller: (value: string) => void;
}) {
  const [sellerSort, setSellerSort] = useState<"revenue" | "sales">("revenue");
  const [historySort, setHistorySort] = useState<"date" | "value">("date");
  const [historyDescending, setHistoryDescending] = useState(true);
  const [historyPage, setHistoryPage] = useState(1);
  const filteredOrders = filterReportOrders(orders, period, customStart, customEnd, seller);
  const validOrders = filteredOrders.filter((order) => order.status !== "Cancelado");
  const revenue = validOrders.reduce((total, order) => total + order.total, 0);
  const salesCount = validOrders.length;
  const averageTicket = salesCount ? revenue / salesCount : 0;
  const productsSold = validOrders.reduce((total, order) => total + order.items.reduce((sum, item) => sum + item.quantity, 0), 0);
  const sellers = [...new Set(orders.map((order) => order.seller))].sort();
  const { start: rangeStart, end: rangeEnd } = getReportRange(period, customStart, customEnd);

  const chartData = (() => {
    if (period === "today") {
      return Array.from({ length: 24 }, (_, hour) => ({
        key: String(hour),
        label: `${String(hour).padStart(2, "0")}h`,
        value: validOrders.filter((order) => order.confirmedAt.getHours() === hour).reduce((sum, order) => sum + order.total, 0),
      }));
    }
    const duration = Math.ceil((rangeEnd.getTime() - rangeStart.getTime()) / 86400000);
    if (duration > 62) {
      const buckets = new Map<string, { label: string; value: number }>();
      const cursor = new Date(rangeStart.getFullYear(), rangeStart.getMonth(), 1);
      while (cursor <= rangeEnd) {
        const key = `${cursor.getFullYear()}-${cursor.getMonth()}`;
        buckets.set(key, { label: cursor.toLocaleDateString("pt-BR", { month: "short", year: "2-digit" }).replace(".", ""), value: 0 });
        cursor.setMonth(cursor.getMonth() + 1);
      }
      validOrders.forEach((order) => {
        const key = `${order.confirmedAt.getFullYear()}-${order.confirmedAt.getMonth()}`;
        const bucket = buckets.get(key);
        if (bucket) bucket.value += order.total;
      });
      return [...buckets.entries()].map(([key, value]) => ({ key, ...value }));
    }
    const buckets: Array<{ key: string; label: string; value: number }> = [];
    const cursor = new Date(rangeStart);
    cursor.setHours(0, 0, 0, 0);
    while (cursor <= rangeEnd) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`;
      buckets.push({ key, label: cursor.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" }), value: 0 });
      cursor.setDate(cursor.getDate() + 1);
    }
    validOrders.forEach((order) => {
      const key = `${order.confirmedAt.getFullYear()}-${order.confirmedAt.getMonth()}-${order.confirmedAt.getDate()}`;
      const bucket = buckets.find((item) => item.key === key);
      if (bucket) bucket.value += order.total;
    });
    return buckets;
  })();
  const chartMax = Math.max(...chartData.map((item) => item.value), 1);

  const productRanking = products
    .map((product) => {
      const soldItems = validOrders.flatMap((order) => order.items.filter((item) => item.productId === product.id));
      const quantity = soldItems.reduce((total, item) => total + item.quantity, 0);
      const productRevenue = soldItems.reduce((total, item) => total + itemUnitPrice(item, product) * item.quantity, 0);
      return { product, quantity, revenue: productRevenue };
    })
    .filter((item) => item.quantity > 0)
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, 5);
  const rankingMax = Math.max(...productRanking.map((item) => item.quantity), 1);

  const sellerPerformance = [...new Set(validOrders.map((order) => order.seller))]
    .map((name) => {
      const sellerOrders = validOrders.filter((order) => order.seller === name);
      const sellerRevenue = sellerOrders.reduce((total, order) => total + order.total, 0);
      return { name, sales: sellerOrders.length, revenue: sellerRevenue, average: sellerOrders.length ? sellerRevenue / sellerOrders.length : 0, share: revenue ? sellerRevenue / revenue * 100 : 0 };
    })
    .sort((a, b) => sellerSort === "revenue" ? b.revenue - a.revenue : b.sales - a.sales);

  const paymentTotals = (Object.keys(paymentLabels) as PaymentMethod[]).map((method) => {
    const value = validOrders.filter((order) => order.paymentMethod === method).reduce((total, order) => total + order.total, 0);
    return { method, value };
  });
  const lastPaidMethod = paymentTotals.reduce((last, row, index) => row.value > 0 ? index : last, -1);
  let accumulatedPercentage = 0;
  const paymentRows = paymentTotals.map((row, index) => {
    const raw = revenue ? row.value / revenue * 100 : 0;
    const percentage = revenue ? (index === lastPaidMethod ? Math.max(0, Math.round((100 - accumulatedPercentage) * 10) / 10) : Math.round(raw * 10) / 10) : 0;
    accumulatedPercentage += percentage;
    return { ...row, percentage };
  });

  const sortedHistory = [...filteredOrders].sort((a, b) => {
    const comparison = historySort === "date" ? a.confirmedAt.getTime() - b.confirmedAt.getTime() : a.total - b.total;
    return historyDescending ? -comparison : comparison;
  });
  const historyPageSize = 6;
  const historyPageCount = Math.max(1, Math.ceil(sortedHistory.length / historyPageSize));
  const currentHistoryPage = Math.min(historyPage, historyPageCount);
  const historyRows = sortedHistory.slice((currentHistoryPage - 1) * historyPageSize, currentHistoryPage * historyPageSize);
  const periodOptions: Array<{ id: ReportPeriod; label: string }> = [
    { id: "today", label: "Hoje" }, { id: "7days", label: "Últimos 7 dias" }, { id: "30days", label: "Últimos 30 dias" }, { id: "month", label: "Este mês" }, { id: "custom", label: "Período personalizado" },
  ];

  const changeHistorySort = (sort: "date" | "value") => {
    if (historySort === sort) setHistoryDescending((value) => !value);
    else { setHistorySort(sort); setHistoryDescending(true); }
  };

  return (
    <div className="dashboard-content reports-content">
      <section className="dashboard-card report-filters">
        <div className="period-options">{periodOptions.map((option) => <button key={option.id} type="button" className={period === option.id ? "active" : ""} onClick={() => { onPeriod(option.id); setHistoryPage(1); }}>{option.label}</button>)}</div>
        <div className="report-filter-fields">
          {period === "custom" && <><label><span>Data inicial</span><input type="date" value={customStart} onChange={(event) => { onCustomStart(event.target.value); setHistoryPage(1); }} /></label><label><span>Data final</span><input type="date" value={customEnd} onChange={(event) => { onCustomEnd(event.target.value); setHistoryPage(1); }} /></label></>}
          <label className="report-seller-filter"><span>Vendedor</span><select value={seller} onChange={(event) => { onSeller(event.target.value); setHistoryPage(1); }}><option value="">Todos os vendedores</option>{sellers.map((name) => <option key={name}>{name}</option>)}</select></label>
        </div>
      </section>

      {orders.every((order) => order.source === "demo") && <div className="report-demo-note"><Icon name="shield" />Resultados calculados com os pedidos demonstrativos identificados na Gestão de Pedidos.</div>}

      <section className="report-metrics-grid">
        <article><span className="revenue"><Icon name="cash" /></span><p><small>Faturamento total</small><strong>{formatCurrency(revenue)}</strong></p></article>
        <article><span className="sales"><Icon name="orders" /></span><p><small>Total de vendas</small><strong>{salesCount}</strong></p></article>
        <article><span className="ticket"><Icon name="sales" /></span><p><small>Ticket médio</small><strong>{formatCurrency(averageTicket)}</strong></p></article>
        <article><span className="items"><Icon name="products" /></span><p><small>Produtos vendidos</small><strong>{productsSold}</strong></p></article>
      </section>

      <div className="report-top-grid">
        <section className="dashboard-card revenue-chart-card">
          <div className="report-section-heading"><div><h2>Evolução das vendas</h2><p>Faturamento no período selecionado.</p></div><strong>{formatCurrency(revenue)}</strong></div>
          {validOrders.length ? <div className="revenue-bars">{chartData.map((item, index) => <div className="revenue-bar-column" key={item.key} title={`${item.label}: ${formatCurrency(item.value)}`}><span className={item.value ? "filled" : ""} style={{ height: `${Math.max(item.value / chartMax * 100, item.value ? 5 : 1)}%` }}><i>{item.value ? formatCurrency(item.value) : ""}</i></span>{(chartData.length <= 10 || index % Math.ceil(chartData.length / 8) === 0) && <small>{item.label}</small>}</div>)}</div> : <div className="report-empty-chart"><Icon name="sales" /><p>Nenhuma venda encontrada para o período selecionado.</p></div>}
        </section>
        <section className="dashboard-card payment-report-card">
          <div className="report-section-heading"><div><h2>Formas de pagamento</h2><p>Participação no faturamento.</p></div></div>
          {validOrders.length ? <div className="payment-report-list">{paymentRows.map((row) => <div key={row.method}><span className={`payment-report-icon ${row.method}`}><Icon name={row.method === "cash" ? "cash" : row.method === "pix" ? "pix" : "credit-card"} /></span><p><strong>{paymentLabels[row.method]}</strong><small>{formatCurrency(row.value)}</small></p><b>{row.percentage.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</b><i><span style={{ width: `${row.percentage}%` }} /></i></div>)}</div> : <div className="report-compact-empty">Nenhum pagamento no período.</div>}
        </section>
      </div>

      <div className="report-analysis-grid">
        <section className="dashboard-card top-products-card">
          <div className="report-section-heading"><div><h2>Produtos mais vendidos</h2><p>Classificação por quantidade vendida.</p></div></div>
          {productRanking.length ? <div className="product-ranking">{productRanking.map((item, index) => <div key={item.product.id}><span className={`ranking-position position-${index + 1}`}>{index + 1}</span><p><strong>{item.product.name}</strong><small>{item.product.category}</small><i><span style={{ width: `${item.quantity / rankingMax * 100}%` }} /></i></p><div><strong>{item.quantity} un.</strong><small>{formatCurrency(item.revenue)}</small></div></div>)}</div> : <div className="report-compact-empty">Nenhum produto vendido no período.</div>}
        </section>
        <section className="dashboard-card seller-performance-card">
          <div className="report-section-heading"><div><h2>Desempenho por vendedor</h2><p>Resultados da equipe no período.</p></div><div className="seller-sort-buttons"><button type="button" className={sellerSort === "revenue" ? "active" : ""} onClick={() => setSellerSort("revenue")}>Faturamento</button><button type="button" className={sellerSort === "sales" ? "active" : ""} onClick={() => setSellerSort("sales")}>Vendas</button></div></div>
          {sellerPerformance.length ? <div className="seller-table-scroll"><table className="seller-report-table"><thead><tr><th>Vendedor</th><th>Vendas</th><th>Faturamento</th><th>Ticket médio</th><th>Participação</th></tr></thead><tbody>{sellerPerformance.map((row) => <tr key={row.name}><td><PersonCell initials={getInitials(row.name)} name={row.name} /></td><td>{row.sales}</td><td><strong>{formatCurrency(row.revenue)}</strong></td><td>{formatCurrency(row.average)}</td><td><span className="seller-share"><i><span style={{ width: `${row.share}%` }} /></i>{row.share.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</span></td></tr>)}</tbody></table></div> : <div className="report-compact-empty">Nenhum resultado por vendedor no período.</div>}
        </section>
      </div>

      <section className="dashboard-card financial-history-card">
        <div className="report-section-heading"><div><h2>Histórico financeiro</h2><p>Vendas e cancelamentos registrados no período.</p></div><span>{filteredOrders.length} registros</span></div>
        {historyRows.length ? <div className="financial-history-scroll"><table className="financial-history-table"><thead><tr><th><button type="button" onClick={() => changeHistorySort("date")}>Data {historySort === "date" && (historyDescending ? "↓" : "↑")}</button></th><th>Nº do pedido</th><th>Vendedor</th><th>Pagamento</th><th><button type="button" onClick={() => changeHistorySort("value")}>Valor {historySort === "value" && (historyDescending ? "↓" : "↑")}</button></th><th>Status</th></tr></thead><tbody>{historyRows.map((order) => <tr key={order.id}><td><span className="order-date">{order.confirmedAt.toLocaleDateString("pt-BR")}<small>{order.confirmedAt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</small></span></td><td><strong>{order.id}</strong></td><td>{order.seller}</td><td>{paymentLabels[order.paymentMethod]}</td><td><strong>{formatCurrency(order.total)}</strong></td><td><OrderStatusBadge status={order.status} /></td></tr>)}</tbody></table></div> : <div className="reports-history-empty">Nenhuma venda encontrada para o período selecionado.</div>}
        <footer className="orders-pagination"><p>Mostrando <strong>{historyRows.length}</strong> de {filteredOrders.length} registros</p><div><button type="button" disabled={currentHistoryPage === 1} onClick={() => setHistoryPage((value) => Math.max(1, value - 1))}><Icon name="chevron-left" /></button>{Array.from({ length: historyPageCount }, (_, index) => <button key={index + 1} type="button" className={currentHistoryPage === index + 1 ? "active" : ""} onClick={() => setHistoryPage(index + 1)}>{index + 1}</button>)}<button type="button" disabled={currentHistoryPage === historyPageCount} onClick={() => setHistoryPage((value) => Math.min(historyPageCount, value + 1))}><Icon name="chevron-right" /></button></div></footer>
      </section>
    </div>
  );
}

type PersistedPrototypeState = {
  clients: ClientRecord[];
  products: ProductRecord[];
  sales: Array<Omit<SaleRecord, "createdAt" | "confirmedAt"> & { createdAt: string; confirmedAt: string }>;
  company: CompanySettings;
  preferences: SalesPreferences;
};

const prototypeStorageKey = "ponto-e-sabor:prototype-data:v1";

/** Funcionário exibido enquanto a sessão ainda não carregou (nunca aparece na tela). */
const emptyUser: EmployeeRecord = { id: 0, initials: "", name: "", cpf: "", email: "", phone: "", username: "", role: "Vendedor", lastAccess: "", status: "Ativo" };

const homePage = (user: Usuario) => user.perfil === "VENDEDOR" ? "sales" as const : "dashboard" as const;

const loadPrototypeState = (): Partial<{
  clients: ClientRecord[];
  products: ProductRecord[];
  sales: SaleRecord[];
  company: CompanySettings;
  preferences: SalesPreferences;
}> => {
  try {
    const raw = window.localStorage.getItem(prototypeStorageKey);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as PersistedPrototypeState;
    return {
      clients: Array.isArray(parsed.clients) ? parsed.clients : undefined,
      products: Array.isArray(parsed.products) ? parsed.products : undefined,
      sales: Array.isArray(parsed.sales) ? parsed.sales.map((sale) => ({ ...sale, createdAt: new Date(sale.createdAt), confirmedAt: new Date(sale.confirmedAt) })) : undefined,
      company: parsed.company,
      preferences: parsed.preferences,
    };
  } catch {
    return {};
  }
};

export default function App() {
  const persistedState = useRef(loadPrototypeState()).current;
  const [sessionUser, setSessionUser] = useState<Usuario | null>(null);
  const [checkingSession, setCheckingSession] = useState(() => Boolean(tokenStorage.get()));
  const [loginNotice, setLoginNotice] = useState("");
  const [activePage, setActivePage] = useState<"dashboard" | "sales" | "orders" | "reports" | "settings" | "registrations" | "review" | "success" | "details">("dashboard");
  const [activeTab, setActiveTab] = useState<TabId>("clients");
  const [clientRecords] = useState<ClientRecord[]>(persistedState.clients || initialClients);
  const [productRecords, setProductRecords] = useState<ProductRecord[]>(persistedState.products || initialProducts);
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
  const [salesRecords, setSalesRecords] = useState<SaleRecord[]>(persistedState.sales || initialSales);
  const [lastSale, setLastSale] = useState<SaleRecord | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<SaleRecord | null>(null);
  const [reportPeriod, setReportPeriod] = useState<ReportPeriod>("7days");
  const [reportStartDate, setReportStartDate] = useState("");
  const [reportEndDate, setReportEndDate] = useState("");
  const [reportSeller, setReportSeller] = useState("");
  const [companySettings, setCompanySettings] = useState<CompanySettings>(persistedState.company || defaultCompany);
  const [salesPreferences, setSalesPreferences] = useState<SalesPreferences>(persistedState.preferences || defaultPreferences);
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

  useEffect(() => {
    try {
      const persistedCompany = { ...companySettings, logo: companySettings.logo.startsWith("blob:") ? "" : companySettings.logo };
      window.localStorage.setItem(prototypeStorageKey, JSON.stringify({
        clients: clientRecords,
        products: productRecords.map((product) => ({ ...product, image: product.image.startsWith("blob:") ? "" : product.image })),
        sales: salesRecords.map((sale) => ({ ...sale, createdAt: sale.createdAt.toISOString(), confirmedAt: sale.confirmedAt.toISOString() })),
        company: persistedCompany,
        preferences: salesPreferences,
      } satisfies PersistedPrototypeState));
    } catch {
      // The prototype continues in memory if browser storage is unavailable.
    }
  }, [clientRecords, productRecords, salesRecords, companySettings, salesPreferences]);

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
      id: preparedOrder?.id || `#PV-${Date.now().toString().slice(-8)}`,
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

  const confirmSale = () => {
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
    const sale: SaleRecord = {
      ...preparedOrder,
      paymentMethod,
      subtotal,
      discountValue,
      total,
      cashReceived: paymentMethod === "cash" ? received : total,
      change: paymentMethod === "cash" ? Math.max(0, received - total) : 0,
      confirmedAt: new Date(),
      status: salesPreferences.initialOrderStatus,
      source: "real",
    };
    setSalesRecords((records) => [sale, ...records]);
    if (salesPreferences.stockControl) {
      setProductRecords((records) => records.map((product) => {
        const item = sale.items.find((entry) => entry.productId === product.id);
        return item ? { ...product, stock: Math.max(0, product.stock - item.quantity) } : product;
      }));
    }
    setLastSale(sale);
    setPreparedOrder(null);
    setCart([]);
    setReviewError("");
    setConfirming(false);
    setActivePage("success");
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
    const rows = filterReportOrders(salesRecords, reportPeriod, reportStartDate, reportEndDate, reportSeller);
    const escapeCsv = (value: string | number) => `"${String(value).replace(/"/g, "\"\"")}"`;
    const header = ["Data", "Número do pedido", "Vendedor", "Cliente", "Forma de pagamento", "Valor", "Status"];
    const lines = rows.map((order) => {
      const client = clientRecords.find((record) => record.id === order.clientId);
      return [
        order.confirmedAt.toLocaleString("pt-BR"),
        order.id,
        order.seller,
        client?.name || "Cliente não identificado",
        paymentLabels[order.paymentMethod],
        order.total.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
        order.status,
      ].map(escapeCsv).join(";");
    });
    const csv = `\uFEFF${header.map(escapeCsv).join(";")}\n${lines.join("\n")}`;
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `relatorio-ponto-e-sabor-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    setToast(`${rows.length} registros exportados em CSV.`);
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
          <DashboardPage orders={salesRecords} products={productRecords} clients={clientRecords} currentRole={currentRole} onNavigate={(page) => { if (page === "registrations") setActiveTab("clients"); setActivePage(page); }} />
        ) : activePage === "sales" ? (
          <SalesPage
            products={productRecords}
            clients={clientRecords}
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
          <OrdersPage
            orders={salesRecords}
            clients={clientRecords}
            currentRole={currentRole}
            currentSeller={currentUser.name}
            onView={setSelectedOrder}
            onComplete={(id) => {
              if (currentRole === "seller") { setToast("Você não possui permissão para acessar esta funcionalidade."); return; }
              setSalesRecords((records) => records.map((order) => order.id === id && order.status === "Em preparo" ? { ...order, status: "Concluído" } : order));
              setToast("Pedido concluído com sucesso.");
            }}
            onCancel={(id, reason) => {
              if (currentRole === "seller") { setToast("Você não possui permissão para acessar esta funcionalidade."); return; }
              setSalesRecords((records) => records.map((order) => order.id === id && order.status === "Em preparo" ? { ...order, status: "Cancelado", cancellationReason: reason } : order));
              setToast("Pedido cancelado e mantido no histórico.");
            }}
          />
        ) : activePage === "reports" ? (
          <ReportsPage
            orders={salesRecords}
            products={productRecords}
            period={reportPeriod}
            customStart={reportStartDate}
            customEnd={reportEndDate}
            seller={reportSeller}
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
            client={clientRecords.find((record) => record.id === lastSale.clientId)}
            details={activePage === "details"}
            onNewSale={startNewSale}
            onViewOrder={() => setActivePage("details")}
            onBack={() => setActivePage("success")}
          />
        ) : (
          <RegistrationsPage activeTab={activeTab} currentRole={currentRole} onTabChange={setActiveTab} onToast={setToast} />
        )}
      </main>
      {selectedOrder && <OrderDetailsModal order={selectedOrder} products={productRecords} client={clientRecords.find((record) => record.id === selectedOrder.clientId)} onClose={() => setSelectedOrder(null)} />}
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
