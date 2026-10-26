package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.ConfiguracaoResponse;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.EmpresaDto;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.IdentidadeResponse;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.PreferenciasVendaDto;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.Imagem;
import com.pontoesabor.lanchonete.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final ImagemService imagemService;

    public ConfiguracaoService(ConfiguracaoRepository configuracaoRepository, ImagemService imagemService) {
        this.configuracaoRepository = configuracaoRepository;
        this.imagemService = imagemService;
    }

    public ConfiguracaoResponse consultar() {
        return ConfiguracaoResponse.de(obter());
    }

    /** Nome e logo da lanchonete, públicos: aparecem na tela de login, antes de entrar. */
    public IdentidadeResponse identidade() {
        Configuracao configuracao = obter();
        return new IdentidadeResponse(configuracao.getNomeFantasia(), Imagem.url(configuracao.getLogoId()));
    }

    /** Troca o logo (PNG ou JPG, até 1 MB); o anterior é apagado. */
    @Transactional
    public ConfiguracaoResponse atualizarLogo(MultipartFile arquivo) {
        Configuracao configuracao = obter();
        configuracao.setLogoId(imagemService.substituir(configuracao.getLogoId(), arquivo));
        return ConfiguracaoResponse.de(configuracaoRepository.saveAndFlush(configuracao));
    }

    /** Volta ao logo padrão do sistema. */
    @Transactional
    public ConfiguracaoResponse removerLogo() {
        Configuracao configuracao = obter();
        Long logoAnterior = configuracao.getLogoId();
        configuracao.setLogoId(null);
        configuracaoRepository.saveAndFlush(configuracao);
        imagemService.remover(logoAnterior);
        return ConfiguracaoResponse.de(configuracao);
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
