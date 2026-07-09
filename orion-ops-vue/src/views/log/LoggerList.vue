<template>
  <div class="log-list-container">
    <!-- 筛选列 -->
    <div class="table-search-columns">
      <a-form-model class="log-list-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="机器" prop="machine">
              <MachineSelector ref="machineSelector" @change="chooseMachine"/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="query.name" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="路径" prop="path">
              <a-input v-model="query.path" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="命令" prop="command">
              <a-input v-model="query.command" allowClear/>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </div>
    <!-- 表格 -->
    <div class="table-wrapper">
      <!-- 工具栏 - 修复布局 -->
      <div class="table-tools-bar">
        <!-- 左侧 -->
        <div class="tools-fixed-left">
          <span class="table-title">日志列表</span>
          <a-divider v-show="selectedRowKeys.length" type="vertical"/>
          <div v-show="selectedRowKeys.length" class="batch-actions">
            <a-popconfirm title="确认删除所选中的执行记录吗?"
                          placement="topRight"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="remove(selectedRowKeys)">
              <a-button type="danger" icon="delete" size="small">删除</a-button>
            </a-popconfirm>
          </div>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
          <a-button icon="filter" @click="cleanAnsi" class="action-btn">清除 ANSI</a-button>
          <a target="_blank" href="#/log/view">
            <a-button type="primary" icon="file-text" class="action-btn">日志面板</a-button>
          </a>
          <a-button type="primary" icon="upload" @click="upload" class="action-btn">上传</a-button>
          <a-button type="primary" icon="plus" @click="add" class="action-btn">添加</a-button>
          <a-divider type="vertical" class="divider-vertical" />
          <a-icon type="export" class="tools-icon" title="导出数据" @click="openExport"/>
          <a-icon type="import" class="tools-icon" title="导入数据" @click="openImport"/>
          <a-icon type="search" class="tools-icon" title="查询" @click="getList({})"/>
          <a-icon type="reload" class="tools-icon" title="重置" @click="resetForm"/>
        </div>
      </div>
      <!-- 表格 -->
      <div class="table-main-container table-scroll-x-auto">
        <a-table :columns="columns"
                 :dataSource="rows"
                 :pagination="pagination"
                 :rowSelection="{selectedRowKeys, onChange: e => selectedRowKeys = e}"
                 rowKey="id"
                 @change="getList"
                 :scroll="{x: '100%'}"
                 :loading="loading"
                 size="middle">
          <!-- 主机 -->
          <template #machine="record">
            <a-tooltip placement="top">
              <template #title>
                <span>{{ `${record.machineName} (${record.machineHost})` }}</span>
              </template>
              <span class="machine-name">{{ record.machineName }}</span>
            </a-tooltip>
          </template>
          <!-- 名称 -->
          <template #name="record">
            <span class="name-text">{{ record.name }}</span>
          </template>
          <!-- 路径 -->
          <template #path="record">
            <span class="pointer path-text" title="预览" @click="previewText(record.path)">
              {{ record.path }}
            </span>
          </template>
          <!-- 命令 -->
          <template #command="record">
            <span class="pointer command-text" title="预览" @click="previewText(record.command)">
              {{ record.command }}
            </span>
          </template>
          <!-- 修改时间 -->
          <template #updateTime="record">
            {{ record.updateTime | formatDate }}
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <!-- 打开 -->
              <a-tooltip title="ctrl 点击打开新页面" v-if="record.machineStatus === ENABLE_STATUS.ENABLE.value">
                <a target="_blank"
                   :href="`#/log/view/${record.id}`"
                   @click="openLogView($event, record.id)"
                   class="action-link">
                  打开
                </a>
              </a-tooltip>
              <a-tooltip title="机器未启用" v-else>
                <span class="action-link-disabled">打开</span>
              </a-tooltip>
              <a-divider type="vertical" class="action-divider" />
              <!-- 修改 -->
              <a @click="update(record.id)" class="action-link">修改</a>
              <a-divider type="vertical" class="action-divider" />
              <!-- 删除 -->
              <a-popconfirm title="是否要删除当前日志记录?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="remove([record.id])">
                <span class="action-link action-danger">删除</span>
              </a-popconfirm>
            </div>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="log-list-event">
      <!-- 文本预览 -->
      <TextPreview ref="previewText"/>
      <!-- 添加模态框 -->
      <AddLogFileModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <!-- 上传模态框 -->
      <UploadLogFileModal ref="uploadModal" @uploaded="getList({})"/>
      <!-- 清除ANSI模态框 -->
      <FileAnsiCleanModal ref="cleanModal"/>
      <!-- 日志模态框 -->
      <LoggerViewModal ref="logView"/>
      <!-- 导出模态框 -->
      <TailFileExportModal ref="export"/>
      <!-- 导入模态框 -->
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { ENABLE_STATUS, IMPORT_TYPE } from '@/lib/enum'
import MachineSelector from '@/components/machine/MachineSelector'
import AddLogFileModal from '@/components/log/AddLogFileModal'
import TextPreview from '@/components/preview/TextPreview'
import LoggerViewModal from '@/components/log/LoggerViewModal'
import TailFileExportModal from '@/components/export/TailFileExportModal'
import DataImportModal from '@/components/import/DataImportModal'
import UploadLogFileModal from '@/components/log/UploadLogFileModal'
import FileAnsiCleanModal from '@/components/log/FileAnsiCleanModal'

