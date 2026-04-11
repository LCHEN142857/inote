// 声明包路径，DTO 层
package com.inote.model.dto;

// 导入 Lombok 全参构造函数注解
import lombok.AllArgsConstructor;
// 导入 Lombok 建造者模式注解
import lombok.Builder;
// 导入 Lombok 数据类注解
import lombok.Data;
// 导入 Lombok 无参构造函数注解
import lombok.NoArgsConstructor;

// 导入 List 集合类型
import java.util.List;

// 自动生成 getter、setter、toString、equals、hashCode 方法
@Data
// 启用 Builder 建造者模式
@Builder
// 生成无参构造函数
@NoArgsConstructor
// 生成全参构造函数
@AllArgsConstructor
// 知识库问答统一响应 DTO，返回给前端
public class InoteResponse {

    // 大模型生成的回答内容
    private String answer;
    // 回答所引用的文档来源列表
    private List<SourceReference> sources;
}
