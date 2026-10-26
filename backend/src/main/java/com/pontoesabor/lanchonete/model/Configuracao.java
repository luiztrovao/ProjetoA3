package com.pontoesabor.lanchonete.model;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Configurações gerais do sistema (tela Configurações do protótipo).
 * Existe um único registro, com id = 1, criado pela migration.
 */
@Entity
@Table(name = "configuracao")
public class Configuracao {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id;

    // ===== Dados da lanchonete =====

    @Column(name = "nome_fantasia", nullable = false, length = 120)
    private String nomeFantasia;

    @Column(name = "razao_social", length = 150)
    private String razaoSocial;

    /** Só números. */
    @Column(length = 14)
    private String cnpj;

    @Column(length = 11)
    private String telefone;

    @Column(length = 150)
    private String email;

    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

    /** Logo da lanchonete (tabela imagem); só o id. */
    @Column(name = "logo_id")
    private Long logoId;

    // ===== Preferências de vendas =====

    @Column(name = "controle_estoque", nullable = false)
    private boolean controleEstoque = true;

    @Column(name = "bloquear_sem_estoque", nullable = false)
    private boolean bloquearSemEstoque = true;

    /** Estoque mínimo sugerido ao cadastrar um produto sem informar o seu. */
    @Column(name = "alerta_estoque_baixo", nullable = false)
    private int alertaEstoqueBaixo = 5;

    @Column(name = "confirmar_cancelamento", nullable = false)
    private boolean confirmarCancelamento = true;

    @Column(name = "permitir_venda_sem_cliente", nullable = false)
    private boolean permitirVendaSemCliente = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_inicial_venda", nullable = false, length = 20)
    private StatusVenda statusInicialVenda = StatusVenda.EM_PREPARO;

    @Column(name = "aceita_dinheiro", nullable = false)
    private boolean aceitaDinheiro = true;

    @Column(name = "aceita_credito", nullable = false)
    private boolean aceitaCredito = true;

    @Column(name = "aceita_debito", nullable = false)
    private boolean aceitaDebito = true;

    @Column(name = "aceita_pix", nullable = false)
    private boolean aceitaPix = true;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Configuracao() {
    }

    /** Usado em testes; em produção o registro vem da migration. */
    public Configuracao(String nomeFantasia) {
        this.id = ID_UNICO;
        this.nomeFantasia = nomeFantasia;
    }

    /** Formas de pagamento habilitadas para a finalização da venda. */
    public Set<FormaPagamento> getFormasPagamentoAceitas() {
        Set<FormaPagamento> formas = EnumSet.noneOf(FormaPagamento.class);
        if (aceitaDinheiro) {
            formas.add(FormaPagamento.DINHEIRO);
        }
        if (aceitaCredito) {
            formas.add(FormaPagamento.CREDITO);
        }
        if (aceitaDebito) {
            formas.add(FormaPagamento.DEBITO);
        }
        if (aceitaPix) {
            formas.add(FormaPagamento.PIX);
        }
        return formas;
    }

    /**
     * Atualiza as preferências de vendas, validando as regras entre elas.
     * "Bloquear sem estoque" só faz sentido com o controle de estoque ligado (no protótipo o botão fica desabilitado).
     */
    public void atualizarPreferencias(
            boolean controleEstoque,
            boolean bloquearSemEstoque,
            int alertaEstoqueBaixo,
            boolean confirmarCancelamento,
            boolean permitirVendaSemCliente,
            StatusVenda statusInicialVenda,
            Set<FormaPagamento> formasPagamento) {
        if (alertaEstoqueBaixo < 0) {
            throw new RegraNegocioException("O alerta de estoque baixo não pode ser negativo.");
        }
        if (statusInicialVenda == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("O status inicial da venda deve ser Em preparo ou Concluída.");
        }
        if (formasPagamento.isEmpty()) {
            throw new RegraNegocioException("Pelo menos uma forma de pagamento deve estar habilitada.");
        }
        this.controleEstoque = controleEstoque;
        this.bloquearSemEstoque = controleEstoque && bloquearSemEstoque;
        this.alertaEstoqueBaixo = alertaEstoqueBaixo;
        this.confirmarCancelamento = confirmarCancelamento;
        this.permitirVendaSemCliente = permitirVendaSemCliente;
        this.statusInicialVenda = statusInicialVenda;
        this.aceitaDinheiro = formasPagamento.contains(FormaPagamento.DINHEIRO);
        this.aceitaCredito = formasPagamento.contains(FormaPagamento.CREDITO);
        this.aceitaDebito = formasPagamento.contains(FormaPagamento.DEBITO);
        this.aceitaPix = formasPagamento.contains(FormaPagamento.PIX);
    }

    public Long getId() {
        return id;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public Long getLogoId() {
        return logoId;
    }

    public void setLogoId(Long logoId) {
        this.logoId = logoId;
    }

    public boolean isControleEstoque() {
        return controleEstoque;
    }

    public boolean isBloquearSemEstoque() {
        return bloquearSemEstoque;
    }

    public int getAlertaEstoqueBaixo() {
        return alertaEstoqueBaixo;
    }

    public boolean isConfirmarCancelamento() {
        return confirmarCancelamento;
    }

    public boolean isPermitirVendaSemCliente() {
        return permitirVendaSemCliente;
    }

    public StatusVenda getStatusInicialVenda() {
        return statusInicialVenda;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
