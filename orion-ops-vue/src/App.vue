<!-- src/App.vue -->
<template>
  <a-config-provider :locale="locale">
    <div id="app">
      <router-view />
      <!-- 添加 AI 助手组件 -->
      <AiAssistant />
    </div>
  </a-config-provider>
</template>

<script>
import './css/common.less'
import './css/layout.less'
import './css/table.less'
import './css/component.less'
import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN'
import AiAssistant from '@/components/common/AiAssistant'

export default {
  name: 'App',
  components: { AiAssistant },
  data() {
    return {
      locale: zhCN
    }
  }
}
</script>
<style>
/* 全局基础样式 */
html,
body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #F8FAFF 0%, #F0F4FE 100%);
}

#app {
  width: 100%;
  height: 100%;
  font-family: Bahnschrift, 'Segoe UI', 'Microsoft YaHei UI', 'PingFang SC', sans-serif;
}

/* ========== 全局浅色主题变量 ========== */
:root {
  --primary-color: #3B82F6;
  --primary-hover: #2563EB;
  --primary-light: #EFF6FF;
  --success-color: #10B981;
  --success-light: #F0FDF4;
  --warning-color: #F59E0B;
  --warning-light: #FFFBEB;
  --error-color: #EF4444;
  --error-light: #FEF2F2;
  --text-primary: #0F172A;
  --text-secondary: #475569;
  --text-tertiary: #64748B;
  --text-disabled: #94A3B8;
  --bg-white: #FFFFFF;
  --bg-gray: #F8FAFF;
  --bg-hover: #F1F5F9;
  --border-color: #E2E8F0;
  --border-light: #F1F5F9;
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
}

/* ========== 覆盖 Ant Design 全局样式 ========== */

/* 卡片样式 */
.ant-card {
  background: white;
  border-color: var(--border-color);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
}

.ant-card-head {
  border-bottom-color: var(--border-light);
  color: var(--text-primary);
}

.ant-card-head-title {
  color: var(--text-primary);
  font-weight: 600;
}

/* 表格样式 */
.ant-table {
  background: white;
  color: var(--text-primary);
  border-radius: 16px;
}

.ant-table-thead > tr > th {
  background: #F8FAFF;
  color: var(--text-secondary);
  font-weight: 600;
  border-bottom-color: var(--border-color);
}

.ant-table-tbody > tr > td {
  border-bottom-color: var(--border-light);
  color: var(--text-primary);
}

.ant-table-tbody > tr:hover > td {
  background: #F8FAFF;
}

/* 按钮样式 */
.ant-btn {
  border-radius: 10px;
}

.ant-btn-primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.ant-btn-primary:hover,
.ant-btn-primary:focus {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

.ant-btn-default {
  background: white;
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.ant-btn-default:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 输入框样式 */
.ant-input,
.ant-input-number,
.ant-select-selector,
.ant-picker {
  background: white;
  border-color: var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
}

.ant-input:hover,
.ant-input-number:hover,
.ant-select-selector:hover,
.ant-picker:hover {
  border-color: var(--primary-color);
}

.ant-input:focus,
.ant-input-focused,
.ant-input-number-focused,
.ant-select-focused .ant-select-selector,
.ant-picker-focused {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

/* 下拉菜单 */
.ant-dropdown-menu,
.ant-select-dropdown {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.ant-dropdown-menu-item,
.ant-select-dropdown-menu-item {
  color: var(--text-secondary);
}

.ant-dropdown-menu-item:hover,
.ant-select-dropdown-menu-item:hover {
  background: #F8FAFF;
  color: var(--primary-color);
}

/* 模态框 */
.ant-modal-content {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.ant-modal-header {
  background: white;
  border-bottom-color: var(--border-light);
  border-radius: 20px 20px 0 0;
}

.ant-modal-title {
  color: var(--text-primary);
  font-weight: 600;
}

.ant-modal-footer {
  border-top-color: var(--border-light);
}

/* 标签页 */
.ant-tabs-tab {
  color: var(--text-tertiary);
}

.ant-tabs-tab-active {
  color: var(--primary-color);
}

.ant-tabs-ink-bar {
  background: var(--primary-color);
}

/* 分页 */
.ant-pagination-item {
  background: white;
  border-color: var(--border-color);
  border-radius: 8px;
}

.ant-pagination-item a {
  color: var(--text-secondary);
}

.ant-pagination-item-active {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.ant-pagination-item-active a {
  color: white;
}

/* 消息通知 */
.ant-message-notice-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

/* 抽屉 */
.ant-drawer-content {
  background: white;
}

.ant-drawer-header {
  border-bottom-color: var(--border-light);
}

/* 树形控件 */
.ant-tree {
  background: transparent;
  color: var(--text-primary);
}

.ant-tree-node-content-wrapper:hover {
  background: #F8FAFF;
}

/* 步骤条 */
.ant-steps-item-title {
  color: var(--text-primary);
}

.ant-steps-item-description {
  color: var(--text-tertiary);
}

/* 面包屑 */
.ant-breadcrumb {
  color: var(--text-tertiary);
}

.ant-breadcrumb a {
  color: var(--text-tertiary);
}

.ant-breadcrumb a:hover {
  color: var(--primary-color);
}

/* 空状态 */
.ant-empty-description {
  color: var(--text-tertiary);
}

/* 进度条 */
.ant-progress-bg {
  background: var(--primary-color);
}

.ant-progress-text {
  color: var(--text-secondary);
}

/* 标签 */
.ant-tag {
  border-radius: 6px;
  background: #F8FAFF;
  border-color: var(--border-color);
  color: var(--text-secondary);
}

/* 开关 */
.ant-switch {
  background: #CBD5E1;
}

.ant-switch-checked {
  background: var(--primary-color);
}

/* 单选/复选框 */
.ant-radio-wrapper,
.ant-checkbox-wrapper {
  color: var(--text-primary);
}

.ant-radio-inner,
.ant-checkbox-inner {
  border-color: var(--border-color);
}

.ant-radio-checked .ant-radio-inner,
.ant-checkbox-checked .ant-checkbox-inner {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

/* 上传组件 */
.ant-upload {
  color: var(--text-secondary);
}

.ant-upload.ant-upload-drag {
  background: #F8FAFF;
  border-color: var(--border-color);
  border-radius: 16px;
}

.ant-upload.ant-upload-drag:hover {
  border-color: var(--primary-color);
}

/* 选择器 */
.ant-select-multiple .ant-select-selection-item {
  background: #F8FAFF;
  border-color: var(--border-color);
  border-radius: 6px;
}

/* 日历 */
.ant-picker-cell-inner {
  color: var(--text-primary);
}

.ant-picker-cell-selected .ant-picker-cell-inner {
  background: var(--primary-color);
  color: white;
}

/* 菜单（侧边栏） */
.ant-menu-light .ant-menu-item-selected {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(6, 182, 212, 0.08));
  color: var(--primary-color);
}

.ant-menu-light .ant-menu-item:hover {
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary-color);
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #F1F5F9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
}
</style>