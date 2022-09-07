package main;

import checker.Checkstyle;
import checker.Checker;
import common.Constants;
import fileio.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The entry point to this homework. It runs the checker that tests your implentation.
 */
public final class Main {
    /**
     * for coding style
     */
    private Main() {
    }

    /**
     * Call the main checker and the coding style checker
     * @param args from command line
     * @throws IOException in case of exceptions to reading / writing
     */
    public static void main(final String[] args) throws IOException {
        File directory = new File(Constants.TESTS_PATH);
        Path path = Paths.get(Constants.RESULT_PATH);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        File outputDirectory = new File(Constants.RESULT_PATH);

        Checker checker = new Checker();
        checker.deleteFiles(outputDirectory.listFiles());

        for (File file : Objects.requireNonNull(directory.listFiles())) {

            String filepath = Constants.OUT_PATH + file.getName();
            File out = new File(filepath);
            boolean isCreated = out.createNewFile();
            if (isCreated) {
                action(file.getAbsolutePath(), filepath);
            }
        }

        checker.iterateFiles(Constants.RESULT_PATH, Constants.REF_PATH, Constants.TESTS_PATH);
        Checkstyle test = new Checkstyle();
        test.testCheckstyle();
    }

    /**
     * @param filePath1 for input file
     * @param filePath2 for output file
     * @throws IOException in case of exceptions to reading / writing
     */
    public static void action(final String filePath1,
                              final String filePath2) throws IOException {
        InputLoader inputLoader = new InputLoader(filePath1);
        Input input = inputLoader.readData();

        Writer fileWriter = new Writer(filePath2);

        //TODO add here the entry point to your implementation

       try {
           fileWriter.closeJSON(processActions(input, fileWriter));
       } catch (IOException exception) {

       };
    }

    public static JSONArray processActions(Input input, Writer writer) throws IOException {
        List<ActionInputData> actions = input.getCommands();
        JSONArray arrayResult = new JSONArray();

        for (ActionInputData action : actions) {
            if (action.getActionType().equals("command")) {
                if (action.getType().equals("favorite")) {
                   arrayResult.add(processFavoriteCommand(action, input.getUsers(), writer));
                } else if (action.getType().equals("view")) {
                    arrayResult.add(processViewCommands(action, input.getUsers(), writer));
                } else if (action.getType().equals("rating")) {
                    arrayResult.add(processRatingCommands(action, input.getUsers(), writer));
                }
            } else if (action.getActionType().equals("query")) {
                if (action.getType().equals("average")) {
                    arrayResult.add(processAverageQuery(action, input.getActors(), writer));
                }
            }
        }

        return arrayResult;
    }

    public static JSONObject processFavoriteCommand(ActionInputData action, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (user.getFavoriteMovies().contains(action.getTitle())) {
                    obj = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() +  " is already in favourite list");
                } else if (!user.getHistory().containsKey(action.getTitle())) {
                    obj = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() + " is not seen");
                } else {
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was added as favourite");
                }
            }
        }

        return obj;
    }

    public static JSONObject processViewCommands(ActionInputData action, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (!user.getHistory().containsKey(action.getTitle())) {
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was viewed with total views of 1");
                }
            }
        }

        return obj;
    }

    public static JSONObject processRatingCommands(ActionInputData action, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (user.getHistory().containsKey(action.getTitle())) {
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was rated with " + action.getGrade() + " by " + action.getUsername());
                }
            }
        }

        return obj;
    }

    public static JSONObject processAverageQuery(ActionInputData action, List<ActorInputData> actors, Writer writer) {
        JSONObject obj = new JSONObject();

        return obj;
    }


}
