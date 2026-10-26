package com.pontoesabor.lanchonete.config;

import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Gera um histórico de vendas fictício e realista para a apresentação: picos no almoço e no jantar,
 * sexta e sábado mais fortes, dezembro e julho em alta e crescimento leve mês a mês.
 * Usa uma semente fixa: os mesmos parâmetros geram sempre os mesmos dados.
 * As vendas passadas não mexem no estoque atual (o histórico é gravado com o controle de estoque desligado).
 */
@Component
public class GeradorVendasDemo {

    /** Semente fixa: a apresentação mostra sempre os mesmos números. */
    static final long SEMENTE = 2026L;

    /** Média de vendas num dia "normal" (multiplicada pelos pesos de dia, mês e tendência). */
    private static final double VENDAS_POR_DIA = 20;

    /** Peso de cada hora de funcionamento (10h às 22h): picos no almoço e no jantar. */
    private static final Map<Integer, Integer> PESO_HORA = Map.ofEntries(
            Map.entry(10, 2), Map.entry(11, 8), Map.entry(12, 15), Map.entry(13, 11), Map.entry(14, 5),
            Map.entry(15, 3), Map.entry(16, 3), Map.entry(17, 5), Map.entry(18, 9), Map.entry(19, 15),
            Map.entry(20, 13), Map.entry(21, 8), Map.entry(22, 3));

    /** Segunda a domingo: o fim de semana vende mais. */
    private static final double[] PESO_DIA_SEMANA = {0.70, 0.75, 0.85, 0.95, 1.35, 1.55, 1.15};

    /** Janeiro a dezembro: férias de julho e festas de fim de ano em alta. */
    private static final double[] PESO_MES = {0.90, 0.85, 0.95, 0.95, 1.00, 1.05, 1.18, 0.95, 1.00, 1.05, 1.10, 1.30};

    private static final String[] MOTIVOS_CANCELAMENTO = {
        "Cliente desistiu do pedido.", "Pedido registrado em duplicidade.", "Produto em falta na cozinha.",
        "Cliente pediu para trocar a forma de pagamento.", "Demora na entrega ao balcão."
    };

    private final EntityManager entityManager;

