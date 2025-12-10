# 拓扑图功能测试说明

## 新增功能

### 1. 拓扑选择优化
- ✅ 下拉框显示拓扑的 `name` 字段（如"用户登录"、"车票查询"等）
- ✅ 鼠标悬停显示拓扑描述信息

### 2. 布局优化
- ✅ 拓扑图自动居中显示
- ✅ 避免节点重叠的智能布局算法
- ✅ 支持更多服务节点的合理分布

### 3. 缩放功能
- ✅ 鼠标滚轮缩放（向上滚动放大，向下滚动缩小）
- ✅ 缩放按钮控制（放大、缩小、重置）
- ✅ 缩放范围限制（0.3x - 3x）

### 4. 拖拽功能
- ✅ 鼠标拖拽移动整个拓扑图
- ✅ 拖拽时鼠标指针变化（grab/grabbing）
- ✅ 节点点击不受拖拽影响

### 5. 视觉优化
- ✅ 更美观的节点卡片设计
- ✅ 渐变背景和阴影效果
- ✅ 悬停动画效果
- ✅ 保持完整服务名称显示

## 使用方法

### 缩放操作
1. **鼠标滚轮**：在拓扑图上滚动鼠标滚轮进行缩放
2. **按钮控制**：
   - 点击放大按钮（+）放大
   - 点击缩小按钮（-）缩小
   - 点击重置按钮（↔）恢复原始大小

### 拖拽操作
1. **移动拓扑**：在空白区域按住鼠标左键拖拽
2. **点击节点**：点击服务节点进行故障注入

### 布局特性
1. **自动居中**：拓扑图会自动居中显示
2. **智能分布**：节点会自动避免重叠
3. **响应式**：支持不同屏幕尺寸

## 技术实现

### 缩放实现
```javascript
// 缩放控制
let currentScale = 1;
function zoom(factor) {
    currentScale *= factor;
    currentScale = Math.max(0.3, Math.min(3, currentScale));
    updateTransform();
}
```

### 拖拽实现
```javascript
// 拖拽控制
let isDragging = false;
let currentTranslateX = 0;
let currentTranslateY = 0;

container.addEventListener('mousedown', function(e) {
    if (e.target.classList.contains('node-card')) return;
    isDragging = true;
    // ... 拖拽逻辑
});
```

### 居中布局
```javascript
// 计算边界并居中
let minX = Infinity, maxX = -Infinity;
// ... 计算所有节点边界
const offsetX = (width - topologyWidth) / 2 - minX;
const offsetY = (height - topologyHeight) / 2 - minY;
```

## 测试步骤

1. **启动应用**：
   ```bash
   cd fault-control-service
   mvn spring-boot:run
   ```

2. **访问拓扑页面**：
   ```
   http://localhost:8090/topology
   ```

3. **测试功能**：
   - 选择不同的拓扑图
   - 使用鼠标滚轮缩放
   - 拖拽移动拓扑图
   - 点击节点进行故障注入
   - 测试缩放按钮

## 预期效果

- 拓扑图在容器中居中显示
- 支持流畅的缩放和拖拽操作
- 节点布局合理，避免重叠
- 视觉效果美观，交互友好 