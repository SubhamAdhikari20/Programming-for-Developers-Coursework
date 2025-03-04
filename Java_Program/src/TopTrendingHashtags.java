import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

// This class manages auto-increment primary keys by reading from/writing to a text file.
class PrimaryKeyGenerator {
    private static final String FILE_NAME = "ids.txt";
    private static int currentUserId;
    private static int currentTweetId;

    // Static initializer: load keys from file if exists; otherwise, set default values.
    static {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                if (scanner.hasNextLine()) {
                    // Expecting a line like "userId: 135"
                    String userLine = scanner.nextLine().trim();
                    currentUserId = Integer.parseInt(userLine.split(":")[1].trim());
                }
                if (scanner.hasNextLine()) {
                    // Expecting a line like "tweetId: 13"
                    String tweetLine = scanner.nextLine().trim();
                    currentTweetId = Integer.parseInt(tweetLine.split(":")[1].trim());
                }
            }
            catch (Exception e) {
                System.err.println("Error reading primary keys. Using default values.");
                currentUserId = 0;
                currentTweetId = 0;
            }
        }
        else {
            currentUserId = 0;
            currentTweetId = 0;
        }
    }

    // Updates the file with the current keys in the format:
    // userId: <currentUserId>
    // tweetId: <currentTweetId>
    private static void updateFile() {
        try (FileWriter writer = new FileWriter(FILE_NAME, false)) {
            writer.write("userId: " + currentUserId + "\n");
            writer.write("tweetId: " + currentTweetId);
        } catch (IOException e) {
            System.err.println("Error updating primary keys file: " + e.getMessage());
        }
    }

    public static synchronized int getNextUserId() {
        int id = currentUserId;
        currentUserId++;
        updateFile();
        return id;
    }

    public static synchronized int getNextTweetId() {
        int id = currentTweetId;
        currentTweetId++;
        updateFile();
        return id;
    }
}

class Tweet {
    private final int userId;
    private final int tweetId;
    private final LocalDate tweetDate;
    private final String tweet;

    // Store ALL extracted hashtags (including duplicates if they appear).
    private final List<String> hashtags;

    // Constructor uses PrimaryKeyGenerator to assign unique IDs.
    public Tweet(LocalDate tweetDate, String tweet) {
        this.userId = PrimaryKeyGenerator.getNextUserId();
        this.tweetId = PrimaryKeyGenerator.getNextTweetId();
        this.tweetDate = tweetDate;
        this.tweet = tweet;

        // Extract hashtags from the tweet text.
        this.hashtags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(tweet);
        while (matcher.find()) {
            this.hashtags.add(matcher.group());
        }
    }

    public int getUserId() {
        return userId;
    }
    public int getTweetId() {
        return tweetId;
    }
    public LocalDate getTweetDate() {
        return tweetDate;
    }
    public String getTweet() {
        return tweet;
    }
    public List<String> getHashtags() {
        return hashtags;
    }
}

public class TopTrendingHashtags {
    /**
     * Prints the input tweets in an ASCII table with columns:
     * user_id, tweet_id, tweet, hashtags, tweet_date.
     */
    private static void printInputTable(List<Tweet> tweets) {
        int userIdWidth = 7;    // "user_id"
        int tweetIdWidth = 8;   // "tweet_id"
        int tweetWidth = 80;    // "tweet"
        int tagsWidth = 50;     // "hashtags"
        int dateWidth = 10;     // "tweet_date"

        String headerLine = "+"
                + repeat('-', userIdWidth + 2) + "+"
                + repeat('-', tweetIdWidth + 2) + "+"
                + repeat('-', tweetWidth + 2) + "+"
                + repeat('-', tagsWidth + 2) + "+"
                + repeat('-', dateWidth + 2) + "+";

        System.out.println(headerLine);
        System.out.printf("| %-" + userIdWidth + "s | %-" + tweetIdWidth + "s | %-"
                        + tweetWidth + "s | %-" + tagsWidth + "s | %-" + dateWidth + "s |\n",
                "user_id", "tweet_id", "tweet", "hashtags", "tweet_date");
        System.out.println(headerLine);

        for (Tweet t : tweets) {
            String hashtagsStr = String.join(",", t.getHashtags());
            System.out.printf("| %-" + userIdWidth + "d | %-" + tweetIdWidth + "d | %-" +
                            tweetWidth + "s | %-" + tagsWidth + "s | %-" + dateWidth + "s |\n",
                    t.getUserId(),
                    t.getTweetId(),
                    t.getTweet(),
                    hashtagsStr,
                    t.getTweetDate().toString());
        }
        System.out.println(headerLine);
    }

