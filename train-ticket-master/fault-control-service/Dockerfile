FROM openjdk:11-jre-slim

# 配置USTC镜像源
RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list \
    && sed -i 's/security.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list

# 安装Python 3.10
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*


# 复制kubectl二进制文件和配置文件
COPY docker/kubectl /usr/local/bin/kubectl
# COPY docker/config /root/.kube/config

# 设置kubectl执行权限
RUN chmod +x /usr/local/bin/kubectl

WORKDIR /app

# 复制train-ticket-auto-query目录
COPY train-ticket-auto-query/ ./train-ticket-auto-query/

# 安装pip 包
RUN cd train-ticket-auto-query && pip3 install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 复制拓扑配置文件
COPY src/main/resources/topologies/ ./topologies/

# 复制JAR文件
COPY target/fault-control-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 