    public GeradorVendasDemo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Grava vendas de {@code inicio} até {@code agora} (as de hoje só até a hora atual).
     *
     * @param produtos  cardápio, com o peso de popularidade de cada item (mesma ordem de {@code pesos})
     * @return quantidade de vendas geradas
     */
    public int gerar(LocalDate inicio, LocalDateTime agora, List<Produto> produtos, int[] pesos,
            List<Usuario> vendedores, List<Cliente> clientes) {
        Random aleatorio = new Random(SEMENTE);
        int[] pesoVendedor = vendedores.stream().mapToInt(GeradorVendasDemo::pesoDoVendedor).toArray();
        int total = 0;
        for (LocalDate dia = inicio; !dia.isAfter(agora.toLocalDate()); dia = dia.plusDays(1)) {
            int quantidade = vendasNoDia(dia, inicio, aleatorio);
            List<LocalDateTime> horarios = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                LocalDateTime momento = dia.atTime(sortearHora(aleatorio), aleatorio.nextInt(60));
                if (!momento.isAfter(agora)) {
                    horarios.add(momento);
                }
            }
            horarios.sort(null);
            for (int i = 0; i < horarios.size(); i++) {
                boolean recente = dia.equals(agora.toLocalDate()) && i >= horarios.size() - 2;
                Usuario vendedor = vendedores.get(sortear(pesoVendedor, aleatorio));
                Cliente cliente = aleatorio.nextInt(100) < 40 ? clientes.get(aleatorio.nextInt(clientes.size())) : null;
                entityManager.persist(montarVenda(horarios.get(i), vendedor, cliente, produtos, pesos, recente, aleatorio));
                total++;
            }
            // Grava em lotes para não acumular milhares de objetos na memória
            if (dia.getDayOfMonth() % 7 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return total;
    }

    private Venda montarVenda(LocalDateTime momento, Usuario vendedor, Cliente cliente, List<Produto> produtos,
            int[] pesos, boolean recente, Random aleatorio) {
        Venda venda = new Venda(vendedor, cliente, formaDePagamento(aleatorio), momento);
        // De 1 a 4 produtos diferentes por pedido (nunca mais do que o cardápio tem)
        int linhas = Math.min(1 + sortear(new int[] {35, 40, 18, 7}, aleatorio), produtos.size());
        List<Integer> escolhidos = new ArrayList<>();
        while (escolhidos.size() < linhas) {
            int indice = sortear(pesos, aleatorio);
            if (!escolhidos.contains(indice)) {
                escolhidos.add(indice);
                venda.adicionarItem(produtos.get(indice), 1 + sortear(new int[] {70, 22, 8}, aleatorio));
            }
        }
        // Só Dono e Gerente dão desconto (regra do protótipo): de vez em quando, 5% a 10%
        if (vendedor.getPerfil() != Perfil.VENDEDOR && aleatorio.nextInt(100) < 15) {
            BigDecimal percentual = BigDecimal.valueOf(5 + aleatorio.nextInt(6), 2);
            venda.aplicarDesconto(venda.getSubtotal().multiply(percentual).setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal recebido = venda.getFormaPagamento() == FormaPagamento.DINHEIRO ? valorEmDinheiro(venda.getTotal()) : null;
        venda.confirmar(recente ? StatusVenda.EM_PREPARO : StatusVenda.CONCLUIDA, recebido, false, false, momento);
        if (!recente && aleatorio.nextInt(100) < 3) {
            venda.cancelar(MOTIVOS_CANCELAMENTO[aleatorio.nextInt(MOTIVOS_CANCELAMENTO.length)], momento.plusMinutes(10));
        }
        return venda;
    }

    /** Quantidade de vendas do dia: média × peso do dia da semana × peso do mês × crescimento, com variação. */
    private static int vendasNoDia(LocalDate dia, LocalDate inicio, Random aleatorio) {
        double crescimento = 1 + 0.015 * ChronoUnit.MONTHS.between(inicio.withDayOfMonth(1), dia.withDayOfMonth(1));
        double esperado = VENDAS_POR_DIA
                * PESO_DIA_SEMANA[dia.getDayOfWeek().getValue() - 1]
                * PESO_MES[dia.getMonthValue() - 1]
                * crescimento;
        double variacao = 0.85 + aleatorio.nextDouble() * 0.30;
        return (int) Math.round(esperado * variacao);
    }

    private static int sortearHora(Random aleatorio) {
        int[] horas = PESO_HORA.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
        int[] pesos = Arrays.stream(horas).map(PESO_HORA::get).toArray();
        return horas[sortear(pesos, aleatorio)];
    }

    private static FormaPagamento formaDePagamento(Random aleatorio) {
        return switch (sortear(new int[] {40, 25, 20, 15}, aleatorio)) {
            case 0 -> FormaPagamento.PIX;
            case 1 -> FormaPagamento.CREDITO;
            case 2 -> FormaPagamento.DEBITO;
            default -> FormaPagamento.DINHEIRO;
        };
    }

    /** Valor entregue em dinheiro: o total arredondado para cima até a próxima dezena. */
    private static BigDecimal valorEmDinheiro(BigDecimal total) {
        BigDecimal dez = BigDecimal.TEN;
        return total.divide(dez, 0, RoundingMode.CEILING).multiply(dez).setScale(2);
    }

    /** Vendedores atendem mais que o Gerente, que atende mais que o Dono. */
    private static int pesoDoVendedor(Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case VENDEDOR -> 30;
            case GERENTE -> 12;
            case DONO -> 5;
        };
    }

    /** Sorteia um índice com probabilidade proporcional ao peso. */
    static int sortear(int[] pesos, Random aleatorio) {
        int soma = Arrays.stream(pesos).sum();
        int valor = aleatorio.nextInt(soma);
        for (int i = 0; i < pesos.length; i++) {
            valor -= pesos[i];
            if (valor < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }
}