/**
 * 列
 */
const columns = [
  {
    title: '机器',
    key: 'machine',
    width: 150,
    ellipsis: true,
    sorter: (a, b) => a.machineName.localeCompare(b.machineName),
    scopedSlots: { customRender: 'machine' }
  },
  {
    title: '名称',
    key: 'name',
    dataIndex: 'name',
    width: 150,
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name),
    scopedSlots: { customRender: 'name' }
  },
  {
    title: '路径',
    key: 'path',
    width: 200,
    ellipsis: true,
    sorter: (a, b) => a.path.localeCompare(b.path),
    scopedSlots: { customRender: 'path' }
  },
  {
    title: '命令',
    key: 'command',
    width: 200,
    ellipsis: true,
    scopedSlots: { customRender: 'command' }
  },
  {
    title: '偏移量(行)',
    key: 'offset',
    dataIndex: 'offset',
    width: 100,
    align: 'center',
    sorter: (a, b) => a.offset - b.offset
  },
  {
    title: '编码',
    key: 'charset',
    dataIndex: 'charset',
    width: 80,
    align: 'center'
  },
  {
    title: '修改时间',
    key: 'updateTime',
    width: 160,
    ellipsis: true,
    align: 'center',
    sorter: (a, b) => a.updateTime - b.updateTime,
    scopedSlots: { customRender: 'updateTime' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 150,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'LoggerList',
  components: {
    FileAnsiCleanModal,
    UploadLogFileModal,
    DataImportModal,
    TailFileExportModal,
    LoggerViewModal,
    MachineSelector,
    AddLogFileModal,
    TextPreview
  },
  data() {
    return {
      ENABLE_STATUS,
      query: {
        machineId: undefined,
        name: undefined,
        path: undefined,
        command: undefined
      },
      rows: [],
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: function(total) {
          return `共 ${total} 条`
        }
      },
      selectedRowKeys: [],
      loading: false,
      columns,
      importType: IMPORT_TYPE.TAIL_FILE
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getTailList({
        ...this.query,
        page: page.current,
        limit: page.pageSize
      }).then(({ data }) => {
        const pagination = { ...this.pagination }
        pagination.total = data.total
        pagination.current = data.page
        this.rows = data.rows || []
        this.pagination = pagination
        this.selectedRowKeys = []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    chooseMachine(machineId) {
      this.query.machineId = machineId
      this.getList({})
    },
    upload() {
      this.$refs.uploadModal.open()
    },
    cleanAnsi() {
      this.$refs.cleanModal.open()
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(idList) {
      this.$api.deleteTailFile({
        idList
      }).then(() => {
        this.$message.success('已删除')
        this.getList({})
      })
    },
    previewText(value) {
      this.$refs.previewText.preview(value)
    },
    openExport() {
      this.$refs.export.open()
    },
    openImport() {
      this.$refs.import.open()
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.$refs.machineSelector.reset()
      this.query.machineId = undefined
      this.getList({})
    },
    openLogView(e, id) {
      if (!e.ctrlKey) {
        e.preventDefault()
        this.$refs.logView.open(id)
        return false
      } else {
        return true
      }
    }
  },
  filters: {
    formatDate
  },
  mounted() {
    this.getList({})
  }
}
</script>

<style lang="less" scoped>
.log-list-container {
  background: white;
  border-radius: 16px;
  padding: 0;
}

/* 搜索列样式 */
.table-search-columns {
  padding: 20px 20px 0;
}

/* 表格包装器 */
.table-wrapper {
  padding: 0 20px 20px;
}

/* 工具栏 - 核心修复：按钮左右分布 */
.table-tools-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #F1F5F9;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 -20px;
}

/* 左侧按钮组 */
.tools-fixed-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 批量操作按钮组 */
.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 右侧按钮组 - 靠右对齐 */
.tools-fixed-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-left: auto;
}

/* 表格标题 */
.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
}

/* 操作按钮样式 */
.action-btn {
  border-radius: 10px;
  font-weight: 500;
}

/* 分隔线样式 */
.divider-vertical {
  height: 24px;
  margin: 0 4px;
  background: #E2E8F0;
}

/* 工具栏图标 */
.tools-icon {
  font-size: 18px;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px;
  border-radius: 8px;
}

.tools-icon:hover {
  color: #3B82F6;
  background: #EFF6FF;
}

/* 表格容器 */
.table-main-container {
  margin-top: 16px;
}

/* 机器名称样式 */
.machine-name {
  color: #0F172A;
  font-weight: 500;
}

/* 名称文本样式 */
.name-text {
  color: #1E293B;
  font-weight: 500;
}

/* 路径文本样式 - 优化字体清晰度 */
.path-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0F172A;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Cascadia Code', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  font-weight: 500;
  background: #F8FAFF;
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 0.3px;
  transition: all 0.2s;
}

