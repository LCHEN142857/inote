// 声明当前源文件所属包。
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

// 应用 `DataJpaTest` 注解声明当前行为。
@DataJpaTest
// 声明测试运行时使用的配置环境。
@ActiveProfiles("test")
// 定义 `PersistenceLayerTest` 类型。
class PersistenceLayerTest {

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明文档repository变量，供后续流程使用。
    private DocumentRepository documentRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明问答会话repository变量，供后续流程使用。
    private ChatSessionRepository chatSessionRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明问答消息repository变量，供后续流程使用。
    private ChatMessageRepository chatMessageRepository;

    /**
     * 处理用户repositoryfinds用户byusernameand认证令牌相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void userRepositoryFindsUsersByUsernameAndAuthToken() throws Exception {
        // 计算并保存用户结果。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 更新createdat字段。
        user.setCreatedAt(null);
        // 更新updatedat字段。
        user.setUpdatedAt(null);
        // 更新id字段。
        user.setId(null);
        // 保存saved用户对象。
        User savedUser = userRepository.save(user);
        // 断言当前结果符合测试预期。
        assertThat(savedUser.getId()).isNotBlank();
        // 断言当前结果符合测试预期。
        assertThat(savedUser.getCreatedAt()).isNotNull();
        // 断言当前结果符合测试预期。
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        // 断言当前结果符合测试预期。
        assertThat(userRepository.findByUsername("tester")).isPresent();
        // 断言当前结果符合测试预期。
        assertThat(userRepository.findByAuthToken("token-1")).isPresent();
    }

    /**
     * 处理文档repositoryfiltersby所属用户id相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void documentRepositoryFiltersByOwnerId() throws Exception {
        // 保存first用户对象。
        User firstUser = userRepository.save(TestDataFactory.user("user-1", "tester-a", "token-a"));
        // 保存second用户对象。
        User secondUser = userRepository.save(TestDataFactory.user("user-2", "tester-b", "token-b"));
        // 保存first文档对象。
        Document firstDocument = documentRepository.save(TestDataFactory.document("doc-1", firstUser, "COMPLETED"));
        // 保存当前对象到持久化层。
        documentRepository.save(TestDataFactory.document("doc-2", secondUser, "FAILED"));
        // 查询文档数据。
        List<Document> documents = documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(firstUser.getId());
        // 断言当前结果符合测试预期。
        assertThat(documents).hasSize(1);
        // 断言当前结果符合测试预期。
        assertThat(documents.get(0).getId()).isEqualTo(firstDocument.getId());
        // 断言当前结果符合测试预期。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), firstUser.getId())).isPresent();
        // 断言当前结果符合测试预期。
        assertThat(documentRepository.findByIdAndOwnerId(firstDocument.getId(), secondUser.getId())).isEmpty();
    }

    /**
     * 处理问答repositoriessupport所属用户queriesand消息counting相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void chatRepositoriesSupportOwnerQueriesAndMessageCounting() throws Exception {
        // 保存用户对象。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 保存会话对象。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "Session Title"));
        // 保存first消息对象。
        ChatMessage firstMessage = chatMessageRepository.save(TestDataFactory.message("msg-1", session, "user", "hello"));
        // 保存second消息对象。
        ChatMessage secondMessage = chatMessageRepository.save(TestDataFactory.message("msg-2", session, "assistant", "hi"));
        // 断言当前结果符合测试预期。
        assertThat(chatSessionRepository.findByIdAndOwnerId(session.getId(), user.getId())).isPresent();
        // 断言当前结果符合测试预期。
        assertThat(chatSessionRepository.existsByIdAndOwnerId(session.getId(), user.getId())).isTrue();
        // 断言当前结果符合测试预期。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).extracting(ChatMessage::getId).contains(firstMessage.getId(), secondMessage.getId());
        // 断言当前结果符合测试预期。
        assertThat(chatMessageRepository.countBySessionIds(List.of(session.getId()))).singleElement().satisfies(row -> assertThat(row[1]).isEqualTo(2L));
    }
}
