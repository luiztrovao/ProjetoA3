import { FormEvent, useState } from "react";
import { authApi, errorMessage, tokenStorage, type Usuario } from "../api";
import { BrandMark, Icon } from "../components/Icon";

export function LoginPage({
  businessName,
  businessLogo,
  notice,
  onLogin,
}: {
  businessName: string;
  businessLogo: string;
  /** Aviso exibido ao voltar para o login (ex.: sessão expirada). */
  notice?: string;
  onLogin: (user: Usuario) => void;
}) {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState("");
  const [recoveryMessage, setRecoveryMessage] = useState(notice || "");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (submitting) return;
    if (!identifier.trim() || !password) {
      setError("Informe o usuário ou e-mail e a senha.");
      return;
    }
    setSubmitting(true);
    setRecoveryMessage("");
    try {
      const response = await authApi.login(identifier.trim(), password);
      tokenStorage.set(response.token, remember);
      onLogin(response.usuario);
    } catch (loginError) {
      setError(errorMessage(loginError, "Não foi possível entrar. Tente novamente."));
      setSubmitting(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-shell">
        <aside className="presentation-panel">
          <div className="presentation-content">
            <div className="brand-lockup"><BrandMark src={businessLogo} /><div><p className="brand-name">{businessName}</p><p className="brand-type">Gestão para lanchonetes</p></div></div>
            <div className="hero-copy"><span className="eyebrow">Gestão simples, sabor em primeiro lugar</span><h1>Seu negócio organizado em um só lugar.</h1><p>Controle vendas, pedidos, produtos e resultados com agilidade para focar no que realmente importa: seus clientes.</p></div>
            <div className="order-illustration" aria-hidden="true">
              <div className="order-card order-card-back" />
              <div className="order-card order-card-front">
                <div className="order-card-head"><span className="mini-brand"><BrandMark src={businessLogo} /></span><div><b>Pedido #0142</b><small>Balcão · Agora</small></div><i /></div>
                <div className="order-line"><span>2× X-Burger</span><strong>R$ 36,00</strong></div>
                <div className="order-line muted"><span>1× Refrigerante</span><strong>R$ 6,00</strong></div>
                <div className="order-total"><span>Total do pedido</span><strong>R$ 42,00</strong></div>
              </div>
              <div className="status-pill"><span />Pedido em preparo</div>
            </div>
          </div>
          <p className="presentation-footer">Ponto &amp; Sabor · Gestão para lanchonetes</p>
        </aside>
        <section className="auth-panel">
          <div className="mobile-brand"><BrandMark src={businessLogo} /><div><p className="brand-name">{businessName}</p><p className="brand-type">Gestão para lanchonetes</p></div></div>
          <form className="login-form" onSubmit={submit}>
            <header><span className="form-kicker">Bem-vindo de volta</span><h2>Acesse sua conta</h2><p>Entre com seu usuário ou e-mail para continuar.</p></header>
            <div className="fields">
              <div className="field"><label htmlFor="login-identifier">Usuário ou e-mail</label><div className="input-wrap"><input id="login-identifier" value={identifier} autoComplete="username" placeholder="Digite seu usuário ou e-mail" onChange={(event) => { setIdentifier(event.target.value); setError(""); }} /></div></div>
              <div className="field"><label htmlFor="login-password">Senha</label><div className="input-wrap"><input id="login-password" type={showPassword ? "text" : "password"} value={password} autoComplete="current-password" placeholder="Digite sua senha" onChange={(event) => { setPassword(event.target.value); setError(""); }} /><button className="password-toggle" type="button" aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"} onClick={() => setShowPassword((value) => !value)}><Icon name="eye" className="eye-icon" /></button></div></div>
            </div>
            <div className="form-options"><label className="checkbox-label"><input type="checkbox" checked={remember} onChange={(event) => setRemember(event.target.checked)} /><span />Lembrar de mim</label><button className="forgot-password-button" type="button" onClick={() => setRecoveryMessage("Para recuperar o acesso, procure o Dono da lanchonete.")}>Esqueci minha senha</button></div>
            {error && <div className="login-feedback error" role="alert">{error}</div>}
            {recoveryMessage && <div className="login-feedback">{recoveryMessage}</div>}
            <button className="submit-button" type="submit" disabled={submitting}>{submitting ? "Entrando..." : "Entrar"} <Icon name="chevron-right" /></button>
            <p className="security-note">Não compartilhe sua senha. O acesso fica registrado no sistema.</p>
          </form>
        </section>
      </section>
    </main>
  );
}
