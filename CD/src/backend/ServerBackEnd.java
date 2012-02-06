package backend;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Scanner;

import util.ServerCommands;
import util.ServerStatus;

public class ServerBackEnd implements Interface {

    private static Enum<?> estado = null;

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

    public static void main(final String args[]) {
        final Scanner teclado = new Scanner(System.in);

        try {
            final ServerBackEnd obj = new ServerBackEnd();
            final Interface stub = (Interface) UnicastRemoteObject.exportObject(obj, 0);

            final Registry registry = LocateRegistry.getRegistry();
            registry.bind("Interface", stub);
            estado = ServerStatus.ONLINE;

            printHelp();

            while (true) {

                switch (ServerCommands.parse(getComando(teclado).toString())) {
                    case HALT:
                        estado = ServerStatus.OFLINE;
                        break;
                    case RESTART:
                        estado = ServerStatus.ONLINE;
                        break;
                    case HELP:
                        printHelp();
                        break;
                    case EXIT:
                        System.exit(-1);
                        break;
                    case INVALID:
                        System.out.println("Comando inválido!");
                        break;
                }
            }

        } catch (final Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("\t---Comandos suportados pelo BackEnd---");
        System.out.println();
        System.out.println("\t" + ServerCommands.HALT + "    -> Para de responder a pedidos");
        System.out.println("\t" + ServerCommands.RESTART + " -> Reinicia o servidor");
        System.out.println("\t" + ServerCommands.HELP + "    -> Lista os comandos disponiveis");
        System.out.println("\t" + ServerCommands.EXIT + "    -> Termina o servidor\n");
    }

    private ServerBackEnd() {
    }

    @Override
    public String getTime() {

        if (estado == ServerStatus.ONLINE) {
            final Calendar cal = Calendar.getInstance();
            return cal.getTime().toString();
        } else {
            return null;
        }
    }

}
