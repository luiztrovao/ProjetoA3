// Tela Cadastros: Clientes, Funcionários e Produtos, com dados da API.
import { useCallback, useEffect, useState } from "react";
import { categoriasApi, clientesApi, errorMessage, produtosApi, usuariosApi, type Categoria } from "../../api";
import { Icon } from "../../components/Icon";
import type { ClientRecord, EmployeeRecord, FormValues, ModalMode, ProductRecord, TabId, UserRole } from "../../types";
import {
  clientRequest,
  clientToForm,
  employeeRequest,
  employeeToForm,
  productRequest,
  productToForm,
  toClientRecord,
  toEmployeeRecord,
  toProductRecord,
} from "./mappers";
import { emptyValues, modalCopy, RecordModal } from "./RecordModal";
import { ClientsTable, EmployeesTable, ProductsTable, tabs, type TableActions } from "./tables";

const PAGE_SIZE = 10;
const SEARCH_DELAY_MS = 300;
const permissionDenied = "Você não possui permissão para acessar esta funcionalidade.";

const tabContent = {
  clients: {
    title: "Clientes",
    description: "Gerencie os clientes cadastrados.",
    action: "Novo cliente",
  },
  employees: {
    title: "Funcionários",
    description: "Gerencie os usuários e permissões da equipe.",
    action: "Novo funcionário",
  },
  products: {
    title: "Produtos",
    description: "Gerencie os produtos disponíveis para venda.",
    action: "Novo produto",
  },
};

type Listing = {
  clients: ClientRecord[];
  employees: EmployeeRecord[];
  products: ProductRecord[];
};

type PageInfo = { page: number; total: number; totalPages: number };

const statusApi = {
  clients: clientesApi,
  employees: usuariosApi,
  products: produtosApi,
};

const perTab = <T,>(value: T): Record<TabId, T> => ({ clients: value, employees: value, products: value });

