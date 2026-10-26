// Tela Configurações: Dados da Lanchonete, Permissões, Preferências de Vendas e Minha Conta.
import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { ApiError, configuracoesApi, contaApi, errorMessage, type Usuario } from "../../api";
import { BrandMark, Icon, type IconName } from "../../components/Icon";
import { paymentLabels, type EmployeeRecord, type PaymentMethod, type UserRole } from "../../types";
import { maskCep, maskCnpj, maskPhone } from "../../utils/format";
import { toEmployeeRecord } from "../cadastros/mappers";
import {
  accountFieldNames,
  companyFieldNames,
  toCompanySettings,
  toContaRequest,
  toEmpresa,
  toPreferenciasVenda,
  toSalesPreferences,
  type CompanySettings,
  type SalesPreferences,
} from "./mappers";

const permissionRows: Array<{ feature: string; owner: boolean; manager: boolean; seller: boolean }> = [
  { feature: "Dashboard gerencial", owner: true, manager: true, seller: false },
  { feature: "Realizar vendas", owner: true, manager: true, seller: true },
  { feature: "Consultar próprios pedidos", owner: true, manager: true, seller: true },
  { feature: "Consultar todos os pedidos", owner: true, manager: true, seller: false },
  { feature: "Cancelar pedidos", owner: true, manager: true, seller: false },
  { feature: "Cadastrar clientes", owner: true, manager: true, seller: true },
  { feature: "Editar clientes", owner: true, manager: true, seller: false },
  { feature: "Gerenciar produtos", owner: true, manager: true, seller: false },
  { feature: "Gerenciar funcionários", owner: true, manager: false, seller: false },
  { feature: "Consultar relatórios", owner: true, manager: true, seller: false },
  { feature: "Exportar relatórios", owner: true, manager: true, seller: false },
  { feature: "Configurações gerais", owner: true, manager: false, seller: false },
  { feature: "Alterar permissões", owner: true, manager: false, seller: false },
];

function SettingToggle({ checked, disabled, onChange }: { checked: boolean; disabled?: boolean; onChange: (checked: boolean) => void }) {
  return <button type="button" className={`setting-toggle${checked ? " active" : ""}`} role="switch" aria-checked={checked} disabled={disabled} onClick={() => onChange(!checked)}><span /></button>;
}

const FieldError = ({ message }: { message?: string }) => message ? <small className="field-error">{message}</small> : null;

