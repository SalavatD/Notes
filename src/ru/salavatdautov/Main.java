package ru.salavatdautov;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private enum OS {
        WINDOWS,
        LINUX,
        MAC,
        UNKNOWN
    }

    private static final String DATA_FILE_NAME = "notes_data.json";

    private static String notesPassword = "";
    private static ArrayList<Note> notes = new ArrayList<>();

    public static void main(String[] args) {
        try {
            parseArgs(args);
        } catch (IllegalArgumentException exception) {
            System.out.println("Argument error: " + exception.getMessage());
            System.out.println("Use -h for help.");
            exit(-1, false);
        }
        readFromFile();
        readPassword();
        mainLoop();
    }

    private static void parseArgs(String[] args) {
        if (args.length == 1 && args[0].equals("-h")) {
            printHelp();
            exit(0, false);
        } else if (args.length == 1) {
            throw new IllegalArgumentException("Not enough arguments.");
        }
        if (args.length > 2) {
            throw new IllegalArgumentException("Too many arguments.");
        }
        if (args.length == 2 && args[0].equals("-p")) {
            notesPassword = args[1];
        } else if (args.length == 2) {
            throw new IllegalArgumentException("Undefined arguments.");
        }
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("About:");
        System.out.println("    Utility for storing notes.");
        System.out.println("    Created 11.07.2021.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("    notes.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    -p <password> Password");
        System.out.println("    -h            Call help");
        System.out.println();
    }

    private static void exit(int code, boolean save) {
        if (save) {
            writeToFile();
        }
        System.exit(code);
    }

    private static void writeToFile() {
        sortNotes();
        try (FileWriter fileWriter = new FileWriter(DATA_FILE_NAME)) {
            new Gson().toJson(notes, fileWriter);
        } catch (IOException exception) {
            System.out.println("Input/output error: " + exception.getMessage());
            exit(-1, false);
        }
    }

    private static void sortNotes() {
        Collections.sort(notes);
    }

    private static void readFromFile() {
        File dataFile = new File(DATA_FILE_NAME);
        if (dataFile.exists() && dataFile.length() > 0) {
            try {
                notes = new Gson().fromJson(new FileReader(dataFile), new TypeToken<ArrayList<Note>>() {
                }.getType());
            } catch (IOException exception) {
                System.out.println("Input/output error: " + exception.getMessage());
                exit(-1, false);
            } catch (JsonSyntaxException exception) {
                System.out.println("Json syntax error: " + exception.getMessage());
            }
        }
        sortNotes();
    }

    private static void readPassword() {
        if (notesPassword.isEmpty()) {
            Scanner scanner = new Scanner(System.in, "UTF-8");
            while (notesPassword.isEmpty()) {
                System.out.print("Enter password: ");
                notesPassword = scanner.nextLine();
            }
        }
    }

    private static void mainLoop() {
        while (true) {
            clearScreen();
            printMenu();
            selectAction(readNumber());
        }
    }

    private static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OS.LINUX;
        } else if (osName.contains("mac")) {
            return OS.MAC;
        }
        return OS.UNKNOWN;
    }

    private static void clearScreen() {
        switch (getOS()) {
            case WINDOWS:
                try {
                    clearScreenWindows();
                } catch (InterruptedException exception) {
                    System.out.println("Input/output error: " + exception.getMessage());
                    exit(-1, false);
                } catch (IOException exception) {
                    System.out.println("Interrupted error: " + exception.getMessage());
                    exit(-1, false);
                }
                break;
            case LINUX:
            case MAC:
                clearScreenUnix();
                break;
            case UNKNOWN:
            default:
                System.out.println("Unknown OS.");
                System.exit(0);
        }
    }

    private static void clearScreenUnix() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void clearScreenWindows() throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
    }

    private static void printMenu() {
        System.out.println("1. List of notes");
        System.out.println("2. Details of note");
        System.out.println("3. Add new note");
        System.out.println("4. Edit note");
        System.out.println("5. Delete note");
        System.out.println("0. Exit");
        System.out.println();
        System.out.print("Action: ");
    }

    private static int readNumber() {
        return new Scanner(System.in).nextInt();
    }

    private static void selectAction(int optionNumber) {
        switch (optionNumber) {
            case 1:
                showAllNotes();
                break;
            case 2:
                showNoteDetails();
                break;
            case 3:
                newNote();
                break;
            case 4:
                editNote();
                break;
            case 5:
                deleteNote();
                break;
            case 0:
            default:
                exit(0, true);
        }
    }

    private static void showAllNotes() {
        clearScreen();
        for (int i = 0; i < notes.size(); i++) {
            System.out.printf("%d\n", i + 1);
            System.out.printf("Date:  %s\n", dateToString(notes.get(i).date));
            System.out.printf("Title: %s\n", decryptString(notes.get(i).title));
        }
        System.out.println();
        pause();
    }

    private static String decryptString(byte[] data) {
        AesCrypt aesCrypt = new AesCrypt();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            aesCrypt.decryptStream(inputStream, outputStream, notesPassword, data.length);
        } catch (GeneralSecurityException exception) {
            System.err.println("Internal error: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("Input/output error: " + exception.getMessage());
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void showNoteDetails() {
        System.out.print("\nEnter note number: ");
        int position = readNumber() - 1;
        clearScreen();
        try {
            System.out.printf("Date:  %s\n", dateToString(notes.get(position).date));
            System.out.printf("Title: %s\n", decryptString(notes.get(position).title));
            System.out.printf("Body:  %s\n", decryptString(notes.get(position).body));
        } catch (IndexOutOfBoundsException exception) {
            System.out.println("Wrong note number!");
        }
        System.out.println();
        pause();
    }

    private static void newNote() {
        clearScreen();
        Scanner scanner = new Scanner(System.in, "UTF-8");
        Note note = new Note();
        String dateString = "";
        Date date = null;
        while (dateString.isEmpty() || date == null) {
            System.out.print("Enter date (required): ");
            dateString = scanner.nextLine();
            date = tryParseDate(dateString);
        }
        note.date = date;
        String title = "";
        while (title.isEmpty()) {
            System.out.print("Enter title (required): ");
            title = scanner.nextLine();
        }
        note.title = encryptString(title);
        String body = "";
        while (body.isEmpty()) {
            System.out.print("Enter body (required): ");
            body = scanner.nextLine();
        }
        note.body = encryptString(body);
        notes.add(note);
        sortNotes();
        System.out.println();
        pause();
    }

    private static String dateToString(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return String.format("%02d.%02d.%04d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
    }

    private static void pause() {
        System.out.println("Press Enter to continue...");
        try {
            System.in.read();
        } catch (IOException exception) {
            System.out.println("Input/output error: " + exception.getMessage());
            exit(-1, false);
        }
    }

    private static Date tryParseDate(String dateString) {
        Date result;
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        dateFormat.setLenient(false);
        try {
            result = dateFormat.parse(dateString);
        } catch (ParseException e) {
            System.out.println("Date format: dd.MM.yyyy");
            return null;
        }
        return result;
    }

    private static byte[] encryptString(String data) {
        AesCrypt aesCrypt = new AesCrypt();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            aesCrypt.encryptStream(inputStream, outputStream, notesPassword);
        } catch (GeneralSecurityException exception) {
            System.err.println("Internal error: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("Input/output error: " + exception.getMessage());
        }
        return outputStream.toByteArray();
    }

    private static void editNote() {
        System.out.print("\nEnter note number: ");
        int position = readNumber() - 1;
        clearScreen();
        try {
            showNoteDetailsForEdit(position);
            Scanner scanner = new Scanner(System.in, "UTF-8");
            System.out.print("\nEnter number of value to edit: ");
            switch (readNumber()) {
                case 1:
                    String dateString = "";
                    Date date = null;
                    while (dateString.isEmpty() || date == null) {
                        System.out.print("Enter date (required): ");
                        dateString = scanner.nextLine();
                        date = tryParseDate(dateString);
                    }
                    notes.get(position).date = date;
                    sortNotes();
                    break;
                case 2:
                    String title = "";
                    while (title.isEmpty()) {
                        System.out.print("Enter title (required): ");
                        title = scanner.nextLine();
                    }
                    notes.get(position).title = encryptString(title);
                    break;
                case 3:
                    String body = "";
                    while (body.isEmpty()) {
                        System.out.print("Enter body (required): ");
                        body = scanner.nextLine();
                    }
                    notes.get(position).body = encryptString(body);
                    break;
                default:
                    break;
            }
        } catch (IndexOutOfBoundsException exception) {
            System.out.println("Wrong note number!");
        }
        System.out.println();
        pause();
    }

    private static void showNoteDetailsForEdit(int position) {
        System.out.printf("1. Date:  %s\n", dateToString(notes.get(position).date));
        System.out.printf("2. Title: %s\n", decryptString(notes.get(position).title));
        System.out.printf("3. Body:  %s\n", decryptString(notes.get(position).body));
        System.out.println("0. Exit");
    }

    private static void deleteNote() {
        System.out.print("\nEnter note number: ");
        int position = readNumber() - 1;
        clearScreen();
        try {
            notes.remove(position);
            System.out.println("Note removed.");
        } catch (IndexOutOfBoundsException exception) {
            System.out.println("Wrong note number!");
        }
        System.out.println();
        pause();
    }
}
