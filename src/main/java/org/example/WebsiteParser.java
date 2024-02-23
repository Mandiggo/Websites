package org.example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.util.stream.Collectors;

public class WebsiteParser {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        List<String> websites = Files.lines(Paths.get("websites.txt")).collect(Collectors.toList());

        CompletableFuture<Void> asyncTask = CompletableFuture.runAsync(() -> {
            for (String website : websites) {
                try {
                    String title = getTitle(website);
                    System.out.println("Website: " + website + ", Title: " + title.substring(0, Math.min(50, title.length())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        List<CompletableFuture<String>> parsingTasks = websites.stream()
                .map(website -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return parseWebsite(website);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return "";
                    }
                }))
                .collect(Collectors.toList());

        List<String> pages = CompletableFuture.allOf(parsingTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> parsingTasks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .get();

        List<CompletableFuture<String>> titleTasks = pages.stream()
                .map(page -> CompletableFuture.supplyAsync(() -> getTitleAttribute(page)))
                .collect(Collectors.toList());

        List<String> titles = CompletableFuture.allOf(titleTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> titleTasks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .get();

        Files.write(Paths.get("titles.txt"), String.join(",", titles).getBytes());

        asyncTask.get();
    }

    private static String getTitle(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        return document.title();
    }

    private static String parseWebsite(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        return document.outerHtml();
    }

    private static String getTitleAttribute(String page) {
        Document document = Jsoup.parse(page);
        Element titleElement = document.select("title").first();
        return (titleElement != null) ? titleElement.text() : "";
    }
}