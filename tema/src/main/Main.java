package main;

import actor.ActorsAwards;
import checker.Checker;
import checker.Checkstyle;
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

import static java.util.stream.Collectors.toMap;
import static utils.Utils.stringToAwards;

/**
 * The entry point to this homework. It runs the checker that tests your implentation.
 */
public final class Main {
    /**
     * for coding style
     */
    public static Map<String, Integer> moviesRatedByUsers = new HashMap<>();
    public static Map<String, Double> moviesRatings = new HashMap<>();
    public static Map<String, Integer> usersThatRated = new HashMap<>();


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
                    arrayResult.add(processRatingCommands(action, input.getUsers(), input.getSerials(), writer));
                }
            } else if (action.getActionType().equals("query")) {
                if (action.getCriteria().equals("average") && action.getObjectType().equals("actors")) {
                    arrayResult.add(processAverageQuery(actions, writer, input.getMovies(), input.getSerials(), action));
                } else if ((action.getCriteria().equals("favorite") && action.getObjectType().equals("movies")) || (action.getCriteria().equals("favorite") && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processFavoriteQuery(action, input.getMovies(), input.getSerials(), input.getUsers(), writer));
                } else if (action.getCriteria().equals("longest") && action.getObjectType().equals("movies")) {
                    arrayResult.add(processLongestQuery(action, input.getMovies(), writer));
                } else if (action.getCriteria().equals("longest") && action.getObjectType().equals("shows")) {
                    arrayResult.add(processLongestShowQuery(action, input.getSerials(), writer));
                } else if ((action.getCriteria().equals("most_viewed") && action.getObjectType().equals("movies")) || (action.getCriteria().equals("most_viewed") && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processMostViewedMovieQuery(action, input.getMovies(), input.getSerials(), input.getUsers(), writer));
                } else if (action.getCriteria().equals("awards") && action.getObjectType().equals("actors")) {
                    arrayResult.add(processAwardsActors(action, input.getActors(), writer));
                } else if (action.getCriteria().equals("filter_description") && action.getObjectType().equals("actors")) {
                    arrayResult.add(processFilterDescription(action, input.getActors(), writer));
                } else if (action.getCriteria().equals("ratings") && action.getObjectType().equals("movies")) {
                    arrayResult.add(processRatingsMovies(action, input.getMovies(), writer));
                } else if (action.getCriteria().equals("ratings") && action.getObjectType().equals("shows")) {
                    arrayResult.add(processRatingsShows(action, input.getSerials(), writer));
                } else if (action.getCriteria().equals("num_ratings") && action.getObjectType().equals("users")) {
                    arrayResult.add(processUsersRatings(action, actions, writer));
                }
            }
        }
        return arrayResult;
    }

    public static JSONObject processFavoriteCommand(ActionInputData action, List<UserInputData> users, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();

        for (UserInputData user : users) {

            if (user.getUsername().equals(action.getUsername())) {
                if (user.getFavoriteMovies().contains(action.getTitle())) {
                    jsonObject = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() +  " is already in favourite list");
                } else if (!user.getHistory().containsKey(action.getTitle())) {
                    jsonObject = writer.writeFile(action.getActionId(), "", "error -> " + action.getTitle() + " is not seen");
                } else {
                    jsonObject = writer.writeFile(action.getActionId(), "", "success -> " + action.getTitle() + " was added as favourite");
                }
            }
        }

        return jsonObject;
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

    public static JSONObject processRatingCommands(ActionInputData action, List<UserInputData> users, List<SerialInputData> serials, Writer writer) throws IOException {
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
                    obj = writer.writeFile(action.getActionId(), "",
                            "success -> " + action.getTitle() +
                                    " was rated with " + action.getGrade() +
                                    " by " + action.getUsername());
                    moviesRatedByUsers.put(currentUserTitle, action.getSeasonNumber());
                    if (usersThatRated.containsKey(currentUser)) {
                        int value = usersThatRated.get(currentUser);
                        usersThatRated.put(currentUser, value + 1);
                    } else {
                        usersThatRated.put(currentUser, 1);
                    }
                }
            }
        }

        return obj;
    }

    public static JSONObject processAverageQuery(List<ActionInputData> actions, Writer writer, List<MovieInputData> movies, List<SerialInputData> serials, ActionInputData action) throws IOException {
        JSONObject obj = new JSONObject();
        List<ShowInput> all = new ArrayList<>();
        all.addAll(movies);
        all.addAll(serials);
        Map<ShowInput, Double> resultingFilms = new HashMap<>();
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
        Map<String, Integer> sorted;
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
        Map<String, Integer> sorted;
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

    public static JSONObject processAwardsActors (ActionInputData action, List<ActorInputData> actors, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> actionAwards = action.getFilters().get(3);
        List<ActorsAwards> actorsAwardsList = new ArrayList<>();
        Map<String, Integer> nrOfAwards = new HashMap<>();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();

        for (String s : actionAwards) {
            actorsAwardsList.add(stringToAwards(s));
        }

        for (ActorInputData actor : actors) {
            if (!(actor.getAwards().isEmpty())) {
                if ((actor.getAwards().keySet()).containsAll(actorsAwardsList)) {
                    int sum = actor.getAwards().values().stream().reduce(0, Integer::sum);
                    nrOfAwards.put(actor.getName(), sum);
                }
            }
        }

        if (action.getSortType().equals("asc")) {
            sorted = nrOfAwards.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = nrOfAwards.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processFilterDescription (ActionInputData action, List<ActorInputData> actors, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        List<String> actionWords = action.getFilters().get(2);
        List<String> filteredActors = new ArrayList<>();

        for (ActorInputData actor : actors) {
            String actorDescription = actor.getCareerDescription();
            List<String> temp = new ArrayList<>();

            for (String string : actionWords) {
                if (actorDescription.indexOf(string) != -1) {
                    temp.add(string);
                }
            }

            if (temp.containsAll(actionWords)) {
                filteredActors.add(actor.getName());
            }

            Collections.sort(filteredActors);
        }

        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + filteredActors);
        return jsonObject;
    }

    public static JSONObject processRatingsMovies(ActionInputData action, List<MovieInputData> movies, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Map<String, Double> moviesWithRating = new HashMap<>();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        Map<String, Double> sorted;
        Set<String> result = new LinkedHashSet<>();

        if (action.getSeasonNumber() == 0) {
            if (moviesRatings.containsKey(action.getTitle())) {
                Double temp = action.getGrade() + moviesRatings.get(action.getTitle());
                moviesRatings.put(action.getTitle(), temp);
            } else {
                moviesRatings.put(action.getTitle(), action.getGrade());
            }
        }

        for (MovieInputData movie : movies) {
           String year = String.valueOf(movie.getYear());
           List<String> genres = movie.getGenres();
           if ((year.equals(actionYear)) && (genres.contains(actionGender))) {
                if (moviesRatings.keySet().contains(movie.getTitle())) {
                    moviesWithRating.put(movie.getTitle(), moviesRatings.get(movie.getTitle()));
                }
           }
        }

        if (action.getSortType().equals("asc")) {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + result);

        return jsonObject;
    }

    public static JSONObject processRatingsShows (ActionInputData action, List<SerialInputData> serials, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Map<String, Double> moviesWithRating = new HashMap<>();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        Map<String, Double> sorted;
        Set<String> result = new LinkedHashSet<>();

        for (SerialInputData serial : serials) {
            if (serial.getTitle().equals(action.getTitle()) && moviesRatings.containsKey(action.getTitle())) {
                int nrOfSeasons = serial.getNumberSeason();
                Double temp = action.getGrade() + moviesRatings.get(action.getTitle());
                moviesRatings.put(action.getTitle(), temp/nrOfSeasons);
            } else {
                moviesRatings.put(action.getTitle(), action.getGrade());
            }
        }

        for (SerialInputData serial : serials) {
            String year = String.valueOf(serial.getYear());
            List<String> genres = serial.getGenres();
            if ((year.equals(actionYear)) && (genres.contains(actionGender))) {
                if (moviesRatings.keySet().contains(serial.getTitle())) {
                    moviesWithRating.put(serial.getTitle(), moviesRatings.get(serial.getTitle()));
                }
            }
        }

        if (action.getSortType().equals("asc")) {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return jsonObject;
    }

    public static JSONObject processUsersRatings (ActionInputData action, List<ActionInputData> actions, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();
        Map<String, Integer> ac = new HashMap<>();

        for (ActionInputData a : actions) {
                if (usersThatRated.containsKey(a.getUsername())) {
                    ac.put(a.getUsername(), usersThatRated.get(a.getUsername()));
                }
        }

        if (action.getSortType().equals("asc")) {
            sorted = ac.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = ac.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return jsonObject;
    }

}