export function RegistrationsPage({
  activeTab: requestedTab,
  currentRole,
  onTabChange,
  onToast,
}: {
  activeTab: TabId;
  currentRole: UserRole;
  onTabChange: (tab: TabId) => void;
  onToast: (message: string) => void;
}) {
  const visibleTabs = currentRole === "owner" ? tabs : currentRole === "manager" ? tabs.filter((tab) => tab.id !== "employees") : tabs.filter((tab) => tab.id === "clients");
  const activeTab = visibleTabs.some((tab) => tab.id === requestedTab) ? requestedTab : visibleTabs[0].id;
  const content = tabContent[activeTab];

  const [searches, setSearches] = useState<Record<TabId, string>>(perTab(""));
  const [terms, setTerms] = useState<Record<TabId, string>>(perTab(""));
  const [pages, setPages] = useState<Record<TabId, number>>(perTab(0));
  const [activeFilters, setActiveFilters] = useState<Record<TabId, string[]>>(perTab<string[]>([]));
  const [listing, setListing] = useState<Listing>({ clients: [], employees: [], products: [] });
  const [pageInfo, setPageInfo] = useState<Record<TabId, PageInfo>>(perTab({ page: 0, total: 0, totalPages: 0 }));
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [categories, setCategories] = useState<Categoria[]>([]);
  const [modal, setModal] = useState<{ type: TabId; mode: ModalMode; id?: number } | null>(null);
  const [menuId, setMenuId] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<{ type: TabId; id: number; name: string; inactive: boolean } | null>(null);
  const [toggling, setToggling] = useState(false);

  const reload = () => setReloadKey((key) => key + 1);

  useEffect(() => {
    categoriasApi.listar().then(setCategories).catch(() => setCategories([]));
  }, []);

  // A busca só vai para a API depois que o usuário para de digitar.
  const search = searches[activeTab];
  useEffect(() => {
    const next = search.trim();
    if (next === terms[activeTab]) return;
    const timeout = window.setTimeout(() => {
      setTerms((current) => ({ ...current, [activeTab]: next }));
      setPages((current) => ({ ...current, [activeTab]: 0 }));
    }, SEARCH_DELAY_MS);
    return () => window.clearTimeout(timeout);
  }, [activeTab, search, terms]);

  const page = pages[activeTab];
  const term = terms[activeTab];
  useEffect(() => {
    let cancelled = false;
    const params = { termo: term || undefined, page, size: PAGE_SIZE };
    setLoading(true);
    setLoadError("");
    const load = async () => {
      if (activeTab === "clients") {
        const result = await clientesApi.listar(params);
        return { result, records: { clients: result.conteudo.map(toClientRecord) } };
      }
      if (activeTab === "employees") {
        const result = await usuariosApi.listar(params);
        return { result, records: { employees: result.conteudo.map(toEmployeeRecord) } };
      }
      const result = await produtosApi.listar(params);
      return { result, records: { products: result.conteudo.map(toProductRecord) } };
    };
    load()
      .then(({ result, records }) => {
        if (cancelled) return;
        setListing((current) => ({ ...current, ...records }));
        setPageInfo((current) => ({ ...current, [activeTab]: { page: result.pagina, total: result.totalElementos, totalPages: result.totalPaginas } }));
      })
      .catch((error) => {
        if (cancelled) return;
        setListing((current) => ({ ...current, [activeTab]: [] }));
        setLoadError(errorMessage(error, "Não foi possível carregar os registros."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeTab, page, term, reloadKey]);

  const canManageTab = (type: TabId) => currentRole === "owner" || (currentRole === "manager" && type !== "employees");
  const canCreateRegistration = canManageTab(activeTab) || (currentRole === "seller" && activeTab === "clients");

  const getFormValues = (): FormValues => {
    if (!modal?.id) return emptyValues[modal?.type || activeTab];
    if (modal.type === "clients") {
      const record = listing.clients.find((item) => item.id === modal.id);
      return record ? clientToForm(record) : emptyValues.clients;
    }
    if (modal.type === "employees") {
      const record = listing.employees.find((item) => item.id === modal.id);
      return record ? employeeToForm(record) : emptyValues.employees;
    }
    const record = listing.products.find((item) => item.id === modal.id);
    return record ? productToForm(record) : emptyValues.products;
  };

  const saveRecord = async (values: FormValues) => {
    if (!modal) return;
    const canSave = canManageTab(modal.type) || (currentRole === "seller" && modal.type === "clients" && modal.mode === "create");
    if (!canSave) {
      onToast(permissionDenied);
      setModal(null);
      return;
    }
    const editId = modal.mode === "edit" ? modal.id : undefined;
    let saved: { id: number; ativo: boolean };
    if (modal.type === "clients") {
      const body = clientRequest(values);
      saved = editId ? await clientesApi.atualizar(editId, body) : await clientesApi.criar(body);
    } else if (modal.type === "employees") {
      const body = employeeRequest(values);
      saved = editId ? await usuariosApi.atualizar(editId, body) : await usuariosApi.criar(body);
    } else {
      const body = productRequest(values);
      saved = editId ? await produtosApi.atualizar(editId, body) : await produtosApi.criar(body);
    }

    const singular = modalCopy[modal.type].singular;
    let message = `${singular.charAt(0).toUpperCase()}${singular.slice(1)} ${editId ? "atualizado" : "cadastrado"} com sucesso!`;
    // O status é alterado por rotas próprias (DELETE desativa, PATCH .../ativar reativa).
    const shouldBeActive = values.status !== "Inativo";
    if (saved.ativo !== shouldBeActive) {
      try {
        const api = statusApi[modal.type];
        await (shouldBeActive ? api.ativar(saved.id) : api.inativar(saved.id));
      } catch (error) {
        message = `Dados salvos, mas o status não foi alterado: ${errorMessage(error)}`;
      }
    }
    setModal(null);
    onToast(message);
    reload();
  };

  const requestToggleStatus = (id: number) => {
    setMenuId(null);
    if (!canManageTab(activeTab)) { onToast(permissionDenied); return; }
    const records: Array<{ id: number; name: string; status: string }> = listing[activeTab];
    const record = records.find((item) => item.id === id);
    if (!record) return;
    setConfirmation({ type: activeTab, id, name: record.name, inactive: record.status === "Inativo" });
  };

  const confirmToggleStatus = async () => {
    if (!confirmation || toggling) return;
    setToggling(true);
    try {
      const api = statusApi[confirmation.type];
      await (confirmation.inactive ? api.ativar(confirmation.id) : api.inativar(confirmation.id));
      onToast(`Registro ${confirmation.inactive ? "ativado" : "desativado"} com sucesso!`);
      reload();
    } catch (error) {
      onToast(errorMessage(error));
    } finally {
      setToggling(false);
      setConfirmation(null);
    }
  };

  const toggleFilter = (filter: string) => setActiveFilters((current) => ({
    ...current,
    [activeTab]: current[activeTab].includes(filter)
      ? current[activeTab].filter((item) => item !== filter)
      : [...current[activeTab], filter],
  }));

  const tableActions: TableActions = {
    menuId,
    onOpen: (mode, id) => {
      setMenuId(null);
      setModal({ type: activeTab, mode, id });
    },
    onMenu: (id) => setMenuId((current) => current === id ? null : id),
    onToggleStatus: requestToggleStatus,
    canEdit: canManageTab(activeTab),
    canManage: canManageTab(activeTab),
  };

  const info = pageInfo[activeTab];
  const pagination = {
    page: info.page,
    size: PAGE_SIZE,
    total: info.total,
    totalPages: info.totalPages,
    onPage: (next: number) => setPages((current) => ({ ...current, [activeTab]: next })),
  };
  const emptyMessage = loading ? "Carregando registros..." : loadError || "Nenhum registro encontrado.";
  const onSearch = (value: string) => setSearches((current) => ({ ...current, [activeTab]: value }));
  const tableProps = { search, activeFilters: activeFilters[activeTab], onSearch, onToggleFilter: toggleFilter, pagination, emptyMessage, ...tableActions };

  const table = activeTab === "clients"
    ? <ClientsTable records={listing.clients} {...tableProps} />
    : activeTab === "employees"
      ? <EmployeesTable records={listing.employees} {...tableProps} />
      : <ProductsTable records={listing.products} {...tableProps} />;

  const closeModal = useCallback(() => setModal(null), []);

  return (
    <>
      <div className="dashboard-content registrations-content">
        <div className="registration-tabs" role="tablist" aria-label="Tipos de cadastro">
          {visibleTabs.map((tab) => (
            <button
              key={tab.id}
              id={`tab-${tab.id}`}
              className={activeTab === tab.id ? "active" : ""}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`panel-${tab.id}`}
              onClick={() => { onTabChange(tab.id); setMenuId(null); }}
            >
              <Icon name={tab.icon} />
              {tab.label}
            </button>
          ))}
        </div>

        <section
          className="dashboard-card management-card"
          id={`panel-${activeTab}`}
          role="tabpanel"
          aria-labelledby={`tab-${activeTab}`}
          aria-busy={loading}
        >
          <div className="management-heading">
            <div>
              <h2>{content.title}</h2>
              <p>{content.description}</p>
            </div>
            {canCreateRegistration && <button className="new-record-button" type="button" onClick={() => setModal({ type: activeTab, mode: "create" })}>
              <Icon name="plus" />
              {content.action}
            </button>}
          </div>
          {table}
        </section>
      </div>
      {modal && <RecordModal key={`${modal.type}-${modal.mode}-${modal.id || "new"}`} type={modal.type} mode={modal.mode} initial={getFormValues()} categories={categories} canEdit={canManageTab(modal.type)} onClose={closeModal} onEdit={() => setModal({ ...modal, mode: "edit" })} onSave={saveRecord} />}
      {confirmation && (
        <div className="confirm-backdrop" role="presentation">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
            <span className="confirm-icon"><Icon name="power" /></span>
            <h2 id="confirm-title">{confirmation.inactive ? "Ativar registro?" : "Desativar registro?"}</h2>
            <p>{confirmation.inactive ? `${confirmation.name} voltará a aparecer como ativo no sistema.` : `${confirmation.name} ficará indisponível para novas operações.`}</p>
            <div><button type="button" onClick={() => setConfirmation(null)}>Cancelar</button><button className="danger-button" type="button" disabled={toggling} onClick={confirmToggleStatus}>{confirmation.inactive ? "Ativar" : "Desativar"}</button></div>
          </section>
        </div>
      )}
    </>
  );
}

