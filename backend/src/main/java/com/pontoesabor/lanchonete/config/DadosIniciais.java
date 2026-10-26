package com.pontoesabor.lanchonete.config;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria dados ao subir a API, apenas quando o banco ainda não tem usuários:
 * <ul>
 *   <li><b>Dados de demonstração</b> ({@code app.dados-demo.habilitado=true}, perfil h2): os funcionários,
 *       clientes e produtos do protótipo do Figma, para testar o sistema localmente.</li>
 *   <li><b>Primeiro Dono</b> ({@code ADMIN_USERNAME} e {@code ADMIN_SENHA}): em produção, para que alguém
 *       consiga entrar e cadastrar os demais funcionários.</li>
 * </ul>
 */
@Component
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean demoHabilitado;
    private final String senhaDemo;
    private final String adminUsername;
    private final String adminSenha;

    public DadosIniciais(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            CategoriaRepository categoriaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dados-demo.habilitado:false}") boolean demoHabilitado,
            @Value("${app.dados-demo.senha:}") String senhaDemo,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.senha:}") String adminSenha) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoHabilitado = demoHabilitado;
        this.senhaDemo = senhaDemo;
        this.adminUsername = adminUsername;
        this.adminSenha = adminSenha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (demoHabilitado && !senhaDemo.isBlank()) {
            criarDadosDemonstracao();
        } else if (!adminUsername.isBlank() && !adminSenha.isBlank()) {
            usuarioRepository.save(new Usuario(
                    "Administrador", adminUsername.strip().toLowerCase(), passwordEncoder.encode(adminSenha), Perfil.DONO));
            log.info("Usuário Dono inicial '{}' criado.", adminUsername);
        } else {
            log.warn("Nenhum usuário cadastrado. Defina ADMIN_USERNAME e ADMIN_SENHA para criar o primeiro Dono.");
        }
    }

    private void criarDadosDemonstracao() {
        String hash = passwordEncoder.encode(senhaDemo);
        usuarioRepository.save(funcionario("Matheus Almeida", "matheus", "matheus@pontoesabor.com", "81999881100", Perfil.DONO, hash));
        usuarioRepository.save(funcionario("Carlos Lima", "carlos", "carlos@pontoesabor.com", "81988772200", Perfil.GERENTE, hash));
        usuarioRepository.save(funcionario("Fernanda Souza", "fernanda", "fernanda@pontoesabor.com", "81977663300", Perfil.VENDEDOR, hash));
        Usuario inativo = funcionario("Rafael Mendes", "rafael", "rafael@pontoesabor.com", "81966554400", Perfil.VENDEDOR, hash);
        inativo.setAtivo(false);
        usuarioRepository.save(inativo);

        clienteRepository.save(cliente("João Silva", "81999991234", "joao@email.com", "Recife", "Prefere contato pelo WhatsApp."));
        clienteRepository.save(cliente("Ana Souza", "81988885678", "ana@email.com", "Recife", null));
        clienteRepository.save(cliente("Maria Santos", "81966668910", "maria@email.com", "Recife", null));
        clienteRepository.save(cliente("Rafael Costa", "81955551122", "rafael@email.com", "Olinda", null));
        Cliente pedro = cliente("Pedro Lima", "81977774321", "pedro@email.com", "Recife", null);
        pedro.setAtivo(false);
        clienteRepository.save(pedro);

        Map<String, Categoria> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getNome, Function.identity()));
        produtoRepository.save(produto("XBG001", "X-Burger", "Pão, carne, queijo e molho da casa.", categorias.get("Hambúrgueres"), "18.00", 24));
        produtoRepository.save(produto("XSL001", "X-Salada", "Pão, carne, queijo, salada e molho da casa.", categorias.get("Hambúrgueres"), "21.00", 15));
        produtoRepository.save(produto("SUC001", "Suco Natural", "Suco natural de fruta, 400 ml.", categorias.get("Bebidas"), "9.00", 5));
        produtoRepository.save(produto("BTF001", "Batata Frita", "Porção individual de batatas fritas.", categorias.get("Acompanhamentos"), "12.00", 18));
        produtoRepository.save(produto("REF001", "Refrigerante Lata", "Refrigerante em lata, 350 ml.", categorias.get("Bebidas"), "6.00", 4));

        log.info("Dados de demonstração criados: usuários matheus (Dono), carlos (Gerente) e fernanda (Vendedor).");
    }

    private static Usuario funcionario(String nome, String username, String email, String telefone, Perfil perfil, String hash) {
        Usuario usuario = new Usuario(nome, username, hash, perfil);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        return usuario;
    }

    private static Cliente cliente(String nome, String telefone, String email, String cidade, String observacoes) {
        Cliente cliente = new Cliente(nome);
        cliente.setTelefone(telefone);
        cliente.setEmail(email);
        cliente.setCidade(cidade);
        cliente.setUf("PE");
        cliente.setObservacoes(observacoes);
        return cliente;
    }

    private static Produto produto(String codigo, String nome, String descricao, Categoria categoria, String preco, int estoque) {
        Produto produto = new Produto(codigo, nome, categoria, new BigDecimal(preco), estoque, 6);
        produto.setDescricao(descricao);
        return produto;
    }
}
