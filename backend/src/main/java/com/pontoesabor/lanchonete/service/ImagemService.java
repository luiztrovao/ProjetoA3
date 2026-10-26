package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Imagem;
import com.pontoesabor.lanchonete.repository.ImagemRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Guarda e lê as imagens (logo e fotos de produto) no banco. */
@Service
@Transactional(readOnly = true)
public class ImagemService {

    /** Tamanho máximo aceito: 1 MB. */
    public static final int TAMANHO_MAXIMO = 1024 * 1024;

    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final ImagemRepository imagemRepository;

    public ImagemService(ImagemRepository imagemRepository) {
        this.imagemRepository = imagemRepository;
    }

    public Imagem buscar(Long id) {
        return imagemRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Imagem", id));
    }

    /**
     * Valida e grava a nova imagem e apaga a anterior (se houver).
     *
     * @return id da imagem gravada
     */
    @Transactional
    public Long substituir(Long idAnterior, MultipartFile arquivo) {
        byte[] conteudo = lerConteudo(arquivo);
        Long novoId = imagemRepository.save(new Imagem(conteudo, detectarTipo(conteudo))).getId();
        remover(idAnterior);
        return novoId;
    }

    @Transactional
    public void remover(Long id) {
        if (id != null) {
            imagemRepository.deleteById(id);
        }
    }

    private static byte[] lerConteudo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Selecione uma imagem para enviar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("A imagem deve ter no máximo 1 MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a imagem enviada.", e);
        }
    }

    /** Identifica PNG ou JPG pelos primeiros bytes do arquivo (não confia na extensão nem no tipo informado). */
    static String detectarTipo(byte[] conteudo) {
        if (comecaCom(conteudo, ASSINATURA_PNG)) {
            return "image/png";
        }
        if (comecaCom(conteudo, ASSINATURA_JPEG)) {
            return "image/jpeg";
        }
        throw new RegraNegocioException("Envie uma imagem PNG ou JPG.");
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
