// 声明测试类所在包。
package com.inote.repository;

import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;
import com.inote.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 标记当前类为 JPA 切片测试。
@DataJpaTest
// 指定当前测试启用 test 配置文件。
@ActiveProfiles("test")
class PersistenceLayerTest {

    // 注入用户仓储。
    @Autowired
    private UserRepository userRepository;

    // 注入文档仓储。
    @Autowired
    private DocumentRepository documentRepository;

    // 注入会话仓储。
    @Autowired
    private ChatSessionRepository chatSessionRepository;

    // 注入消息仓储。
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * 验证用户仓储支持按用户名与令牌查询。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void userRepositoryFindsUsersByUsernameAndAuthToken() throws Exception {
        // 构造待保存用户实体。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 清空预置时间以覆盖实体回调逻辑。
        user.setCreatedAt(null);
        // 清空更新时间以覆盖实体回调逻辑。
        user.setUpdatedAt(null);
        // 清空主键以覆盖自动主键生成逻辑。
        user.setId(null);
        // 保存用户实体。
        User savedUser = userRepository.save(user);
        // 断言主键已在持久化前生成。
        assertThat(savedUser.getId()).isNotBlank();
        // 断言创建时间已写入。
        assertThat(savedUser.getCreatedAt()).isNotNull();
        // 断言更新时间已写入。
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        // 断言按用户名可查询到用户。
        assertThat(userRepository.findByUsername("tester")).isPresent();
        // 断言按令牌可查询到用户。
        assertThat(userRepository.findByAuthToken("token-1")).isPresent();
    }

    /**
     * 验证文档仓储只会返回指定用户的文档。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void documentRepositoryFiltersByOwnerId() throws Exception {
        // 保存第一个测试用户。
        User firstUser = userRepository.save(TestDataFactory.user("user-1", "tester-a", "token-a"));
        // 保存第二个测试用户。
        User secondUser = userRepository.save(TestDataFactory.user("user-2", "tester-b", "token-b"));
        // 保存第一个用户的文档。
        Document firstDocument = documentRepository.save(TestDataFactory.document("doc-1", firstUser, "COMPLETED"));
        // 保存第二个用户的文档。
        documentRepository.save(TestDataFactory.document("doc-2", secondUser, "FAILED"));
        // 按第一个用户查询文档列表。
        List<Document> documents = documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(firstUser.getId());
        // 断言仅返回一个文档。
        assertThat(documents).hasSize(1);
        // 断言返回文档属于第一个用户。
        assertThat(documents.get(0).getId()).isEqualTo(firstDocument.getId());
        // 断言按主键和归属用户可命中结果。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), firstUser.getId())).isPresent();
        // 断言跨用户查询不会命中文档。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), secondUser.getId())).isEmpty();
    }

    /**
     * 验证会话与消息仓储的聚合查询行为。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void chatRepositoriesSupportOwnerQueriesAndMessageCounting() throws Exception {
        // 保存测试用户实体。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 保存测试会话实体。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "Session Title"));
        // 保存第一条消息。
        ChatMessage firstMessage = chatMessageRepository.save(TestDataFactory.message("msg-1", session, "user", "hello"));
        // 保存第二条消息。
        ChatMessage secondMessage = chatMessageRepository.save(TestDataFactory.message("msg-2", session, "assistant", "hi"));
        // 断言按归属用户可查到会话。
        assertThat(chatSessionRepository.findByIdAndOwnerId(session.getId(), user.getId())).isPresent();
        // 断言存在性检查返回 true。
        assertThat(chatSessionRepository.existsByIdAndOwnerId(session.getId(), user.getId())).isTrue();
        // 断言消息按时间顺序返回。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).extracting(ChatMessage::getId).contains(firstMessage.getId(), secondMessage.getId());
        // 断言按会话聚合计数返回两条消息。
        assertThat(chatMessageRepository.countBySessionIds(List.of(session.getId()))).singleElement().satisfies(row -> assertThat(row[1]).isEqualTo(2L));
    }
}
