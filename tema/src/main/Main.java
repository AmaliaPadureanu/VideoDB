package main;

import checker.Checkstyle;
import checker.Checker;
import common.Constants;
import entertainment.Season;
import fileio.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;

/**
 * The entry point to this homework. It runs the checker that tests your implentation.
 */
public final class Main {
    /**
     * for coding style
     */
    public static Map<String, Integer> moviesRatedByUsers = new HashMap<>();

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
                if (action.getCriteria().equals("average") && action.getObjectType().equals("actors")) {
                    arrayResult.add(processAverageQuery(actions, input.getActors(), writer, input.getUsers(), input.getMovies(), input.getSerials(), action));
                } else if ((action.getCriteria().equals("favorite") && action.getObjectType().equals("movies")) || (action.getCriteria().equals("favorite") && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processFavoriteQuery(action, input.getMovies(), input.getSerials(), input.getUsers(), writer));
                } else if (action.getCriteria().equals("longest") && action.getObjectType().equals("movies")) {
                    arrayResult.add(processLongestQuery(action, input.getMovies(), writer));
                } else if (action.getCriteria().equals("longest") && action.getObjectType().equals("shows")) {
                    arrayResult.add(processLongestShowQuery(action, input.getSerials(), writer));
                } else if ((action.getCriteria().equals("most_viewed") && action.getObjectType().equals("movies")) || (action.getCriteria().equals("most_viewed") && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processMostViewedMovieQuery(action, input.getMovies(), input.getSerials(), input.getUsers(), writer));
                }
            }
        }