.path-text:hover {
  color: #3B82F6;
  background: #EFF6FF;
}

/* 命令文本样式 - 优化字体清晰度 */
.command-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0F172A;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Cascadia Code', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  font-weight: 500;
  background: #F1F5F9;
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 0.3px;
  transition: all 0.2s;
}

.command-text:hover {
  color: #3B82F6;
  background: #EFF6FF;
}

/* 表格操作组 */
.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

/* 操作链接样式 */
.action-link {
  color: #3B82F6;
  cursor: pointer;
  font-size: 13px;
}

.action-link:hover {
  color: #2563EB;
  text-decoration: underline;
}

/* 禁用操作链接样式 */
.action-link-disabled {
  color: #94A3B8;
  cursor: not-allowed;
  font-size: 13px;
}

/* 危险操作链接样式 */
.action-danger {
  color: #EF4444;
}

.action-danger:hover {
  color: #DC2626;
  text-decoration: underline;
}

/* 操作分隔线 */
.action-divider {
  height: 12px;
  margin: 0;
}

/* 移除按钮默认边距 */
.ml8 {
  margin-left: 0;
}

.mr8 {
  margin-right: 0;
}

/* 表格单元格内边距优化 */
::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}

/* 表格行悬停效果 */
::v-deep .ant-table-tbody > tr:hover > td {
  background: #F8FAFF;
}

/* 表格操作列按钮样式 */
::v-deep .ant-btn-link {
  padding: 0;
}

/* 删除按钮样式 */
::v-deep .ant-popover-inner {
  border-radius: 12px;
}

::v-deep .ant-popover-message {
  padding: 8px 0;
}
</style>