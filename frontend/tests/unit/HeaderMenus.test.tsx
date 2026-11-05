// Sino de notificações e menu da conta (componente), com a API simulada.
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { Notificacao } from "../../src/api";
import { AccountMenu, NotificationBell } from "../../src/components/HeaderMenus";
import { simularApi } from "./api";

const aviso = (id: string, lida: boolean, tipo: Notificacao["tipo"] = "ESTOQUE_BAIXO"): Notificacao => ({
  id, tipo, titulo: tipo === "ESTOQUE_BAIXO" ? "Estoque baixo" : "Pedido atrasado", mensagem: `Mensagem ${id}`,
  data: new Date().toISOString(), lida, referenciaId: 1, referencia: "#PV-000001",
});

describe("NotificationBell", () => {
  it("mostra o contador, abre a lista e leva à tela do aviso clicado", async () => {
    simularApi([{ caminho: "/api/notificacoes", corpo: { naoLidas: 2, itens: [aviso("a", false), aviso("b", false, "PEDIDO_ATRASADO"), aviso("c", true)] } }]);
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<NotificationBell onSelect={onSelect} />);

    const sino = await screen.findByRole("button", { name: "Notificações: 2 não lidas" });
    expect(sino).toHaveTextContent("2");

    await user.click(sino);
    const painel = screen.getByRole("region", { name: "Notificações" });
    expect(within(painel).getAllByRole("listitem")).toHaveLength(3);

    await user.click(within(painel).getByText("Mensagem b"));
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: "b", tipo: "PEDIDO_ATRASADO" }));
    expect(screen.queryByRole("region", { name: "Notificações" })).not.toBeInTheDocument();
  });

  it("acima de 9 avisos o contador mostra 9+", async () => {
    simularApi([{ caminho: "/api/notificacoes", corpo: { naoLidas: 12, itens: [] } }]);
    render(<NotificationBell onSelect={vi.fn()} />);
    expect(await screen.findByRole("button", { name: "Notificações: 12 não lidas" })).toHaveTextContent("9+");
  });

  it("'Marcar todas como lidas' zera o contador", async () => {
    const fetchFalso = simularApi([
      { caminho: "/api/notificacoes", corpo: { naoLidas: 1, itens: [aviso("a", false)] } },
      { metodo: "POST", caminho: "/api/notificacoes/lidas", status: 204 },
    ]);
    const user = userEvent.setup();
    render(<NotificationBell onSelect={vi.fn()} />);

    await user.click(await screen.findByRole("button", { name: "Notificações: 1 não lidas" }));
    await user.click(screen.getByRole("button", { name: "Marcar todas como lidas" }));

    expect(fetchFalso).toHaveBeenCalledWith("http://api.test/api/notificacoes/lidas", expect.objectContaining({ method: "POST" }));
    expect(screen.getByRole("button", { name: "Notificações" })).not.toHaveTextContent("1");
    expect(screen.queryByRole("button", { name: "Marcar todas como lidas" })).not.toBeInTheDocument();
  });

  it("sem avisos mostra 'Tudo em dia' e, com a API fora do ar, a mensagem de erro", async () => {
    simularApi([{ caminho: "/api/notificacoes", corpo: { naoLidas: 0, itens: [] } }]);
    const user = userEvent.setup();
    const { unmount } = render(<NotificationBell onSelect={vi.fn()} />);
    await user.click(await screen.findByRole("button", { name: "Notificações" }));
    expect(await screen.findByText("Tudo em dia por aqui.")).toBeInTheDocument();
    unmount();

    simularApi([{ caminho: "/api/notificacoes", status: 500 }]);
    render(<NotificationBell onSelect={vi.fn()} />);
    await user.click(screen.getByRole("button", { name: "Notificações" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível concluir a operação");
  });
});

describe("AccountMenu", () => {
  it.each([
    [true, ["Minha conta", "Alterar senha", "Dados da lanchonete", "Sair"]],
    [false, ["Minha conta", "Alterar senha", "Sair"]], // Gerente e Vendedor não veem os dados da lanchonete
  ])("Dono = %s mostra %j", async (isOwner, opcoes) => {
    const onAction = vi.fn();
    const user = userEvent.setup();
    render(<AccountMenu buttonClassName="user-menu" label="Abrir menu do usuário" isOwner={isOwner} onAction={onAction}>Conta</AccountMenu>);

    await user.click(screen.getByRole("button", { name: "Abrir menu do usuário" }));
    expect(screen.getAllByRole("menuitem").map((item) => item.textContent)).toEqual(opcoes);

    await user.click(screen.getByRole("menuitem", { name: "Sair" }));
    expect(onAction).toHaveBeenCalledWith("logout");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("conta compartilhada do Visitante só tem a opção Sair", async () => {
    const user = userEvent.setup();
    render(<AccountMenu buttonClassName="user-menu" label="Abrir menu do usuário" isOwner={false} onlyLogout onAction={vi.fn()}>Conta</AccountMenu>);

    await user.click(screen.getByRole("button", { name: "Abrir menu do usuário" }));

    expect(screen.getAllByRole("menuitem").map((item) => item.textContent)).toEqual(["Sair"]);
  });

  it("fecha com Esc e com clique fora", async () => {
    const user = userEvent.setup();
    render(<div><p>fora</p><AccountMenu buttonClassName="x" label="Conta" isOwner onAction={vi.fn()}>Conta</AccountMenu></div>);

    await user.click(screen.getByRole("button", { name: "Conta" }));
    await user.keyboard("{Escape}");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Conta" }));
    await user.click(screen.getByText("fora"));
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });
});
