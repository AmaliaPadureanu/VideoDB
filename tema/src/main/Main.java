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

import static utils.Utils.stringToAwards;

/**
 * The entry point to this homework. It runs the checker that tests your implentation.
 */
public final class Main {
    /**
     * for coding style
     */
    public static Map<String, Integer> moviesRatedByUsers = new HashMap<>();
    public static Map<String, Map<Integer, List<Double>>> moviesRatings = new HashMap<>();
    public static Map<String, Integer> usersThatRated = new HashMap<>();
    public static Map<String, Integer> moviesViewedByUser = new HashMap<>();

    private Main() {}

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

        initializeMoviesRatings(input);

        try {
            fileWriter.closeJSON(processActions(input, fileWriter));
        } catch (IOException exception) {

        };

        moviesRatings.clear();
        moviesRatedByUsers.clear();
        usersThatRated.clear();
        moviesViewedByUser.clear();
    }

    static void initializeMoviesRatings(Input input) {
        List<ShowInput> shows = new ArrayList<>();
        shows.addAll(input.getMovies());
        shows.addAll(input.getSerials());

        for (var show : shows) {
            Map<Integer, List<Double>> allSeasonsRatings = new HashMap<>();

            if (show instanceof SerialInputData) {
                for (int i = 1; i <= ((SerialInputData) show).getSeasons().size(); i++) {
                    List<Double> ratings = new ArrayList<Double>();
                    allSeasonsRatings.put(i, ratings);
                }
            } else {
                List<Double> ratings = new ArrayList<Double>();
                allSeasonsRatings.put(0, ratings);
            }

            moviesRatings.put(show.getTitle(), allSeasonsRatings);
        }
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
                if (action.getCriteria().equals("average")
                        && action.getObjectType().equals("actors")) {
                    arrayResult.add(processAverageQuery(actions, writer, input.getMovies(),
                            input.getSerials(), action, input.getActors()));
                } else if ((action.getCriteria().equals("favorite")
                        && action.getObjectType().equals("movies"))
                        || (action.getCriteria().equals("favorite")
                        && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processFavoriteQuery(action, input.getMovies(),
                            input.getSerials(), input.getUsers(), writer));
                } else if ((action.getCriteria().equals("longest")
                        && action.getObjectType().equals("movies"))
                        || (action.getCriteria().equals("longest")
                        && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processLongestQuery(action, input.getMovies(),
                            input.getSerials(), writer));
                }else if ((action.getCriteria().equals("most_viewed")
                        && action.getObjectType().equals("movies"))
                        || (action.getCriteria().equals("most_viewed")
                        && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processMostViewedMovieQuery(action, input.getMovies(),
                            input.getSerials(), input.getUsers(), writer));
                } else if (action.getCriteria().equals("awards")
                        && action.getObjectType().equals("actors")) {
                    arrayResult.add(processAwardsActors(action, input.getActors(), writer));
                } else if (action.getCriteria().equals("filter_description")
                        && action.getObjectType().equals("actors")) {
                    arrayResult.add(processFilterDescription(action, input.getActors(), writer));
                } else if ((action.getCriteria().equals("ratings")
                        && action.getObjectType().equals("movies"))
                        || (action.getCriteria().equals("ratings")
                        && action.getObjectType().equals("shows"))) {
                    arrayResult.add(processRatingsVideos(action, input.getSerials(), input.getMovies(),
                            writer));
                } else if (action.getCriteria().equals("num_ratings")
                        && action.getObjectType().equals("users")) {
                    arrayResult.add(processUsersRatings(action, actions, writer));
                }
            } else if (action.getActionType().equals("recommendation")) {
                if (action.getType().equals("standard")) {
                    arrayResult.add(processStandardRecommendation(action, input.getUsers(),
                            input.getMovies(), input.getSerials(), writer));
                } else if (action.getType().equals("best_unseen")) {
                    arrayResult.add(processBestUnseenRecommendation(action, input.getUsers(),
                            input.getMovies(), input.getSerials(), writer));
                } else if (action.getType().equals("popular")) {
                    arrayResult.add(processPopularRecommendation(action, input.getUsers(),
                            input.getMovies(), input.getSerials(), writer));
                } else if (action.getType().equals("favorite")) {
                    arrayResult.add(processFavoriteRecommendation(action, input.getUsers(),
                            input.getMovies(), input.getSerials(), writer));
                } else if (action.getType().equals("search")) {
                    arrayResult.add(processSearchRecommendation(action, input.getUsers(),
                            input.getMovies(), input.getSerials(), writer));
                }
            }
        }
        return arrayResult;
    }

    public static List<String> filterMovies(ActionInputData action, List<MovieInputData> movies) {
        List<String> actionsYear = action.getFilters().get(0);
        List<String> actionsGenre = action.getFilters().get(1);
        List<String> filtered = new ArrayList<>();

        for (MovieInputData movie : movies) {
            if (actionsYear.isEmpty() && actionsGenre.isEmpty()) {
                for (MovieInputData m : movies) {
                    filtered.add(m.getTitle());
                }
                break;
            }

            boolean isValid = true;

            if (!actionsYear.contains(null)
                    && !actionsYear.contains(String.valueOf(movie.getYear()))) {
                isValid = false;
            }

            if (!actionsGenre.contains(null)
                    && !movie.getGenres().containsAll(actionsGenre)) {
                isValid = false;
            }

            if (isValid) {
                filtered.add(movie.getTitle());
            }
        }
        return filtered;
    }

    public static List<String> filteredSerials(ActionInputData action, List<SerialInputData> serials) {
        List<String> actionsYear = action.getFilters().get(0);
        List<String> actionsGenre = action.getFilters().get(1);
        List<String> filtered = new ArrayList<>();

        for (SerialInputData serial : serials) {
            if (actionsYear.isEmpty() && actionsGenre.isEmpty()) {
                for (SerialInputData s : serials) {
                    filtered.add(s.getTitle());
                }
                break;
            }

            boolean isValid = true;

            if (!actionsYear.contains(null)
                    && !actionsYear.contains(String.valueOf(serial.getYear()))) {
                isValid = false;
            }

            if (!actionsGenre.contains(null)
                    && !serial.getGenres().containsAll(actionsGenre)) {
                isValid = false;
            }

            if (isValid) {
                filtered.add(serial.getTitle());
            }
        }
        return filtered;
    }

    public static List<String> filteredActors(ActionInputData action,
        List<ActorInputData> actors) {
        List<String> actionsWords = action.getFilters().get(2);
        List<String> actionsAwards = action.getFilters().get(3);
        List<String> filtered = new ArrayList<>();

        for (ActorInputData actor : actors) {
            if (actionsWords == null && actionsAwards == null) {
                for (ActorInputData act : actors) {
                    filtered.add(act.getName());
                }
                break;
            }

            boolean isValid = true;

            if (actionsWords !=null) {
                for (String word : actionsWords) {
                    if (!actor.getCareerDescription().contains(word)) {
                        isValid = false;
                        break;
                    }
                }
                isValid = false;
            }

            if (actionsAwards != null) {
                for (String award : actionsAwards)
                    if (!actor.getAwards().keySet().contains(award)) {
                        isValid = false;
                        break;
                    }
                isValid = false;
            }

            if (isValid) {
                filtered.add(actor.getName());
            }
        }
        return filtered;
    }

    public static JSONObject processFavoriteCommand(ActionInputData action,
        List<UserInputData> users, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();

        for (UserInputData user : users) {

            if (user.getUsername().equals(action.getUsername())) {
                if (user.getFavoriteMovies().contains(action.getTitle())) {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "error -> " + action.getTitle() +  " is already in favourite list");
                } else if (!user.getHistory().containsKey(action.getTitle())) {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "error -> " + action.getTitle() + " is not seen");
                } else {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "success -> " + action.getTitle() + " was added as favourite");
                    user.getFavoriteMovies().add(action.getTitle());
                }
            }
        }
        return jsonObject;
    }

    public static JSONObject processViewCommands(ActionInputData action,
                                                 List<UserInputData> users, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        String currentUserTitle = action.getUsername() + action.getTitle();

        for(UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (user.getHistory().containsKey(action.getTitle())) {
                    Integer currentNrOfViews = user.getHistory().get(action.getTitle());
                    currentNrOfViews++;
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "success -> " + action.getTitle()
                                    + " was viewed with total views of " + currentNrOfViews);
                    user.getHistory().put(action.getTitle(), currentNrOfViews);
                } else if (moviesViewedByUser.containsKey(currentUserTitle)) {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "success -> "
                                    + action.getTitle() + " was viewed with total views of "
                                    + (moviesViewedByUser.get(currentUserTitle)+1));
                    user.getHistory().put(action.getTitle(), moviesViewedByUser.get(currentUserTitle)+1);
                } else {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "success -> "
                                    + action.getTitle() + " was viewed with total views of 1");
                    user.getHistory().put(action.getTitle(), 1);
                    moviesViewedByUser.put(currentUserTitle, 1);
                }
            }
        }
        return jsonObject;
    }

    public static JSONObject processRatingCommands(ActionInputData action,
                                                   List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        String currentUserTitle = action.getUsername() + action.getTitle();

        for (UserInputData user : users) {
            String currentUser = user.getUsername();

            if (currentUser.equals(action.getUsername())) {
                if (!user.getHistory().containsKey(action.getTitle())) {
                    obj = writer.writeFile(action.getActionId(), "",
                            "error -> " + action.getTitle() + " is not seen");
                } else if (moviesRatedByUsers.containsKey(currentUserTitle)
                        && moviesRatedByUsers.get(currentUserTitle) == action.getSeasonNumber()) {
                    obj = writer.writeFile(action.getActionId(), "",
                            "error -> " + action.getTitle() + " has been already rated");
                } else {
                    obj = writer.writeFile(action.getActionId(), "",
                            "success -> " + action.getTitle() +
                                    " was rated with " + action.getGrade() +
                                    " by " + action.getUsername());
                    moviesRatedByUsers.put(currentUserTitle, action.getSeasonNumber());

                    Map<Integer, List<Double>> allSeasonsRatings = moviesRatings.get(action.getTitle());
                    List<Double> ratings = allSeasonsRatings.get(action.getSeasonNumber());
                    ratings.add(action.getGrade());

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

    public static JSONObject processAverageQuery(List<ActionInputData> actions,
                                                 Writer writer, List<MovieInputData> movies, List<SerialInputData> serials,
                                                 ActionInputData action, List <ActorInputData> actors) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> result = new ArrayList<>();
        List<String> filtered = filteredActors(action, actors);
        Map<String, Double> sorted;
        Map<String, List<Double>> videosContainingActors = new HashMap<>();

        for (String actor : filtered) {
            for (MovieInputData movie : movies) {
                if (movie.getCast().contains(actor) && moviesRatings.keySet().contains(movie.getTitle())) {
                    Double average = Helpers.getRatingAverageForShow(moviesRatings.get(movie.getTitle()));

                    if (!videosContainingActors.containsKey(actor)) {
                        videosContainingActors.put(actor, new ArrayList<>());
                    }

                    if (average > 0) {
                        videosContainingActors.get(actor).add(average);
                    }
                }
            }

            for (SerialInputData serial : serials) {
                if (serial.getCast().contains(actor) && moviesRatings.keySet().contains(serial.getTitle())) {
                    Double averageSerial = Helpers.getRatingAverageForShow(moviesRatings.get(serial.getTitle()));

                    if (!videosContainingActors.containsKey(actor)) {
                        videosContainingActors.put(actor, new ArrayList<>());
                    }

                    if (averageSerial > 0) {
                        videosContainingActors.get(actor).add(averageSerial);
                    }
                }
            }
        }

        videosContainingActors = videosContainingActors.entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));;

        Map<String, Double> actorRatings =
                videosContainingActors.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e-> e.getValue()
                                        .stream()
                                        .mapToDouble(Double::doubleValue)
                                        .average()
                                        .getAsDouble()));

        if (action.getSortType().equals("desc")) {
            sorted  = actorRatings.entrySet().stream()
                    .sorted(Helpers.ascDoubleComparator.reversed())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = actorRatings.entrySet().stream()
                    .sorted(Helpers.ascDoubleComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processFavoriteQuery (ActionInputData action,
                                                   List<MovieInputData> movies,List<SerialInputData> serials,
                                                   List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();

        List<String> totalFavorites = new ArrayList<>();
        List<String> filtered = new ArrayList<>();
        String filteredMovie;
        Map<String, Integer> frequency = new HashMap<>();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();

        if (action.getObjectType().equals("movies")) {
            filtered = filterMovies(action, movies);
        } else if (action.getObjectType().equals("shows")) {
            filtered = filteredSerials(action, serials);
        }

        for (String video : filtered) {
            for (UserInputData user : users) {
                for (String movie : user.getFavoriteMovies()) {
                    if (movie.equals(video)) {
                        totalFavorites.add(movie);
                    }
                }
            }
        }

        Set<String> distinct = new HashSet<>(totalFavorites);
        for (String s : distinct) {
            frequency.put(s, Collections.frequency(totalFavorites, s));
        }

        if (action.getSortType().equals("desc")) {
            sorted  = frequency.entrySet().stream()
                    .sorted(Helpers.ascComparator.reversed())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = frequency.entrySet().stream()
                    .sorted(Helpers.ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processLongestQuery (ActionInputData action,
                                                  List<MovieInputData> movies, List<SerialInputData> serials, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> filtered = new ArrayList<>();
        Map<String, Integer> videosWithDuration = new HashMap<>();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet();

        if (action.getObjectType().equals("movies")) {
            filtered = filterMovies(action, movies);
            for (MovieInputData movie : movies) {
                if (filtered.contains(movie.getTitle())) {
                    videosWithDuration.put(movie.getTitle(), movie.getDuration());
                }
            }
        } else if (action.getObjectType().equals("shows")) {
            filtered = filteredSerials(action, serials);
            for (SerialInputData serial : serials) {
                if (filtered.contains(serial.getTitle())) {
                    int sumSeasons = 0;
                    for (Season season : serial.getSeasons()) {
                        sumSeasons += season.getDuration();
                    }
                    videosWithDuration.put(serial.getTitle(), sumSeasons);
                }
            }
        }
        if (action.getSortType().equals("desc")) {
            sorted  = videosWithDuration.entrySet().stream()
                    .sorted(Helpers.ascComparator.reversed())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = videosWithDuration.entrySet().stream()
                    .sorted(Helpers.ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processMostViewedMovieQuery (ActionInputData action,
                                                          List<MovieInputData> movies, List<SerialInputData> serials,
                                                          List<UserInputData> users, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> filtered = new ArrayList<>();
        Map<String, Integer> moviesViews = new HashMap<>();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();

        if (action.getObjectType().equals("movies")) {
            filtered = filterMovies(action, movies);
        } else if (action.getObjectType().equals("shows")) {
            filtered = filteredSerials(action, serials);
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

        if (action.getSortType().equals("desc")) {
            sorted  = moviesViews.entrySet().stream()
                    .sorted(Helpers.ascComparator.reversed())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = moviesViews.entrySet().stream()
                    .sorted(Helpers.ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processAwardsActors (ActionInputData action,
                                                  List<ActorInputData> actors, Writer writer) throws IOException {
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

        if (action.getSortType().equals("desc")) {
            sorted  = nrOfAwards.entrySet().stream()
                    .sorted(Helpers.ascComparator.reversed())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = nrOfAwards.entrySet().stream()
                    .sorted(Helpers.ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processFilterDescription (ActionInputData action,
                                                       List<ActorInputData> actors, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        List<String> actionWords = action.getFilters().get(2);
        List<String> filteredActors = new ArrayList<>();

        for (ActorInputData actor : actors) {
            List<String> partialActionWords = new ArrayList<>();
            String[] actorDescription = actor.getCareerDescription().toLowerCase().split("[ -.]");

            for (String actionWord : actionWords) {
                if (Arrays.stream(actorDescription).toList().contains(actionWord)) {
                    partialActionWords.add(actionWord);
                }
            }

            if (partialActionWords.containsAll(actionWords)) {
                filteredActors.add(actor.getName());
            }


            if (action.getSortType().equals("desc")) {
                Collections.sort(filteredActors, Collections.reverseOrder());
            } else {
                Collections.sort(filteredActors);
            }

        }

        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + filteredActors);
        return jsonObject;
    }

    public static JSONObject processRatingsVideos (ActionInputData action,
                                                   List<SerialInputData> serials, List<MovieInputData> movies, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        List<String> filtered = new ArrayList<>();
        Map<String, Double> moviesWithRating = new HashMap<>();
        Map<String, Double> sorted;
        Set<String> result = new LinkedHashSet();

        if (action.getObjectType().equals("movies")) {
            filtered = filterMovies(action, movies);
            for (MovieInputData movie : movies) {
                if (filtered.contains(movie.getTitle())) {
                    double average = Helpers.getRatingAverageForShow(moviesRatings.get(movie.getTitle()));
                    if (average != 0.0) {
                        moviesWithRating.put(movie.getTitle(), average);
                    }
                }
            }
        } else if (action.getObjectType().equals("shows")) {
            filtered = filteredSerials(action, serials);
            for (SerialInputData serial : serials) {
                if (filtered.contains(serial.getTitle())) {
                    double average = Helpers.getRatingAverageForShow(moviesRatings.get(serial.getTitle()));
                    if (average != 0) {
                        moviesWithRating.put(serial.getTitle(), average);
                    }
                }
            }
        }

        if (action.getSortType().equals("asc")) {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesWithRating.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return jsonObject;
    }

    public static JSONObject processUsersRatings (ActionInputData action,
                                                  List<ActionInputData> actions, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Map<String, Integer> sorted;
        Set<String> result = new LinkedHashSet<>();
        Map<String, Integer> ac = new HashMap<>();

        for (ActionInputData a : actions) {
            if (usersThatRated.containsKey(a.getUsername())) {
                ac.put(a.getUsername(), usersThatRated.get(a.getUsername()));
            }
        }

        if (action.getSortType().equals("desc")) {
            sorted  = ac.entrySet().stream()
                    .sorted(Helpers.ascComparator.reversed())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = ac.entrySet().stream()
                    .sorted(Helpers.ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }
        result.addAll(sorted.keySet());

        jsonObject = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return jsonObject;
    }

    public static JSONObject processStandardRecommendation(ActionInputData action,
                                                           List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
                                                           Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Set<String> seenByUser = new HashSet<>();
        LinkedHashSet<String> unseen = new LinkedHashSet<>();
        String result = new String();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                seenByUser = user.getHistory().keySet();
            }
        }

        for (MovieInputData movie : movies) {
            if (!(seenByUser.contains(movie.getTitle()))) {
                unseen.add(movie.getTitle());
            }
        }

        if (unseen.isEmpty()) {
            for (SerialInputData serial : serials) {
                if (!(seenByUser.contains(serial.getTitle()))) {
                    unseen.add(serial.getTitle());
                }
            }
        }

        if (unseen.isEmpty()) {
            jsonObject = writer.writeFile(action.getActionId(), "",
                    "StandardRecommendation cannot be applied!");
            return jsonObject;
        } else {
            result = unseen.stream().findFirst().get();
        }

        jsonObject = writer.writeFile(action.getActionId(), "",
                "StandardRecommendation result: " + result);
        return jsonObject;
    }

    public static JSONObject processBestUnseenRecommendation(ActionInputData action,
    List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
    Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        String best = new String();
        Set<String> seenByUser = new HashSet<>();
        String result = null;
        Set<ShowInput> videos = new LinkedHashSet<>();
        videos.addAll(movies);
        videos.addAll(serials);

        Map<String, Double> averageRatings = Helpers
                .getMapKeyTitleValueAverageRatings(moviesRatings);

        Map<String, Double> sorted = averageRatings.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                seenByUser = user.getHistory().keySet();

                for (String bestRated : sorted.keySet()) {
                    if (!seenByUser.contains(bestRated)) {
                        result = bestRated;
                        return writer.writeFile(action.getActionId(), "",
                                "BestRatedUnseenRecommendation result: " + bestRated);
                    }
                }
            }
        }

        if (result == null) {
            for (UserInputData user : users) {
                if (user.getUsername().equals(action.getUsername())) {
                    for (ShowInput video : videos) {
                        if (!seenByUser.contains(video.getTitle())) {
                            result = video.getTitle();
                            jsonObject = writer.writeFile(action.getActionId(), "",
                                    "BestRatedUnseenRecommendation result: " + result);
                            return jsonObject;
                        }
                    }
                }
            }
        }

        return writer.writeFile(action.getActionId(), "",
                "BestRatedUnseenRecommendation cannot be applied!");
    }

    public static JSONObject processPopularRecommendation(ActionInputData action,
    List<UserInputData> users, List<MovieInputData> movies,
    List<SerialInputData> serials, Writer writer) throws IOException {
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);
        Map<String, Integer> nrOfViews = new HashMap<>();
        List<String> mostPopularGenre = new ArrayList<>();
        List<String> videosInAlphaOrder = new ArrayList<>();

        if (Helpers.isPremium(users, action.getUsername())) {
            return writer.writeFile(action.getActionId(), "",
                    "PopularRecommendation cannot be applied!");
        }

        for (UserInputData user : users) {
            for (String key : user.getHistory().keySet()) {
                if (nrOfViews.containsKey(key)) {
                    int value = user.getHistory().get(key) + nrOfViews.get(key);
                    nrOfViews.put(key, value);
                } else {
                    nrOfViews.put(key, user.getHistory().get(key));
                }
            }
        }

        while (!nrOfViews.isEmpty()) {
            Map.Entry<String, Integer> entryWithMaxValue = null;
            for (Map.Entry<String, Integer> entry : nrOfViews.entrySet()) {

                if (entryWithMaxValue == null || entry.getValue().compareTo(entryWithMaxValue.getValue()) > 0) {
                    entryWithMaxValue = entry;
                }
            }

            nrOfViews.remove(entryWithMaxValue.getKey());

            for (ShowInput video : videos) {
                if (video.getTitle().equals(entryWithMaxValue.getKey())) {
                    mostPopularGenre = video.getGenres();
                }
            }

            for (ShowInput video : videos) {
                boolean matchGenre = video.getGenres().stream().anyMatch(mostPopularGenre::contains);
                if (matchGenre) {
                    videosInAlphaOrder.add(video.getTitle());
                }
            }

            videosInAlphaOrder.stream().sorted();

            for (UserInputData user : users) {
                if (user.getUsername().equals(action.getUsername())) {
                    for (String videoTitle : videosInAlphaOrder) {
                        if (!user.getHistory().keySet().contains(videoTitle)) {
                            return writer.writeFile(action.getActionId(), "",
                                    "PopularRecommendation result: " + videoTitle);
                        }
                    }
                }
            }
        }

        return writer.writeFile(action.getActionId(), "",
                "PopularRecommendation cannot be applied!");
    }

    public static JSONObject processFavoriteRecommendation(ActionInputData action,
    List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
    Writer writer) throws IOException {
        if (Helpers.isPremium(users, action.getUsername())) {
           return writer.writeFile(action.getActionId(), "",
                    "FavoriteRecommendation cannot be applied!");
        }

        List<String> favorites = users.stream()
                .map(user -> user.getFavoriteMovies())
                .flatMap(List::stream)
                .collect(Collectors.toList());

        Set<String> countFavorites = new HashSet<>(favorites);
        Map<String, Integer> nrOfFavorites = new HashMap<>();

        for (String title : countFavorites) {
            nrOfFavorites.put(title, Collections.frequency(favorites, title));
        }

        Map<String, Integer> sorted = nrOfFavorites.entrySet().stream()
                .sorted(Helpers.nonAlphabeticComparator.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                for (String key : sorted.keySet()) {
                    if (!user.getHistory().containsKey(key)) {
                        return writer.writeFile(action.getActionId(), "",
                                "FavoriteRecommendation result: " + key);
                    }
                }
            }
        }

        return writer.writeFile(action.getActionId(), "",
                "FavoriteRecommendation cannot be applied!");
    }

    public static JSONObject processSearchRecommendation(ActionInputData action,
                                                         List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
                                                         Writer writer) throws IOException {
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);
        List<ShowInput> actionGenreVideos = new ArrayList<>();
        Map<String, Double> videosByRatings = new HashMap<>();
        Map<String, Double> sorted = new HashMap<>();
        Set<String> result = new LinkedHashSet<>();

        if (Helpers.isPremium(users, action.getUsername())) {
            return writer.writeFile(action.getActionId(), "",
                    "SearchRecommendation cannot be applied!");
        }

        for (ShowInput video : videos) {
            if (video.getGenres().contains(action.getGenre())) {
                actionGenreVideos.add(video);
            }
        }

        Map<String, Double> videosRatings = Helpers.getMapKeyTitleValueAverageRatings(moviesRatings);

        for (ShowInput video : actionGenreVideos) {
            if (videosRatings.containsKey(video.getTitle())) {
                videosByRatings.put(video.getTitle(), videosRatings.get(video.getTitle()));
            } else {
                videosByRatings.put(video.getTitle(), 0.0);
            }
        }

        sorted  = videosByRatings.entrySet().stream()
                .sorted(Helpers.ascDoubleComparator)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                for (String videoTitle : sorted.keySet()) {
                    if (!user.getHistory().containsKey(videoTitle)) {
                        result.add(videoTitle);
                    }
                }
            }
        }

        if (result.isEmpty()) {
            return writer.writeFile(action.getActionId(), "", "SearchRecommendation cannot be applied!" );
        }

        return writer.writeFile(action.getActionId(), "", "SearchRecommendation result: " + result);
    }
}
