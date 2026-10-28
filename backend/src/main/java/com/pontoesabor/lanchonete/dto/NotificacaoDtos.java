package com.pontoesabor.lanchonete.dto;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs do sino de notificações. */
public final class NotificacaoDtos {

    private NotificacaoDtos() {
    }

    public enum TipoNotificacao {
        /** Produto ativo com estoque igual ou abaixo do mínimo (Dono e Gerente). */
        ESTOQUE_BAIXO,
        /** Pedido "em preparo" além do tempo configurado (todos; o Vendedor só os dele). */
        PEDIDO_ATRASADO,
        /** Pedido do Vendedor cancelado nos últimos 7 dias. */
        PEDIDO_CANCELADO
    }

    /**
     * Um aviso.
     *
     * @param id          identificador estável (ex.: "estoque-5", "atraso-123")
     * @param referenciaId id do produto ou do pedido, para abrir a tela certa ao clicar
     * @param referencia  código do produto ou número do pedido (ex.: "REF001", "#PV-000123")
     */
    public record Notificacao(
            String id, TipoNotificacao tipo, String titulo, String mensagem, LocalDateTime data, boolean lida,
            Long referenciaId, String referencia) {
    }

    public record NotificacoesResponse(long naoLidas, List<Notificacao> itens) {
    }
}
