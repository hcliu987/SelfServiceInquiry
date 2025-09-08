# SearchService HTML解析功能说明

## 功能概述

新增的HTML解析功能可以处理包含`<a href="...">` 标签的HTML格式数据，提取其中的链接并格式化为纯文本输出。

## 支持的输入格式

### HTML格式示例
```html
【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】
视频：<a href="https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ">百度云盘</a>&nbsp; &nbsp; 提取码：1234

【[蜘蛛侠：英雄归来][2017][动作/科幻][美国]】
视频：<a href="https://pan.baidu.com/s/1aJbD0_lqFXx1GdweADtFYw?pwd=1234">百度网盘</a>&nbsp; &nbsp; 提取码：1234

【[美国队长 1~4][动作/科幻][美国]】
美国队长1~3.视频：<a href="https://pan.quark.cn/s/268e4ae3ea67">夸克网盘</a>
美国队长1~3.视频：<a href="https://pan.baidu.com/s/1uguy_PBk27o1S9Y_1ElDBg?pwd=1234">百度网盘</a>&nbsp; &nbsp; 提取码：1234
美国队长4.视频：<a href="https://pan.baidu.com/s/1rVfuHY7g-gy9YqNXK1EaMg?pwd=1234">百度网盘</a>&nbsp; &nbsp; 提取码：1234
```

## 输出格式

### 处理后的纯文本格式
```
【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】
视频：链接: https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ 提取码：1234

【[蜘蛛侠：英雄归来][2017][动作/科幻][美国]】
视频：链接: https://pan.baidu.com/s/1aJbD0_lqFXx1GdweADtFYw?pwd=1234 提取码：1234

【[美国队长 1~4][动作/科幻][美国]】
美国队长1~3.视频：链接: https://pan.quark.cn/s/268e4ae3ea67
美国队长1~3.视频：链接: https://pan.baidu.com/s/1uguy_PBk27o1S9Y_1ElDBg?pwd=1234 提取码：1234
美国队长4.视频：链接: https://pan.baidu.com/s/1rVfuHY7g-gy9YqNXK1EaMg?pwd=1234 提取码：1234

【漫威75年：从俚俗到全球！[2014][纪录片][美国]】
视频：链接: https://pan.baidu.com/s/1QQ_rqzCMI0RhLyNpbCAo-A
```

## 核心功能

### 1. HTML解析方法
- `parseHtmlContent(String htmlContent, String defaultTitle)`: 解析HTML内容，提取标题和内容
- `processContentWithLinks(String content)`: 处理内容中的链接，提取href属性

### 2. 正则表达式匹配
- **标题匹配**: `【([^】]+)】` - 匹配【标题】格式
- **链接匹配**: `<a\s+href="([^"]+)"[^>]*>([^<]*)</a>` - 匹配href链接
- **提取码匹配**: `提取码[：:]\s*(\w+)` - 匹配提取码信息

### 3. 数据处理流程
1. **尝试JSON解析**: 首先尝试按照原有的JSON格式解析
2. **HTML解析兜底**: 如果JSON解析失败，自动切换到HTML解析模式
3. **链接提取**: 从`<a href="...">` 标签中提取真实链接地址
4. **格式化输出**: 转换为统一的纯文本格式

### 4. 优化特性
- **智能识别**: 自动识别输入是JSON还是HTML格式
- **容错处理**: 多层次的异常处理和兜底方案
- **链接独立保存**: href链接被独立提取和保存
- **缓存机制**: 解析结果会被缓存，提高性能

## 技术实现

### 关键代码片段
```java
// HTML解析主方法
private List<ResultItem> parseHtmlContent(String htmlContent, String defaultTitle) {
    // 1. 使用正则表达式匹配标题和内容
    // 2. 逐行解析，区分标题和内容块
    // 3. 调用链接处理方法
}

// 链接处理方法
private String processContentWithLinks(String content) {
    // 1. 使用正则表达式匹配 <a href="...">...</a>
    // 2. 提取href属性值
    // 3. 格式化为 "链接: URL" 的形式
    // 4. 清理HTML标签和多余空格
}
```

## 集成说明

### SearchService集成
- 新功能已集成到 `searchAndMergeRaw()` 方法中
- 自动处理混合格式的搜索结果
- 遵循现有的JSON数据处理规范
- 与缓存机制和重试机制兼容

### 页面展示兼容
- 输出格式符合页面展示规范
- 标题以【开头并会被加粗显示
- 链接独立一行展示（纯文本形式）
- 提取码信息会被高亮显示

## 使用场景

1. **混合数据源**: 处理同时返回JSON和HTML格式的API响应
2. **链接提取**: 从HTML内容中提取真实的下载链接
3. **格式统一**: 将不同格式的数据统一转换为标准输出格式
4. **用户体验**: 提供清晰、结构化的搜索结果展示

这个功能增强了SearchService的数据处理能力，使其能够处理更多样化的数据格式，同时保持输出的一致性和可读性。