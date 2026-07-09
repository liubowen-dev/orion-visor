<template>
  <div class="app-repo-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="app-repo-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="query.name" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="url" prop="url">
              <a-input v-model="query.url" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="资源用户" prop="username">
              <a-input v-model="query.username" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="描述" prop="description">
              <a-input v-model="query.description" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="4">
            <a-form-model-item label="状态" prop="status">
              <a-select v-model="query.status" placeholder="全部" @change="getList({})" allowClear>
                <a-select-option :value="type.value" v-for="type in REPOSITORY_STATUS" :key="type.value">
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
      <!-- 工具栏 - 修复布局 -->
      <div class="table-tools-bar">
        <!-- 左侧 -->
        <div class="tools-fixed-left">
          <span class="table-title">仓库列表</span>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
          <a-button type="primary" icon="plus" @click="add" class="action-btn">新建</a-button>
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
          <!-- url -->
          <template #url="record">
            <span class="span-blue pointer" title="预览" @click="$refs.preview.preview(record.url)">
              {{ record.url }}
            </span>
          </template>
          <!-- 资源用户 -->
          <template #username="record">
            {{ record.authType === REPOSITORY_AUTH_TYPE.PASSWORD.value ? record.username : '私人令牌' }}
          </template>
          <!-- 状态 -->
          <template #status="record">
            <a-tag :color="record.status | formatRepoStatus('color')">
              {{ record.status | formatRepoStatus('label') }}
            </a-tag>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a v-if="REPOSITORY_STATUS.UNINITIALIZED.value === record.status || REPOSITORY_STATUS.ERROR.value === record.status" 
                 @click="init(record)" class="action-link">初始化</a>
              <a-popconfirm title="确定要重新初始化吗?"
                            v-else-if="REPOSITORY_STATUS.OK.value === record.status"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            :disabled="REPOSITORY_STATUS.INITIALIZING.value === record.status"
                            @confirm="reInit(record)">
                <span class="action-link action-warning">重新初始化</span>
              </a-popconfirm>
              <a-divider type="vertical" class="action-divider" v-if="REPOSITORY_STATUS.UNINITIALIZED.value === record.status ||
                    REPOSITORY_STATUS.ERROR.value === record.status ||
                    REPOSITORY_STATUS.OK.value === record.status"/>
              <a-button class="p0 action-link"
                        type="link"
                        :disabled="REPOSITORY_STATUS.INITIALIZING.value === record.status"
                        @click="update(record.id)">
                修改
              </a-button>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确认删除当前仓库? 将会清空所有应用的关联!"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            :disabled="REPOSITORY_STATUS.INITIALIZING.value === record.status"
                            @confirm="remove(record.id)">
                <span class="action-link action-danger" :class="{'disabled': REPOSITORY_STATUS.INITIALIZING.value === record.status}">
                  删除
                </span>
              </a-popconfirm>
            </div>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="app-repo-event">
      <AddRepositoryModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <TextPreview ref="preview"/>
      <AppRepositoryExportModal ref="export"/>
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { enumValueOf, IMPORT_TYPE, REPOSITORY_AUTH_TYPE, REPOSITORY_STATUS } from '@/lib/enum'
import AddRepositoryModal from '@/components/app/AddRepositoryModal'
import TextPreview from '@/components/preview/TextPreview'
import AppRepositoryExportModal from '@/components/export/AppRepositoryExportModal'
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
    key: 'name',
    dataIndex: 'name',
    ellipsis: true,
    width: 160,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: 'url',
    key: 'url',
    width: 300,
    ellipsis: true,
    scopedSlots: { customRender: 'url' }
  },
  {
    title: '资源用户',
    key: 'username',
    ellipsis: true,
    width: 180,
    scopedSlots: { customRender: 'username' }
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    scopedSlots: { customRender: 'status' }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    width: 150,
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 220,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'AppRepository',
  components: {
    DataImportModal,
    AppRepositoryExportModal,
    TextPreview,
    AddRepositoryModal
  },
  data: function() {
    return {
      REPOSITORY_AUTH_TYPE,
      REPOSITORY_STATUS,
      query: {
        id: undefined,
        name: undefined,
        url: undefined,
        username: undefined,
        description: undefined,
        status: undefined
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
      importType: IMPORT_TYPE.APP_REPOSITORY,
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getRepositoryList({
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
    remove(id) {
      this.$api.deleteRepository({ id }).then(() => {
        this.$message.success('删除成功')
        this.getList({})
      })
    },
    init(record) {
      record.status = REPOSITORY_STATUS.INITIALIZING.value
      this.$api.initRepository({ id: record.id }).then(() => {
        this.$message.success('正在初始化')
      })
    },
    reInit(record) {
      record.status = REPOSITORY_STATUS.INITIALIZING.value
      this.$api.reInitRepository({ id: record.id }).then(() => {
        this.$message.success('正在重新初始化')
      })
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    openExport() {
      this.$refs.export.open()
    },
    openImport() {
      this.$refs.import.open()
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.query.id = undefined
      this.getList({})
    }
  },
  filters: {
    formatRepoStatus(status, f) {
      return enumValueOf(REPOSITORY_STATUS, status)[f]
    }
  },
  mounted() {
    this.query.id = this.$route.query.id
    this.getList({})
  }
}
</script>

<style scoped>
.app-repo-container {
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

.action-warning {
  color: #F59E0B;
}

.action-warning:hover {
  color: #D97706;
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

.span-blue {
  color: #3B82F6;
}

.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>