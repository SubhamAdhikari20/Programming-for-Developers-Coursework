import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.util.stream.Collectors;

public class MultithreadedWebCrawler {
    // Configuration constants
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MyWebCrawler/1.0)";
    private static final int REQUEST_TIMEOUT_MS = 10_000;
    private static final int MAX_RETRIES = 3;
    private static final int CRAWL_DELAY_MS = 1000;

    // Thread-safe data structures
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, String> crawledData = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> domainDelays = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final int numThreads;

    public MultithreadedWebCrawler(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    public void startCrawling(List<String> seedUrls) {
        seedUrls.stream()
                .filter(url -> !visitedUrls.contains(url))
                .forEach(urlQueue::add);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(this::crawlTask);
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
                System.err.println("Crawling timeout reached");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void crawlTask() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String url = urlQueue.poll(500, TimeUnit.MILLISECONDS);
                if (url == null) continue;

                if (visitedUrls.add(url)) {
                    processUrl(url);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processUrl(String url) {
        try {
            if (!isCrawlAllowed(url)) {
                return;
            }

            String content = fetchWebPageWithRetry(url);
            if (content == null) return;

            crawledData.put(url, content);
            System.out.println("\nSuccessfully crawled: " + url);

            List<String> links = extractAndFilterLinks(url, content);
            links.stream()
                    .filter(link -> !visitedUrls.contains(link))
                    .forEach(link -> urlQueue.offer(link));

        } catch (Exception e) {
            System.err.println("Error processing " + url + ": " + e.getMessage());
        }
    }

    private String fetchWebPageWithRetry(String url) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                enforceCrawlDelay(url);

                Document doc = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(REQUEST_TIMEOUT_MS)
                        .get();

                return doc.outerHtml();
            } catch (IOException e) {
                if (attempt == MAX_RETRIES - 1) {
                    System.err.println("Failed to fetch " + url + " after " + MAX_RETRIES + " attempts");
                }
            }
        }
        return null;
    }

    private void enforceCrawlDelay(String url) {
        try {
            String domain = new java.net.URL(url).getHost();
            Long lastAccess = domainDelays.get(domain);
            if (lastAccess != null) {
                long elapsed = System.currentTimeMillis() - lastAccess;
                if (elapsed < CRAWL_DELAY_MS) {
                    Thread.sleep(CRAWL_DELAY_MS - elapsed);
                }
            }
            domainDelays.put(domain, System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("Error enforcing crawl delay: " + e.getMessage());
        }
    }

    private List<String> extractAndFilterLinks(String url, String html) {
        try {
            Document doc = Jsoup.parse(html, url);
            Elements links = doc.select("a[href]");

            return links.stream()
                    .map(link -> link.absUrl("href"))
                    .filter(this::isValidLink)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error parsing links from " + url);
            return Collections.emptyList();
        }
    }

    private boolean isValidLink(String url) {
        return url.startsWith("http") &&
                !url.contains("mailto:") &&
                !url.contains("javascript:") &&
                !url.endsWith(".pdf") &&
                !url.endsWith(".zip");
    }

    private boolean isCrawlAllowed(String url) {
        try {
            java.net.URL target = new java.net.URL(url);
            String robotsUrl = target.getProtocol() + "://" + target.getHost() + "/robots.txt";

            if (!visitedUrls.contains(robotsUrl)) {
                visitedUrls.add(robotsUrl);
                // Implement robots.txt parsing here using crawler-commons
                // For simplicity, we'll allow all crawls in this example
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error checking robots.txt for " + url);
            return false;
        }
    }

    public void printResults() {
        System.out.println("\n\nCrawling Results:");
        System.out.println("==================");

        crawledData.forEach((url, content) -> {
            Document doc = Jsoup.parse(content);
            String title = doc.title();
            String textSnippet = doc.body() != null ?
                    doc.body().text().replaceAll("\\s+", " ").substring(0, 150) + "..." :
                    "[No text content]";

            System.out.println("URL: " + url);
            System.out.println("Title: " + title);
            System.out.println("Content: " + textSnippet);
            System.out.println("----------------------------");
        });
    }

    public static void main(String[] args) {
        List<String> seedUrls = List.of(
//                "https://en.wikipedia.org/wiki/Web_crawler",
                "https://solo-leveling.fandom.com/wiki/Solo_Leveling_Wiki"
        );

        MultithreadedWebCrawler crawler = new MultithreadedWebCrawler(4);
        crawler.startCrawling(seedUrls);
        crawler.printResults();
    }
}