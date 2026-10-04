---
status: accepted
---

# 受控下载使用短时一次性票据

Meteor 先通过带 CSRF 保护的授权请求为当前用户签发五分钟有效的一次性 Download Ticket，数据库只保存 token 的 SHA-256；真正读取文件时按票据用户重新校验 ACTIVE Grant、PUBLISHED Release 和 ManifestItem 归属，成功准备文件响应后消费票据并记录 Download Audit。相比直接暴露存储地址或长期 URL，这一选择允许撤权和版本禁用立即阻断尚未消费的票据；代价是当前 Classpath 最小实现需要把小型学习素材完整读入内存，未来接入对象存储时应保留授权语义并替换文件传输实现。
