// Menus suspensos do topo: sino de notificações e menu da conta.
import { useCallback, useEffect, useRef, useState, type ReactNode, type RefObject } from "react";
import { errorMessage, notificacoesApi, type Notificacao, type Notificacoes, type TipoNotificacao } from "../api";
import { Icon, type IconName } from "./Icon";

/** De quanto em quanto tempo o sino consulta a API (1 minuto). */
const INTERVALO_NOTIFICACOES = 60_000;

/** Fecha o menu ao clicar fora dele ou apertar Esc. */
function useDismiss(ref: RefObject<HTMLElement | null>, open: boolean, onClose: () => void) {
  useEffect(() => {
    if (!open) return;
    const onPointer = (event: MouseEvent) => { if (!ref.current?.contains(event.target as Node)) onClose(); };
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    document.addEventListener("mousedown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [ref, open, onClose]);
}

const notificationIcons: Record<TipoNotificacao, IconName> = {
  ESTOQUE_BAIXO: "products",
  PEDIDO_ATRASADO: "clock",
  PEDIDO_CANCELADO: "close",
};

/** "agora", "há 5 min", "há 2 h", "ontem" ou a data. */
export const timeAgo = (iso: string | null, now = new Date()) => {
  if (!iso) return "";
  const date = new Date(iso);
  const minutes = Math.floor((now.getTime() - date.getTime()) / 60_000);
  if (minutes < 1) return "agora";
  if (minutes < 60) return `há ${minutes} min`;
  if (minutes < 24 * 60) return `há ${Math.floor(minutes / 60)} h`;
  if (minutes < 48 * 60) return "ontem";
  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
};

export function NotificationBell({ onSelect }: { onSelect: (notification: Notificacao) => void }) {
  const [open, setOpen] = useState(false);
  const [data, setData] = useState<Notificacoes | null>(null);
  const [error, setError] = useState("");
  const ref = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useDismiss(ref, open, close);

  const load = useCallback(() => {
    notificacoesApi.listar()
      .then((resposta) => { setData(resposta); setError(""); })
      .catch((reason) => setError(errorMessage(reason, "Não foi possível carregar as notificações.")));
  }, []);

  useEffect(() => {
    load();
    const timer = window.setInterval(load, INTERVALO_NOTIFICACOES);
    return () => window.clearInterval(timer);
  }, [load]);

  const toggle = () => {
    if (!open) load();
    setOpen((current) => !current);
  };

  const markAllRead = async () => {
    try {
      await notificacoesApi.marcarLidas();
      setData((current) => current && { naoLidas: 0, itens: current.itens.map((item) => ({ ...item, lida: true })) });
    } catch (reason) {
      setError(errorMessage(reason));
    }
  };

  const unread = data?.naoLidas ?? 0;
  return (
    <div className="header-menu" ref={ref}>
      <button className="icon-button notification-button" type="button" aria-label={unread ? `Notificações: ${unread} não lidas` : "Notificações"} aria-haspopup="true" aria-expanded={open} onClick={toggle}>
        <Icon name="bell" />
        {unread > 0 && <span className="notification-count" aria-hidden="true">{unread > 9 ? "9+" : unread}</span>}
      </button>
      {open && (
        <section className="dropdown-panel notifications-panel" aria-label="Notificações">
          <header>
            <strong>Notificações</strong>
            {unread > 0 && <button type="button" onClick={markAllRead}>Marcar todas como lidas</button>}
          </header>
          {error && <p className="notifications-error" role="alert">{error}</p>}
          {data && data.itens.length === 0 && !error && (
            <p className="notifications-empty"><Icon name="check" />Tudo em dia por aqui.</p>
          )}
          {data && data.itens.length > 0 && (
            <ul>
              {data.itens.map((item) => (
                <li key={item.id}>
                  <button type="button" className={`notification-item ${item.tipo.toLowerCase().replace("_", "-")}${item.lida ? "" : " unread"}`} onClick={() => { setOpen(false); onSelect(item); }}>
                    <span className="notification-icon"><Icon name={notificationIcons[item.tipo]} /></span>
                    <span className="notification-copy">
                      <strong>{item.titulo}</strong>
                      <small>{item.mensagem}</small>
                      <time dateTime={item.data ?? undefined}>{timeAgo(item.data)}</time>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}

export type AccountAction = "account" | "password" | "business" | "logout";

/** Menu da conta, aberto pela seta do topo ou pelo "⋯" da barra lateral. */
export function AccountMenu({
  buttonClassName,
  label,
  placement = "down",
  isOwner,
  onAction,
  children,
}: {
  buttonClassName: string;
  label: string;
  placement?: "down" | "up";
  isOwner: boolean;
  onAction: (action: AccountAction) => void;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useDismiss(ref, open, close);

  const items: Array<{ action: AccountAction; icon: IconName; label: string }> = [
    { action: "account", icon: "users", label: "Minha conta" },
    { action: "password", icon: "lock", label: "Alterar senha" },
    ...(isOwner ? [{ action: "business" as const, icon: "settings" as const, label: "Dados da lanchonete" }] : []),
  ];
  const choose = (action: AccountAction) => { setOpen(false); onAction(action); };

  return (
    <div className={`header-menu account-menu ${placement}`} ref={ref}>
      <button className={buttonClassName} type="button" aria-label={label} aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
        {children}
      </button>
      {open && (
        <div className="dropdown-panel account-panel" role="menu">
          {items.map((item) => (
            <button key={item.action} type="button" role="menuitem" onClick={() => choose(item.action)}><Icon name={item.icon} />{item.label}</button>
          ))}
          <hr />
          <button type="button" role="menuitem" className="logout" onClick={() => choose("logout")}><Icon name="power" />Sair</button>
        </div>
      )}
    </div>
  );
}
