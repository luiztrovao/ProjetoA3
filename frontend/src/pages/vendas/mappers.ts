// Conversão dos pedidos da API (/api/vendas) para o formato usado nas telas de Vendas, Pedidos e Relatórios.
import type { Venda } from "../../api";
import { paymentFromCode, statusLabels, type SaleRecord } from "../../types";

export const toSaleRecord = (venda: Venda): SaleRecord => {
  const confirmedAt = new Date(venda.confirmadaEm || venda.dataHora);
  return {
    apiId: venda.id,
    id: venda.numero,
    createdAt: new Date(venda.dataHora),
    confirmedAt,
    seller: venda.vendedor.nome,
    clientId: venda.cliente?.id ?? null,
    clientName: venda.cliente?.nome || "Cliente não identificado",
    items: venda.itens.map((item) => ({
      productId: item.produtoId,
      quantity: item.quantidade,
      unitPrice: item.precoUnitario,
      productName: item.nome,
      productCode: item.codigo,
    })),
    discount: "",
    paymentMethod: paymentFromCode(venda.formaPagamento),
    subtotal: venda.subtotal,
    discountValue: venda.desconto,
    total: venda.total,
    cashReceived: venda.valorRecebido ?? venda.total,
    change: venda.troco ?? 0,
    status: statusLabels[venda.status],
    cancellationReason: venda.motivoCancelamento || undefined,
  };
};

/** Data local no formato aaaa-mm-dd (o que a API espera nos filtros de período). */
export const isoDate = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
