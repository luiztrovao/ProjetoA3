package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;
import org.hibernate.validator.constraints.br.CNPJ;

/** DTOs da tela Configurações: dados da lanchonete e preferências de vendas. */
public final class ConfiguracaoDtos {

    private ConfiguracaoDtos() {
    }

    /** Aba "Dados da Lanchonete". CNPJ, telefone e CEP podem vir formatados. */
    public record EmpresaDto(
            @NotBlank(message = "O nome fantasia é obrigatório.")
            @Size(max = 120, message = "O nome fantasia deve ter no máximo 120 caracteres.")
            String nomeFantasia,

            @Size(max = 150, message = "A razão social deve ter no máximo 150 caracteres.")
            String razaoSocial,

            @CNPJ(message = "CNPJ inválido.")
            String cnpj,

            @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter DDD + número (10 ou 11 dígitos).")
            String telefone,

            @Email(message = "E-mail inválido.")
            @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
            String email,

            @Pattern(regexp = "\\d{8}", message = "O CEP deve ter 8 dígitos.")
            String cep,

            @Size(max = 150, message = "O endereço deve ter no máximo 150 caracteres.")
            String logradouro,

            @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
            String numero,

            @Size(max = 80, message = "O bairro deve ter no máximo 80 caracteres.")
            String bairro,

            @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
            String cidade,

            @Pattern(regexp = "[A-Z]{2}", message = "A UF deve ter 2 letras.")
            String uf,

            @Size(max = 500, message = "O endereço do logo deve ter no máximo 500 caracteres.")
            String logoUrl) {

        public EmpresaDto {
            nomeFantasia = Textos.limpar(nomeFantasia);
            razaoSocial = Textos.limpar(razaoSocial);
            cnpj = Textos.somenteDigitos(cnpj);
            telefone = Textos.somenteDigitos(telefone);
            email = Textos.email(email);
            cep = Textos.somenteDigitos(cep);
            logradouro = Textos.limpar(logradouro);
            numero = Textos.limpar(numero);
            bairro = Textos.limpar(bairro);
            cidade = Textos.limpar(cidade);
            uf = Textos.maiusculas(uf);
            logoUrl = Textos.limpar(logoUrl);
        }

        public static EmpresaDto de(Configuracao c) {
            return new EmpresaDto(c.getNomeFantasia(), c.getRazaoSocial(), c.getCnpj(), c.getTelefone(), c.getEmail(),
                    c.getCep(), c.getLogradouro(), c.getNumero(), c.getBairro(), c.getCidade(), c.getUf(), c.getLogoUrl());
        }
    }

    /** Aba "Preferências de Vendas". */
    public record PreferenciasVendaDto(
            @NotNull(message = "Informe se o controle de estoque está ativo.")
            Boolean controleEstoque,

            @NotNull(message = "Informe se a venda sem estoque deve ser bloqueada.")
            Boolean bloquearSemEstoque,

            @NotNull(message = "O alerta de estoque baixo é obrigatório.")
            @PositiveOrZero(message = "O alerta de estoque baixo não pode ser negativo.")
            Integer alertaEstoqueBaixo,

            @NotNull(message = "Informe se o cancelamento exige confirmação.")
            Boolean confirmarCancelamento,

            @NotNull(message = "Informe se a venda sem cliente é permitida.")
            Boolean permitirVendaSemCliente,

            @NotNull(message = "O status inicial da venda é obrigatório.")
            StatusVenda statusInicialVenda,

            @NotEmpty(message = "Pelo menos uma forma de pagamento deve estar habilitada.")
            Set<FormaPagamento> formasPagamento) {

        public static PreferenciasVendaDto de(Configuracao c) {
            return new PreferenciasVendaDto(c.isControleEstoque(), c.isBloquearSemEstoque(), c.getAlertaEstoqueBaixo(),
                    c.isConfirmarCancelamento(), c.isPermitirVendaSemCliente(), c.getStatusInicialVenda(),
                    c.getFormasPagamentoAceitas());
        }
    }

    /** Resposta completa de GET /api/configuracoes. */
    public record ConfiguracaoResponse(
            @Valid EmpresaDto empresa,
            @Valid PreferenciasVendaDto preferenciasVenda,
            LocalDateTime atualizadoEm) {

        public static ConfiguracaoResponse de(Configuracao c) {
            return new ConfiguracaoResponse(EmpresaDto.de(c), PreferenciasVendaDto.de(c), c.getAtualizadoEm());
        }
    }
}
