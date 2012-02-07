package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import util.FrontEndCommands;
import util.ServerCommands;

public class Client {

    private static RoundRobinList frontEndsList = null;
    private static int portTcp = 3000;
    private static HashMap<String, Double> table = null;
    private static String nodesConfigFilePath = "client/nodes.config";

    private static final boolean DEBUG = false;

    private static String createMessage(final FrontEndCommands command) {
        final StringBuilder msg = new StringBuilder();
        msg.append(command);
        return msg.toString();
    }

    private static void debug(final String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    private static String getMessage(final FrontEndCommands cmd) {
        String message = null;

        switch (cmd) {
            case TIME:
                message = createMessage(FrontEndCommands.TIME);
                break;

            case GET_TABLE:
                message = createMessage(FrontEndCommands.GET_TABLE);
                break;

            case LIST_TABLE:
                message = createMessage(FrontEndCommands.LIST_TABLE);
                break;

            default:
                message = createMessage(FrontEndCommands.INVALID);
                break;
        }

        return message;
    }

    private static String getMessage(final String cmd) {
        return getMessage(FrontEndCommands.parse(cmd));
    }

    private static List<String> getSortedIpList() {
        List<String> ipList = null;

        if (table != null) {
            ipList = new ArrayList<String>(table.keySet());

            Collections.sort(ipList, new Comparator<String>() {

                @Override
                public int compare(final String a, final String b) {
                    if (table.get(a) < table.get(b)) {
                        return 1;
                    }
                    if (table.get(a) > table.get(b)) {
                        return -1;
                    }
                    return 0;
                }
            });
        }

        return ipList;
    }

    @SuppressWarnings("unchecked")
    private static void getTable(final String host) {
        Socket socket = null;
        PrintStream output = null;
        ObjectInputStream input = null;

        try {
            socket = new Socket(host, portTcp);
            output = new PrintStream(socket.getOutputStream(), true);
            output.println(getMessage(FrontEndCommands.GET_TABLE));

            input = new ObjectInputStream(socket.getInputStream());

            table = (HashMap<String, Double>) input.readObject();
        } catch (final IOException e) {
            e.printStackTrace();
            table = null;
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
            table = null;
        }
    }

    public static boolean hashMapToFile(final String file, final HashMap<String, ?> table) {
        String key;
        Iterator<String> iterator;

        try {
            final File inFile = new File(file);
            if (inFile.isFile()) {
                if (!inFile.delete()) {
                    return false;
                }
            }

            final File newFile = new File(file);

            final FileWriter fstream = new FileWriter(newFile, true);
            final BufferedWriter escritor = new BufferedWriter(fstream);
            iterator = table.keySet().iterator();
            while (iterator.hasNext()) {
                key = iterator.next().toString();
                escritor.write(key + "\n");
                escritor.flush();
            }
            escritor.close();
        } catch (final Exception exception) {
            return false;
        }
        return true;
    }

    public static void main(final String args[]) throws Exception {
        printHelp();
        frontEndsList = new RoundRobinList(nodesConfigFilePath);

        if (frontEndsList.size() == 0) {
            System.out.println("Não existem servidores disponiveis, tente mais tarde.");
            System.exit(1);
        }

        String cmd, receivedMessage = null;

        String host = frontEndsList.getNextHost();
        String lastHost = null;
        int triesCount = 0;
        Long lastAttempt = null;
        cmd = "";
        // obter tabela
        while (true) {
            try {
                debug("[DEBUG] Before getNextHost() " + frontEndsList.size());

                if (host == null) {
                    host = frontEndsList.getNextHost();
                    frontEndsList.ignoreHost();
                }

                debug("[DEBUG] After getNextHost() " + frontEndsList.size());

                if (frontEndsList.isEmpty()) {
                    frontEndsList.reset();
                }

                if (triesCount == 0 || System.currentTimeMillis() - lastAttempt >= 1000) {

                    if (lastHost == null || !lastHost.equalsIgnoreCase(host)) {
                        lastHost = host;

                        System.out.println("A aguardar ligação com um servidor..." + (DEBUG ? " " + host : ""));
                        getTable(host);
                        //Guarda na sortedList a tabela enviada pelo front end devidamente ordenada
                        final List<String> sortedIpList = getSortedIpList();

                        updateNodesConfigFile(sortedIpList);
                        updateFrontEndsList(); 
                        if (!host.equalsIgnoreCase(sortedIpList.get(0))) {
                            host = lastHost = sortedIpList.get(0);
                            break;
                        } 
                    }
                }
            }
            catch (Exception e) {
                System.out.println("Ocorreu um erro ao tentar aceder ao servidor. Aguarde enquanto tentamos uma nova ligação...");
                frontEndsList.ignoreHost();
                host = null;
            }
        }

        //Ligação ao servidor com menos carga
        while (true) {
            try {              
                System.out.println("A tentar ligar ao FrontEnd: " + host);              

                // Criação do SocketChannel
                SocketChannel client = SocketChannel.open();
                client.configureBlocking(false);
                InetSocketAddress address = new InetSocketAddress(host,portTcp);
                client.connect(address);

                // Criar selector
                Selector selector = Selector.open();

                // Grava o seletor (OP_CONNECT type)
                SelectionKey clientKey = client.register(selector,
                        SelectionKey.OP_CONNECT);

                // Espera por conecções               
                while (selector.select(500) > 0) {

                    // obtem chaves
                    @SuppressWarnings("rawtypes")
                    Set keys = selector.selectedKeys();

                    @SuppressWarnings("rawtypes")
                    Iterator i = keys.iterator();


                    while (i.hasNext()) {
                        SelectionKey key = (SelectionKey) i.next();
                        // remove atual chave
                        i.remove();

                        SocketChannel channel = (SocketChannel) key.channel();

                        if (key.isConnectable()) {                         
                            if (channel.isConnectionPending())

                                channel.finishConnect();                        
                            System.out.println("Ligacao ao servidor estabelecida!");

                            // Escreve no buffer
                            int BUFFER_SIZE = 1024;
                            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

                            cmd ="";
                            final Scanner scan = new Scanner(System.in);
                            System.out.print(host + ":" + portTcp + "> ");
                            for (;;) {
                                if (cmd.isEmpty()) {                                        
                                    cmd = scan.nextLine();
                                    if (FrontEndCommands.parse(cmd.toUpperCase()).toString().equals(FrontEndCommands.EXIT.name())) {
                                        System.exit(-1);
                                    }
                                }


                                buffer = ByteBuffer.wrap(getMessage(cmd.toUpperCase()).getBytes());
                                channel.write(buffer);
                                buffer.clear();
                                buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

                                Thread.sleep(50);                       

                                channel.read(buffer);
                                buffer.flip();

                                Charset charset = Charset.forName("ISO-8859-1");
                                CharsetDecoder decoder = charset.newDecoder();
                                CharBuffer charBuffer = decoder.decode(buffer);                  


                                final StringBuilder str = new StringBuilder();
                                while (!(receivedMessage = charBuffer.toString()).isEmpty()) {
                                    str.append(receivedMessage + "\n");
                                }

                                receivedMessage = str.toString();
                                cmd="";

                                if (!receivedMessage.isEmpty()) {
                                    System.out.println("\nRecebido : \n" + receivedMessage);
                                    if (cmd.equalsIgnoreCase("quit")) {
                                        System.out.println("a sair..");
                                        break;// ver se o Break Funcionaehdhdd
                                    }
                                }                                                            
                            }
                        }              
                    }
                }
            }catch (final Exception ex) {

                if (triesCount == 0) {
                    System.out.println("Ocorreu um erro ao tentar aceder ao servidor. Aguarde enquanto tentamos uma nova ligação...");
                } else if (triesCount <= 5) {
                    System.out.println("Tentativa de ligação número " + triesCount);
                } else {
                    System.out.println("Ocorreu um erro ao tentar aceder ao servidor. Aguarde enquanto tentamos uma ligação com outro servidor...");
                }
                if (triesCount > 5) {
                    host = null;
                    triesCount = 0;
                } else {
                    triesCount++;
                    lastAttempt = System.currentTimeMillis();
                }
                lastHost = null;
            }
        }
    }
    private static void printHelp() {
        System.out.println("\t---Comandos suportados pelo Cliente---");
        System.out.println();
        System.out.println("\t" + FrontEndCommands.LIST_TABLE + " -> Listar os servidores disponiveis e a sua carga");
        System.out.println("\t" + FrontEndCommands.TIME + "       -> Solicita a hora remota do servidor");
        System.out.println("\t" + ServerCommands.HELP + "       -> Lista os comandos disponiveis");
        System.out.println("\t" + ServerCommands.EXIT + "       -> Termina a aplicação\n");
    }

    private static void updateFrontEndsList() {
        frontEndsList.loadFromTextFile(nodesConfigFilePath, true);
    }

    private static void updateNodesConfigFile(final List<String> sortedIpList) {
        final File nodesConfigFile = new File(nodesConfigFilePath);
        PrintWriter fileAccess = null;

        try {
            if (sortedIpList != null) {
                fileAccess = new PrintWriter(nodesConfigFile);
                for (final String ip : sortedIpList) {
                    fileAccess.println(ip);
                }
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fileAccess != null) {
                fileAccess.close();
            }
        }
    }
}
