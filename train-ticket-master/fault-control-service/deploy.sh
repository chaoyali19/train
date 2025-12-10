#!/bin/bash

# 故障控制服务部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java未安装或不在PATH中"
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_info "检测到Java版本: $java_version"
}

# 检查Maven环境
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven未安装或不在PATH中"
        exit 1
    fi
    
    maven_version=$(mvn -version 2>&1 | head -n 1)
    print_info "检测到Maven: $maven_version"
}

# 构建项目
build_project() {
    print_info "构建故障控制服务..."
    
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml 不存在，请确保在项目根目录执行"
        exit 1
    fi
    
    mvn clean package -DskipTests
    print_success "项目构建完成"
}

# 运行服务
run_service() {
    print_info "启动故障控制服务..."
    
    if [ ! -f "target/fault-control-service-1.0.0.jar" ]; then
        print_error "JAR文件不存在，请先构建项目"
        exit 1
    fi
    
    java -jar target/fault-control-service-1.0.0.jar
}

# 构建Docker镜像
build_docker() {
    print_info "构建Docker镜像..."
    
    if [ ! -f "Dockerfile" ]; then
        print_error "Dockerfile 不存在"
        exit 1
    fi
    
    docker build -t fault-control-service:latest .
    print_success "Docker镜像构建完成"
}

# 运行Docker容器
run_docker() {
    print_info "启动Docker容器..."
    
    # 停止并删除已存在的容器
    if docker ps -a | grep -q fault-control-service; then
        print_info "停止已存在的容器..."
        docker stop fault-control-service || true
        docker rm fault-control-service || true
    fi
    
    # 运行新容器
    docker run -d -p 8080:8080 \
        --name fault-control-service \
        fault-control-service:latest
    
    print_success "Docker容器启动完成"
    print_info "访问地址: http://localhost:8080/fault-control"
}

# 停止服务
stop_service() {
    print_info "停止故障控制服务..."
    
    # 停止Java进程
    pkill -f "fault-control-service" || true
    
    # 停止Docker容器
    if docker ps | grep -q fault-control-service; then
        docker stop fault-control-service
        print_success "Docker容器已停止"
    fi
    
    print_success "服务已停止"
}

# 查看日志
show_logs() {
    print_info "查看服务日志..."
    
    if docker ps | grep -q fault-control-service; then
        docker logs -f fault-control-service
    else
        print_warning "Docker容器未运行，尝试查看Java进程日志..."
        tail -f logs/spring.log 2>/dev/null || print_error "日志文件不存在"
    fi
}

# 检查服务状态
check_status() {
    print_info "检查服务状态..."
    
    # 检查Java进程
    if pgrep -f "fault-control-service" > /dev/null; then
        print_success "Java进程正在运行"
    else
        print_warning "Java进程未运行"
    fi
    
    # 检查Docker容器
    if docker ps | grep -q fault-control-service; then
        print_success "Docker容器正在运行"
        docker ps | grep fault-control-service
    else
        print_warning "Docker容器未运行"
    fi
    
    # 检查端口
    if netstat -tuln 2>/dev/null | grep -q ":8080 "; then
        print_success "端口8080正在监听"
    else
        print_warning "端口8080未监听"
    fi
}

# 清理
cleanup() {
    print_info "清理项目..."
    
    # 停止服务
    stop_service
    
    # 清理Maven构建
    mvn clean
    
    # 删除Docker镜像
    if docker images | grep -q fault-control-service; then
        docker rmi fault-control-service:latest
        print_success "Docker镜像已删除"
    fi
    
    print_success "清理完成"
}

# 显示帮助信息
show_help() {
    echo "故障控制服务部署脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  build       构建项目"
    echo "  run         运行服务（Java）"
    echo "  docker      构建并运行Docker容器"
    echo "  stop        停止服务"
    echo "  logs        查看日志"
    echo "  status      检查服务状态"
    echo "  cleanup     清理项目"
    echo "  help        显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 build     # 构建项目"
    echo "  $0 run       # 运行服务"
    echo "  $0 docker    # 使用Docker运行"
}

# 主函数
main() {
    case "${1:-help}" in
        "build")
            check_java
            check_maven
            build_project
            ;;
        "run")
            check_java
            run_service
            ;;
        "docker")
            check_maven
            build_project
            build_docker
            run_docker
            ;;
        "stop")
            stop_service
            ;;
        "logs")
            show_logs
            ;;
        "status")
            check_status
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@" 