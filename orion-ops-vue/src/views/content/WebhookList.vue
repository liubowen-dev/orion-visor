<template>
  <div class="webhook-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="webhook-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="query.name" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="6">
            <a-form-model-item label="url" prop="url">
              <a-input v-model="query.url" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="6">
            <a-form-model-item label="类型" prop="type">
              <a-select v-model="query.type" placeholder="请选择" @change="getList({})" allowClear>
                <a-select-option v-for="type of WEBHOOK_TYPE" :key="type.value" :value="type.value">
                  {{ type.label }}
                </a-select-option>
              </a-select>
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
          <span class="table-title">webhook 列表</span>
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
                 rowKey="id"
                 @change="getList"
                 :scroll="{x: '100%'}"
                 :loading="loading"
                 size="middle">
          <!-- 类型 -->
          <template #type="record">
            <a-tag :color="record.type | formatType('color')">
              {{ record.type | formatType('label') }}
            </a-tag>
          </template>
          <!-- url -->
          <template #url="record">
            <div class="webhook-url-wrapper">
              <a class="copy-icon" @click="$copy(record.url)" title="复制">
                <a-icon type="copy"/>
              </a>
              <div class="webhook-url" :title="record.url">
                {{ record.url }}
              </div>
            </div>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a @click="update(record.id)" class="action-link">修改</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确认删除当前行?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="remove(record.id)">
                <span class="action-link action-danger">删除</span>
              </a-popconfirm>
            </div>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="webhook-event-container">
      <AddWebhookModal ref="addModal" :mask="true" @added="getList({})" @updated="getList({})"/>
      <WebhookExportModal ref="export"/>
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { enumValueOf, IMPORT_TYPE, WEBHOOK_TYPE } from '@/lib/enum'
import AddWebhookModal from '@/components/content/AddWebhookModal'
import WebhookExportModal from '@/components/export/WebhookExportModal'
import DataImportModal from '@/components/import/DataImportModal'

const columns = [
  {
    title: '序号',
    key: 'seq',
    width: 60,
    align: 'center',
    customRender: (text, record, index) => `${index + 1}`
  },
  {
    title: '名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
    ellipsis: true
  },
  {
    title: '类型',
    key: 'type',
    width: 120,
    scopedSlots: { customRender: 'type' }
  },
  {
    title: 'webhook url',
    key: 'url',
    ellipsis: true,
    scopedSlots: { customRender: 'url' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 140,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'WebhookList',
  components: {
    DataImportModal,
    WebhookExportModal,
    AddWebhookModal
  },
  data() {
    return {
      WEBHOOK_TYPE,
      query: {
        name: undefined,
        url: undefined,
        type: undefined
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
      importType: IMPORT_TYPE.WEBHOOK,
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getWebhookConfigList({
        ...this.query,
        page: page.current,
        limit: page.pageSize
      }).then(({ data }) => {
        const pagination = { ...this.pagination }
        pagination.total = data.total
        pagination.current = data.page
        this.rows = data.rows || []
        this.pagination = pagination
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(id) {
      this.$api.deleteWebhookConfig({ id }).then(() => {
        this.$message.success('删除成功')
        this.getList({})
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
    }
  },
  filters: {
    formatType(status, f) {
      return enumValueOf(WEBHOOK_TYPE, status)[f]
    }
  },
  mounted() {
    this.getList({})
  }
}
</script>

<style scoped>
.webhook-container {
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

/* webhook URL 样式 - 优化字体清晰度 */
.webhook-url-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  
  .copy-icon {
    color: #94A3B8;
    transition: color 0.2s;
    flex-shrink: 0;
    margin-top: 2px;
    
    &:hover {
      color: #3B82F6;
    }
  }
  
  .webhook-url {
    flex: 1;
    font-family: 'SF Mono', 'Monaco', 'Menlo', 'Cascadia Code', 'Consolas', monospace;
    font-size: 12px;
    line-height: 1.5;
    color: #1E293B;
    background: #F8FAFF;
    padding: 4px 8px;
    border-radius: 8px;
    word-break: break-all;
    white-space: normal;
    transition: all 0.2s;
    border: 1px solid transparent;
  }
  
  .webhook-url:hover {
    background: #EFF6FF;
    border-color: #E2E8F0;
  }
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