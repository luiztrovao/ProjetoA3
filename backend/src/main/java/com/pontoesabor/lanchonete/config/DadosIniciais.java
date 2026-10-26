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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final GeradorVendasDemo geradorVendas;
    private final Clock relogio;
    private final boolean demoHabilitado;
    private final int mesesDeHistorico;
    private final String senhaDemo;
    private final String adminUsername;
    private final String adminSenha;

    public DadosIniciais(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            CategoriaRepository categoriaRepository,
            PasswordEncoder passwordEncoder,
            GeradorVendasDemo geradorVendas,
            Clock relogio,
            @Value("${app.dados-demo.habilitado:false}") boolean demoHabilitado,
            @Value("${app.dados-demo.meses-de-historico:18}") int mesesDeHistorico,
            @Value("${app.dados-demo.senha:}") String senhaDemo,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.senha:}") String adminSenha) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.geradorVendas = geradorVendas;
        this.relogio = relogio;
        this.demoHabilitado = demoHabilitado;
        this.mesesDeHistorico = mesesDeHistorico;
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
        List<Usuario> equipe = usuarioRepository.saveAll(List.of(
                funcionario("Matheus Almeida", "matheus", "matheus@pontoesabor.com", "81999881100", Perfil.DONO, hash),
                funcionario("Carlos Lima", "carlos", "carlos@pontoesabor.com", "81988772200", Perfil.GERENTE, hash),
                funcionario("Fernanda Souza", "fernanda", "fernanda@pontoesabor.com", "81977663300", Perfil.VENDEDOR, hash),
                funcionario("Júlia Ribeiro", "julia", "julia@pontoesabor.com", "81976554411", Perfil.VENDEDOR, hash),
                funcionario("Bruno Carvalho", "bruno", "bruno@pontoesabor.com", "81975443322", Perfil.VENDEDOR, hash),
                funcionario("Rafael Mendes", "rafael", "rafael@pontoesabor.com", "81966554400", Perfil.VENDEDOR, hash)));

        List<Cliente> clientes = clienteRepository.saveAll(List.of(
                cliente("João Silva", "81999991234", "joao@email.com", "Recife", "Prefere contato pelo WhatsApp."),
                cliente("Ana Souza", "81988885678", "ana@email.com", "Recife", null),
                cliente("Maria Santos", "81966668910", "maria@email.com", "Recife", null),
                cliente("Rafael Costa", "81955551122", "rafael.costa@email.com", "Olinda", null),
                cliente("Pedro Lima", "81977774321", "pedro@email.com", "Recife", null),
                cliente("Larissa Oliveira", "81991237788", "larissa@email.com", "Jaboatão dos Guararapes", "Alérgica a amendoim."),
                cliente("Gustavo Pereira", "81992348899", "gustavo@email.com", "Recife", null),
                cliente("Camila Ferreira", "81993459900", "camila@email.com", "Olinda", null),
                cliente("Diego Rocha", "81994561122", "diego@email.com", "Recife", "Sempre pede sem cebola."),
                cliente("Beatriz Alves", "81995672233", "beatriz@email.com", "Recife", null),
                cliente("Thiago Nascimento", "81996783344", "thiago@email.com", "Paulista", null),
                cliente("Patrícia Gomes", "81997894455", "patricia@email.com", "Recife", null),
                cliente("Lucas Martins", "81998905566", "lucas@email.com", "Jaboatão dos Guararapes", null),
                cliente("Fernanda Araújo", "81981016677", "fernanda.araujo@email.com", "Recife", null),
                cliente("Ricardo Barbosa", "81982127788", "ricardo@email.com", "Olinda", "Cliente fiel desde a inauguração."),
                cliente("Aline Moreira", "81983238899", "aline@email.com", "Recife", null),
                cliente("Eduardo Teixeira", "81984349900", "eduardo@email.com", "Camaragibe", null),
                cliente("Juliana Cardoso", "81985451122", "juliana@email.com", "Recife", null),
                cliente("Marcelo Dias", "81986562233", "marcelo@email.com", "Recife", null),
                cliente("Vanessa Lopes", "81987673344", "vanessa@email.com", "Olinda", null)));

        Map<String, Categoria> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getNome, Function.identity()));
        List<Produto> produtos = new ArrayList<>();
        List<Integer> popularidade = new ArrayList<>();
        for (ItemCardapio item : CARDAPIO) {
            Produto produto = new Produto(item.codigo(), item.nome(), categorias.get(item.categoria()),
                    new BigDecimal(item.preco()), item.estoque(), 6);
            produto.setDescricao(item.descricao());
            produtos.add(produtoRepository.save(produto));
            popularidade.add(item.popularidade());
        }

        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate inicio = agora.toLocalDate().withDayOfMonth(1).minusMonths(Math.max(0, mesesDeHistorico - 1));
        int vendas = geradorVendas.gerar(inicio, agora, produtos,
                popularidade.stream().mapToInt(Integer::intValue).toArray(), equipe, clientes);

        // Rafael saiu da equipe e Pedro deixou de ser cliente: continuam no histórico, mas inativos
        usuarioRepository.findByUsername("rafael").ifPresent(usuario -> usuario.setAtivo(false));
        clienteRepository.findAll().stream()
                .filter(cliente -> cliente.getNome().equals("Pedro Lima"))
                .forEach(cliente -> cliente.setAtivo(false));

        log.info("Dados de demonstração criados: {} funcionários, {} clientes, {} produtos e {} vendas desde {}.",
                equipe.size(), clientes.size(), produtos.size(), vendas, inicio);
        log.info("Usuários: matheus (Dono), carlos (Gerente), fernanda, julia e bruno (Vendedores).");
    }

    /** Item do cardápio de demonstração; {@code popularidade} é o peso usado na geração das vendas. */
    private record ItemCardapio(
            String codigo, String nome, String descricao, String categoria, String preco, int estoque, int popularidade) {
    }

    private static final List<ItemCardapio> CARDAPIO = List.of(
            new ItemCardapio("XBG001", "X-Burger", "Pão, carne, queijo e molho da casa.", "Hambúrgueres", "18.00", 24, 30),
            new ItemCardapio("XSL001", "X-Salada", "Pão, carne, queijo, salada e molho da casa.", "Hambúrgueres", "21.00", 15, 25),
            new ItemCardapio("XBC001", "X-Bacon", "Pão, carne, queijo, bacon crocante e molho da casa.", "Hambúrgueres", "24.00", 20, 22),
            new ItemCardapio("XTD001", "X-Tudo", "Pão, duas carnes, queijo, bacon, ovo, presunto e salada.", "Hambúrgueres", "29.00", 12, 15),
            new ItemCardapio("XFR001", "X-Frango", "Pão, filé de frango grelhado, queijo e salada.", "Hambúrgueres", "22.00", 14, 12),
            new ItemCardapio("REF001", "Refrigerante Lata", "Refrigerante em lata, 350 ml.", "Bebidas", "6.00", 4, 35),
            new ItemCardapio("SUC001", "Suco Natural", "Suco natural de fruta, 400 ml.", "Bebidas", "9.00", 5, 18),
            new ItemCardapio("AGU001", "Água Mineral", "Água mineral sem gás, 500 ml.", "Bebidas", "4.00", 40, 12),
            new ItemCardapio("MLK001", "Milk-shake", "Milk-shake de chocolate, morango ou baunilha, 400 ml.", "Bebidas", "16.00", 18, 10),
            new ItemCardapio("BTF001", "Batata Frita", "Porção individual de batatas fritas.", "Acompanhamentos", "12.00", 18, 28),
            new ItemCardapio("ONR001", "Onion Rings", "Anéis de cebola empanados, porção individual.", "Acompanhamentos", "15.00", 10, 9),
            new ItemCardapio("NGG001", "Nuggets (10 un.)", "Nuggets de frango com molho à escolha.", "Acompanhamentos", "17.00", 16, 8),
            new ItemCardapio("CMB001", "Combo Clássico", "X-Burger, batata frita e refrigerante.", "Combos", "32.00", 20, 14),
            new ItemCardapio("CMB002", "Combo Família", "4 X-Burgers, 2 batatas grandes e refrigerante 2 L.", "Combos", "79.00", 8, 4),
            new ItemCardapio("SOR001", "Sorvete (2 bolas)", "Sorvete artesanal, dois sabores.", "Sobremesas", "10.00", 25, 8),
            new ItemCardapio("BRW001", "Brownie", "Brownie de chocolate com calda.", "Sobremesas", "9.00", 3, 7));

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
}
