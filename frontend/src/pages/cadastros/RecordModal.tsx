// Formulário (modal) de cadastro, visualização e edição de Clientes, Funcionários e Produtos.
import { ChangeEvent, FormEvent, ReactNode, useEffect, useState } from "react";
import { ApiError, errorMessage, type Categoria } from "../../api";
import { Icon } from "../../components/Icon";
import type { FormValues, ModalMode, TabId } from "../../types";
import { maskCep, maskCpf, maskCurrency, maskPhone, onlyDigits } from "../../utils/format";
import { apiFieldNames } from "./mappers";

export const emptyValues: Record<TabId, FormValues> = {
  clients: { name: "", cpf: "", phone: "", email: "", cep: "", street: "", number: "", complement: "", district: "", city: "", state: "", notes: "", status: "Ativo" },
  employees: { name: "", cpf: "", phone: "", email: "", username: "", password: "", confirmPassword: "", role: "", status: "Ativo" },
  products: { name: "", code: "", categoryId: "", description: "", price: "", stock: "0", minimumStock: "", image: "", imageName: "", status: "Ativo" },
};

export const modalCopy = {
  clients: { singular: "cliente", create: "Novo cliente", description: "Cadastre os dados pessoais, contato e endereço do cliente." },
  employees: { singular: "funcionário", create: "Novo funcionário", description: "Cadastre os dados e defina o acesso deste integrante da equipe." },
  products: { singular: "produto", create: "Novo produto", description: "Cadastre as informações comerciais e de estoque do produto." },
};

function FormField({
  label,
  name,
  required,
  error,
  wide,
  children,
}: {
  label: string;
  name: string;
  required?: boolean;
  error?: string;
  wide?: boolean;
  children: ReactNode;
}) {
  return (
    <div className={`modal-field${wide ? " wide" : ""}${error ? " has-error" : ""}`}>
      <label htmlFor={`field-${name}`}>{label}{required && <span aria-hidden="true"> *</span>}</label>
      {children}
      {error && <small className="field-error">{error}</small>}
    </div>
  );
}

function StatusSelector({ value, disabled, onChange }: { value: string; disabled: boolean; onChange: (value: string) => void }) {
  return (
    <div className="status-selector" aria-label="Status">
      {["Ativo", "Inativo"].map((status) => (
        <button key={status} type="button" className={value === status ? "active" : ""} disabled={disabled} onClick={() => onChange(status)}>
          <i />{status}
        </button>
      ))}
    </div>
  );
}

