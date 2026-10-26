package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.ItemVenda;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs das telas Vendas (PDV) e Pedidos. */
public final class VendaDtos {

    private VendaDtos() {
    }

    /** Confirmação de uma venda montada no PDV. */
    public record VendaRequest(
            /** Opcional se as configurações permitirem venda sem cliente. */
            Long clienteId,

            @NotEmpty(message = "O pedido não possui produtos para confirmar.")
            List<@Valid ItemRequest> itens,

            @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo.")
            @Digits(integer = 8, fraction = 2, message = "O desconto deve ter no máximo 2 casas decimais.")
            BigDecimal desconto,

            @NotNull(message = "Selecione uma forma de pagamento para continuar.")
            FormaPagamento formaPagamento,

            /** Só para dinheiro: valor entregue pelo cliente. */
            @Digits(integer = 8, fraction = 2, message = "O valor recebido deve ter no máximo 2 casas decimais.")
            BigDecimal valorRecebido,

            @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
            String observacao) {

        public VendaRequest {
            desconto = desconto == null ? BigDecimal.ZERO : desconto;
            observacao = Textos.limpar(observacao);
        }
    }

    public record ItemRequest(
            @NotNull(message = "Informe o produto.")
            Long produtoId,

            @NotNull(message = "Informe a quantidade.")
            @Min(value = 1, message = "A quantidade deve ser maior que zero.")
            @Max(value = 999, message = "A quantidade deve ser no máximo 999.")
            Integer quantidade) {
    }

    /** Cancelamento: o motivo é obrigatório (modal "Cancelar pedido" do protótipo). */
    public record CancelamentoRequest(
            @NotBlank(message = "Informe o motivo do cancelamento.")
            @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
            String motivo) {
    }

    public record PessoaResumo(Long id, String nome) {
    }

    public record ItemResponse(
            Long produtoId, String codigo, String nome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {

        static ItemResponse de(ItemVenda item) {
            return new ItemResponse(item.getProduto().getId(), item.getProduto().getCodigo(), item.getProduto().getNome(),
                    item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal());
        }
    }

    public record VendaResponse(
            Long id,
            String numero,
            StatusVenda status,
            LocalDateTime dataHora,
            LocalDateTime confirmadaEm,
            PessoaResumo cliente,
            PessoaResumo vendedor,
            FormaPagamento formaPagamento,
            List<ItemResponse> itens,
            int quantidadeItens,
            BigDecimal subtotal,
            BigDecimal desconto,
            BigDecimal total,
            BigDecimal valorRecebido,
            BigDecimal troco,
            String observacao,
            LocalDateTime canceladaEm,
            String motivoCancelamento) {

        public static VendaResponse de(Venda venda) {
            var cliente = venda.getCliente() == null
                    ? null
                    : new PessoaResumo(venda.getCliente().getId(), venda.getCliente().getNome());
            var vendedor = new PessoaResumo(venda.getUsuario().getId(), venda.getUsuario().getNome());
            List<ItemResponse> itens = venda.getItens().stream().map(ItemResponse::de).toList();
            int quantidadeItens = venda.getItens().stream().mapToInt(ItemVenda::getQuantidade).sum();
            return new VendaResponse(venda.getId(), venda.getNumero(), venda.getStatus(), venda.getDataHora(),
                    venda.getConfirmadaEm(), cliente, vendedor, venda.getFormaPagamento(), itens, quantidadeItens,
                    venda.getSubtotal(), venda.getDesconto(), venda.getTotal(), venda.getValorRecebido(), venda.getTroco(),
                    venda.getObservacao(), venda.getCanceladaEm(), venda.getMotivoCancelamento());
        }
    }
}
