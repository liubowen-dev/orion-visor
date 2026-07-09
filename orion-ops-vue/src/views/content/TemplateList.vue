<template>
  <div class="command-template-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="command-template-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="模板名称" prop="name">
              <a-input v-model="query.name" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="模板内容" prop="value">
              <a-input v-model="query.value" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="模板描述" prop="description">
              <a-input v-model="query.description" allowClear/>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </div>
    <!-- 表格 -->
    <div class="table-wrapper">
      <!-- 工具栏 -->
      <div class="table-tools-bar">
        <!-- 左侧 -->
        <div class="tools-fixed-left">
          <span class="table-title">模板列表</span>
          <a-divider v-show="selectedRowKeys.length" type="vertical"/>
          <div v-show="selectedRowKeys.length" class="batch-actions">
            <a-popconfirm placement="topRight"
                          title="是否删除选中模板?"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="remove(selectedRowKeys)">
              <a-button type="danger" icon="delete" size="small">删除</a-button>
            </a-popconfirm>
          </div>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
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
          <!-- 模板内容 -->
          <template #value="record">
            <div class="template-value-wrapper">
              <span class="pointer template-value" title="预览" @click="preview(record.value)">
                {{ record.value }}
              </span>
            </div>
          </template>
          <!-- 修改时间 -->
          <template #updateTime="record">
            {{ record.updateTime | formatDate }}
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a @click="update(record.id)" class="action-link">修改</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确认删除当前模板?"
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
    <div class="command-template-event">
      <AddTemplateModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <EditorPreview ref="preview"/>
      <CommandTemplateExportModal ref="export"/>
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { IMPORT_TYPE } from '@/lib/enum'
import AddTemplateModal from '@/components/content/AddTemplateModal'
import EditorPreview from '@/components/preview/EditorPreview'
import CommandTemplateExportModal from '@/components/export/CommandTemplateExportModal'
import DataImportModal from '@/components/import/DataImportModal'

const columns = [
  {
    title: '模板名称',
    dataIndex: 'name',
    key: 'name',
    width: 120,
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: '模板内容',
    key: 'value',
    width: 450,
    ellipsis: true,
    scopedSlots: { customRender: 'value' },
    sorter: (a, b) => a.value.localeCompare(b.value)
  },
  {
    title: '模板描述',
    dataIndex: 'description',
    key: 'description',
    width: 140,
    ellipsis: true
  },
  {
    title: '创建人',
    dataIndex: 'createUserName',
    key: 'createUserName',
    width: 100,
    ellipsis: true,
    sorter: (a, b) => a.createUserName.localeCompare(b.createUserName)
  },
  {
    title: '修改人',
    dataIndex: 'updateUserName',
    key: 'updateUserName',
    width: 100,
    ellipsis: true,
    sorter: (a, b) => a.updateUserName.localeCompare(b.updateUserName)
  },
  {
    title: '修改时间',
    key: 'updateTime',
    width: 150,
    align: 'center',
    sorter: (a, b) => a.updateTime - b.updateTime,
    scopedSlots: { customRender: 'updateTime' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 120,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'TemplateList',
  components: {
    DataImportModal,
    CommandTemplateExportModal,
    AddTemplateModal,
    EditorPreview
  },
  data() {
    return {
      query: {
        name: undefined,
        value: undefined,
        description: undefined
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
      loading: false,
      selectedRowKeys: [],
      importType: IMPORT_TYPE.COMMAND_TEMPLATE,
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getTemplateList({
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
    openExport() {
      this.$refs.export.open()
    },
    openImport() {
      this.$refs.import.open()
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.getList({})
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(idList) {
      this.$api.deleteTemplate({ idList }).then(() => {
        this.$message.success('删除成功')
        this.getList({})
      })
    },
    preview(value) {
      this.$refs.preview.preview(value)
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

<style scoped>
.command-template-container {
  background: white;
  border-radius: 16px;
  padding: 0;
}

.table-search-columns {
  padding: 20px 20px 0;
}

.table-wrapper {
  padding: 0 20px 20px;
}

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

.tools-fixed-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tools-fixed-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-left: auto;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
}

.action-btn {
  border-radius: 10px;
  font-weight: 500;
}

.divider-vertical {
  height: 24px;
  margin: 0 4px;
  background: #E2E8F0;
}

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

.table-main-container {
  margin-top: 16px;
}

/* 模板内容样式 - 优化字体清晰度 */
.template-value-wrapper {
  max-width: 100%;
}

.template-value {
  display: block;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #1E293B;
  background: #F8FAFF;
  padding: 4px 8px;
  border-radius: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.template-value:hover {
  color: #3B82F6;
  background: #EFF6FF;
  border-color: #E2E8F0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.action-link {
  color: #3B82F6;
  cursor: pointer;
  font-size: 13px;
}

.action-link:hover {
  color: #2563EB;
  text-decoration: underline;
}

.action-danger {
  color: #EF4444;
}

.action-danger:hover {
  color: #DC2626;
}

.action-divider {
  height: 12px;
  margin: 0;
}

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>