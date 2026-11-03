// Períodos dos Relatórios e tempo das notificações (unitário, com a data do sistema fixa).
import { beforeEach, describe, expect, it, vi } from "vitest";
import { timeAgo } from "../../src/components/HeaderMenus";
import { reportRange } from "../../src/pages/relatorios/ReportsPage";

describe("reportRange", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 3, 15, 0)); // 03/11/2026
  });

  it.each([
    ["today", "2026-11-03", "2026-11-03"],
    ["7days", "2026-10-28", "2026-11-03"], // hoje + 6 dias para trás
    ["30days", "2026-10-05", "2026-11-03"], // hoje + 29 dias para trás
    ["month", "2026-11-01", "2026-11-03"],
  ] as const)("%s vai de %s a %s", (periodo, inicio, fim) => {
    expect(reportRange(periodo, "", "")).toEqual({ inicio, fim });
  });

  it("período personalizado usa as datas escolhidas", () => {
    expect(reportRange("custom", "2026-01-01", "2026-06-30")).toEqual({ inicio: "2026-01-01", fim: "2026-06-30" });
  });

  it("período personalizado sem início volta 30 dias a partir do fim", () => {
    expect(reportRange("custom", "", "2026-03-31")).toEqual({ inicio: "2026-03-02", fim: "2026-03-31" });
  });
});

describe("timeAgo", () => {
  const agora = new Date(2026, 10, 3, 15, 0);
  const antes = (minutos: number) => new Date(agora.getTime() - minutos * 60_000).toISOString();

  it.each([
    [0, "agora"],
    [1, "há 1 min"],
    [59, "há 59 min"], // último valor em minutos
    [60, "há 1 h"], // primeiro valor em horas
    [24 * 60 - 1, "há 23 h"],
    [24 * 60, "ontem"],
  ])("%i minutos → %s", (minutos, esperado) => {
    expect(timeAgo(antes(minutos), agora)).toBe(esperado);
  });

  it("depois de dois dias mostra a data, e sem data não mostra nada", () => {
    expect(timeAgo(antes(48 * 60), agora)).toBe("01/11");
    expect(timeAgo(null, agora)).toBe("");
  });
});
