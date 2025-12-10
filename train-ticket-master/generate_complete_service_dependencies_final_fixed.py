#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re
import json
from pathlib import Path
from collections import defaultdict

def extract_service_name_from_path(file_path):
    """从文件路径中提取服务名称"""
    parts = file_path.split('/')
    for part in parts:
        if part.startswith('ts-') and part.endswith('-service'):
            return part
    return None

def parse_controller_file(file_path):
    """解析Controller文件，提取接口和方法名"""
    service_name = extract_service_name_from_path(file_path)
    if not service_name:
        return None
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"无法读取文件 {file_path}: {e}")
        return None
    
    # 提取包名
    package_match = re.search(r'package\s+([^;]+);', content)
    package_name = package_match.group(1) if package_match else ""
    
    # 提取类名
    class_match = re.search(r'public\s+class\s+(\w+)', content)
    class_name = class_match.group(1) if class_match else ""
    
    # 完整的类名（包含包名）
    full_class_name = f"{package_name}.{class_name}" if package_name and class_name else ""
    
    # 查找@RequestMapping注解
    request_mapping_match = re.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
    base_path = request_mapping_match.group(1) if request_mapping_match else ""
    
    # 改进的注解模式，支持path和value参数，以及更灵活的格式
    method_patterns = [
        (r'@GetMapping\s*\(\s*(?:path\s*=\s*|value\s*=\s*)?["\']([^"\']+)["\']', 'GET'),
        (r'@PostMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', 'POST'),
        (r'@PutMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', 'PUT'),
        (r'@DeleteMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', 'DELETE'),
        (r'@PatchMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', 'PATCH')
    ]
    
    # 处理没有指定路径的注解
    no_path_patterns = [
        (r'@GetMapping\s*\(\s*\)', 'GET'),
        (r'@PostMapping\s*\(\s*\)', 'POST'),
        (r'@PutMapping\s*\(\s*\)', 'PUT'),
        (r'@DeleteMapping\s*\(\s*\)', 'DELETE'),
        (r'@PatchMapping\s*\(\s*\)', 'PATCH')
    ]
    
    interfaces = []
    
    # 处理有路径的注解
    for pattern, method_type in method_patterns:
        matches = re.finditer(pattern, content)
        for match in matches:
            path = match.group(1)
            full_path = base_path + path if base_path else path
            
            # 查找对应的方法名
            start_pos = match.end()
            method_content = content[start_pos:start_pos + 1000]
            
            # 改进的方法名查找，支持更多返回类型
            method_match = re.search(r'public\s+(?:\w+\.\w+|\w+)\s+(\w+)\s*\(', method_content)
            if method_match:
                method_name = method_match.group(1)
                interfaces.append({
                    "interface_name": full_path,
                    "method_name": method_name,
                    "http_method": method_type,
                    "class_name": full_class_name
                })
    
    # 处理没有路径的注解
    for pattern, method_type in no_path_patterns:
        matches = re.finditer(pattern, content)
        for match in matches:
            # 使用base_path作为路径
            full_path = base_path if base_path else ""
            
            # 查找对应的方法名
            start_pos = match.end()
            method_content = content[start_pos:start_pos + 1000]
            
            method_match = re.search(r'public\s+(?:\w+\.\w+|\w+)\s+(\w+)\s*\(', method_content)
            if method_match:
                method_name = method_match.group(1)
                interfaces.append({
                    "interface_name": full_path,
                    "method_name": method_name,
                    "http_method": method_type,
                    "class_name": full_class_name
                })
    
    return {
        "service_name": service_name,
        "interfaces": interfaces
    }

def find_service_impl_files():
    """查找所有ServiceImpl文件"""
    impl_files = []
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith('ServiceImpl.java'):
                impl_files.append(os.path.join(root, file))
    return impl_files

