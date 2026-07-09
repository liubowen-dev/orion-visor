<template>
  <div class="alarm-group-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="alarm-group-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="query.name" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="6">
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
          <span class="table-title">报警组列表</span>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
          <a-button type="primary" icon="plus" @click="add" class="action-btn">添加</a-button>
          <a-divider type="vertical" class="divider-vertical" />
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
          <!-- 组员 -->
          <template #groupUsers="record">
            <div class="alarm-group-user-wrapper">
              <a-tooltip v-for="(groupUser, index) of record.groupUsers"
                         placement="top"
                         :key="groupUser.id"
                         :title="groupUser.username">
                <span class="span-blue pointer" @click="$copy(groupUser.username, true)">
                  {{ groupUser.nickname }}
                  <template v-if="index !== record.groupUsers.length - 1">,</template>
                </span>
              </a-tooltip>
            </div>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a @click="update(record.id)" class="action-link">修改</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确认删除当前报警组?"
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
    <!-- 新建模态框 -->
    <AddAlarmGroup ref="addModal" :mask="true" @added="getList({})" @updated="getList({})"/>
  </div>
</template>

<script>
import AddAlarmGroup from '@/components/user/AddAlarmGroup'

const columns = [
  {
    title: '序号',
    key: 'seq',
    width: 60,
    align: 'center',
    customRender: (text, record, index) => `${index + 1}`
  },
  {
    title: '报警组名称',
    dataIndex: 'name',
    key: 'name',
    width: 240,
    ellipsis: true
  },
  {
    title: '报警组组员',
    key: 'groupUsers',
    ellipsis: true,
    scopedSlots: { customRender: 'groupUsers' }
  },
  {
    title: '报警组描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
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
  name: 'AlarmGroupList',
  components: {
    AddAlarmGroup
  },
  data() {
    return {
      query: {
        name: undefined,
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
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getAlarmGroupList({
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
    remove(id) {
      this.$api.deleteAlarmGroup({ id }).then(() => {
        this.$message.success('删除成功')
        this.getList({})
      })
    }
  },
  mounted() {
    this.getList({})
  }
}
</script>

<style scoped>
.alarm-group-container {
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

.alarm-group-user-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.span-blue {
  color: #3B82F6;
  cursor: pointer;
}

.span-blue:hover {
  color: #2563EB;
  text-decoration: underline;
}

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>