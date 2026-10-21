package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pontoesabor.lanchonete.dto.ClienteRequest;
import com.pontoesabor.lanchonete.dto.ClienteResponse;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void criarGuardaCpfETelefoneSomenteComDigitos() {
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteResponse resposta = clienteService.criar(
                ClienteRequest.basico(" João Silva ", "529.982.247-25", "(81) 99999-1234", "JOAO@EMAIL.COM"));

        assertThat(resposta.nome()).isEqualTo("João Silva");
        assertThat(resposta.cpf()).isEqualTo("52998224725");
        assertThat(resposta.telefone()).isEqualTo("81999991234");
        assertThat(resposta.email()).isEqualTo("joao@email.com");
    }

    @Test
    void criarSemCpfNaoConsultaDuplicidade() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteResponse resposta = clienteService.criar(ClienteRequest.basico("Cliente de balcão", "", null, null));

        assertThat(resposta.cpf()).isNull();
        verify(clienteRepository, never()).existsByCpf(any());
    }

    @Test
    void criarComCpfRepetidoLancaExcecao() {
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(ClienteRequest.basico("João", "52998224725", null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void atualizarComCpfDeOutroClienteLancaExcecao() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(new Cliente("João")));
        when(clienteRepository.existsByCpfAndIdNot("52998224725", 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.atualizar(1L, ClienteRequest.basico("João", "52998224725", null, null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void inativarMarcaClienteComoInativo() {
        Cliente cliente = new Cliente("João");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.inativar(1L);

        assertThat(cliente.isAtivo()).isFalse();
    }
}
