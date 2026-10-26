package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

class ImagemServiceTest {

    @Test
    void detectaPngPelosPrimeirosBytes() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

        assertThat(ImagemService.detectarTipo(png)).isEqualTo("image/png");
    }

    @Test
    void detectaJpegPelosPrimeirosBytes() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};

        assertThat(ImagemService.detectarTipo(jpeg)).isEqualTo("image/jpeg");
    }

    @Test
    void recusaOutrosFormatosMesmoComExtensaoDeImagem() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a'};
        byte[] texto = "não sou uma imagem".getBytes();
        byte[] curto = {(byte) 0xFF};

        assertThatThrownBy(() -> ImagemService.detectarTipo(gif)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> ImagemService.detectarTipo(texto)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> ImagemService.detectarTipo(curto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Envie uma imagem PNG ou JPG.");
    }
}
