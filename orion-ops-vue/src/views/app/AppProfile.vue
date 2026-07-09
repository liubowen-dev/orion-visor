<template>
  <div class="app-profile-container">
    <!-- 筛选列 -->
    <div class="table-search-columns">
      <a-form-model class="app-profile-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="环境名称" prop="name">
              <ProfileAutoComplete ref="profileSelector" @change="selectedProfile" @choose="getList({})"/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="标识" prop="tag">
              <a-input v-model="query.tag" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="描述" prop="description">
              <a-input v-model="query.description" allowClear/>
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
          <span class="table-title">环境列表</span>
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
          <!-- tag -->
          <template #tag="record">
            <span class="span-blue">{{ record.tag }}</span>
          </template>
          <!-- 审核 -->
          <template #releaseAudit="record">
            <span class="span-blue">{{ record.releaseAudit | formatReleaseAudit('label') }}</span>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a @click="update(record.id)" class="action-link">修改</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确认删除当前环境?"
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
    <div class="profile-list-event">
      <AddAppProfileModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <AppProfileExportModal ref="export"/>
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { enumValueOf, IMPORT_TYPE, PROFILE_AUDIT_STATUS } from '@/lib/enum'
import AddAppProfileModal from '@/components/app/AddAppProfileModal'
import AppProfileExportModal from '@/components/export/AppProfileExportModal'
import DataImportModal from '@/components/import/DataImportModal'
import ProfileAutoComplete from '@/components/app/ProfileAutoComplete'

const columns = [
  {
    title: '序号',
    key: 'seq',
    width: 65,
    align: 'center',
    customRender: (text, record, index) => `${index + 1}`
  },
  {
    title: '环境名称',
    key: 'name',
    dataIndex: 'name',
    width: 180,
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: '唯一标识',
    key: 'tag',
    ellipsis: true,
    width: 150,
    sorter: (a, b) => a.tag.localeCompare(b.tag),
    scopedSlots: { customRender: 'tag' }
  },
  {
    title: '发布审核',
    key: 'releaseAudit',
    ellipsis: true,
    align: 'center',
    width: 110,
    scopedSlots: { customRender: 'releaseAudit' }
  },
  {
    title: '描述',
    key: 'description',
    dataIndex: 'description',
    ellipsis: true,
    width: 220
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
  name: 'AppProfile',
  components: {
    ProfileAutoComplete,
    DataImportModal,
    AppProfileExportModal,
    AddAppProfileModal
  },
  data() {
    return {
      query: {
        id: undefined,
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
      importType: IMPORT_TYPE.APP_PROFILE,
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getProfileList({
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
      this.$emit('reloadProfile')
    },
    selectedProfile(id, name) {
      if (id) {
        this.query.id = id
        this.query.name = undefined
      } else {
        this.query.id = undefined
        this.query.name = name
      }
      if (id === undefined && name === undefined) {
        this.getList({})
      }
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(id) {
      this.$api.deleteProfile({ id })
      .then(() => {
        this.$message.success('已删除')
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
      this.$refs.profileSelector.reset()
      this.query.id = undefined
      this.query.name = undefined
      this.getList({})
    }
  },
  filters: {
    formatReleaseAudit(status, f) {
      return enumValueOf(PROFILE_AUDIT_STATUS, status)[f]
    }
  },
  mounted() {
    this.getList({})
  }
}
</script>

<style scoped>
.app-profile-container {
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

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>