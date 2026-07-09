<template>
  <div class="scheduler-record-container">
    <!-- 调度任务菜单 -->
    <div class="scheduler-task-menu gray-box-shadow">
      <!-- 任务列表头 -->
      <div class="scheduler-task-header">
        <a-page-header @back="() => {}">
          <template #title>
            <span class="ant-page-header-heading-title pointer" title="刷新" @click="getSchedulerTask">任务列表</span>
          </template>
          <template #backIcon>
            <a-icon type="stop" title="清空" @click="chooseTask(0)"/>
          </template>
        </a-page-header>
      </div>
      <!-- 任务菜单 -->
      <a-spin :spinning="taskLoading">
        <div class="task-list-wrapper">
          <a-menu mode="inline" v-model="selectedTaskIds">
            <a-menu-item v-for="task in taskList" :key="task.id" :title="task.name" @click="chooseTask(task.id)">
              <a-icon type="carry-out"/>
              {{ task.name }}
            </a-menu-item>
          </a-menu>
        </div>
      </a-spin>
    </div>
    <!-- 调度明细表格 -->
    <div class="scheduler-task-record-container">
      <!-- 搜索列 -->
      <div class="search-columns-wrapper">
        <div class="table-search-columns search-columns">
          <a-form-model class="scheduler-search-form" ref="query" :model="query">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-model-item label="任务名称" prop="taskName">
                  <a-input v-model="query.taskName" allowClear/>
                </a-form-model-item>
              </a-col>
              <a-col :span="8">
                <a-form-model-item label="执行状态" prop="status">
                  <a-select v-model="query.status" placeholder="全部" @change="getList({})" allowClear>
                    <a-select-option :value="type.value" v-for="type in SCHEDULER_TASK_STATUS" :key="type.value">
                      {{ type.label }}
                    </a-select-option>
                  </a-select>
                </a-form-model-item>
              </a-col>
            </a-row>
          </a-form-model>
        </div>
      </div>
      <!-- 表格 -->
      <div class="table-wrapper record-table-wrapper">
        <!-- 工具栏 - 修复布局 -->
        <div class="table-tools-bar">
          <!-- 左侧 -->
          <div class="tools-fixed-left">
            <span class="table-title">任务明细</span>
            <a-divider v-show="selectedRowKeys.length" type="vertical"/>
            <div v-show="selectedRowKeys.length" class="batch-actions">
              <a-popconfirm title="是否要删除选中调度记录?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="deleteRecord(selectedRowKeys)">
                <a-button type="danger" icon="delete" size="small">删除</a-button>
              </a-popconfirm>
            </div>
          </div>
          <!-- 右侧 -->
          <div class="tools-fixed-right">
            <a-icon type="delete" class="tools-icon" title="清理" @click="openClear"/>
            <a-icon type="search" class="tools-icon" title="查询" @click="getList({})"/>
            <a-icon type="reload" class="tools-icon" title="重置" @click="resetForm"/>
          </div>
        </div>
        <!-- 表格 -->
        <div class="table-main-container table-scroll-x-auto">
          <a-table :columns="columns"
                   :dataSource="rows"
                   :pagination="pagination"
                   :rowSelection="rowSelection"
                   :expandedRowKeys.sync="expandedRowKeys"
                   rowKey="id"
                   @change="getList"
                   @expand="expandMachine"
                   :loading="loading"
                   size="middle">
            <!-- 展开明细 -->
            <template #expandedRowRender="record">
              <a-table
                v-if="record.machines"
                :rowKey="(record, index) => index"
                :columns="innerColumns"
                :dataSource="record.machines"
                :loading="record.loading"
                :pagination="false"
                size="middle">
                <!-- 状态 -->
                <template #status="machine">
                  <a-tag :color="machine.status | formatMachineStatus('color')">
                    {{ machine.status | formatMachineStatus('label') }}
                  </a-tag>
                </template>
                <!-- 操作 -->
                <template #action="machine">
                  <div class="table-actions">
                    <!-- 日志 -->
                    <a-button class="p0"
                              type="link"
                              style="height: 22px"
                              :disabled="!visibleHolder.visibleMachineLog(machine.status)">
                      <a-tooltip title="ctrl 点击打开新页面">
                        <a target="_blank"
                           :href="`#/task/machine/log/view/${machine.id}`"
                           @click="openMachineLog($event, machine.id)">日志</a>
                      </a-tooltip>
                    </a-button>
                    <a-divider type="vertical" class="action-divider" />
                    <!-- 命令 -->
                    <span class="action-link" @click="previewCommand(machine.command)">命令</span>
                    <a-divider type="vertical" class="action-divider" v-if="visibleHolder.visibleMachineTerminate(machine.status)"/>
                    <!-- 停止 -->
                    <a-popconfirm v-if="visibleHolder.visibleMachineTerminate(machine.status)"
                                  title="是否要停止执行?"
                                  placement="topRight"
                                  ok-text="确定"
                                  cancel-text="取消"
                                  @confirm="terminateMachine(record.id, machine.id)">
                      <span class="action-link action-warning">停止</span>
                    </a-popconfirm>
                    <a-divider type="vertical" class="action-divider" v-if="visibleHolder.visibleMachineSkip(machine.status)"/>
                    <!-- 跳过 -->
                    <a-popconfirm v-if="visibleHolder.visibleMachineSkip(machine.status)"
                                  title="是否要跳过执行?"
                                  placement="topRight"
                                  ok-text="确定"
                                  cancel-text="取消"
                                  @confirm="skipMachine(record.id, machine.id)">
                      <span class="action-link">跳过</span>
                    </a-popconfirm>
                  </div>
                </template>
              </a-table>
            </template>
            <!-- 状态 -->
            <template #status="record">
              <a-tag :color="record.status | formatMachineStatus('color')">
                {{ record.status | formatMachineStatus('label') }}
              </a-tag>
            </template>
            <!-- 开始时间 -->
            <template #startTime="record">
              {{ record.startTime | formatDate }}
            </template>
            <!-- 结束时间 -->
            <template #endTime="record">
              <template v-if="record.endTime">
                {{ record.endTime | formatDate }}
              </template>
            </template>
            <!-- 操作 -->
            <template #action="record">
              <div class="table-actions">
                <!-- 日志 -->
                <a-button class="p0"
                          type="link"
                          style="height: 22px"
                          :disabled="!visibleHolder.visibleRecordLog(record.status)">
                  <a-tooltip title="ctrl 点击打开新页面">
                    <a target="_blank"
                       :href="`#/task/log/view/${record.id}`"
                       @click="openTaskLog($event, record.id)">日志</a>
                  </a-tooltip>
                </a-button>
                <a-divider type="vertical" class="action-divider" v-if="visibleHolder.visibleRecordTerminate(record.status)"/>
                <!-- 停止 -->
                <a-popconfirm v-if="visibleHolder.visibleRecordTerminate(record.status)"
                              title="是否要停止执行?"
                              placement="topRight"
                              ok-text="确定"
                              cancel-text="取消"
                              @confirm="terminate(record.id)">
                  <span class="action-link action-warning">停止</span>
                </a-popconfirm>
                <a-divider type="vertical" class="action-divider" v-if="visibleHolder.visibleRecordDelete(record.status)"/>
                <!-- 删除 -->
                <a-popconfirm v-if="visibleHolder.visibleRecordDelete(record.status)"
                              title="是否要删除当前调度记录?"
                              placement="topRight"
                              ok-text="确定"
                              cancel-text="取消"
                              @confirm="deleteRecord([record.id])">
                  <span class="action-link action-danger">删除</span>
                </a-popconfirm>
              </div>
            </template>
          </a-table>
        </div>
      </div>
    </div>
    <!-- 事件 -->
    <div class="scheduler-event-container">
      <!-- 预览 -->
      <EditorPreview ref="preview"/>
      <!-- 任务日志 -->
      <SchedulerTaskLogAppenderModal ref="taskLoggerView"/>
      <!-- 机器日志 -->
      <SchedulerTaskMachineLogAppenderModal ref="machineLoggerView"/>
      <!-- 批量清理 -->
      <SchedulerRecordClearModal ref="clear" @clear="getList({})"/>
    </div>
  </div>
