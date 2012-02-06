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
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

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
    private static void getTable(final Socket socket) {
        PrintStream output = null;
        ObjectInputStream input = null;

        try {
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
        ;
        String host = frontEndsList.getNextHost();
        String lastHost = null;
        PrintStream output = null;
        Socket socket = null;
        BufferedReader stringInput = null;
        int triesCount = 0;
        Long lastAttempt = null;
        cmd = "";

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
                        socket = new Socket(host, portTcp);
                        getTable(socket);

                        final List<String> sortedIpList = getSortedIpList();

                        updateNodesConfigFile(sortedIpList);
                        updateFrontEndsList();

                        if (!host.equalsIgnoreCase(sortedIpList.get(0))) {
                            host = lastHost = sortedIpList.get(0);
                            socket = new Socket(host, portTcp);
                        }
                    }

                    stringInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output = new PrintStream(socket.getOutputStream(), true);

                    System.out.print(host + ":" + portTcp + "> ");

                    if (cmd.isEmpty()) {
                        final Scanner scan = new Scanner(System.in);
                        cmd = scan.nextLine();
                        if (FrontEndCommands.parse(cmd.toUpperCase()).toString().equals(FrontEndCommands.EXIT.name())) {
                            System.exit(-1);
                        }
                    }

                    output.println(getMessage(cmd.toUpperCase()));

                    final StringBuilder str = new StringBuilder();
                    while (!(receivedMessage = stringInput.readLine()).isEmpty()) {
                        str.append(receivedMessage + "\n");
                    }

                    receivedMessage = str.toString();

                    if (!receivedMessage.isEmpty()) {
                        System.out.println("\nRecebido : \n" + receivedMessage);
                        if (cmd.equalsIgnoreCase("quit")) {
                            System.out.println("a sair..");
                            break;
                        }
                        cmd = "";
                    } else {
                        host = null;
                        try {
                            stringInput.close();
                        } catch (final Exception e) {
                        }
                        try {
                            output.close();
                        } catch (final Exception e) {
                        }

                        try {
                            socket.close();
                        } catch (final Exception e) {
                        }
                    }
                }

            } catch (final Exception ex) {
                if (triesCount == 0) {
                    System.out.println("Ocorreu um erro ao tentar aceder ao servidor. Aguarde enquanto tentamos uma nova ligação...");
                } else if (triesCount <= 5) {
                    System.out.println("Tentativa de ligação número " + triesCount);
                } else {
                    System.out.println("Ocorreu um erro ao tentar aceder ao servidor. Aguarde enquanto tentamos uma ligação com outro servidor...");
                }

                try {
                    stringInput.close();
                } catch (final Exception e) {
                }

                try {
                    output.close();
                } catch (final Exception e) {
                }

                try {
                    socket.close();
                } catch (final Exception e) {
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

        stringInput.close();
        output.close();
        socket.close();
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