    /**
     * Finds the top 3 trending hashtags for February 2024.
     * This counts EVERY occurrence of a hashtag (duplicates included).
     * When counts tie, hashtags are sorted in ascending alphabetical order.
     * Returns a list of [hashtag, count] pairs.
     */
    public List<List<String>> findTopTrendingHashtags(List<Tweet> tweets) {
        Map<String, Integer> hashtagCounts = new HashMap<>();

        for (Tweet tweet : tweets) {
            LocalDate date = tweet.getTweetDate();
            if (date.getYear() == 2024 && date.getMonthValue() == 2) {
                for (String hashtag : tweet.getHashtags()) {
                    hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(hashtagCounts.entrySet());
        sortedList.sort((a, b) -> {
            int countCmp = b.getValue().compareTo(a.getValue());
            if (countCmp == 0) {
                return a.getKey().compareTo(b.getKey());
            }
            return countCmp;
        });

        List<List<String>> result = new ArrayList<>();
        int limit = 3;
        for (Map.Entry<String, Integer> entry : sortedList) {
            if (limit-- == 0) break;
            result.add(Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())));
        }
        return result;
    }

    /**
     * Prints the top hashtags in an ASCII table with columns: Hashtag, Count.
     */
    private static void printHashtagsTable(List<List<String>> topHashtags) {
        String line = "+----------------------+-------+";
        System.out.println(line);
        System.out.printf("| %-20s | %-5s |\n", "Hashtag", "Count");
        System.out.println(line);
        for (List<String> row : topHashtags) {
            System.out.printf("| %-20s | %-5s |\n", row.get(0), row.get(1));
        }
        System.out.println(line);
    }

    // Helper method to build a string of repeated characters.
    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // Create tweets using auto-generated userId and tweetId from file.
        List<Tweet> tweets = Arrays.asList(
                new Tweet(LocalDate.of(2024, 2, 1),
                        "Enjoying a great start to the day. #HappyDay #MorningVibes #HappyDay"),
                new Tweet(LocalDate.of(2024, 2, 3),
                        "Another #HappyDay with good vibes! #FeelGood"),
                new Tweet(LocalDate.of(2024, 2, 4),
                        "Productivity peaks! #WorkLife #ProductiveDay"),
                new Tweet(LocalDate.of(2024, 2, 5),
                        "Exploring new tech frontiers. #TechLife #Innovation"),
                new Tweet(LocalDate.of(2024, 2, 5),
                        "Gratitude for today's moments. #HappyDay #Thankful"),
                new Tweet(LocalDate.of(2024, 2, 7),
                        "Innovation drives us. #TechLife #FutureTech"),
                new Tweet(LocalDate.of(2024, 2, 8),
                        "Connecting with nature's serenity. #Nature #Peaceful")
        );

        // 1) Print the input table with extracted hashtags.
        System.out.println("Input Tweets Table:\n");
        printInputTable(tweets);

        // 2) Compute top trending hashtags in February 2024.
        TopTrendingHashtags solver = new TopTrendingHashtags();
        List<List<String>> topHashtags = solver.findTopTrendingHashtags(tweets);

        // 3) Print the output table of top hashtags.
        System.out.println("\nTop 3 Trending Hashtags (February 2024):\n");
        printHashtagsTable(topHashtags);
    }

    /*
    public static void main(String[] args) {
        // Example usage
        List<Tweet> tweets = new ArrayList<>();
        tweets.add(new Tweet(LocalDate.of(2024, 2, 13), "Enjoying the #HappyDay! #HappyDay"));
        tweets.add(new Tweet(LocalDate.of(2024, 2, 14), "#HappyDay with friends"));
        tweets.add(new Tweet(LocalDate.of(2024, 2, 15), "#WorkLife balance is key"));
        tweets.add(new Tweet(LocalDate.of(2024, 2, 16), "#TechLife is awesome #TechLife"));
        tweets.add(new Tweet(LocalDate.of(2024, 2, 17), "#HappyDay vibes"));

        tweets.add(new Tweet(LocalDate.of(2024, 2, 1), "New month, new goals! #NewMonth #Goals"));
        tweets.add(new Tweet(LocalDate.of(2024, 2, 2), "Exploring the city's best cafes. #Foodie #WeekendFun"));


        TopTrendingHashtags solver = new TopTrendingHashtags();
        List<List<String>> topHashtags = solver.findTopTrendingHashtags(tweets);

        // Print formatted table
        System.out.println("+----------------------+-------+");
        System.out.println("| Hashtag              | Count |");
        System.out.println("+----------------------+-------+");
        for (List<String> entry : topHashtags) {
            String hashtag = entry.get(0);
            String count = entry.get(1);
            System.out.printf("| %-20s | %-5s |%n", hashtag, count);
        }
        System.out.println("+----------------------+-------+");
    }
     */
}