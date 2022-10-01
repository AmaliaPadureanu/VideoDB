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
import java.util.stream.StreamSupport;

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
    public static Map<String, Integer> moviesViewedByUser = new HashMap<>();

    static Comparator<Map.Entry<String, Integer>> ascComparator = new Comparator<>() {
        @Override public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
            String v1 = e1.getKey();
            String v2 = e2.getKey();

            if (e1.getValue() == e2.getValue()) {
                return v1.compareTo(v2);
            }

            return e1.getValue() - e2.getValue();
        }
    };

    static Comparator<Map.Entry<String, Integer>> descComparator = new Comparator<>() {
        @Override public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
            String v1 = e1.getKey();
            String v2 = e2.getKey();

            if (e1.getValue() == e2.getValue()) {
                return v2.compareTo(v1);
            }

            return e2.getValue() - e1.getValue();
        }
    };

    static Comparator<Map.Entry<String, Double>> ascDoubleComparator = new Comparator<>() {
        @Override
        public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
            String v1 = o1.getKey();
            String v2 = o2.getKey();

            if (o1.getValue() == o2.getValue()) {
                return v1.compareTo(v2);
            }
            return (int) (o1.getValue() - o2.getValue());
        }
    };

    static Comparator<Map.Entry<String, Double>> descDoubleComparator = new Comparator<>() {
        @Override public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
            String v1 = o1.getKey();
            String v2 = o2.getKey();

            if (o1.getValue() == o2.getValue()) {
                return v2.compareTo(v1);
            }

            return (int) (o2.getValue() - o1.getValue());
        }
    };



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

       moviesRatings.clear();
       moviesRatedByUsers.clear();
       usersThatRated.clear();
       moviesViewedByUser.clear();
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
                } else if (action.getCriteria().equals("longest")
                        && action.getObjectType().equals("movies")) {
                    arrayResult.add(processLongestQuery(action, input.getMovies(), writer));
                } else if (action.getCriteria().equals("longest")
                        && action.getObjectType().equals("shows")) {
                    arrayResult.add(processLongestShowQuery(action, input.getSerials(), writer));
                } else if ((action.getCriteria().equals("most_viewed")
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
                } else if (action.getCriteria().equals("ratings")
                        && action.getObjectType().equals("movies")) {
                    arrayResult.add(processRatingsMovies(action, input.getMovies(), writer));
                } else if (action.getCriteria().equals("ratings")
                        && action.getObjectType().equals("shows")) {
                    arrayResult.add(processRatingsShows(action, input.getSerials(), writer));
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

            if (!actionsYear.contains(null) && !actionsYear.contains(String.valueOf(movie.getYear()))) {
                isValid = false;
            }

            if (!actionsGenre.contains(null) && !movie.getGenres().containsAll(actionsGenre)) {
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

            if (!actionsYear.contains(null) && !actionsYear.contains(String.valueOf(serial.getYear()))) {
                isValid = false;
            }

            if (!actionsGenre.contains(null) && !serial.getGenres().containsAll(actionsGenre)) {
                isValid = false;
            }

            if (isValid) {
                filtered.add(serial.getTitle());
            }
        }
        return filtered;
    }

    public static List<String> filteredActors(ActionInputData action, List<ActorInputData> actors) {
        List<String> actionsWards = action.getFilters().get(2);
        List<String> actionsAwards = action.getFilters().get(3);
        List<String> filtered = new ArrayList<>();

        for (ActorInputData actor : actors) {
            if (actionsWards == null && actionsAwards == null) {
                for (ActorInputData act : actors) {
                    filtered.add(act.getName());
                }
                break;
            }

            boolean isValid = true;

            if (actionsWards !=null) {
                for (String word : actionsWards) {
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
                    moviesRatings.put(action.getTitle(), action.getGrade());
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
        List<String> filtered;
        filtered = filteredActors(action, actors);
        Map<String, Double> sorted;

        System.out.println(filtered);

        List<ShowInput> all = new ArrayList<>();
        all.addAll(movies);
        all.addAll(serials);
        Map<String, Double> videosContainingActors = new HashMap<>();
        Map<String, Double> actorsGrades = new HashMap<>();

        for (String actor : filtered) {
            for (ShowInput s : all) {
                if (s.getCast().contains(actor)) {
                   videosContainingActors.put(actor,  moviesRatings.get(s.getTitle()));

                }
            }
        }

        for (Map.Entry<String, Double> entry : videosContainingActors.entrySet()) {
            if (entry.getValue() != null) {
                actorsGrades.put(entry.getKey(), entry.getValue());
            }
        }

            System.out.println(actorsGrades);

        if (action.getSortType().equals("desc")) {
            sorted  = actorsGrades.entrySet().stream()
                    .sorted(descDoubleComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = actorsGrades.entrySet().stream()
                    .sorted(ascDoubleComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }


//        if (action.getSortType().equals("asc")) {
//            sorted = actorsGrades.entrySet().stream()
//                    .sorted(Map.Entry.comparingByValue())
//                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
//                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
//        } else {
//            sorted = actorsGrades.entrySet().stream()
//                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
//                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
//        }

        result.addAll(sorted.keySet());
        System.out.println(result);
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
                   .sorted(descComparator)
                   .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                           Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = frequency.entrySet().stream()
                    .sorted(ascComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());

        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processLongestQuery (ActionInputData action,
        List<MovieInputData> movies, Writer writer) throws IOException {
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
            sorted = moviesWithDuration.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesWithDuration.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

    public static JSONObject processLongestShowQuery (ActionInputData action,
        List<SerialInputData> serials, Writer writer) throws IOException {
        JSONObject obj = new JSONObject();
        String actionYear = action.getFilters().get(0).get(0);
        String actionGender = action.getFilters().get(1).get(0);
        List<SerialInputData> filtered = new ArrayList<>();
        Map<String, Integer> serialsWithDuration = new HashMap<>();
        Map<String, Integer> sorted;
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
            sorted = serialsWithDuration.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = serialsWithDuration.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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

        if (action.getSortType().equals("asc")) {
            sorted = moviesViews.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = moviesViews.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        result.addAll(sorted.keySet());
        obj = writer.writeFile(action.getActionId(), "", "Query result: " + result);
        return obj;
    }

//    private static boolean compareLists(ArrayList<String> list1, ArrayList<String> list2) {
//        int count = Math.min(list1.size(), list2.size());
//
//        for (int i = 0; i < list1.size(); i++) {
//            if (list1.get(1) != list2.get(1)) {
//                return false;
//            }
//        }
//
//        return true;
//    }

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

        if (action.getSortType().equals("asc")) {
            sorted = nrOfAwards.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted = nrOfAwards.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(action.getNumber())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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
        List<String> filtered = filteredActors(action, actors);
        //System.out.println(filtered);


        for (ActorInputData actor : actors) {
            List<String> temp = new ArrayList<>();
            String[] actorDescription = actor.getCareerDescription().toLowerCase().split(" ");

            for (String string : actionWords) {
                if (Arrays.stream(actorDescription).toList().contains(string)) {
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

    public static JSONObject processRatingsMovies(ActionInputData action,
        List<MovieInputData> movies, Writer writer) throws IOException {
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

    public static JSONObject processRatingsShows (ActionInputData action,
        List<SerialInputData> serials, Writer writer) throws IOException {
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
                    .sorted(descComparator)
                    .limit(action.getNumber()).collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            sorted  = ac.entrySet().stream()
                    .sorted(ascComparator)
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
            result = "";
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
        String result = new String();
        Set<ShowInput> videos = new LinkedHashSet<>();
        videos.addAll(movies);
        videos.addAll(serials);

        Map<String, Double> sorted = moviesRatings.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        best = sorted.keySet().stream().findFirst().get();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                seenByUser = user.getHistory().keySet();
                if (seenByUser.contains(best)) {
                    for (ShowInput video : videos) {
                        if (!seenByUser.contains(video.getTitle())) {
                            result = video.getTitle();
                            break;
                        }
                    }
                } else {
                    result = best;
                }
            }
        }

        jsonObject = writer.writeFile(action.getActionId(), "",
                "BestRatedUnseenRecommendation result: " + result);
        return jsonObject;
    }

    public static JSONObject processPopularRecommendation(ActionInputData action,
         List<UserInputData> users, List<MovieInputData> movies,
             List<SerialInputData> serials, Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        String result = new String();

        Map<String, Integer> noViews = new HashMap<>();

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (!user.getSubscriptionType().equals("PREMIUM")) {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "PopularRecommendation cannot be applied!");
                    return jsonObject;
                }
            }
        }


        return jsonObject;
    }

    public static JSONObject processFavoriteRecommendation(ActionInputData action,
        List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
            Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Map<String, Integer> nrOfFavorites = new HashMap<>();
        Map<String, Integer> sorted;
        List<String> favorites = new ArrayList<>();
        String result = "";
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                if (!user.getSubscriptionType().equals("PREMIUM")) {
                    jsonObject = writer.writeFile(action.getActionId(), "",
                            "FavoriteRecommendation cannot be applied!");
                    return jsonObject;
                }
            }
        }

        for (UserInputData user : users) {
            if (!user.getUsername().equals(action.getUsername())) {
                favorites.addAll(user.getFavoriteMovies());
            }
        }

        Set<String> countFavorites = new HashSet<>(favorites);
        for (String title : countFavorites) {
            nrOfFavorites.put(title, Collections.frequency(favorites, title));
        }

        sorted = nrOfFavorites.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                for (String key : sorted.keySet()) {
                    if (!user.getHistory().containsKey(key)) {
                        result = key;
                        break;
                    }
                }
            }
        }

        if (!result.equals("")) {
            jsonObject = writer.writeFile(action.getActionId(), "",
                    "FavoriteRecommendation result: " + result);
        } else {
            for (UserInputData user : users) {
                if (user.getUsername().equals(action.getUsername())) {
                    for (ShowInput video : videos) {
                        if (!user.getHistory().containsKey(video.getTitle())) {
                            result = video.getTitle();
                            jsonObject = writer.writeFile(action.getActionId(), "",
                                    "FavoriteRecommendation result: " + result);
                            break;
                        }
                    }
                }
            }

        }
        return jsonObject;
    }

    public static JSONObject processSearchRecommendation(ActionInputData action,
        List<UserInputData> users, List<MovieInputData> movies, List<SerialInputData> serials,
            Writer writer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        List<ShowInput> videos = new ArrayList<>();
        videos.addAll(movies);
        videos.addAll(serials);
        List<String> notSeen = new ArrayList<>();
        List<String> result;
        Map<String, Double> ratingsGenre = new HashMap<>();
        Set<String> resultt = new LinkedHashSet<>();

        List<String> allGenres = new ArrayList<>();
        for (ShowInput video : videos) {
            allGenres.addAll(video.getGenres());
        }

        if (!allGenres.contains(action.getGenre())) {
            jsonObject = writer.writeFile(action.getActionId(), "",
                    "SearchRecommendation cannot be applied!");
            return jsonObject;
        }

        Map<String, Double> sorted = moviesRatings.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

//        for (UserInputData user : users) {
//            if (user.getUsername().equals(action.getUsername())) {
//                for (ShowInput video : videos) {
//                    if (!(user.getHistory().containsKey(video)) && video.getGenres().contains(action.getGenre())) {
//                        notSeen.add(video.getTitle());
//                    }
//                }
//            }
//        }
//
//
//
//        for (String st : sorted.keySet()) {
//            if (notSeen.contains(st)) {
//                resultt.add(st);
//            }
//        }
//
//        Set<String> s = new LinkedHashSet<>();
//
//                s = notSeen.stream().sorted().collect(Collectors.toSet());
//                resultt.addAll(s);




        for (UserInputData user : users) {
            if (user.getUsername().equals(action.getUsername())) {
                for (ShowInput video : videos) {
                    if (!user.getHistory().containsKey(video.getTitle()) && video.getGenres().contains(action.getGenre())) {
                        notSeen.add(video.getTitle());
                    }
                }
            }
        }

        if (notSeen.isEmpty()) {
            jsonObject = writer.writeFile(action.getActionId(), "",
                    "SearchRecommendation cannot be applied!");
            return jsonObject;
        } else  {
            notSeen.stream().sorted().collect(Collectors.toList());
            resultt.addAll(notSeen);
        }
        jsonObject = writer.writeFile(action.getActionId(), "", "SearchRecommendation result: " + resultt);
        System.out.println(resultt);

//        if (resultt.isEmpty()) {
//            jsonObject = writer.writeFile(action.getActionId(), "",
//                    "SearchRecommendation cannot be applied!");
//            return jsonObject;
//        }
//
//        jsonObject = writer.writeFile(action.getActionId(), "", "SearchRecommendation result: " + resultt);
//        System.out.println(resultt);
        return jsonObject;
    }
}
