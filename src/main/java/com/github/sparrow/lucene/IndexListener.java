package com.github.sparrow.lucene;

import com.github.sparrow.exception.IndexingException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Start indexing all the Indexers as this bean initializes
 */
@Component
@RequiredArgsConstructor
@PropertySource("classpath:sparrow.properties")
public class IndexListener {

  private final Logger logger = LoggerFactory.getLogger(IndexListener.class);
  private final List<Indexer<?>> indexers;
  private final LuceneContextFactory contextFactory;
  private final ExecutorService executorService;

  @Value("${indexing.parallel}")
  private boolean parallelExecution;
  @Value("${index.force}")
  private boolean forceIndex;

  @PostConstruct
  public void initIndex() {
    if (parallelExecution) {
      final List<CompletableFuture<Void>> tasks = new ArrayList<>();
      for (Indexer<?> indexer : indexers) {
        tasks.add(CompletableFuture.runAsync(() -> runIndexer(indexer), executorService));
      }
      try {
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
      } catch (Exception ex) {
        logger.error("Indexing failed during startup", ex);
        throw new IndexingException("One or more indexers failed during parallel indexing", ex);
      }
    } else {
      for (Indexer<?> indexer : indexers) {
        runIndexer(indexer);
      }
    }
  }

  private void runIndexer(Indexer<?> indexer) throws IndexingException {
    EngineType engineType = indexer.getEngineType();
    logger.info("Initializing Indexer : {}", engineType.getName());
    try (LuceneContext context = contextFactory.createLuceneContext(engineType, LuceneMode.INDEXING)) {
      logger.info("Created index context : {}", context);
      if (!forceIndex && !indexer.needsIndexing(context)) {
        logger.info("{} already indexed. Skipping indexing...", engineType);
        return;
      }
      indexer.index(context);
    } catch (Exception ex) {
      String msg = "Indexing failed for " + engineType.getName();
      logger.error(msg, ex);
      throw new IndexingException(msg);
    }
  }

}
