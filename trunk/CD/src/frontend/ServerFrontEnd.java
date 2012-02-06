package frontend;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import util.FrontEndCommands;
import util.ServerCommands;
import util.ServerStatus;
import backend.Interface;

public class ServerFrontEnd {

    private static class DHTUpdateTimerTask extends TimerTask {

        @Override
        public void run() {
            actualizaCarga(ip, load());

            final Set<String> keys = serversTable.keySet();

            for (final String chave : keys) {
                if (!chave.equalsIgnoreCase(ip)) {
                    final Socket socket = new Socket();
                    final StringBuilder message = new StringBuilder();
                    message.append(FrontEndCommands.SEND_LOAD.name());
                    message.append("|");
                    message.append(ip);
                    message.append("|");
                    message.append(load());
                    sendMessageTcp(socket, message.toString());
                }
            }
        }
    }

    private static class HandleReceivedMessageThread implements Runnable {

        private final Socket socket;

        public HandleReceivedMessageThread(final Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String message[] = null;
            BufferedReader input = null;
            final String sourceIp = socket.getInetAddress().getHostAddress().toString();

            System.out.println();
            System.out.println("Conectado com " + sourceIp);
            System.out.print("Comando> ");

            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (true) {
                    final String line = input.readLine();
                    message = line.split("\\|");

                    switch (FrontEndCommands.parse(message[0])) {
                        case GET_TABLE:
                            sendTable(socket);
                            break;

                        case SEND_LOAD:
                            actualizaCarga(message[1], message[2]);
                            updateServerLifetime(message[1]);
                            break;
                        case TIME:
                            try {
                                sendMessageTcp(socket, stub.getTime());
                            } catch (final ConnectException e) {
                                sendMessageTcp(socket, "Não foi possivel realizar o pedido tente mais tarde.");

                                waitForBackendConnection(host);
                            }
                            break;
                        case LIST_TABLE:
                            final StringBuilder listaTabelaCarga = new StringBuilder();

                            final Set<String> keys = serversTable.keySet();

                            final NumberFormat doubleFormat = NumberFormat.getNumberInstance();
                            doubleFormat.setMaximumFractionDigits(2);
                            doubleFormat.setMinimumFractionDigits(2);

                            listaTabelaCarga.append("Chave\t\tValor\n");

                            for (final String chave : keys) {
                                listaTabelaCarga.append(chave + "\t" + doubleFormat.format(serversTable.get(chave)) + "\n");
                            }

                            sendMessageTcp(socket, listaTabelaCarga.toString());
                            break;
                        case EXIT:
                            sendMessageTcp(socket, "ate logo!");
                            System.out.println("cliente " + sourceIp + " desconectado!");
                            return;
                        default:
                            sendMessageTcp(socket, "Comando inválido!");
                            break;
                    }

                }

            } catch (final SocketException e) {
                System.out.println("\nCliente " + sourceIp + " desconectado!");
                System.out.print("Comando> ");
            } catch (final Exception e) {
                System.out.println("\nCliente " + sourceIp + " desconectado!");
                System.out.print("Comando> ");

            } finally {
                try {
                    input.close();
                    socket.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static class NodeActivityTimerTask extends TimerTask {

        @Override
        public void run() {
            Long count = null;
            final Set<String> keys_ = serversTable.keySet();
            final List<String> keysToRemove = new ArrayList<String>();

            for (final String chave : keys_) {
                if (serversTable.get(chave) == null && !chave.equalsIgnoreCase(ip)) {
                    keysToRemove.add(chave);
                }
            }

            for (final String key : keysToRemove) {
                serversTable.remove(key);
                serversLifetime.remove(key);
            }

            final Set<String> keys = serversLifetime.keySet();

            for (final String chave : keys) {
                if (!chave.equalsIgnoreCase(ip)) {
                    count = System.currentTimeMillis() - serversLifetime.get(chave);
                    if (count >= NODE_ACTIVITY_PERIOD) {
                        actualizaCarga(chave, (Double) null);
                    }
                }
            }
        }

    }

    private static final char MESSAGE_SEPARATOR = '|';
    private static final String EMPTY_STRING = "";
    private static final String NETWORK_CONFIG_PATH = "frontend/network.config";
    private static final String MAP_XML_PATH = "frontend/map.xml";
    private static final int NODE_ACTIVITY_PERIOD = 60000;
    private static final int DHT_UPDATE_PERIOD = 10000;
    private static List<String> listaRedes = null;
    private static DatagramSocket noVizinho = null;
    private static Map<String, Double> serversTable = Collections.synchronizedMap(new HashMap<String, Double>());
    private static Map<String, Long> serversLifetime = Collections.synchronizedMap(new HashMap<String, Long>());
    private static int portUdp = 4000;
    private static int portTcp = 3000;
    private static Interface stub = null;
    private static ServerSocket socketToReceiveMessages = null;

    private static Enum<?> estado = null;

    private static String host = null;

    private static String ip = null;

    public static Double count = 0.0;

    private static void actualizaCarga(final String ip, final Double valor) {
        serversTable.put(ip, valor);
    }

    private static void actualizaCarga(final String ip, final String valor) {
        try {
            actualizaCarga(ip, Double.parseDouble(valor));
        } catch (NumberFormatException | NullPointerException e) {
            actualizaCarga(ip, (Double) null);
        }
    }

    private static Boolean connectBackEnd(final String host) {

        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(host);
            stub = (Interface) registry.lookup("Interface");
            return stub.getTime() != null;

        } catch (RemoteException | NotBoundException e) {
            return false;
        }
    }

    private static boolean fileExist(final String file) {
        final File inFile = new File(file);
        if (!inFile.exists()) {
            return false;
        } else {
            return true;
        }
    }

    private static Enum<?> getComando(final Scanner teclado) {
        String comando;
        System.out.print("Comando> ");

        try {
            comando = teclado.nextLine();
            final ServerCommands testeCmd = ServerCommands.valueOf(comando.toUpperCase());
            return testeCmd;

        } catch (final IllegalArgumentException e) {
            return ServerCommands.INVALID;
        }
    }

    private static void getNearbyNode(final List<String> listaRedes) {
        StringBuilder novoIp = null;

        for (final String ip : listaRedes) {
            for (int i = 1; i < 255; i++) {
                novoIp = new StringBuilder();
                novoIp.append(ip);
                novoIp.deleteCharAt(ip.length() - 1);
                novoIp.append(i);

                try {
                    udpSoket(InetAddress.getByName(novoIp.toString()));
                } catch (final UnknownHostException e) {
                    novoIp = null;
                }

                if (noVizinho != null) {
                    return;
                }
            }
        }
    }

    private static void initializeServersLifetimeTable() {
        if (serversTable != null && !serversTable.isEmpty()) {
            final Set<String> keys = serversTable.keySet();

            serversLifetime.clear();

            for (final String key : keys) {
                serversLifetime.put(key, 0L);
            }
        }
    }

    private static void initializeServersTable() {
        serversTable.put(ip, load());
    }

    private static Double load() {
        final double mb = 1024 * 1024;
        double usedMb, freeMb, totalMb;

        final Runtime runtime = Runtime.getRuntime();
        freeMb = runtime.freeMemory() / mb;
        totalMb = runtime.totalMemory() / mb;
        usedMb = totalMb - freeMb;
        return usedMb;
    }

    @SuppressWarnings("unchecked")
    public static void main(final String args[]) {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            e.printStackTrace();
        }

        final Scanner teclado = new Scanner(System.in);
        if (args.length == 1) {
            host = args[0];
        } else {
            host = "localhost";
        }
        estado = util.ServerStatus.ONLINE;

        waitForBackendConnection(host);

        if (!fileExist(MAP_XML_PATH)) {
            listaRedes = new ArrayList<String>();
            listaRedes.addAll(readFileToList(NETWORK_CONFIG_PATH));

            getNearbyNode(listaRedes);

            if (noVizinho != null) {
                final InetAddress ip = noVizinho.getInetAddress();
                Socket socket = null;
                ObjectInputStream input = null;
                PrintStream output = null;

                try {
                    socket = new Socket(ip, portTcp);
                    input = new ObjectInputStream(socket.getInputStream());
                    output = new PrintStream(socket.getOutputStream());
                } catch (final IOException e1) {
                    e1.printStackTrace();
                }

                final StringBuilder mensagem = new StringBuilder();
                mensagem.append(FrontEndCommands.GET_TABLE.toString());
                mensagem.append(MESSAGE_SEPARATOR);
                mensagem.append(ip);
                mensagem.append(MESSAGE_SEPARATOR);
                mensagem.append(EMPTY_STRING);

                output.println(mensagem);
                output.close();

                try {
                    serversTable = (HashMap<String, Double>) input.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    initializeServersTable();
                }

            } else {
                initializeServersTable();
            }

        } else {
            XMLDecoder decoder = null;

            try {
                decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(MAP_XML_PATH)));
                serversTable = (HashMap<String, Double>) decoder.readObject();
                decoder.close();
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        initializeServersLifetimeTable();

        try {
            socketToReceiveMessages = new ServerSocket(portTcp);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final Runnable listenToConnections = new Runnable() {

            @Override
            public void run() {
                if (estado == ServerStatus.ONLINE) {
                    while (true) {
                        Socket socketC = null;
                        try {
                            socketC = socketToReceiveMessages.accept();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }

                        new Thread(new HandleReceivedMessageThread(socketC)).start();
                    }
                }

            }
        };

        new Thread(listenToConnections).start();
        new Timer().scheduleAtFixedRate(new DHTUpdateTimerTask(), 0, DHT_UPDATE_PERIOD);
        new Timer().scheduleAtFixedRate(new NodeActivityTimerTask(), NODE_ACTIVITY_PERIOD, NODE_ACTIVITY_PERIOD);
        printHelp();

        while (true) {

            switch (ServerCommands.parse(getComando(teclado).toString())) {
                case HALT:
                    estado = util.ServerStatus.OFLINE;
                    break;
                case RESTART:
                    estado = ServerStatus.ONLINE;
                    break;
                case HELP:
                    printHelp();
                    break;
                case EXIT:
                    updateMapXmlFile();

                    System.exit(-1);
                    break;
                case INVALID:
                    System.out.println("Comando inválido!");
                    break;

            }
        }
    }

    private static void printHelp() {
        System.out.println("\t---Comandos suportados pelo FrontEnd---");
        System.out.println();
        System.out.println("\t" + ServerCommands.HALT + "    -> Para de responder a pedidos");
        System.out.println("\t" + ServerCommands.RESTART + " -> Reinicia o servidor");
        System.out.println("\t" + ServerCommands.HELP + "    -> Lista os comandos disponiveis");
        System.out.println("\t" + ServerCommands.EXIT + "    -> Termina o servidor\n");

    }

    private static ArrayList<String> readFileToList(final String ficheiro) {
        final ArrayList<String> dados = new ArrayList<String>();
        String linha;
        FileReader leitor = null;
        BufferedReader in = null;

        try {
            leitor = new FileReader(ficheiro);
            in = new BufferedReader(leitor);

            while ((linha = in.readLine()) != null) {
                dados.add(linha);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                leitor.close();
                in.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        return dados;
    }

    private static boolean sendMessageTcp(final Socket socket, final String message) {
        PrintStream output = null;

        try {
            output = new PrintStream(socket.getOutputStream(), true);
            output.println(message);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    private static void sendTable(final Socket s) throws IOException {
        final ObjectOutputStream outputObject = new ObjectOutputStream(s.getOutputStream());
        outputObject.writeObject(serversTable);
        outputObject.flush();
    }

    private static void udpSoket(final InetAddress ip) {

        try {
            noVizinho = new DatagramSocket();
            noVizinho.bind(new InetSocketAddress(ip, portUdp));
        } catch (final SocketException e) {
            noVizinho = null;
        }
    }

    private static void updateMapXmlFile() {
        if (serversTable != null) {
            XMLEncoder encoder = null;

            try {
                encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(MAP_XML_PATH)));
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }

            encoder.writeObject(serversTable);
            encoder.flush();
            encoder.close();
        }
    }

    private static void updateServerLifetime(final String ip) {
        serversLifetime.put(ip, System.currentTimeMillis());
    }

    private static void waitForBackendConnection(final String host) {
        while (!connectBackEnd(host)) {
            System.out.println("Aguardar ligação!");
        }
        System.out.println("Servidor iniciado na porta 3000 para o BackEnd " + host);
    }
}