</template>

<script>
import { defineArrayKey } from '@/lib/utils'
import { formatDate } from '@/lib/filters'
import { enumValueOf, SCHEDULER_TASK_MACHINE_STATUS, SCHEDULER_TASK_STATUS } from '@/lib/enum'
import EditorPreview from '@/components/preview/EditorPreview'
import SchedulerTaskMachineLogAppenderModal from '@/components/log/SchedulerTaskMachineLogAppenderModal'
import SchedulerTaskLogAppenderModal from '@/components/log/SchedulerTaskLogAppenderModal'
import SchedulerRecordClearModal from '@/components/clear/SchedulerRecordClearModal'

/**
 * 列
 */
const columns = [
  {
    title: '任务名称',
    dataIndex: 'taskName',
    key: 'taskName',
    ellipsis: true
  },
  {
    title: '状态',
    key: 'status',
    width: 140,
    align: 'center',
    sorter: (a, b) => a.status - b.status,
    scopedSlots: { customRender: 'status' }
  },
  {
    title: '开始时间',
    key: 'startTime',
    align: 'center',
    ellipsis: true,
    sorter: (a, b) => (a.startTime || 0) - (b.startTime || 0),
    scopedSlots: { customRender: 'startTime' }
  },
  {
    title: '结束时间',
    key: 'endTime',
    align: 'center',
    ellipsis: true,
    sorter: (a, b) => (a.endTime || 0) - (b.endTime || 0),
    scopedSlots: { customRender: 'endTime' }
  },
  {
    title: '持续时间',
    key: 'keepTime',
    dataIndex: 'keepTime',
    ellipsis: true,
    sorter: (a, b) => (a.used || 0) - (b.used || 0)
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

/**
 * 展开列
 */
const innerColumns = [
  {
    title: '机器名称',
    key: 'name',
    dataIndex: 'machineName',
    width: 200,
    ellipsis: true,
    sorter: (a, b) => a.machineName.localeCompare(b.machineName)
  },
  {
    title: '机器主机',
    key: 'host',
    dataIndex: 'machineHost',
    width: 200,
    ellipsis: true,
    sorter: (a, b) => a.machineHost.localeCompare(b.machineHost)
  },
  {
    title: '状态',
    key: 'status',
    width: 140,
    align: 'center',
    sorter: (a, b) => a.status - b.status,
    scopedSlots: { customRender: 'status' }
  },
  {
    title: '持续时间',
    key: 'keepTime',
    dataIndex: 'keepTime',
    width: 120,
    sorter: (a, b) => (a.used || 0) - (b.used || 0)
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

/**
 * 显示判断
 */
const visibleHolder = {
  visibleRecordLog(status) {
    return status !== SCHEDULER_TASK_STATUS.WAIT.value
  },
  visibleRecordTerminate(status) {
    return status === SCHEDULER_TASK_STATUS.RUNNABLE.value
  },
  visibleRecordDelete(status) {
    return status !== SCHEDULER_TASK_STATUS.WAIT.value &&
      status !== SCHEDULER_TASK_STATUS.RUNNABLE.value
  },
  visibleMachineLog(status) {
    return status !== SCHEDULER_TASK_MACHINE_STATUS.WAIT.value &&
      status !== SCHEDULER_TASK_MACHINE_STATUS.SKIPPED.value
  },
  visibleMachineTerminate(status) {
    return status === SCHEDULER_TASK_MACHINE_STATUS.RUNNABLE.value
  },
  visibleMachineSkip(status) {
    return status === SCHEDULER_TASK_MACHINE_STATUS.WAIT.value
  }
}

export default {
  name: 'SchedulerRecord',
  components: {
    SchedulerRecordClearModal,
    SchedulerTaskLogAppenderModal,
    SchedulerTaskMachineLogAppenderModal,
    EditorPreview
  },
  data() {
    return {
      SCHEDULER_TASK_STATUS,
      taskLoading: false,
      taskList: [],
      selectedTaskIds: [0],
      query: {
        taskName: undefined,
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
      pollId: null,
      columns,
      innerColumns,
      visibleHolder,
      selectedRowKeys: [],
      expandedRowKeys: []
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        columnWidth: '40px',
        onChange: e => {
          this.selectedRowKeys = e
        },
        getCheckboxProps: record => ({
          props: {
            disabled: record.status === SCHEDULER_TASK_STATUS.WAIT.value ||
              record.status === SCHEDULER_TASK_STATUS.RUNNABLE.value
          }
        })
      }
    }
  },
  methods: {
    async getSchedulerTask() {
      this.taskLoading = true
      await this.$api.getSchedulerTaskList({
        limit: 10000
      }).then(({ data }) => {
        this.taskLoading = false
        this.taskList = data.rows || []
      }).catch(() => {
        this.taskLoading = false
      })
    },
    chooseTask(id) {
      this.selectedTaskIds = [id]
      this.getList({})
    },
    getList(page = this.pagination) {
      this.loading = true
      this.expandedRowKeys = []
      this.$api.getSchedulerTaskRecordList({
        ...this.query,
        taskId: this.selectedTaskIds[0] || undefined,
        page: page.current,
        limit: page.pageSize
      }).then(({ data }) => {
        const pagination = { ...this.pagination }
        pagination.total = data.total
        pagination.current = data.page
        defineArrayKey(data.rows, 'loading', false)
        defineArrayKey(data.rows, 'machines', [])
        this.rows = data.rows || []
        this.pagination = pagination
        this.selectedRowKeys = []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    expandMachine(expand, record) {
      if (!expand || record.machines.length) {
        return
      }
      record.loading = true
      this.$api.getSchedulerTaskMachinesRecordList({
        recordId: record.id
      }).then(({ data }) => {
        record.loading = false
        record.machines = data
      }).catch(() => {
        record.loading = false
      })
    },
    openTaskLog(e, id) {
      if (!e.ctrlKey) {
        e.preventDefault()
        this.$refs.taskLoggerView.open(id)
        return false
      } else {
        return true
      }
    },
    openMachineLog(e, id) {
      if (!e.ctrlKey) {
        e.preventDefault()
        this.$refs.machineLoggerView.open(id)
        return false
      } else {
        return true
      }
    },
    deleteRecord(idList) {
      this.$api.deleteSchedulerTaskRecord({
        idList
      }).then(() => {
        this.$message.success('已删除')
        this.getList({})
      })
    },
    terminate(id) {
      this.$api.terminateAllSchedulerTaskRecord({
        id
      }).then(() => {
        this.$message.success('已停止')
      })
    },
    terminateMachine(id, machineRecordId) {
      this.$api.terminateMachineSchedulerTaskRecord({
        id,
        machineRecordId
      }).then(() => {
        this.$message.success('已停止')
      })
    },
    skipMachine(id, machineRecordId) {
      this.$api.skipMachineSchedulerTaskRecord({
        id,
        machineRecordId
      }).then(() => {
        this.$message.success('已跳过')
      })
    },
    previewCommand(command) {
      this.$refs.preview.preview(command)
    },
    openClear() {
      this.$refs.clear.open(this.selectedTaskIds[0])
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.query.status = undefined
      this.getList({})
    },
    pollStatus() {
      if (!this.rows || !this.rows.length) {
        return
      }
      const pollItems = this.rows.filter(r => r.status === SCHEDULER_TASK_STATUS.WAIT.value ||
        r.status === SCHEDULER_TASK_STATUS.RUNNABLE.value)
      if (!pollItems.length) {
        return
      }
      const idList = pollItems.map(s => s.id)
      if (!idList.length) {
        return
      }
      const machineRecordIdList = pollItems.map(s => s.machines)
        .filter(s => s && s.length)
        .flat()
        .map(s => s.id)
      this.$api.getSchedulerTaskRecordStatus({
        idList,
        machineRecordIdList
      }).then(({ data }) => {
        if (!data || !data.length) {
          return
        }
        for (const status of data) {
          this.rows.filter(s => s.id === status.id).forEach(row => {
            row.status = status.status
            row.keepTime = status.keepTime
            row.used = status.used
            row.startTime = status.startTime
            row.endTime = status.endTime
            if (!status.machines || !status.machines.length || !row.machines || !row.machines.length) {
              return
            }
            for (const machine of status.machines) {
              row.machines.filter(m => m.id === machine.id).forEach(m => {
                m.status = machine.status
                m.keepTime = machine.keepTime
                m.used = machine.used
              })
            }
          })
        }
        this.$set(this.rows, 0, this.rows[0])
      })
    }
  },
  filters: {
    formatDate,
    formatTaskStatus(status, f) {
      return enumValueOf(SCHEDULER_TASK_STATUS, status)[f]
    },
    formatMachineStatus(status, f) {
      return enumValueOf(SCHEDULER_TASK_MACHINE_STATUS, status)[f]
    }
  },
  async mounted() {
    await this.getSchedulerTask()
    if (this.$route.params.id) {
      this.chooseTask(parseInt(this.$route.params.id))
    } else if (this.taskList.length) {
      this.chooseTask(this.taskList[0].id)
    }
    this.pollId = setInterval(this.pollStatus, 5000)
  },
  beforeDestroy() {
    this.pollId !== null && clearInterval(this.pollId)
    this.pollId = null
  }
}
</script>

<style lang="less" scoped>
.scheduler-record-container {
  display: flex;
  gap: 16px;

  .scheduler-task-menu {
    width: 240px;
    padding: 0 8px 8px 8px;
    background-color: white;
    border-radius: 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  }

  .scheduler-task-record-container {
    flex: 1;
    background-color: white;
    border-radius: 16px;
    min-height: calc(100vh - 84px);
    overflow: hidden;
  }

  .search-columns-wrapper {
    padding: 16px 20px 0;
    background: white;
    
    .search-columns {
      margin-bottom: 0;
    }
  }

  .record-table-wrapper {
    padding: 0 20px 20px;
  }
}

/* 工具栏样式 */
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

/* 表格操作组 */
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

/* 任务菜单样式 */
.task-list-wrapper {
  height: calc(100vh - 180px);
  overflow-y: auto;
  
  ::v-deep .ant-menu {
    background: transparent;
    border-right: none;
  }
  
  ::v-deep .ant-menu-item {
    margin: 4px 0;
    border-radius: 10px;
    height: 40px;
    line-height: 40px;
    
    &:hover {
      background: #F8FAFF;
      color: #3B82F6;
    }
  }
  
  ::v-deep .ant-menu-item-selected {
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(6, 182, 212, 0.08));
    color: #3B82F6;
  }
}

/* 表格单元格内边距 */
::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}

::v-deep .ant-table-expand-icon-th, 
::v-deep .ant-table-row-expand-icon-cell {
  width: 45px;
  min-width: 45px;
}
</style>