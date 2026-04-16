// 声明当前源文件的包。
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

// 应用当前注解。
@DataJpaTest
// 应用当前注解。
@ActiveProfiles("test")
// 声明当前类型。
class PersistenceLayerTest {

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private UserRepository userRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private DocumentRepository documentRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ChatSessionRepository chatSessionRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ChatMessageRepository chatMessageRepository;

    /**
     * 描述 `userRepositoryFindsUsersByUsernameAndAuthToken` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void userRepositoryFindsUsersByUsernameAndAuthToken() throws Exception {
        // 执行当前语句。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 执行当前语句。
        user.setCreatedAt(null);
        // 执行当前语句。
        user.setUpdatedAt(null);
        // 执行当前语句。
        user.setId(null);
        // 执行当前语句。
        User savedUser = userRepository.save(user);
        // 执行当前语句。
        assertThat(savedUser.getId()).isNotBlank();
        // 执行当前语句。
        assertThat(savedUser.getCreatedAt()).isNotNull();
        // 执行当前语句。
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        // 执行当前语句。
        assertThat(userRepository.findByUsername("tester")).isPresent();
        // 执行当前语句。
        assertThat(userRepository.findByAuthToken("token-1")).isPresent();
    // 结束当前代码块。
    }

    /**
     * 描述 `documentRepositoryFiltersByOwnerId` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void documentRepositoryFiltersByOwnerId() throws Exception {
        // 执行当前语句。
        User firstUser = userRepository.save(TestDataFactory.user("user-1", "tester-a", "token-a"));
        // 执行当前语句。
        User secondUser = userRepository.save(TestDataFactory.user("user-2", "tester-b", "token-b"));
        // 执行当前语句。
        Document firstDocument = documentRepository.save(TestDataFactory.document("doc-1", firstUser, "COMPLETED"));
        // 执行当前语句。
        documentRepository.save(TestDataFactory.document("doc-2", secondUser, "FAILED"));
        // 执行当前语句。
        List<Document> documents = documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(firstUser.getId());
        // 执行当前语句。
        assertThat(documents).hasSize(1);
        // 执行当前语句。
        assertThat(documents.get(0).getId()).isEqualTo(firstDocument.getId());
        // 执行当前语句。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), firstUser.getId())).isPresent();
        // 执行当前语句。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), secondUser.getId())).isEmpty();
    // 结束当前代码块。
    }

    /**
     * 描述 `chatRepositoriesSupportOwnerQueriesAndMessageCounting` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void chatRepositoriesSupportOwnerQueriesAndMessageCounting() throws Exception {
        // 执行当前语句。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 执行当前语句。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "Session Title"));
        // 执行当前语句。
        ChatMessage firstMessage = chatMessageRepository.save(TestDataFactory.message("msg-1", session, "user", "hello"));
        // 执行当前语句。
        ChatMessage secondMessage = chatMessageRepository.save(TestDataFactory.message("msg-2", session, "assistant", "hi"));
        // 执行当前语句。
        assertThat(chatSessionRepository.findByIdAndOwnerId(session.getId(), user.getId())).isPresent();
        // 执行当前语句。
        assertThat(chatSessionRepository.existsByIdAndOwnerId(session.getId(), user.getId())).isTrue();
        // 执行当前语句。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).extracting(ChatMessage::getId).contains(firstMessage.getId(), secondMessage.getId());
        // 执行当前语句。
        assertThat(chatMessageRepository.countBySessionIds(List.of(session.getId()))).singleElement().satisfies(row -> assertThat(row[1]).isEqualTo(2L));
    // 结束当前代码块。
    }
// 结束当前代码块。
}
