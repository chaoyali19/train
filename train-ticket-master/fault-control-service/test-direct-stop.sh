#!/bin/bash

# 测试直接停止所有故障功能
# 作者: 故障控制服务
# 日期: $(date +%Y-%m-%d)

echo "=========================================="
echo "测试直接停止所有故障功能"
echo "=========================================="

# 配置
BASE_URL="http://localhost:8090"
DIRECT_STOP_URL="$BASE_URL/stop-all-faults-direct"
CONFIRM_URL="$BASE_URL/stop-all-faults"

echo "测试时间: $(date)"
echo "基础URL: $BASE_URL"
echo "直接停止URL: $DIRECT_STOP_URL"
echo "确认页面URL: $CONFIRM_URL"
echo ""

# 检查服务是否运行
echo "1. 检查服务状态..."
if curl -s "$BASE_URL" > /dev/null; then
    echo "✅ 服务运行正常"
else
    echo "❌ 服务未运行，请先启动服务"
    exit 1
fi

echo ""

# 测试直接停止接口
echo "2. 测试直接停止所有故障接口..."
echo "请求URL: $DIRECT_STOP_URL"

# 发送GET请求
response=$(curl -s -w "\n%{http_code}" "$DIRECT_STOP_URL")
http_code=$(echo "$response" | tail -n1)
content=$(echo "$response" | head -n -1)

echo "HTTP状态码: $http_code"

if [ "$http_code" = "200" ]; then
    echo "✅ 接口调用成功"
    
    # 检查返回内容
    if echo "$content" | grep -q "故障已恢复"; then
        echo "✅ 返回成功页面"
    else
        echo "⚠️  返回内容可能不是预期的成功页面"
    fi
    
    if echo "$content" | grep -q "即将自动跳转"; then
        echo "✅ 包含自动跳转提示"
    else
        echo "⚠️  缺少自动跳转提示"
    fi
    
    if echo "$content" | grep -q "countdown"; then
        echo "✅ 包含倒计时功能"
    else
        echo "⚠️  缺少倒计时功能"
    fi
    
else
    echo "❌ 接口调用失败"
    echo "响应内容:"
    echo "$content"
fi

echo ""

# 测试确认页面（对比）
echo "3. 对比确认页面接口..."
echo "请求URL: $CONFIRM_URL"

response=$(curl -s -w "\n%{http_code}" "$CONFIRM_URL")
http_code=$(echo "$response" | tail -n1)
content=$(echo "$response" | head -n -1)

echo "HTTP状态码: $http_code"

if [ "$http_code" = "200" ]; then
    echo "✅ 确认页面接口正常"
    
    if echo "$content" | grep -q "确认停止所有故障"; then
        echo "✅ 确认页面包含确认提示"
    else
        echo "⚠️  确认页面可能缺少确认提示"
    fi
else
    echo "❌ 确认页面接口异常"
fi

echo ""

# 功能对比
echo "4. 功能对比..."
echo "直接停止接口 ($DIRECT_STOP_URL):"
echo "  - 无需确认，直接执行停止操作"
echo "  - 显示成功页面，2秒后自动跳转"
echo "  - 适合自动化脚本或紧急情况"
echo ""
echo "确认页面接口 ($CONFIRM_URL):"
echo "  - 显示确认页面，需要用户确认"
echo "  - 提供确认和取消选项"
echo "  - 适合用户手动操作"
echo ""

# 浏览器测试建议
echo "5. 浏览器测试建议..."
echo "在浏览器中测试以下URL:"
echo "  - 直接停止: $DIRECT_STOP_URL"
echo "  - 确认页面: $CONFIRM_URL"
echo ""
echo "预期行为:"
echo "  - 访问直接停止URL后，应该显示成功页面"
echo "  - 页面显示'故障已恢复'和倒计时"
echo "  - 2秒后自动跳转到首页"
echo "  - 可以点击'立即跳转'按钮立即跳转"
echo ""

# 检查页面文件
echo "6. 检查页面文件..."
if [ -f "src/main/resources/templates/stop-all-success.html" ]; then
    echo "✅ 成功页面模板文件存在"
else
    echo "❌ 成功页面模板文件不存在"
fi

if [ -f "src/main/resources/templates/stop-all-confirm.html" ]; then
    echo "✅ 确认页面模板文件存在"
else
    echo "❌ 确认页面模板文件不存在"
fi

echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="

# 提供使用示例
echo ""
echo "使用示例:"
echo "1. 直接停止所有故障:"
echo "   curl $DIRECT_STOP_URL"
echo ""
echo "2. 在浏览器中访问:"
echo "   $DIRECT_STOP_URL"
echo ""
echo "3. 查看确认页面:"
echo "   $CONFIRM_URL" 