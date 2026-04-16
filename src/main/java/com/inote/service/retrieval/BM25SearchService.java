// 声明当前源文件的包。
package com.inote.service.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 声明当前类型。
public class BM25SearchService {

    // 声明当前字段。
    private static final String FIELD_CONTENT = "content";
    // 声明当前字段。
    private static final String FIELD_DOC_ID = "doc_id";

    // 声明当前字段。
    private final Directory directory;
    // 声明当前字段。
    private final Analyzer analyzer;

    /**
     * 描述 `BM25SearchService` 操作。
     *
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    public BM25SearchService() {
        // 执行当前语句。
        this.directory = new ByteBuffersDirectory();
        // 执行当前语句。
        this.analyzer = new SmartChineseAnalyzer();
    // 结束当前代码块。
    }

    /**
     * 描述 `indexDocuments` 操作。
     *
     * @param documents 输入参数 `documents`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public synchronized void indexDocuments(List<Document> documents) {
        // 执行当前流程控制分支。
        if (documents == null || documents.isEmpty()) {
            // 执行当前语句。
            return;
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            // 执行当前语句。
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            // 执行当前流程控制分支。
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 执行当前流程控制分支。
                for (Document doc : documents) {
                    // 执行当前语句。
                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                    // 执行当前语句。
                    luceneDoc.add(new TextField(FIELD_CONTENT, doc.getText(), Field.Store.YES));
                    // 执行当前语句。
                    luceneDoc.add(new StringField(FIELD_DOC_ID, doc.getId(), Field.Store.YES));

                    // 执行当前语句。
                    Map<String, Object> metadata = doc.getMetadata();
                    // 执行当前流程控制分支。
                    if (metadata != null) {
                        // 执行当前流程控制分支。
                        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                            // 处理当前代码结构。
                            luceneDoc.add(new StringField(
                                    // 处理当前代码结构。
                                    "meta_" + entry.getKey(),
                                    // 处理当前代码结构。
                                    String.valueOf(entry.getValue()),
                                    // 执行当前语句。
                                    Field.Store.YES));
                        // 结束当前代码块。
                        }
                    // 结束当前代码块。
                    }

                    // 执行当前语句。
                    writer.addDocument(luceneDoc);
                // 结束当前代码块。
                }
                // 执行当前语句。
                writer.commit();
            // 结束当前代码块。
            }

            // 执行当前语句。
            log.info("Indexed {} documents for BM25 search", documents.size());
        // 处理当前代码结构。
        } catch (IOException e) {
            // 执行当前语句。
            log.error("Failed to index documents for BM25 search", e);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `search` 操作。
     *
     * @param queryText 输入参数 `queryText`。
     * @param topK 输入参数 `topK`。
     * @return 类型为 `List<BM25Result>` 的返回值。
     */
    // 处理当前代码结构。
    public List<BM25Result> search(String queryText, int topK) {
        // 执行当前语句。
        List<BM25Result> results = new ArrayList<>();

        // 执行当前流程控制分支。
        try {
            // 执行当前流程控制分支。
            if (!DirectoryReader.indexExists(directory)) {
                // 执行当前语句。
                log.debug("BM25 index is empty, returning no results");
                // 返回当前结果。
                return results;
            // 结束当前代码块。
            }

            // 执行当前流程控制分支。
            try (IndexReader reader = DirectoryReader.open(directory)) {
                // 执行当前语句。
                IndexSearcher searcher = new IndexSearcher(reader);
                // 执行当前语句。
                QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);

                // 执行当前语句。
                String escaped = QueryParser.escape(queryText);
                // 执行当前语句。
                Query query = parser.parse(escaped);

                // 执行当前语句。
                TopDocs topDocs = searcher.search(query, topK);

                // 执行当前流程控制分支。
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    // 执行当前语句。
                    org.apache.lucene.document.Document luceneDoc = searcher.doc(scoreDoc.doc);
                    // 执行当前语句。
                    String content = luceneDoc.get(FIELD_CONTENT);
                    // 执行当前语句。
                    String docId = luceneDoc.get(FIELD_DOC_ID);

                    // 执行当前语句。
                    Map<String, Object> metadata = new HashMap<>();
                    // 执行当前流程控制分支。
                    for (var field : luceneDoc.getFields()) {
                        // 执行当前流程控制分支。
                        if (field.name().startsWith("meta_")) {
                            // 执行当前语句。
                            metadata.put(field.name().substring(5), field.stringValue());
                        // 结束当前代码块。
                        }
                    // 结束当前代码块。
                    }

                    // 执行当前语句。
                    results.add(new BM25Result(docId, content, metadata, scoreDoc.score));
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("BM25 search failed: {}", e.getMessage());
        // 结束当前代码块。
        }

        // 返回当前结果。
        return results;
    // 结束当前代码块。
    }

    /**
     * 描述 `search` 操作。
     *
     * @param queryText 输入参数 `queryText`。
     * @param topK 输入参数 `topK`。
     * @param ownerId 输入参数 `ownerId`。
     * @return 类型为 `List<BM25Result>` 的返回值。
     */
    // 处理当前代码结构。
    public List<BM25Result> search(String queryText, int topK, String ownerId) {
        // 返回当前结果。
        return search(queryText, topK).stream()
                // 处理当前代码结构。
                .filter(result -> ownerId.equals(String.valueOf(result.metadata().get("owner_id"))))
                // 执行当前语句。
                .toList();
    // 结束当前代码块。
    }

    /**
     * 描述 `close` 操作。
     *
     * @return 无返回值。
     */
    // 应用当前注解。
    @PreDestroy
    // 处理当前代码结构。
    public void close() {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            directory.close();
        // 处理当前代码结构。
        } catch (IOException e) {
            // 执行当前语句。
            log.warn("Failed to close BM25 directory", e);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `BM25Result` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @param content 输入参数 `content`。
     * @param metadata 输入参数 `metadata`。
     * @param score 输入参数 `score`。
     * @return 类型为 `record` 的返回值。
     */
    // 声明当前类型。
    public record BM25Result(
            // 处理当前代码结构。
            String documentId,
            // 处理当前代码结构。
            String content,
            // 处理当前代码结构。
            Map<String, Object> metadata,
            // 处理当前代码结构。
            double score
    // 处理当前代码结构。
    ) {
    // 结束当前代码块。
    }
// 结束当前代码块。
}
