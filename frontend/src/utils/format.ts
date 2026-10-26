// Máscaras e formatação de valores exibidos nas telas.

export const getInitials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

export const onlyDigits = (value: string) => value.replace(/\D/g, "");
export const maskCpf = (value: string) =>
  onlyDigits(value).slice(0, 11).replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d{1,2})$/, "$1-$2");
export const maskPhone = (value: string) => {
  const digits = onlyDigits(value).slice(0, 11);
  return digits.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{5})(\d{4})$/, "$1-$2").replace(/(\d{4})(\d{4})$/, "$1-$2");
};
export const maskCep = (value: string) => onlyDigits(value).slice(0, 8).replace(/(\d{5})(\d)/, "$1-$2");
export const maskCnpj = (value: string) =>
  onlyDigits(value).slice(0, 14).replace(/^(\d{2})(\d)/, "$1.$2").replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3").replace(/\.(\d{3})(\d)/, ".$1/$2").replace(/(\d{4})(\d)/, "$1-$2");
export const maskCurrency = (value: string) => {
  const cents = onlyDigits(value);
  if (!cents) return "";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(cents) / 100);
};

export const parseCurrency = (value: string) => Number(value.replace(/[^\d]/g, "")) / 100;
export const formatCurrency = (value: number) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

/** "2024-05-12T10:00:00" → "12/05/2024". */
export const formatDate = (iso: string | null | undefined) => {
  if (!iso) return "";
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleDateString("pt-BR");
};

/** Último acesso no formato do protótipo: "Hoje, 19:48", "Ontem, 21:10" ou "12/05/2024, 08:30". */
export const formatLastAccess = (iso: string | null | undefined) => {
  if (!iso) return "Ainda não acessou";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Ainda não acessou";
  const time = date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return `Hoje, ${time}`;
  if (date.toDateString() === yesterday.toDateString()) return `Ontem, ${time}`;
  return `${date.toLocaleDateString("pt-BR")}, ${time}`;
};

/** Campo opcional: texto vazio vira `null` para a API. */
export const optional = (value: string | undefined) => {
  const trimmed = (value || "").trim();
  return trimmed ? trimmed : null;
};

/** Campo com máscara: envia só os dígitos, ou `null` se vazio. */
export const optionalDigits = (value: string | undefined) => optional(onlyDigits(value || ""));
