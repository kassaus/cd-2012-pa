package backend;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Scanner;

import util.ServerCommands;
import util.ServerStatus;

public class ServerBackEnd extends UnicastRemoteObject implements Interface {

    protected ServerBackEnd() throws RemoteException {
        
    }

    private static final long serialVersionUID = -3630196051037801150L;
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

        System.out.println("RMI server started");

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
            System.out.println("Security manager installed.");
        } else
            System.out.println("Security manager already exists.");

        try {
            LocateRegistry.createRegistry(2005);
            System.out.println("java RMI registry created.");
        } catch (RemoteException e) {
            System.out.println("java RMI registry already exists.");
        }

        try {
            for (String arg : args) {
                System.out.println(arg);
            }

            final ServerBackEnd obj = new ServerBackEnd();

            Naming.rebind("ServerBackEnd", obj);
            System.out.println("ServerBackEnd bound in registry");

        } catch (Exception e) {
            System.err.println("RMI server exception:");
            e.printStackTrace();
        }

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
    }

    private static void printHelp() {
        System.out.println("\t---Comandos suportados pelo BackEnd---");
        System.out.println();
        System.out.println("\t" + ServerCommands.HALT + "    -> Para de responder a pedidos");
        System.out.println("\t" + ServerCommands.RESTART + " -> Reinicia o servidor");
        System.out.println("\t" + ServerCommands.HELP + "    -> Lista os comandos disponiveis");
        System.out.println("\t" + ServerCommands.EXIT + "    -> Termina o servidor\n");
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