//        System.out.println(arrayResult);
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

        for(UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (user.getHistory().containsKey(action.getTitle())) {
                   Integer currentNrOfViews = user.getHistory().get(action.getTitle());
                   currentNrOfViews++;
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was viewed with total views of " + currentNrOfViews);
                } else {
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was viewed with total views of 1");
                }
            }
        }
        return obj;
    }

    public static JSONObject processRatingCommands(ActionInputData action, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        String currentUserTitle = action.getUsername() + action.getTitle();

        for (UserInputData user : users) {
            String currentUser = user.getUsername();

            if (currentUser.equals(action.getUsername())) {
                if (!(user.getHistory().containsKey(action.getTitle()))) {
                    obj = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() + " is not seen");
                } else if (moviesRatedByUsers.containsKey(currentUserTitle) && moviesRatedByUsers.get(currentUserTitle) == action.getSeasonNumber()) {
                    obj = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() + " has been already rated");
                } else {
                    obj = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was rated with " + action.getGrade() + " by " + action.getUsername());
                    moviesRatedByUsers.put(currentUserTitle, action.getSeasonNumber());
                }
            }
        }

        return obj;
    }

    public static JSONObject processAverageQuery(List<ActionInputData> actions, List<ActorInputData> actors, Writer writer, List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials, ActionInputData action) throws IOException {
        JSONObject obj = new JSONObject();
        List<ShowInput> all = new ArrayList<>();
        all.addAll(movies);
        all.addAll(serials);

        Map<ShowInput, Double> resultingFilms = new HashMap<>();
        LinkedHashMap<ShowInput, Double> sortedMap = new LinkedHashMap<>();
        int nr = action.getNumber();

        for (ShowInput s : all) {
            int count = 0;
            Double partialGrade = 0.0;
            Double finalGrade = 0.0;

            for (ActionInputData a : actions) {
                if (s.getTitle().equals(a.getTitle())) {
                    partialGrade += a.getGrade();
                    count++;
                }

                if (s instanceof SerialInputData) {
                    int nrOFseasons = ((SerialInputData) s).getNumberSeason();
                    finalGrade = partialGrade / nrOFseasons;
                } else {
                    finalGrade = partialGrade / count;
                }
            }

            if (finalGrade != 0.0 && !finalGrade.isNaN()) {
                resultingFilms.put(s, finalGrade);
            }
        }
        Map<ShowInput, Double> sortedByValue = resultingFilms.entrySet()
                .stream()
                .sorted(Map.Entry.<ShowInput, Double>comparingByValue())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        List<String> result = new ArrayList<>();

        List<String> nm = new ArrayList<>();
        for (ShowInput name : sortedByValue.keySet()) {
            nm.addAll(name.getCast().stream().sorted().collect(Collectors.toList()));
        }

        for (int i = 0; i < nr; i++) {
            if (i < nm.size()) {
                result.add(nm.get(i));
            }
        }

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processFavoriteQuery (ActionInputData action, List<MovieInputData> movies,List<SerialInputData> serials, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);
        List<String> totalFavorites = new ArrayList<>();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGenre = action.getFilters().get(1).get(0);
        String filteredMovie;
        Map<String, Integer> frequency = new HashMap<>();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();

        for (ShowInput video : videos) {
            String year = String.valueOf(video.getYear());
            List<String> genres = video.getGenres();
            if (actionYear != null) {
                if (actionYear.equals(year) && genres.contains(actionGenre)) {
                    filteredMovie = video.getTitle();
                    for (UserInputData user : users) {
                        for (String mo : user.getFavoriteMovies()) {
                            if (mo.equals(filteredMovie))
                                totalFavorites.add(mo);
                        }
                    }
                }
            }
        }

        Set<String> distinct = new HashSet<>(totalFavorites);
        for (String s : distinct) {
            frequency.put(s, Collections.frequency(totalFavorites, s));
        }

        if (action.getSortType().equals("desc")) {
           sorted  = frequency.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                   .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = frequency.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        }

        result.addAll(sorted.keySet());

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processLongestQuery (ActionInputData action, List<MovieInputData> movies, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        Map<String, Integer> moviesWithDuration = new HashMap<>();
        Map<String, Integer> sorted = new LinkedHashMap<>();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        Set<String> result = new LinkedHashSet<>();

        for (MovieInputData movie : movies) {
            String year = String.valueOf(movie.getYear());
            List<String> genres = movie.getGenres();
            if (actionYear != null) {
                if (actionYear.equals(year) && genres.contains(actionGender)) {
                    moviesWithDuration.put(movie.getTitle(), movie.getDuration());
                }
            }
        }

        if (action.getSortType().equals("asc")) {
            sorted = moviesWithDuration.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesWithDuration.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processLongestShowQuery (ActionInputData action, List<SerialInputData> serials, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        List<SerialInputData> filtered = new ArrayList<>();
        Map<String, Integer> serialsWithDuration = new HashMap<>();
        Map<String, Integer> sorted = new LinkedHashMap<>();
        Set<String> result = new HashSet<>();

        for (SerialInputData serial : serials) {
            String year = String.valueOf(serial.getYear());
            List<String> genres = serial.getGenres();
            if (actionYear != null) {
                if (actionYear.equals(year) && genres.contains(actionGender)) {
                    filtered.add(serial);
                }
            }
        }

        for (SerialInputData s : filtered) {
            ArrayList<Season> seasons = s.getSeasons();
            int total = 0;
            for (Season currentSeasion : seasons) {
                total += currentSeasion.getDuration();
            }
            serialsWithDuration.put(s.getTitle(), total);
        }

        if (action.getSortType().equals("asc")) {
            sorted = serialsWithDuration.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = serialsWithDuration.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processMostViewedMovieQuery (ActionInputData action, List<MovieInputData> movies, List<SerialInputData> serials,  List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        List<String> filtered = new ArrayList<>();
        Map<String, Integer> moviesViews = new HashMap<>();
        Map<String, Integer> sorted = new LinkedHashMap<>();
        Set<String> result = new LinkedHashSet<>();

        for (ShowInput video : videos) {
            String year = String.valueOf(video.getYear());
            List<String> genres = video.getGenres();
            if (actionYear != null) {
                if (year.equals(actionYear) && genres.contains(actionGender)) {
                    filtered.add(video.getTitle());
                }
            }
        }

        for (UserInputData user : users) {
            Map<String, Integer> viewedByUser = user.getHistory();
            for (String title : viewedByUser.keySet()) {
                if (filtered.contains(title) && (!moviesViews.containsKey(title))) {
                    moviesViews.put(title, viewedByUser.get(title));
                } else if (filtered.contains(title) && moviesViews.containsKey(title)) {
                    int value = viewedByUser.get(title) + moviesViews.get(title);
                    moviesViews.put(title, value);
                }
            }
        }

        if (action.getSortType().equals("asc")) {
            sorted = moviesViews.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesViews.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

}
