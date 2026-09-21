# 1. 重新构建镜像并后台启动（仅重启代码发生变化的服务，其余服务保持运行）
docker compose up -d --build [服务名]

# 2. （可选）清理构建过程中产生的临时缓存和旧镜像
docker image prune -f