def find_rest_template_calls(file_path):
    """查找文件中的RestTemplate调用"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"无法读取文件 {file_path}: {e}")
        return []
    
    calls = []
    
    # 查找getServiceUrl调用，这通常用于构建服务URL
    service_url_pattern = r'getServiceUrl\s*\(\s*["\']([^"\']+)["\']'
    service_url_matches = re.finditer(service_url_pattern, content)
    
    for match in service_url_matches:
        service_name = match.group(1)
        # 查找该服务URL后面的API路径
        start_pos = match.end()
        # 查找该服务URL后面的exchange调用
        exchange_context = content[start_pos:start_pos + 1000]
        
        # 查找exchange调用
        exchange_pattern = r'restTemplate\.exchange\s*\(\s*([^,]+),\s*HttpMethod\.(\w+)'
        exchange_matches = re.finditer(exchange_pattern, exchange_context, re.IGNORECASE)
        
        for exchange_match in exchange_matches:
            url_part = exchange_match.group(1).strip()
            http_method = exchange_match.group(2).upper()
            
            # 查找API路径
            api_pattern = r'["\'](/api/v1/[^"\']+)["\']'
            api_match = re.search(api_pattern, url_part)
            if api_match:
                api_path = api_match.group(1)
                calls.append({
                    'type': 'rest',
                    'service_name': service_name,
                    'url': f"http://{service_name}:8080{api_path}",
                    'api_path': api_path,
                    'http_method': http_method,
                    'file': file_path
                })
    
    # 查找直接的URL调用
    direct_url_pattern = r'restTemplate\.exchange\s*\(\s*["\']([^"\']+)["\']'
    direct_url_matches = re.finditer(direct_url_pattern, content, re.IGNORECASE)
    
    for match in direct_url_matches:
        url = match.group(1)
        if '/api/v1/' in url:
            # 从URL中提取服务名
            if 'ts-' in url and '-service' in url:
                service_match = re.search(r'ts-[^:]+-service', url)
                if service_match:
                    service_name = service_match.group(0)
                    api_path = url.split('/api/v1/')[1] if '/api/v1/' in url else ''
                    calls.append({
                        'type': 'rest',
                        'service_name': service_name,
                        'url': url,
                        'api_path': f"/api/v1/{api_path}",
                        'http_method': 'UNKNOWN',
                        'file': file_path
                    })
    
    # 查找字符串拼接的URL调用
    # 匹配模式：service_url + "/api/v1/..."
    service_url_plus_pattern = r'(\w+_service_url)\s*\+\s*["\'](/api/v1/[^"\']+)["\']'
    service_url_plus_matches = re.finditer(service_url_plus_pattern, content)
    
    for match in service_url_plus_matches:
        service_url_var = match.group(1)
        api_path = match.group(2)
        
        # 查找service_url_var的定义
        service_url_def_pattern = rf'{service_url_var}\s*=\s*getServiceUrl\s*\(\s*["\']([^"\']+)["\']'
        service_url_def_match = re.search(service_url_def_pattern, content)
        
        if service_url_def_match:
            service_name = service_url_def_match.group(1)
            calls.append({
                'type': 'rest',
                'service_name': service_name,
                'url': f"http://{service_name}:8080{api_path}",
                'api_path': api_path,
                'http_method': 'UNKNOWN',
                'file': file_path
            })
    
    return calls

def normalize_interface_path(interface_path):
    """标准化接口路径，移除开头的斜杠"""
    if interface_path.startswith('/'):
        return interface_path[1:]
    return interface_path

def is_path_match(target_path, call_path):
    """更精确的路径匹配逻辑"""
    # 标准化路径
    target_path = normalize_interface_path(target_path)
    call_path = normalize_interface_path(call_path)
    
    # 完全匹配
    if target_path == call_path:
        return True
    
    # 处理路径参数的情况，如 /api/v1/configservice/configs/{configName} 匹配 /api/v1/configservice/configs/DirectTicketAllocationProportion
    if '{' in target_path:
        # 移除路径参数部分进行匹配
        target_base = target_path.split('{')[0].rstrip('/')
        call_base = call_path.split('/')[:-1]  # 移除最后一部分
        call_base = '/'.join(call_base)
        if target_base == call_base:
            return True
    
    # 更严格的匹配：只有当目标路径以斜杠结尾时，才允许调用路径以目标路径开头
    if target_path.endswith('/'):
        return call_path.startswith(target_path)
    
    # 对于不以斜杠结尾的目标路径，只允许完全匹配或路径参数匹配
    return False

def analyze_upstream_calls(service_dependencies, impl_files):
    """分析上游调用关系"""
    
    # 首先收集所有服务的调用信息
    all_calls = []
    for impl_file in impl_files:
        calls = find_rest_template_calls(impl_file)
        all_calls.extend(calls)
    
    print(f"找到 {len(all_calls)} 个RestTemplate调用")
    
    # 统计添加的upstream数量
    added_upstream_count = 0
    
    # 为每个接口添加上游服务信息
    for service in service_dependencies:
        for interface in service['interfaces']:
            upstream_services = []
            
            # 查找调用该接口的服务
            target_service = service['service_name']
            target_interface = interface['interface_name']
            target_method = interface['method_name']
            target_http_method = interface['http_method']
            
            for call in all_calls:
                if call['type'] == 'rest':
                    # 检查服务名和API路径是否匹配
                    if call['service_name'] == target_service:
                        # 使用更精确的路径匹配
                        if is_path_match(target_interface, call['api_path']):
                            caller_service = extract_service_name_from_path(call['file'])
                            if caller_service:
                                upstream_services.append({
                                    'service_name': caller_service,
                                    'interface_name': call['api_path'],
                                    'method_name': f"{call['http_method']}_call"
                                })
            
            if upstream_services:
                interface['upstream_services'] = upstream_services
                added_upstream_count += 1
                print(f"为 {target_service} 的 {target_method} ({target_http_method}) 找到 {len(upstream_services)} 个上游服务")
    
    print(f"总共为 {added_upstream_count} 个接口添加了upstream信息")
    return service_dependencies

def main():
    # 指定的服务列表
    target_services = [
        "ts-seat-service",
        "ts-config-service", 
        "ts-order-service",
        "ts-route-service",
        "ts-verification-code-service",
        "ts-order-other-service",
        "ts-train-service",
        "ts-station-service",
        "ts-user-service",
        "ts-travel-service",
        "ts-basic-service",
        "ts-price-service",
        "ts-travel2-service",
        "ts-contacts-service",
        "ts-food-service",
        "ts-train-food-service",
        "ts-route-plan-service",
        "ts-travel-plan-service",
        "ts-station-food-service",
        "ts-security-service",
        "ts-assurance-service",
        "ts-preserve-service",
        "ts-consign-service"
    ]
    
    print("=== 第一步：解析Controller文件 ===")
    
    # 查找所有Controller文件
    controller_files = []
    for service in target_services:
        service_path = Path(service)
        if service_path.exists():
            # 查找该服务下的所有Controller文件
            controller_files.extend(service_path.glob("src/main/java/**/controller/*Controller.java"))
    
    print(f"找到 {len(controller_files)} 个Controller文件")
    
    # 解析每个Controller文件
    service_dependencies = []
    processed_services = set()
    
    for controller_file in controller_files:
        print(f"正在解析: {controller_file}")
        result = parse_controller_file(str(controller_file))
        if result and result["interfaces"]:
            service_name = result["service_name"]
            if service_name not in processed_services:
                service_dependencies.append(result)
                processed_services.add(service_name)
                print(f"  - 服务: {service_name}")
                print(f"  - 接口数量: {len(result['interfaces'])}")
            else:
                # 如果服务已存在，合并接口
                for existing_service in service_dependencies:
                    if existing_service["service_name"] == service_name:
                        existing_service["interfaces"].extend(result["interfaces"])
                        print(f"  - 合并到现有服务: {service_name}")
                        print(f"  - 新增接口数量: {len(result['interfaces'])}")
                        break
    
    print(f"\n=== 第二步：分析上游调用关系 ===")
    
    print("正在查找ServiceImpl文件...")
    impl_files = find_service_impl_files()
    print(f"找到 {len(impl_files)} 个ServiceImpl文件")
    
    print("正在分析调用关系...")
    updated_dependencies = analyze_upstream_calls(service_dependencies, impl_files)
    
    # 生成配置文件
    output_data = {
        "service_dependencies": updated_dependencies
    }
    
    # 写入文件
    with open('service_dependencies_with_upstream.json', 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)
    
    print(f"\n=== 生成完成 ===")
    print(f"共处理了 {len(updated_dependencies)} 个服务")
    print("结果已保存到 service_dependencies_with_upstream.json")
    
    # 统计信息
    total_interfaces = sum(len(service['interfaces']) for service in updated_dependencies)
    interfaces_with_upstream = sum(
        len([i for i in service['interfaces'] if 'upstream_services' in i])
        for service in updated_dependencies
    )
    
    print(f"总接口数: {total_interfaces}")
    print(f"有upstream信息的接口数: {interfaces_with_upstream}")
    print(f"覆盖率: {interfaces_with_upstream/total_interfaces*100:.1f}%")

if __name__ == "__main__":
    main() 