export function SettingsPage({
  currentUser,
  currentRole,
  company,
  preferences,
  onCompanySaved,
  onPreferencesSaved,
  onAccountSaved,
  onDenied,
  onLogout,
  onSuccess,
}: {
  currentUser: EmployeeRecord;
  currentRole: UserRole;
  company: CompanySettings;
  preferences: SalesPreferences;
  onCompanySaved: (settings: CompanySettings) => void;
  onPreferencesSaved: (preferences: SalesPreferences) => void;
  onAccountSaved: (user: Usuario) => void;
  onDenied: () => void;
  onLogout: () => void;
  onSuccess: (message: string) => void;
}) {
  const [activeTab, setActiveTab] = useState<"business" | "permissions" | "sales" | "account">("business");
  const [companyDraft, setCompanyDraft] = useState(company);
  const [companyErrors, setCompanyErrors] = useState<Record<string, string>>({});
  const [companyError, setCompanyError] = useState("");
  const [savingCompany, setSavingCompany] = useState(false);
  const [accountDraft, setAccountDraft] = useState(currentUser);
  const [accountErrors, setAccountErrors] = useState<Record<string, string>>({});
  const [accountError, setAccountError] = useState("");
  const [savingAccount, setSavingAccount] = useState(false);
  const [passwordModal, setPasswordModal] = useState(false);
  const [passwords, setPasswords] = useState({ current: "", next: "", confirm: "" });
  const [passwordError, setPasswordError] = useState("");
  const [savingPassword, setSavingPassword] = useState(false);
  const isOwner = currentRole === "owner";

  useEffect(() => {
    if (!isOwner) setActiveTab("account");
  }, [isOwner]);

  // Minha Conta sempre abre com os dados atuais da API.
  useEffect(() => {
    let cancelled = false;
    contaApi.consultar()
      .then((usuario) => { if (!cancelled) setAccountDraft(toEmployeeRecord(usuario)); })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, []);

  const tabsConfig = [
    { id: "business" as const, label: "Dados da Lanchonete", icon: "registers" as IconName, restricted: !isOwner },
    { id: "permissions" as const, label: "Usuários e Permissões", icon: "shield" as IconName, restricted: !isOwner },
    { id: "sales" as const, label: "Preferências de Vendas", icon: "settings" as IconName, restricted: !isOwner },
    { id: "account" as const, label: "Minha Conta", icon: "users" as IconName, restricted: false },
  ];
  const updateCompany = (field: keyof CompanySettings, value: string) => {
    setCompanyDraft((current) => ({ ...current, [field]: value }));
    setCompanyErrors((current) => ({ ...current, [field]: "" }));
    setCompanyError("");
  };
  const updateAccount = (field: "name" | "email" | "phone" | "username", value: string) => {
    setAccountDraft((current) => ({ ...current, [field]: value }));
    setAccountErrors((current) => ({ ...current, [field]: "" }));
    setAccountError("");
  };
  const handleLogo = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) updateCompany("logo", URL.createObjectURL(file));
  };

  const saveCompany = async (event: FormEvent) => {
    event.preventDefault();
    if (!isOwner) { onDenied(); return; }
    if (savingCompany) return;
    setSavingCompany(true);
    try {
      const saved = await configuracoesApi.atualizarEmpresa(toEmpresa(companyDraft));
      const next = toCompanySettings(saved.empresa);
      // O logo escolhido no computador continua visível nesta sessão, mesmo sem ir para a API.
      if (companyDraft.logo.startsWith("blob:")) next.logo = companyDraft.logo;
      setCompanyDraft(next);
      onCompanySaved(next);
      onSuccess("Dados da lanchonete atualizados com sucesso.");
    } catch (error) {
      if (error instanceof ApiError) setCompanyErrors(error.fieldErrors(companyFieldNames));
      setCompanyError(errorMessage(error));
    } finally {
      setSavingCompany(false);
    }
  };

  // Cada alteração das preferências é salva na hora; se a API recusar, a tela volta ao valor anterior.
  const savePreferences = async (next: SalesPreferences) => {
    if (!isOwner) { onDenied(); return; }
    const previous = preferences;
    onPreferencesSaved(next);
    try {
      const saved = await configuracoesApi.atualizarVendas(toPreferenciasVenda(next));
      onPreferencesSaved(toSalesPreferences(saved.preferenciasVenda));
    } catch (error) {
      onPreferencesSaved(previous);
      onSuccess(errorMessage(error, "Não foi possível salvar as preferências de vendas."));
    }
  };
  const updatePreference = <K extends keyof SalesPreferences>(field: K, value: SalesPreferences[K]) => savePreferences({ ...preferences, [field]: value });

  const saveAccount = async (event: FormEvent) => {
    event.preventDefault();
    if (savingAccount) return;
    setSavingAccount(true);
    try {
      const saved = await contaApi.atualizar(toContaRequest(accountDraft));
      setAccountDraft(toEmployeeRecord(saved));
      onAccountSaved(saved);
      onSuccess("Dados da conta atualizados com sucesso.");
    } catch (error) {
      if (error instanceof ApiError) setAccountErrors(error.fieldErrors(accountFieldNames));
      setAccountError(errorMessage(error));
    } finally {
      setSavingAccount(false);
    }
  };

  const closePasswordModal = () => {
    setPasswordModal(false);
    setPasswords({ current: "", next: "", confirm: "" });
    setPasswordError("");
  };
  const savePassword = async () => {
    if (savingPassword) return;
    if (!passwords.current || passwords.next.length < 6) {
      setPasswordError("Informe a senha atual e uma nova senha com pelo menos 6 caracteres.");
      return;
    }
    if (passwords.next !== passwords.confirm) {
      setPasswordError("A confirmação da nova senha não confere.");
      return;
    }
    setSavingPassword(true);
    try {
      await contaApi.alterarSenha({ senhaAtual: passwords.current, novaSenha: passwords.next, confirmacaoSenha: passwords.confirm });
      closePasswordModal();
      onSuccess("Senha atualizada com sucesso.");
    } catch (error) {
      const fieldMessage = error instanceof ApiError ? error.campos[0]?.mensagem : undefined;
      setPasswordError(fieldMessage || errorMessage(error));
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div className="dashboard-content settings-content">
      <div className="settings-tabs" role="tablist" aria-label="Configurações">
        {tabsConfig.map((tab) => <button key={tab.id} type="button" role="tab" aria-selected={activeTab === tab.id} className={`${activeTab === tab.id ? "active" : ""}${tab.restricted ? " restricted" : ""}`} onClick={() => { if (tab.restricted) onDenied(); else setActiveTab(tab.id); }}><Icon name={tab.icon} />{tab.label}{tab.restricted && <Icon name="lock" />}</button>)}
      </div>

      {activeTab === "business" && isOwner && (
        <section className="dashboard-card settings-panel">
          <div className="settings-panel-heading"><div><span>Identidade do negócio</span><h2>Dados da Lanchonete</h2><p>Informações utilizadas na identificação do estabelecimento.</p></div></div>
          <form onSubmit={saveCompany}>
            {companyError && <div className="form-alert" role="alert">{companyError}</div>}
            <div className="settings-form-grid">
              <label><span>Nome fantasia</span><input value={companyDraft.tradeName} onChange={(event) => updateCompany("tradeName", event.target.value)} required /><FieldError message={companyErrors.tradeName} /></label>
              <label><span>Razão social</span><input value={companyDraft.legalName} onChange={(event) => updateCompany("legalName", event.target.value)} placeholder="Razão social da empresa" /><FieldError message={companyErrors.legalName} /></label>
              <label><span>CNPJ</span><input value={companyDraft.cnpj} onChange={(event) => updateCompany("cnpj", maskCnpj(event.target.value))} placeholder="00.000.000/0000-00" /><FieldError message={companyErrors.cnpj} /></label>
              <label><span>Telefone</span><input value={companyDraft.phone} onChange={(event) => updateCompany("phone", maskPhone(event.target.value))} placeholder="(00) 00000-0000" /><FieldError message={companyErrors.phone} /></label>
              <label className="wide"><span>E-mail</span><input type="email" value={companyDraft.email} onChange={(event) => updateCompany("email", event.target.value)} placeholder="contato@empresa.com" /><FieldError message={companyErrors.email} /></label>
              <label><span>CEP</span><input value={companyDraft.cep} onChange={(event) => updateCompany("cep", maskCep(event.target.value))} placeholder="00000-000" /><FieldError message={companyErrors.cep} /></label>
              <label className="wide"><span>Endereço</span><input value={companyDraft.address} onChange={(event) => updateCompany("address", event.target.value)} placeholder="Rua ou avenida" /><FieldError message={companyErrors.address} /></label>
              <label><span>Número</span><input value={companyDraft.number} onChange={(event) => updateCompany("number", event.target.value)} /><FieldError message={companyErrors.number} /></label>
              <label><span>Bairro</span><input value={companyDraft.district} onChange={(event) => updateCompany("district", event.target.value)} /><FieldError message={companyErrors.district} /></label>
              <label><span>Cidade</span><input value={companyDraft.city} onChange={(event) => updateCompany("city", event.target.value)} /><FieldError message={companyErrors.city} /></label>
              <label><span>Estado</span><input value={companyDraft.state} maxLength={2} onChange={(event) => updateCompany("state", event.target.value.toUpperCase())} placeholder="UF" /><FieldError message={companyErrors.state} /></label>
            </div>
            <div className="business-logo-setting"><div className={companyDraft.logo ? "filled" : ""}>{companyDraft.logo ? <img src={companyDraft.logo} alt="Logo da lanchonete" /> : <BrandMark />}</div><p><strong>Logo da lanchonete</strong><small>PNG ou JPG. Utilizada para identificar o estabelecimento.</small></p><label htmlFor="business-logo"><Icon name="upload" />Selecionar imagem</label><input className="sr-only" id="business-logo" type="file" accept="image/png,image/jpeg" onChange={handleLogo} /></div>
            <footer className="settings-form-footer"><button type="submit" disabled={savingCompany}>{savingCompany ? "Salvando..." : "Salvar alterações"}</button></footer>
          </form>
        </section>
      )}

      {activeTab === "permissions" && isOwner && (
        <section className="dashboard-card settings-panel permissions-panel">
          <div className="settings-panel-heading"><div><span>Controle de acesso</span><h2>Usuários e Permissões</h2><p>Permissões fixas aplicadas aos três perfis do sistema.</p></div><span className="owner-only-badge"><Icon name="shield" />Somente Dono</span></div>
          <div className="role-cards">
            <article><span className="owner"><Icon name="shield" /></span><p><strong>Dono</strong><small>Administrador geral, com acesso completo.</small></p></article>
            <article><span className="manager"><Icon name="team" /></span><p><strong>Gerente</strong><small>Gestão operacional e acompanhamento das vendas.</small></p></article>
            <article><span className="seller"><Icon name="users" /></span><p><strong>Vendedor</strong><small>Atendimento e registro de vendas.</small></p></article>
          </div>
          <div className="permissions-table-scroll"><table className="permissions-table"><thead><tr><th>Funcionalidade</th><th>Dono</th><th>Gerente</th><th>Vendedor</th></tr></thead><tbody>{permissionRows.map((row) => <tr key={row.feature}><td>{row.feature}</td>{(["owner", "manager", "seller"] as const).map((role) => <td key={role}><span className={row[role] ? "allowed" : "denied"}><Icon name={row[role] ? "check" : "close"} />{row[role] ? "Permitido" : "Restrito"}</span></td>)}</tr>)}</tbody></table></div>
          <div className="fixed-permissions-note"><Icon name="lock" /><p><strong>Permissões fixas nesta versão</strong><small>Os perfis não podem ser personalizados individualmente nesta versão.</small></p></div>
        </section>
      )}

      {activeTab === "sales" && isOwner && (
        <section className="dashboard-card settings-panel preferences-panel">
          <div className="settings-panel-heading"><div><span>Operação</span><h2>Preferências de Vendas</h2><p>Defina regras aplicadas ao PDV e à confirmação dos pedidos.</p></div></div>
          <div className="preference-group"><div className="preference-group-title"><span><Icon name="products" /></span><div><h3>Estoque</h3><p>Controle de disponibilidade dos produtos.</p></div></div>
            <div className="preference-row"><p><strong>Ativar controle de estoque</strong><small>Controla quantidades e realiza baixa após a confirmação.</small></p><SettingToggle checked={preferences.stockControl} onChange={(value) => updatePreference("stockControl", value)} /></div>
            <div className="preference-row"><p><strong>Impedir venda sem estoque</strong><small>Bloqueia produtos cuja quantidade esteja zerada.</small></p><SettingToggle checked={preferences.blockWithoutStock} disabled={!preferences.stockControl} onChange={(value) => updatePreference("blockWithoutStock", value)} /></div>
            <div className="preference-row"><p><strong>Alerta de estoque baixo</strong><small>Quantidade utilizada para sinalizar reposição.</small></p><input className="preference-number" type="number" min="0" value={preferences.lowStockThreshold} onChange={(event) => updatePreference("lowStockThreshold", Math.max(0, Number(event.target.value)))} /></div>
          </div>
          <div className="preference-group"><div className="preference-group-title"><span><Icon name="orders" /></span><div><h3>Pedidos</h3><p>Comportamento do pedido durante a operação.</p></div></div>
            <div className="preference-row"><p><strong>Confirmar antes de cancelar</strong><small>Solicita confirmação e motivo para ações irreversíveis.</small></p><SettingToggle checked={preferences.confirmCancellation} onChange={(value) => updatePreference("confirmCancellation", value)} /></div>
            <div className="preference-row"><p><strong>Permitir venda sem cliente identificado</strong><small>Autoriza vendas rápidas de balcão.</small></p><SettingToggle checked={preferences.allowAnonymousCustomer} onChange={(value) => updatePreference("allowAnonymousCustomer", value)} /></div>
            <div className="preference-row"><p><strong>Status inicial após confirmação</strong><small>Aplicado automaticamente a novas vendas.</small></p><select value={preferences.initialOrderStatus} onChange={(event) => updatePreference("initialOrderStatus", event.target.value as SalesPreferences["initialOrderStatus"])}><option>Em preparo</option><option>Concluído</option></select></div>
          </div>
          <div className="preference-group"><div className="preference-group-title"><span><Icon name="credit-card" /></span><div><h3>Pagamentos</h3><p>Formas disponíveis na finalização da venda.</p></div></div>
            {(Object.keys(paymentLabels) as PaymentMethod[]).map((method) => <div className="preference-row" key={method}><p><strong>{paymentLabels[method]}</strong><small>Disponibilizar esta forma na revisão do pedido.</small></p><SettingToggle checked={preferences.paymentMethods[method]} onChange={(value) => savePreferences({ ...preferences, paymentMethods: { ...preferences.paymentMethods, [method]: value } })} /></div>)}
          </div>
        </section>
      )}

      {activeTab === "account" && (
        <section className="dashboard-card settings-panel account-panel">
          <div className="settings-panel-heading"><div><span>Perfil pessoal</span><h2>Minha Conta</h2><p>Atualize seus dados de contato e acesso.</p></div><span className={`role-badge ${currentUser.role.toLowerCase()}`}>{currentUser.role}</span></div>
          <form onSubmit={saveAccount}>
            {accountError && <div className="form-alert" role="alert">{accountError}</div>}
            <div className="account-identity"><span>{accountDraft.initials}</span><p><strong>{accountDraft.name}</strong><small>{accountDraft.email}</small></p></div>
            <div className="settings-form-grid">
              <label><span>Nome</span><input value={accountDraft.name} required onChange={(event) => updateAccount("name", event.target.value)} /><FieldError message={accountErrors.name} /></label>
              <label><span>E-mail</span><input type="email" value={accountDraft.email} onChange={(event) => updateAccount("email", event.target.value)} /><FieldError message={accountErrors.email} /></label>
              <label><span>Telefone</span><input value={accountDraft.phone} onChange={(event) => updateAccount("phone", maskPhone(event.target.value))} /><FieldError message={accountErrors.phone} /></label>
              <label><span>Nome de usuário</span><input value={accountDraft.username} onChange={(event) => updateAccount("username", event.target.value)} /><FieldError message={accountErrors.username} /></label>
              <label className="wide"><span>Perfil de acesso</span><input value={accountDraft.role} readOnly /></label>
            </div>
            <footer className="account-actions"><button type="button" onClick={() => setPasswordModal(true)}><Icon name="lock" />Alterar senha</button><button type="button" className="logout-button" onClick={onLogout}>Sair da conta</button><button type="submit" disabled={savingAccount}>{savingAccount ? "Salvando..." : "Salvar alterações"}</button></footer>
          </form>
        </section>
      )}

      {passwordModal && <div className="modal-backdrop" role="presentation"><section className="confirm-dialog password-dialog" role="dialog" aria-modal="true" aria-labelledby="password-title"><span className="confirm-icon"><Icon name="lock" /></span><h2 id="password-title">Alterar senha</h2><p>Informe a senha atual e escolha uma nova senha de acesso.</p><div className="password-fields"><label>Senha atual<input type="password" autoComplete="current-password" value={passwords.current} onChange={(event) => setPasswords((current) => ({ ...current, current: event.target.value }))} /></label><label>Nova senha<input type="password" autoComplete="new-password" value={passwords.next} onChange={(event) => setPasswords((current) => ({ ...current, next: event.target.value }))} /></label><label>Confirmar nova senha<input type="password" autoComplete="new-password" value={passwords.confirm} onChange={(event) => setPasswords((current) => ({ ...current, confirm: event.target.value }))} /></label></div>{passwordError && <small className="password-error">{passwordError}</small>}<div><button type="button" onClick={closePasswordModal}>Cancelar</button><button className="danger-button" type="button" disabled={savingPassword} onClick={savePassword}>{savingPassword ? "Salvando..." : "Salvar senha"}</button></div></section></div>}
    </div>
  );
}
