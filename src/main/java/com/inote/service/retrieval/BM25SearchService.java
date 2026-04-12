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

/**
 * BM25 关键词检索服务
 * <p>
 * 注意：当前使用内存存储(ByteBuffersDirectory)，应用重启后索引会丢失。
 * 适用于开发/MVP阶段，生产环境建议改用持久化存储如 FSDirectory。
 */
@Slf4j
@Service
public class BM25SearchService {

    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_DOC_ID = "doc_id";

    private final Directory directory;
    private final Analyzer analyzer;

    public BM25SearchService() {
        // 使用内存存储，应用重启后索引会丢失
        // 生产环境建议改为 FSDirectory.open(Path) 使用持久化存储
        this.directory = new ByteBuffersDirectory();
        this.analyzer = new SmartChineseAnalyzer();
    }

    public synchronized void indexDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (Document doc : documents) {
                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                    luceneDoc.add(new TextField(FIELD_CONTENT, doc.getText(), Field.Store.YES));
                    luceneDoc.add(new StringField(FIELD_DOC_ID, doc.getId(), Field.Store.YES));

                    // 存储元数据字段
                    Map<String, Object> metadata = doc.getMetadata();
                    if (metadata != null) {
                        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                            luceneDoc.add(new StringField(
                                    "meta_" + entry.getKey(),
                                    String.valueOf(entry.getValue()),
                                    Field.Store.YES));
                        }
                    }

                    writer.addDocument(luceneDoc);
                }
                writer.commit();
            }

            log.info("Indexed {} documents for BM25 search", documents.size());
        } catch (IOException e) {
            log.error("Failed to index documents for BM25 search", e);
        }
    }

    public List<BM25Result> search(String queryText, int topK) {
        List<BM25Result> results = new ArrayList<>();

        try {
            if (!DirectoryReader.indexExists(directory)) {
                log.debug("BM25 index is empty, returning no results");
                return results;
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);

                // 转义特殊字符
                String escaped = QueryParser.escape(queryText);
                Query query = parser.parse(escaped);

                TopDocs topDocs = searcher.search(query, topK);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document luceneDoc = searcher.doc(scoreDoc.doc);
                    String content = luceneDoc.get(FIELD_CONTENT);
                    String docId = luceneDoc.get(FIELD_DOC_ID);

                    // 恢复元数据
                    Map<String, Object> metadata = new HashMap<>();
                    for (var field : luceneDoc.getFields()) {
                        if (field.name().startsWith("meta_")) {
                            metadata.put(field.name().substring(5), field.stringValue());
                        }
                    }

                    results.add(new BM25Result(docId, content, metadata, scoreDoc.score));
                }
            }
        } catch (Exception e) {
            log.warn("BM25 search failed: {}", e.getMessage());
        }

        return results;
    }

    @PreDestroy
    public void close() {
        try {
            directory.close();
        } catch (IOException e) {
            log.warn("Failed to close BM25 directory", e);
        }
    }

    public record BM25Result(
            String documentId,
            String content,
            Map<String, Object> metadata,
            double score
    ) {
    }
}
