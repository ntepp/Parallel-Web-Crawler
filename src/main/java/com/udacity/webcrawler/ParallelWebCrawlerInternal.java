package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * A {@link WebCrawler} that downloads and processes one page at a time.
 */
final class ParallelWebCrawlerInternal extends RecursiveAction {

    private Clock clock;
    private PageParserFactory parserFactory;
    Instant deadline;
    private int maxDepth;
    ConcurrentHashMap<String, Integer> counts;
    ConcurrentSkipListSet<String> visitedUrls;
    private List<Pattern> ignoredUrls;
    private String url;


    public ParallelWebCrawlerInternal(Clock clock, String url, PageParserFactory parserFactory, Instant deadline, int maxDepth, ConcurrentHashMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls) {
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.ignoredUrls = ignoredUrls;
        this.url = url;
        this.visitedUrls = visitedUrls;
    }


    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        synchronized (this) {
            if (visitedUrls.contains(url)) {
                return;
            }
            visitedUrls.add(url);
        }

        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }
        List<ParallelWebCrawlerInternal> parallelWebCrawlerInternals = new ArrayList<>();
        for (String link : result.getLinks()) {
            parallelWebCrawlerInternals.add(new ParallelWebCrawlerInternal(clock, link,
                    parserFactory,
                    deadline,
                    maxDepth - 1,
                    counts,
                    visitedUrls,
                    ignoredUrls));

        }
        invokeAll(parallelWebCrawlerInternals);
    }
}
