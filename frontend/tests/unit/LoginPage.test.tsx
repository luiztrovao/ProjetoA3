// Tela de login (componente): validação, login correto, senha errada e recuperação de acesso.
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { tokenStorage } from "../../src/api/client";
import { LoginPage } from "../../src/pages/LoginPage";
import { simularApi } from "./api";

const usuario = { id: 3, nome: "Fernanda Lima", username: "fernanda", perfil: "VENDEDOR" };

function abrir() {
  const onLogin = vi.fn();
  render(<LoginPage businessName="Ponto & Sabor" businessLogo="" onLogin={onLogin} />);
  return { onLogin, user: userEvent.setup() };
}

describe("LoginPage", () => {
  it("não chama a API com campos vazios", async () => {
    const fetchFalso = simularApi([]);
    const { user, onLogin } = abrir();

    await user.click(screen.getByRole("button", { name: /Entrar/ }));

    expect(screen.getByRole("alert")).toHaveTextContent("Informe o usuário ou e-mail e a senha.");
    expect(fetchFalso).not.toHaveBeenCalled();
    expect(onLogin).not.toHaveBeenCalled();
  });

  it("entra com usuário e senha corretos e guarda o token", async () => {
    const fetchFalso = simularApi([{ metodo: "POST", caminho: "/api/auth/login", corpo: { token: "jwt-ok", tipo: "Bearer", usuario } }]);
    const { user, onLogin } = abrir();

    await user.type(screen.getByLabelText("Usuário ou e-mail"), "  fernanda ");
    await user.type(screen.getByLabelText("Senha"), "demo123");
    await user.click(screen.getByRole("button", { name: /Entrar/ }));

    expect(onLogin).toHaveBeenCalledWith(usuario);
    expect(JSON.parse(String(fetchFalso.mock.calls[0][1]?.body))).toEqual({ login: "fernanda", senha: "demo123" });
    expect(tokenStorage.get()).toBe("jwt-ok");
    expect(window.localStorage.getItem("ponto-e-sabor:token")).toBeNull(); // sem "Lembrar de mim"
  });

  it("mostra a mensagem da API quando a senha está errada", async () => {
    simularApi([{ metodo: "POST", caminho: "/api/auth/login", status: 401, corpo: { mensagem: "Usuário ou senha inválidos." } }]);
    const { user, onLogin } = abrir();

    await user.type(screen.getByLabelText("Usuário ou e-mail"), "fernanda");
    await user.type(screen.getByLabelText("Senha"), "errada");
    await user.click(screen.getByRole("button", { name: /Entrar/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário ou senha inválidos.");
    expect(onLogin).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: /Entrar/ })).toBeEnabled();
  });

  it("mostra e oculta a senha e orienta quem esqueceu", async () => {
    const { user } = abrir();
    const senha = screen.getByLabelText("Senha");

    await user.click(screen.getByRole("button", { name: "Mostrar senha" }));
    expect(senha).toHaveAttribute("type", "text");
    await user.click(screen.getByRole("button", { name: "Ocultar senha" }));
    expect(senha).toHaveAttribute("type", "password");

    await user.click(screen.getByRole("button", { name: "Esqueci minha senha" }));
    expect(screen.getByText("Para recuperar o acesso, procure o Dono da lanchonete.")).toBeInTheDocument();
  });
});
