package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

// Padrão Facade
class ECommerceFacade {
    private Inventário inventario;
    private Pagamento pagamento;
    private Envio envio;

    public ECommerceFacade() {
        this.inventario = new Inventário();
        this.pagamento = new ProxyPagamento();
        this.envio = new AdaptadorEnvio(new SistemaDeEnvio());
    }

    public void realizarCompra(List<String> produtos, double valorTotal) {
        for (String produto : produtos) {
            if (inventario.produtoDisponível(produto, 1)) {
                // Produto disponível, continuar
            } else {
                System.out.println("Produto " + produto + " não está disponível.");
                return;
            }
        }

        pagamento.processarPagamento(valorTotal);
        envio.enviarProduto(produtos);

        System.out.println("Compra realizada com sucesso!");
    }
}

// Subsistema de Inventário
class Inventário {
    public boolean produtoDisponível(String produtoId, int quantidade) {
        // Lógica para verificar disponibilidade
        return true;
    }
}

// Subsistema de Pagamento
interface Pagamento {
    void processarPagamento(double valor);
}

class PagamentoReal implements Pagamento {
    public void processarPagamento(double valor) {
        // Lógica para processar pagamento
    }
}

// Padrão Proxy
class ProxyPagamento implements Pagamento {
    private PagamentoReal pagamentoReal;

    public void processarPagamento(double valor) {
        System.out.println("Verificando histórico de compras...");

        if (pagamentoReal == null) {
            pagamentoReal = new PagamentoReal();
        }

        pagamentoReal.processarPagamento(valor);

        // Ações adicionais após o pagamento real, se necessário
        System.out.println("Enviando cupom de desconto para próxima compra.");
    }
}

// Subsistema de Envio
interface Envio {
    void enviarProduto(List<String> produtos);
}

class SistemaDeEnvio implements Envio {
    public void enviar(String[] produtos) {
    }

    @Override
    public void enviarProduto(List<String> produtos) {

    }
}

// Padrão Adapter
class AdaptadorEnvio implements Envio {
    private SistemaDeEnvio sistemaDeEnvio;

    public AdaptadorEnvio(SistemaDeEnvio sistemaDeEnvio) {
        this.sistemaDeEnvio = sistemaDeEnvio;
    }

    public void enviarProduto(List<String> produtos) {
        String[] produtosArray = produtos.toArray(new String[0]);

        sistemaDeEnvio.enviar(produtosArray);
    }
}

// Padrão Observer
interface ObservadorInventario {
    void produtoAtualizado(String produto, int quantidade);
}

class SistemaInventario {
    private Map<ObservadorInventario, Integer> observadores = new HashMap<>();

    public void adicionarObservador(ObservadorInventario observador) {
        observadores.put(observador, 0);
    }

    public void removerObservador(ObservadorInventario observador) {
        observadores.remove(observador);
    }

    public void notificarObservadores(String produto, int quantidade) {
        for (ObservadorInventario observador : observadores.keySet()) {
            observador.produtoAtualizado(produto, quantidade);
        }
    }

    public void atualizarEstoque(String produto, int quantidade) {
        notificarObservadores(produto, quantidade);
    }
}

class LogInventario implements ObservadorInventario {
    public void produtoAtualizado(String produto, int quantidade) {
        System.out.println("Inventário atualizado - Produto: " + produto + ", Quantidade: " + quantidade);
    }
}

public class ECommerceApp {
    public static void main(String[] args) {
        ECommerceFacade ecommerce = new ECommerceFacade();
        ProxyPagamento proxyPagamento = new ProxyPagamento();
        AdaptadorEnvio adaptadorEnvio = new AdaptadorEnvio(new SistemaDeEnvio());
        LogInventario logInventario = new LogInventario();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite o nome do produto:");
        String produtoDigitado = scanner.nextLine();

        System.out.println("Digite o valor total da compra:");
        double valorTotalDigitado = scanner.nextDouble();

        ecommerce.realizarCompra(List.of(produtoDigitado), valorTotalDigitado);

        SistemaInventario sistemaInventario = new SistemaInventario();
        sistemaInventario.adicionarObservador(logInventario);

        sistemaInventario.atualizarEstoque(produtoDigitado, 10);

        // Enviar e-mail com o código
        enviarEmail(
                "lucascolturato@outlook.com",
                "Bem vindo! \n"  +
                          "Código do Pedido: 123456 \n" + "Segue o rastreio do pedido: AKSFG3I5FJO0KK44BR" );

        // Conectar ao banco de dados e salvar informações da compra
        salvarInformacoesCompra(produtoDigitado, valorTotalDigitado);

        scanner.close();
    }

    private static void enviarEmail(String destinatario, String mensagem) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.office365.com"); // se for gmail  "smtp.gmail.com"
        props.put("mail.smtp.port", "587");


        // Credenciais do remetente (ajuste conforme suas credenciais)
        String remetente = "lucascolturato@outlook.com";
        String senha = "Oeste123#";

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remetente, senha);
            }
        });

        try {
            // Criando a mensagem
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remetente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("E-commerce - Detalhes do Pedido");
            message.setText(mensagem);

            // Enviando a mensagem
            Transport.send(message);

            System.out.println("E-mail enviado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao enviar e-mail.");
        }
    }

    private static void salvarInformacoesCompra(String produto, double valorTotal) {
        String url = "jdbc:mysql://localhost:3306/Ecommerce";
        String usuarioBD = "root";
        String senhaBD = "oeste123";

        String sql = "INSERT INTO compras (produto, valor_total) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url, usuarioBD, senhaBD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produto);
            stmt.setDouble(2, valorTotal);
            stmt.executeUpdate();

            System.out.println("Informações da compra salvas no banco de dados.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erro ao conectar ao banco de dados ou salvar informações.");
        }
    }
}