export function RecordModal({
  type,
  mode,
  initial,
  categories,
  onClose,
  onEdit,
  onSave,
  canEdit = true,
}: {
  type: TabId;
  mode: ModalMode;
  initial: FormValues;
  categories: Categoria[];
  onClose: () => void;
  onEdit: () => void;
  /** Salva na API; em caso de erro, os erros de validação aparecem nos campos. */
  onSave: (values: FormValues) => Promise<void>;
  canEdit?: boolean;
}) {
  const [values, setValues] = useState<FormValues>({ ...emptyValues[type], ...initial });
  const [errors, setErrors] = useState<FormValues>({});
  const [showPassword, setShowPassword] = useState(false);
  const [resetPassword, setResetPassword] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [saving, setSaving] = useState(false);
  const readOnly = mode === "view";
  const copy = modalCopy[type];
  const title = mode === "create" ? copy.create : mode === "edit" ? `Editar ${copy.singular}` : `Detalhes do ${copy.singular}`;

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKey);
    };
  }, [onClose]);

  const update = (name: string, value: string) => {
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: "" }));
    setSubmitError("");
  };

  const validate = () => {
    const next: FormValues = {};
    const required = type === "clients"
      ? ["name"]
      : type === "employees"
        ? ["name", "username", "role"]
        : ["name", "code", "categoryId", "price"];
    required.forEach((field) => {
      if (!values[field]?.trim()) next[field] = "Este campo é obrigatório.";
    });
    if (values.cpf && onlyDigits(values.cpf).length !== 11) next.cpf = "Informe um CPF válido.";
    if (values.phone && ![10, 11].includes(onlyDigits(values.phone).length)) next.phone = "Informe um telefone válido.";
    if (values.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) next.email = "Informe um e-mail válido.";
    const passwordRequired = type === "employees" && (mode === "create" || resetPassword);
    if (passwordRequired && !values.password) next.password = "Informe uma senha.";
    if (passwordRequired && values.password.length < 6) next.password = "Use pelo menos 6 caracteres.";
    if (passwordRequired && values.password !== values.confirmPassword) next.confirmPassword = "As senhas não são iguais.";
    if (type === "products" && Number(values.stock) < 0) next.stock = "O estoque não pode ser negativo.";
    if (type === "products" && Number(values.minimumStock) < 0) next.minimumStock = "O estoque mínimo não pode ser negativo.";
    setErrors(next);
    setSubmitError("");
    return Object.keys(next).length === 0;
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (readOnly || saving || !validate()) return;
    setSaving(true);
    try {
      await onSave(values);
    } catch (error) {
      if (error instanceof ApiError) setErrors(error.fieldErrors(apiFieldNames[type]));
      setSubmitError(errorMessage(error));
      setSaving(false);
    }
  };
  const hasErrors = Object.values(errors).some(Boolean);

  const input = (name: string, placeholder = "", typeName = "text", mask?: (value: string) => string) => (
    <input id={`field-${name}`} name={name} type={typeName} value={values[name] || ""} placeholder={placeholder} readOnly={readOnly} aria-invalid={Boolean(errors[name])} onChange={(event) => update(name, mask ? mask(event.target.value) : event.target.value)} />
  );

  const renderClient = () => (
    <>
      <fieldset className="form-section">
        <legend>Dados pessoais</legend>
        <div className="modal-form-grid">
          <FormField label="Nome completo" name="name" required error={errors.name} wide>{input("name", "Ex.: João da Silva")}</FormField>
          <FormField label="CPF" name="cpf" error={errors.cpf}>{input("cpf", "000.000.000-00", "text", maskCpf)}</FormField>
          <FormField label="Status" name="status"><StatusSelector value={values.status} disabled={readOnly} onChange={(value) => update("status", value)} /></FormField>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend>Contato</legend>
        <div className="modal-form-grid">
          <FormField label="Telefone" name="phone" error={errors.phone}>{input("phone", "(00) 00000-0000", "tel", maskPhone)}</FormField>
          <FormField label="E-mail" name="email" error={errors.email}>{input("email", "cliente@email.com", "email")}</FormField>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend>Endereço</legend>
        <div className="modal-form-grid address-grid">
          <FormField label="CEP" name="cep" error={errors.cep}>{input("cep", "00000-000", "text", maskCep)}</FormField>
          <FormField label="Rua" name="street" error={errors.street} wide>{input("street", "Nome da rua")}</FormField>
          <FormField label="Número" name="number" error={errors.number}>{input("number", "Nº")}</FormField>
          <FormField label="Complemento" name="complement" error={errors.complement}>{input("complement", "Apto., bloco...")}</FormField>
          <FormField label="Bairro" name="district" error={errors.district}>{input("district", "Bairro")}</FormField>
          <FormField label="Cidade" name="city" error={errors.city}>{input("city", "Cidade")}</FormField>
          <FormField label="Estado" name="state" error={errors.state}>{input("state", "UF")}</FormField>
          <FormField label="Observações" name="notes" error={errors.notes} wide>
            <textarea id="field-notes" value={values.notes} placeholder="Informações adicionais sobre o cliente" readOnly={readOnly} onChange={(event) => update("notes", event.target.value)} />
          </FormField>
        </div>
      </fieldset>
    </>
  );

  const passwordFieldsVisible = mode === "create" || resetPassword;
  const renderEmployee = () => (
    <>
      <fieldset className="form-section">
        <legend>Dados pessoais</legend>
        <div className="modal-form-grid">
          <FormField label="Nome completo" name="name" required error={errors.name} wide>{input("name", "Ex.: Maria da Silva")}</FormField>
          <FormField label="CPF" name="cpf" error={errors.cpf}>{input("cpf", "000.000.000-00", "text", maskCpf)}</FormField>
          <FormField label="Telefone" name="phone" error={errors.phone}>{input("phone", "(00) 00000-0000", "tel", maskPhone)}</FormField>
          <FormField label="E-mail" name="email" error={errors.email} wide>{input("email", "nome@pontoesabor.com", "email")}</FormField>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend>Acesso ao sistema</legend>
        <div className="modal-form-grid">
          <FormField label="Nome de usuário" name="username" required error={errors.username}>{input("username", "nome.sobrenome")}</FormField>
          <FormField label="Perfil de acesso" name="role" required error={errors.role}>
            <select id="field-role" value={values.role} disabled={readOnly} onChange={(event) => update("role", event.target.value)}>
              <option value="">Selecione um perfil</option>
              <option>Dono</option><option>Gerente</option><option>Vendedor</option>
            </select>
          </FormField>
          {mode === "edit" && !resetPassword && (
            <div className="reset-password-card wide">
              <span><Icon name="lock" /></span>
              <p><strong>Senha de acesso</strong><small>A senha atual permanece protegida e não é exibida.</small></p>
              <button type="button" onClick={() => setResetPassword(true)}>Redefinir senha</button>
            </div>
          )}
          {passwordFieldsVisible && (
            <>
              <FormField label={mode === "create" ? "Senha" : "Nova senha"} name="password" required error={errors.password}>
                <div className="modal-password">{input("password", "Mínimo de 6 caracteres", showPassword ? "text" : "password")}<button type="button" aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"} onClick={() => setShowPassword((value) => !value)}><Icon name="eye" /></button></div>
              </FormField>
              <FormField label="Confirmar senha" name="confirmPassword" required error={errors.confirmPassword}>
                {input("confirmPassword", "Repita a senha", showPassword ? "text" : "password")}
              </FormField>
            </>
          )}
          <FormField label="Status" name="status" wide><StatusSelector value={values.status} disabled={readOnly} onChange={(value) => update("status", value)} /></FormField>
        </div>
        <p className="permission-note"><Icon name="shield" /> Apenas o Dono pode atribuir o perfil Dono e alterar permissões administrativas restritas.</p>
      </fieldset>
    </>
  );

  const handleImage = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    update("image", URL.createObjectURL(file));
    update("imageName", file.name);
  };
  const renderProduct = () => (
    <>
      <fieldset className="form-section">
        <legend>Informações do produto</legend>
        <div className="modal-form-grid">
          <FormField label="Nome do produto" name="name" required error={errors.name}>{input("name", "Ex.: X-Bacon")}</FormField>
          <FormField label="Código do produto" name="code" required error={errors.code}>{input("code", "Ex.: XBC001")}</FormField>
          <FormField label="Categoria" name="categoryId" required error={errors.categoryId}>
            <select id="field-categoryId" value={values.categoryId} disabled={readOnly} onChange={(event) => update("categoryId", event.target.value)}>
              <option value="">Selecione uma categoria</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.nome}</option>)}
            </select>
          </FormField>
          <FormField label="Status" name="status"><StatusSelector value={values.status} disabled={readOnly} onChange={(value) => update("status", value)} /></FormField>
          <FormField label="Descrição" name="description" error={errors.description} wide>
            <textarea id="field-description" value={values.description} placeholder="Ingredientes, tamanho ou outras informações" readOnly={readOnly} onChange={(event) => update("description", event.target.value)} />
          </FormField>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend>Preço e estoque</legend>
        <div className="modal-form-grid three-columns">
          <FormField label="Preço de venda" name="price" required error={errors.price}>{input("price", "R$ 0,00", "text", maskCurrency)}</FormField>
          <FormField label="Quantidade em estoque" name="stock" error={errors.stock}>{input("stock", "0", "number")}</FormField>
          <FormField label="Estoque mínimo" name="minimumStock" error={errors.minimumStock}>{input("minimumStock", "Padrão das configurações", "number")}</FormField>
        </div>
      </fieldset>
      <fieldset className="form-section">
        <legend>Imagem</legend>
        <div className="image-picker">
          <div className={`image-preview${values.image ? " filled" : ""}`}>
            {values.image ? <img src={values.image} alt="Prévia do produto" /> : <Icon name="products" />}
          </div>
          <div>
            <strong>{values.imageName || (values.image ? "Imagem do produto" : "Adicione uma imagem do produto")}</strong>
            <small>PNG ou JPG. A imagem será usada na identificação do produto.</small>
            {!readOnly && <label className="upload-button" htmlFor="field-image"><Icon name="upload" />Selecionar imagem</label>}
            <input className="sr-only" id="field-image" type="file" accept="image/png,image/jpeg" disabled={readOnly} onChange={handleImage} />
          </div>
        </div>
      </fieldset>
    </>
  );

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="record-modal" role="dialog" aria-modal="true" aria-labelledby="record-modal-title">
        <header className="modal-header">
          <div><span className="modal-kicker">{mode === "view" ? "Visualização" : mode === "edit" ? "Edição" : "Novo cadastro"}</span><h2 id="record-modal-title">{title}</h2><p>{copy.description}</p></div>
          <button type="button" aria-label="Fechar formulário" onClick={onClose}><Icon name="close" /></button>
        </header>
        <form onSubmit={submit}>
          <div className="modal-body">
            {(submitError || hasErrors) && <div className="form-alert" role="alert">{submitError || "Preencha os campos obrigatórios para continuar."}</div>}
            {type === "clients" ? renderClient() : type === "employees" ? renderEmployee() : renderProduct()}
          </div>
          <footer className="modal-footer">
            <button className="secondary-modal-button" type="button" onClick={onClose}>{readOnly ? "Fechar" : "Cancelar"}</button>
            {readOnly ? canEdit && <button className="primary-modal-button" type="button" onClick={onEdit}><Icon name="edit" />Editar</button> : <button className="primary-modal-button" type="submit" disabled={saving}>{saving ? "Salvando..." : mode === "edit" ? "Salvar alterações" : `Salvar ${copy.singular}`}</button>}
          </footer>
        </form>
      </section>
    </div>
  );
}
