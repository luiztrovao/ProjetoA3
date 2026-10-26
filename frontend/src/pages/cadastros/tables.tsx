// Tabelas da tela Cadastros (Clientes, Funcionários e Produtos).
import { Fragment } from "react";
import { Icon, type IconName } from "../../components/Icon";
import { PersonCell, StatusBadge } from "../../components/ui";
import type { ClientRecord, EmployeeRecord, ModalMode, ProductRecord, TabId } from "../../types";

export const tabs: Array<{ id: TabId; label: string; icon: IconName }> = [
  { id: "clients", label: "Clientes", icon: "users" },
  { id: "employees", label: "Funcionários", icon: "team" },
  { id: "products", label: "Produtos", icon: "products" },
];

function RowActions({
  name,
  inactive,
  menuOpen,
  onView,
  onEdit,
  onMore,
  onToggleStatus,
  canEdit = true,
  canManage = true,
}: {
  name: string;
  inactive: boolean;
  menuOpen: boolean;
  onView: () => void;
  onEdit: () => void;
  onMore: () => void;
  onToggleStatus: () => void;
  canEdit?: boolean;
  canManage?: boolean;
}) {
  return (
    <div className="row-actions">
      <button type="button" aria-label={`Visualizar ${name}`} title="Visualizar" onClick={onView}><Icon name="eye" /></button>
      {canEdit && <button type="button" aria-label={`Editar ${name}`} title="Editar" onClick={onEdit}><Icon name="edit" /></button>}
      {canManage && <button type="button" aria-label={`Mais opções para ${name}`} title="Mais opções" aria-expanded={menuOpen} onClick={onMore}><Icon name="more" /></button>}
      {canManage && menuOpen && (
        <div className="row-action-menu">
          <button type="button" onClick={onToggleStatus}>
            <Icon name="power" />
            {inactive ? "Ativar registro" : "Desativar registro"}
          </button>
        </div>
      )}
    </div>
  );
}

export type PaginationProps = {
  page: number;
  size: number;
  total: number;
  totalPages: number;
  onPage: (page: number) => void;
};

/** Páginas exibidas: a primeira, a última e as vizinhas da atual, com "..." entre os saltos. */
const visiblePages = (current: number, totalPages: number) => {
  const pages = new Set([0, totalPages - 1, current - 1, current, current + 1]);
  if (current <= 1) pages.add(2);
  if (current >= totalPages - 2) pages.add(totalPages - 3);
  return [...pages].filter((page) => page >= 0 && page < totalPages).sort((a, b) => a - b);
};

function Pagination({ page, size, total, totalPages, onPage }: PaginationProps) {
  const first = total === 0 ? 0 : page * size + 1;
  const last = Math.min(total, (page + 1) * size);
  const pages = visiblePages(page, totalPages);
  return (
    <footer className="pagination">
      <p>Mostrando <strong>{first}–{last}</strong> de {total} registros</p>
      <div>
        <button type="button" aria-label="Página anterior" disabled={page <= 0} onClick={() => onPage(page - 1)}><Icon name="chevron-left" /></button>
        {pages.map((number, index) => (
          <Fragment key={number}>
            {index > 0 && number - pages[index - 1] > 1 && <span>...</span>}
            <button className={number === page ? "current-page" : undefined} type="button" aria-current={number === page ? "page" : undefined} onClick={() => onPage(number)}>{number + 1}</button>
          </Fragment>
        ))}
        <button type="button" aria-label="Próxima página" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}><Icon name="chevron-right" /></button>
      </div>
    </footer>
  );
}

function SearchToolbar({
  placeholder,
  filters,
  value,
  activeFilters,
  onChange,
  onToggleFilter,
}: {
  placeholder: string;
  filters: string[];
  value: string;
  activeFilters: string[];
  onChange: (value: string) => void;
  onToggleFilter: (filter: string) => void;
}) {
  return (
    <div className="management-toolbar">
      <label className="search-field">
        <span className="sr-only">Pesquisar</span>
        <Icon name="search" />
        <input type="search" placeholder={placeholder} value={value} onChange={(event) => onChange(event.target.value)} />
      </label>
      <div className="filter-controls">
        {filters.map((filter) => (
          <button key={filter} className={`filter-button${activeFilters.includes(filter) ? " active" : ""}`} type="button" onClick={() => onToggleFilter(filter)}>
            {filters.length === 1 && <Icon name="filter" />}
            {filter}
            {filters.length > 1 && <Icon name="chevron" />}
          </button>
        ))}
      </div>
    </div>
  );
}

function EmptyRow({ columns, message }: { columns: number; message: string }) {
  return <tr><td colSpan={columns} style={{ textAlign: "center", padding: "28px 12px" }}>{message}</td></tr>;
}

export type TableActions = {
  menuId: string | null;
  onOpen: (mode: ModalMode, id: number) => void;
  onMenu: (id: string) => void;
  onToggleStatus: (id: number) => void;
  canEdit: boolean;
  canManage: boolean;
};

