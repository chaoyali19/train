#!/bin/bash

# 测试停止所有故障页面功能
# 使用方法: ./test-stop-all-page.sh

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 配置
BASE_URL="http://localhost:8090"

echo "=== 测试停止所有故障页面功能 ==="

# 1. 检查应用是否运行
log_info "1. 检查应用是否运行..."
if curl -s "$BASE_URL/" > /dev/null 2>&1; then
    log_success "应用正在运行"
else
    log_error "应用未运行，请先启动应用"
    exit 1
fi

# 2. 测试确认页面访问
log_info "2. 测试确认页面访问..."
CONFIRM_PAGE=$(curl -s "$BASE_URL/stop-all-faults")
if echo "$CONFIRM_PAGE" | grep -q "停止所有故障"; then
    log_success "确认页面可以正常访问"
else
    log_error "确认页面访问失败"
    exit 1
fi

# 3. 检查页面内容
log_info "3. 检查页面内容..."
if echo "$CONFIRM_PAGE" | grep -q "您即将停止所有正在运行的 Chaos Mesh 故障注入"; then
    log_success "页面包含正确的警告信息"
else
    log_warning "页面可能缺少警告信息"
fi

if echo "$CONFIRM_PAGE" | grep -q "确认停止所有故障"; then
    log_success "页面包含确认按钮"
else
    log_error "页面缺少确认按钮"
fi

# 4. 测试 API 接口
log_info "4. 测试 API 接口..."
ACTIVE_FAULTS=$(curl -s "$BASE_URL/api/chaos/active")
echo "当前活跃故障: $ACTIVE_FAULTS"

# 5. 测试停止所有故障的 API
log_info "5. 测试停止所有故障的 API..."
STOP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chaos/stop-all" \
    -H "Content-Type: application/json")
echo "停止响应: $STOP_RESPONSE"

# 6. 检查主页面的停止所有故障链接
log_info "6. 检查主页面的停止所有故障链接..."
MAIN_PAGE=$(curl -s "$BASE_URL/")
if echo "$MAIN_PAGE" | grep -q "停止所有故障"; then
    log_success "主页面包含停止所有故障的链接"
else
    log_error "主页面缺少停止所有故障的链接"
fi

# 7. 验证链接地址
if echo "$MAIN_PAGE" | grep -q "href=\"/stop-all-faults\""; then
    log_success "链接地址正确"
else
    log_error "链接地址不正确"
fi

echo ""
log_success "所有测试完成！"
echo ""
echo "使用说明："
echo "1. 访问 http://localhost:8090/ 查看主页面"
echo "2. 点击导航栏中的 '停止所有故障' 链接"
echo "3. 在确认页面查看当前活跃故障"
echo "4. 点击 '确认停止所有故障' 按钮"
echo "5. 等待操作完成后自动跳转到主页面" 