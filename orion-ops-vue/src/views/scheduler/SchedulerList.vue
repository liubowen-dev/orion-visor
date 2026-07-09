<template>
  <div class="scheduler-list-container">
    <!-- 表格 -->
    <div class="scheduler-table-container">
      <!-- 搜索列 -->
      <div class="table-search-columns">
        <a-form-model class="scheduler-search-form" ref="query" :model="query">
          <a-row :gutter="16">
            <a-col :span="5">
              <a-form-model-item label="任务名称" prop="username">
                <a-input v-model="query.name" allowClear/>
              </a-form-model-item>
            </a-col>
            <a-col :span="5">
              <a-form-model-item label="任务描述" prop="description">
                <a-input v-model="query.description" allowClear/>
              </a-form-model-item>
            </a-col>
            <a-col :span="5">
              <a-form-model-item label="最近状态" prop="type">
                <a-select v-model="query.latelyStatus" placeholder="全部" @change="getList({})" allowClear>
                  <a-select-option :value="type.value" v-for="type in SCHEDULER_TASK_STATUS" :key="type.value">
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
            <span class="table-title">任务列表</span>
          </div>
          <!-- 右侧 -->
          <div class="tools-fixed-right">
            <!-- 状态单选 -->
            <div class="status-radio-group">
              <a-radio-group v-model="query.enableStatus" @change="getList({})" size="small">
                <a-radio-button :value="undefined">全部</a-radio-button>
                <a-radio-button :value="type.value" v-for="type in ENABLE_STATUS" :key="type.value">
                  {{ type.label }}
                </a-radio-button>
              </a-radio-group>
            </div>
            <a-divider type="vertical" class="divider-vertical" />
            <a-button type="primary" icon="plus" @click="add" class="action-btn">新增</a-button>
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
            <!-- 最近状态 -->
            <template #latelyStatus="record">
              <template v-if="record.enableStatus === 1">
                <a-tag :color="record.latelyStatus | formatTaskStatus('color')">
                  {{ record.latelyStatus | formatTaskStatus('label') }}
                </a-tag>
              </template>
              <template v-else>
                <a-tag>停用</a-tag>
              </template>
            </template>
            <!-- 上次调度时间 -->
            <template #latelyScheduleTime="record">
              <template v-if="record.latelyScheduleTime">
                {{ record.latelyScheduleTime | formatDate }}
              </template>
            </template>
            <!-- 下次调度时间 -->
            <template #nextTime="record">
              <template v-if="record.nextTime && record.nextTime.length">
                {{ record.nextTime[0] | formatDate }}
              </template>
            </template>
            <!-- 更新时间 -->
            <template #updateTime="record">
              {{ record.updateTime | formatDate }}
            </template>
            <!-- 操作 -->
            <template #action="record">
              <div class="table-actions">
                <a @click="update(record.id)" class="action-link">修改</a>
                <a-divider type="vertical" class="action-divider" />
                <a-popconfirm :title="`是否要${record.enableStatus === 1 ? '停用' : '启用'}此任务?`"
                              placement="topRight"
                              ok-text="确定"
                              cancel-text="取消"
                              @confirm="updateStatus(record)">
                  <span class="action-link" :class="record.enableStatus === 1 ? 'action-warning' : 'action-success'">
                    {{ record.enableStatus === 1 ? '停用' : '启用' }}
                  </span>
                </a-popconfirm>
                <a-divider type="vertical" class="action-divider" />
                <a-dropdown>
                  <a class="action-link">
                    更多
                    <a-icon type="down" />
                  </a>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="remove(record.id)" class="danger-item">删除</a-menu-item>
                      <a-menu-item :disabled="record.enableStatus !== 1" @click="manualTrigger(record.id)">
                        手动触发
                      </a-menu-item>
                      <a-menu-item @click="historyRecord(record.id)">历史记录</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
            </template>
          </a-table>
        </div>
      </div>
    </div>
    <!-- 事件 -->
    <div class="scheduler-event-container">
      <AddSchedulerTask ref="addModal" @added="getList()" @updated="getList()"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { enumValueOf, ENABLE_STATUS, SCHEDULER_TASK_STATUS } from '@/lib/enum'
