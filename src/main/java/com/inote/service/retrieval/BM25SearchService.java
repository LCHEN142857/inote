// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 定义 BM25 检索服务，负责维护 Lucene 关键词索引。
public class BM25SearchService {

    // 计算并保存field内容结果。
    private static final String FIELD_CONTENT = "content";
    // 计算并保存fielddocid结果。
    private static final String FIELD_DOC_ID = "doc_id";

    // 声明directory变量，供后续流程使用。
    private final Directory directory;
    // 声明analyzer变量，供后续流程使用。
    private final Analyzer analyzer;

    /**
     * 处理BM25searchservice相关逻辑。
     */
    public BM25SearchService() {
        // 创建this.directory对象。
        this.directory = new ByteBuffersDirectory();
        // 创建this.analyzer对象。
        this.analyzer = new SmartChineseAnalyzer();
    }

    /**
     * 将文档分块写入 Lucene 索引。
     * @param documents 文档参数。
     */
    public synchronized void indexDocuments(List<Document> documents) {
        // 根据条件判断当前分支是否执行。
        if (documents == null || documents.isEmpty()) {
            // 继续补全当前链式调用或多行表达式。
            return;
        }

        // 进入异常保护块执行关键逻辑。
        try {
            // 创建config对象。
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            // 更新openmode字段。
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            // 进入异常保护块执行关键逻辑。
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 遍历当前集合或区间中的元素。
                for (Document doc : documents) {
                    // 创建lucenedoc对象。
                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                    // 向当前集合中追加元素。
                    luceneDoc.add(new TextField(FIELD_CONTENT, doc.getText(), Field.Store.YES));
                    // 向当前集合中追加元素。
                    luceneDoc.add(new StringField(FIELD_DOC_ID, doc.getId(), Field.Store.YES));

                    // 计算并保存元数据结果。
                    Map<String, Object> metadata = doc.getMetadata();
                    // 根据条件判断当前分支是否执行。
                    if (metadata != null) {
                        // 遍历当前集合或区间中的元素。
                        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                            // 围绕lucenedocadd补充当前业务语句。
                            luceneDoc.add(new StringField(
                                    "meta_" + entry.getKey(),
                                    // 围绕valueofentry补充当前业务语句。
                                    String.valueOf(entry.getValue()),
                                    // 围绕fieldstoreyes补充当前业务语句。
                                    Field.Store.YES));
                        }
                    }

                    // 调用 `addDocument` 完成当前步骤。
                    writer.addDocument(luceneDoc);
                }
                // 调用 `commit` 完成当前步骤。
                writer.commit();
            }

            // 记录当前流程的运行日志。
            log.info("Indexed {} documents for BM25 search", documents.size());
        } catch (IOException e) {
            // 记录当前流程的运行日志。
            log.error("Failed to index documents for BM25 search", e);
        }
    }

    /**
     * 执行 BM25 关键词检索并返回命中结果。
     * @param queryText 查询text参数。
     * @param topK topk参数。
     * @return 列表形式的处理结果。
     */
    public List<BM25Result> search(String queryText, int topK) {
        // 创建结果对象。
        List<BM25Result> results = new ArrayList<>();

        // 进入异常保护块执行关键逻辑。
        try {
            // 根据条件判断当前分支是否执行。
            if (!DirectoryReader.indexExists(directory)) {
                // 记录当前流程的运行日志。
                log.debug("BM25 index is empty, returning no results");
                // 返回结果。
                return results;
            }

            // 进入异常保护块执行关键逻辑。
            try (IndexReader reader = DirectoryReader.open(directory)) {
                // 创建searcher对象。
                IndexSearcher searcher = new IndexSearcher(reader);
                // 创建parser对象。
                QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);

                // 计算并保存escaped结果。
                String escaped = QueryParser.escape(queryText);
                // 计算并保存查询结果。
                Query query = parser.parse(escaped);

                // 计算并保存topdocs结果。
                TopDocs topDocs = searcher.search(query, topK);

                // 遍历当前集合或区间中的元素。
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    // 计算并保存lucenedoc结果。
                    org.apache.lucene.document.Document luceneDoc = searcher.doc(scoreDoc.doc);
                    // 计算并保存内容结果。
                    String content = luceneDoc.get(FIELD_CONTENT);
                    // 计算并保存docid结果。
                    String docId = luceneDoc.get(FIELD_DOC_ID);

                    // 创建元数据对象。
                    Map<String, Object> metadata = new HashMap<>();
                    // 遍历当前集合或区间中的元素。
                    for (var field : luceneDoc.getFields()) {
                        // 根据条件判断当前分支是否执行。
                        if (field.name().startsWith("meta_")) {
                            // 写入当前映射中的键值对。
                            metadata.put(field.name().substring(5), field.stringValue());
                        }
                    }

                    // 向当前集合中追加元素。
                    results.add(new BM25Result(docId, content, metadata, scoreDoc.score));
                }
            }
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("BM25 search failed: {}", e.getMessage());
        }

        // 返回结果。
        return results;
    }

    /**
     * 执行 BM25 关键词检索并返回命中结果。
     * @param queryText 查询text参数。
     * @param topK topk参数。
     * @param ownerId 所属用户id参数。
     * @return 列表形式的处理结果。
     */
    public List<BM25Result> search(String queryText, int topK, String ownerId) {
        // 返回 `search` 的处理结果。
        return search(queryText, topK).stream()
                // 设置filter字段的取值。
                .filter(result -> ownerId.equals(String.valueOf(result.metadata().get("owner_id"))))
                // 设置tolist字段的取值。
                .toList();
    }

    /**
     * 处理close相关逻辑。
     */
    // 声明当前方法在 Bean 销毁前执行。
    @PreDestroy
    public void close() {
        // 进入异常保护块执行关键逻辑。
        try {
            // 调用 `close` 完成当前步骤。
            directory.close();
        } catch (IOException e) {
            // 记录当前流程的运行日志。
            log.warn("Failed to close BM25 directory", e);
        }
    }

    /**
     * 处理BM25结果相关逻辑。
     * @param documentId 文档id参数。
     * @param content 内容参数。
     * @param metadata 元数据参数。
     * @param score score参数。
     * @return record结果。
     */
    public record BM25Result(
            String documentId,
            String content,
            Map<String, Object> metadata,
            double score
    ) {
    }
}