export function ClientsTable({
  records,
  search,
  activeFilters,
  onSearch,
  onToggleFilter,
  pagination,
  emptyMessage,
  ...actions
}: {
  records: ClientRecord[];
  search: string;
  activeFilters: string[];
  onSearch: (value: string) => void;
  onToggleFilter: (filter: string) => void;
  pagination: PaginationProps;
  emptyMessage: string;
} & TableActions) {
  return (
    <>
      <SearchToolbar placeholder="Pesquisar por nome, CPF ou telefone..." filters={["Filtros"]} value={search} activeFilters={activeFilters} onChange={onSearch} onToggleFilter={onToggleFilter} />
      <div className="management-table-scroll">
        <table className="management-table">
          <thead><tr><th>Cliente</th><th>CPF</th><th>Telefone</th><th>E-mail</th><th>Cadastro</th><th>Status</th><th>Ações</th></tr></thead>
          <tbody>
            {records.length === 0 && <EmptyRow columns={7} message={emptyMessage} />}
            {records.map((client) => (
              <tr key={client.id}>
                <td><PersonCell initials={client.initials} name={client.name} /></td>
                <td>{client.cpf}</td><td>{client.phone}</td><td>{client.email}</td><td>{client.date}</td>
                <td><StatusBadge value={client.status} /></td>
                <td><RowActions name={client.name} inactive={client.status === "Inativo"} menuOpen={actions.menuId === `clients-${client.id}`} canEdit={actions.canEdit} canManage={actions.canManage} onView={() => actions.onOpen("view", client.id)} onEdit={() => actions.onOpen("edit", client.id)} onMore={() => actions.onMenu(`clients-${client.id}`)} onToggleStatus={() => actions.onToggleStatus(client.id)} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination {...pagination} />
    </>
  );
}

export function EmployeesTable({
  records,
  search,
  activeFilters,
  onSearch,
  onToggleFilter,
  pagination,
  emptyMessage,
  ...actions
}: {
  records: EmployeeRecord[];
  search: string;
  activeFilters: string[];
  onSearch: (value: string) => void;
  onToggleFilter: (filter: string) => void;
  pagination: PaginationProps;
  emptyMessage: string;
} & TableActions) {
  return (
    <>
      <div className="access-note">
        <span><Icon name="shield" /></span>
        <p><strong>Níveis de acesso</strong>Dono possui acesso total, Gerente acesso administrativo e Vendedor acesso focado em vendas.</p>
      </div>
      <SearchToolbar placeholder="Pesquisar por nome, CPF ou e-mail..." filters={["Perfil", "Status"]} value={search} activeFilters={activeFilters} onChange={onSearch} onToggleFilter={onToggleFilter} />
      <div className="management-table-scroll">
        <table className="management-table">
          <thead><tr><th>Funcionário</th><th>Contato</th><th>Perfil</th><th>Último acesso</th><th>Status</th><th>Ações</th></tr></thead>
          <tbody>
            {records.length === 0 && <EmptyRow columns={6} message={emptyMessage} />}
            {records.map((employee) => (
              <tr key={employee.id}>
                <td><PersonCell initials={employee.initials} name={employee.name} detail={employee.email} /></td>
                <td>{employee.phone}</td>
                <td><span className={`role-badge ${employee.role.toLowerCase()}`}>{employee.role}</span></td>
                <td>{employee.lastAccess}</td><td><StatusBadge value={employee.status} /></td>
                <td><RowActions name={employee.name} inactive={employee.status === "Inativo"} menuOpen={actions.menuId === `employees-${employee.id}`} canEdit={actions.canEdit} canManage={actions.canManage} onView={() => actions.onOpen("view", employee.id)} onEdit={() => actions.onOpen("edit", employee.id)} onMore={() => actions.onMenu(`employees-${employee.id}`)} onToggleStatus={() => actions.onToggleStatus(employee.id)} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination {...pagination} />
    </>
  );
}

export function ProductsTable({
  records,
  search,
  activeFilters,
  onSearch,
  onToggleFilter,
  pagination,
  emptyMessage,
  ...actions
}: {
  records: ProductRecord[];
  search: string;
  activeFilters: string[];
  onSearch: (value: string) => void;
  onToggleFilter: (filter: string) => void;
  pagination: PaginationProps;
  emptyMessage: string;
} & TableActions) {
  return (
    <>
      <SearchToolbar placeholder="Pesquisar por nome ou código..." filters={["Categoria", "Status", "Estoque"]} value={search} activeFilters={activeFilters} onChange={onSearch} onToggleFilter={onToggleFilter} />
      <div className="management-table-scroll">
        <table className="management-table">
          <thead><tr><th>Produto</th><th>Categoria</th><th>Código</th><th>Preço</th><th>Estoque</th><th>Status</th><th>Ações</th></tr></thead>
          <tbody>
            {records.length === 0 && <EmptyRow columns={7} message={emptyMessage} />}
            {records.map((product) => {
              const displayStatus = product.status === "Inativo" ? "Inativo" : product.stock <= product.minimumStock ? "Estoque baixo" : "Disponível";
              return (
              <tr key={product.id} className={product.status === "Ativo" && product.stock <= product.minimumStock ? "low-stock-row" : ""}>
                <td><PersonCell initials={product.initials} name={product.name} image={product.image} /></td>
                <td>{product.category}</td><td>{product.code}</td><td><strong>{product.price}</strong></td>
                <td><span className={product.stock <= product.minimumStock ? "stock-count low" : "stock-count"}>{product.stock} un.</span></td>
                <td><StatusBadge value={displayStatus} /></td>
                <td><RowActions name={product.name} inactive={product.status === "Inativo"} menuOpen={actions.menuId === `products-${product.id}`} canEdit={actions.canEdit} canManage={actions.canManage} onView={() => actions.onOpen("view", product.id)} onEdit={() => actions.onOpen("edit", product.id)} onMore={() => actions.onMenu(`products-${product.id}`)} onToggleStatus={() => actions.onToggleStatus(product.id)} /></td>
              </tr>
            )})}
          </tbody>
        </table>
      </div>
      <Pagination {...pagination} />
    </>
  );
}