import AddSchedulerTask from '@/components/scheduler/AddSchedulerTask'

/**
 * 列
 */
const columns = [
  {
    title: '序号',
    key: 'seq',
    width: 65,
    align: 'center',
    customRender: (text, record, index) => `${index + 1}`
  },
  {
    title: '任务名称',
    dataIndex: 'name',
    key: 'name',
    ellipsis: true
  },
  {
    title: 'cron表达式',
    dataIndex: 'expression',
    key: 'expression',
    ellipsis: true
  },
  {
    title: '最近状态',
    key: 'latelyStatus',
    width: 120,
    align: 'center',
    sorter: (a, b) => a.latelyStatus - b.latelyStatus,
    scopedSlots: { customRender: 'latelyStatus' }
  },
  {
    title: '上次调度时间',
    key: 'latelyScheduleTime',
    align: 'center',
    ellipsis: true,
    sorter: (a, b) => (a.latelyScheduleTime || 0) - (b.latelyScheduleTime || 0),
    scopedSlots: { customRender: 'latelyScheduleTime' }
  },
  {
    title: '下次调度时间',
    key: 'nextTime',
    align: 'center',
    ellipsis: true,
    sorter: (a, b) => ((a.nextTime && a.nextTime.length && a.nextTime[0]) || 0) -
      ((b.nextTime && b.nextTime.length && b.nextTime[0]) || 0),
    scopedSlots: { customRender: 'nextTime' }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 180,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'ScheduleList',
  components: { AddSchedulerTask },
  data: function() {
    return {
      SCHEDULER_TASK_STATUS,
      ENABLE_STATUS,
      query: {
        name: undefined,
        description: undefined,
        enableStatus: undefined,
        latelyStatus: undefined
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
      this.$api.getSchedulerTaskList({
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
      this.$confirm({
        title: '确认删除',
        content: '是否删除当前调度任务?',
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => {
          this.$api.deleteSchedulerTask({
            id
          }).then(() => {
            this.$message.success('已删除')
            this.getList({})
          })
        }
      })
    },
    historyRecord(id) {
      this.$router.push({
        path: `/scheduler/record/${id}`
      })
    },
    manualTrigger(id) {
      this.$confirm({
        content: '确定要手动触发此任务吗?',
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => {
          this.$api.manualTriggerSchedulerTask({
            id
          }).then(() => {
            this.$message.success('已触发')
          })
        }
      })
    },
    updateStatus(record) {
      this.$api.updateSchedulerTaskStatus({
        id: record.id,
        enableStatus: record.enableStatus === 1 ? 2 : 1
      }).then(() => {
        this.getList()
      })
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.query.enableStatus = undefined
      this.query.latelyStatus = undefined
      this.getList({})
    }
  },
  filters: {
    formatDate,
    formatTaskStatus(status, f) {
      return enumValueOf(SCHEDULER_TASK_STATUS, status)[f]
    }
  },
  mounted() {
    this.getList({})
  }
}
</script>

<style lang="less" scoped>
.scheduler-list-container {
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

/* 状态单选组样式 */
.status-radio-group {
  ::v-deep .ant-radio-button-wrapper {
    padding: 0 12px;
    height: 32px;
    line-height: 30px;
    font-size: 13px;
    border-color: #E2E8F0;
    color: #475569;
    
    &:hover {
      color: #3B82F6;
    }
  }
  
  ::v-deep .ant-radio-button-wrapper-checked {
    background: #3B82F6;
    border-color: #3B82F6;
    color: white;
    
    &:hover {
      color: white;
    }
  }
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

/* 成功操作样式（启用） */
.action-success {
  color: #10B981;
}

.action-success:hover {
  color: #059669;
}

/* 警告操作样式（停用） */
.action-warning {
  color: #F59E0B;
}

.action-warning:hover {
  color: #D97706;
}

/* 操作分隔线 */
.action-divider {
  height: 12px;
  margin: 0;
}

/* 危险菜单项 */
::v-deep .danger-item {
  color: #EF4444;
  
  &:hover {
    background: #FEF2F2;
    color: #DC2626;
  }
}

/* 表格单元格内边距优化 */
::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>