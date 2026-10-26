package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.ConfiguracaoResponse;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.EmpresaDto;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.PreferenciasVendaDto;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;

    public ConfiguracaoService(ConfiguracaoRepository configuracaoRepository) {
        this.configuracaoRepository = configuracaoRepository;
    }

    public ConfiguracaoResponse consultar() {
        return ConfiguracaoResponse.de(obter());
    }

    @Transactional
    public ConfiguracaoResponse atualizarEmpresa(EmpresaDto dados) {
        Configuracao configuracao = obter();
        configuracao.setNomeFantasia(dados.nomeFantasia());
        configuracao.setRazaoSocial(dados.razaoSocial());
        configuracao.setCnpj(dados.cnpj());
        configuracao.setTelefone(dados.telefone());
        configuracao.setEmail(dados.email());
        configuracao.setCep(dados.cep());
        configuracao.setLogradouro(dados.logradouro());
        configuracao.setNumero(dados.numero());
        configuracao.setBairro(dados.bairro());
        configuracao.setCidade(dados.cidade());
        configuracao.setUf(dados.uf());
        configuracao.setLogoUrl(dados.logoUrl());
        return ConfiguracaoResponse.de(configuracaoRepository.saveAndFlush(configuracao));
    }

    @Transactional
    public ConfiguracaoResponse atualizarPreferenciasVenda(PreferenciasVendaDto dados) {
        Configuracao configuracao = obter();
        configuracao.atualizarPreferencias(
                dados.controleEstoque(),
                dados.bloquearSemEstoque(),
                dados.alertaEstoqueBaixo(),
                dados.confirmarCancelamento(),
                dados.permitirVendaSemCliente(),
                dados.statusInicialVenda(),
                dados.formasPagamento());
        return ConfiguracaoResponse.de(configuracaoRepository.saveAndFlush(configuracao));
    }

    /** Registro único de configurações, criado pela migration V3. */
    Configuracao obter() {
        return configuracaoRepository.findById(Configuracao.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException(
                        "Registro de configurações não encontrado: verifique as migrations do banco."));
    }
}
