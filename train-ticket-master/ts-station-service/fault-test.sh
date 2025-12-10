#!/bin/bash

# ts-station-service 故障测试脚本
# 用于演示站点查询接口的故障注入和恢复

# 设置服务地址（根据实际部署情况调整）
SERVICE_URL="http://localhost:12345"
FAULT_BASE_URL="${SERVICE_URL}/api/v1/stationservice/fault"
STATION_QUERY_URL="${SERVICE_URL}/api/v1/stationservice/stations"

echo "=== ts-station-service 故障测试脚本 ==="
echo "服务地址: ${SERVICE_URL}"
echo ""

# 函数：测试站点查询接口
test_station_query() {
    echo "测试站点查询接口..."
    echo "请求地址: ${STATION_QUERY_URL}"
    
    response=$(curl -s -w "\nHTTP状态码: %{http_code}\n响应时间: %{time_total}s" "${STATION_QUERY_URL}")
    echo "$response"
    echo ""
}

# 函数：启用空数据故障
enable_empty_data_fault() {
    echo "启用空数据故障..."
    response=$(curl -s -X POST "${FAULT_BASE_URL}/enableEmptyStationQuery?enable=true")
    echo "响应: $response"
    echo ""
}

# 函数：禁用空数据故障
disable_empty_data_fault() {
    echo "禁用空数据故障..."
    response=$(curl -s -X POST "${FAULT_BASE_URL}/enableEmptyStationQuery?enable=false")
    echo "响应: $response"
    echo ""
}

# 函数：启用延迟故障
enable_delay_fault() {
    local delay_ms=${1:-10000}
    echo "启用延迟故障 (延迟: ${delay_ms}ms)..."
    response=$(curl -s -X POST "${FAULT_BASE_URL}/enableStationQueryDelay?enable=true&delayMs=${delay_ms}")
    echo "响应: $response"
    echo ""
}

# 函数：禁用延迟故障
disable_delay_fault() {
    echo "禁用延迟故障..."
    response=$(curl -s -X POST "${FAULT_BASE_URL}/enableStationQueryDelay?enable=false")
    echo "响应: $response"
    echo ""
}

# 函数：查看故障状态
check_fault_status() {
    echo "查看当前故障状态..."
    response=$(curl -s "${FAULT_BASE_URL}/status")
    echo "状态: $response"
    echo ""
}

# 主测试流程
main() {
    echo "1. 初始状态测试"
    check_fault_status
    test_station_query
    
    echo "2. 启用空数据故障测试"
    enable_empty_data_fault
    check_fault_status
    test_station_query
    
    echo "3. 禁用空数据故障测试"
    disable_empty_data_fault
    check_fault_status
    test_station_query
    
    echo "4. 启用延迟故障测试 (5秒延迟)"
    enable_delay_fault 5000
    check_fault_status
    echo "注意：这次查询会有5秒延迟..."
    test_station_query
    
    echo "5. 禁用延迟故障测试"
    disable_delay_fault
    check_fault_status
    test_station_query
    
    echo "=== 测试完成 ==="
}

# 如果直接运行脚本，执行主流程
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main
fi 