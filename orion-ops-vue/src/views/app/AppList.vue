<template>
  <div class="app-info-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="app-info-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="名称" prop="name">
              <AppAutoComplete ref="appSelector" @change="selectedApp" @choose="getList({})"/>
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
          <span class="table-title">应用列表</span>
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
      <div class="table-main-container">
        <a-table :columns="columns"
                 :dataSource="rows"
                 :pagination="pagination"
                 rowKey="id"
                 @change="getList"
                 @expand="expandMachine"
                 :loading="loading"
                 :expandedRowKeys.sync="expandedRowKeys"
                 ref="table"
                 size="middle">
          <!-- 展开的机器列表 -->
          <template #expandedRowRender="record">
            <a-table
              v-if="record.machines"
              :rowKey="(record, index) => index"
              :columns="innerColumns"
              :dataSource="record.machines"
              :loading="record.loading"
              :pagination="false"
              size="middle">
              <!-- 构建版本 -->
              <template #buildSeq="machine">
                <span class="span-blue" v-if="machine.buildSeq">#{{ machine.buildSeq }}</span>
              </template>
              <!-- 操作 -->
              <template #action="machine">
                <a @click="removeAppMachine(record, machine.id)">删除</a>
              </template>
            </a-table>
          </template>
          <!-- sort -->
          <template #sort="record">
            <div class="sort-column">
              <a @click="adjustSort(record.id, 1)" title="上调排序">
                <a-icon type="up-square"/>
              </a>
              <a-divider type="vertical"/>
              <a @click="adjustSort(record.id, 2)" title="下调排序">
                <a-icon type="down-square"/>
              </a>
            </div>
          </template>
          <!-- tag -->
          <template #tag="record">
            <span class="span-blue">{{ record.tag }}</span>
          </template>
          <!-- 配置 -->
          <template #config="record">
            <a-tag :color=" record.isConfig | formatConfigStatus('color')">
              {{ record.isConfig | formatConfigStatus('label') }}
            </a-tag>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions">
              <a :href="`#/app/conf/${record.id}`" class="action-link">配置</a>
              <a-divider type="vertical" class="action-divider" />
              <a-button class="p0 action-link"
                        type="link"
                        :disabled="record.isConfig !== CONFIG_STATUS.CONFIGURED.value"
                        @click="buildApp(record.id)">
                构建
              </a-button>
              <a-divider type="vertical" class="action-divider" />
              <a-button class="p0 action-link"
                        type="link"
                        :disabled="record.isConfig !== CONFIG_STATUS.CONFIGURED.value"
                        @click="releaseApp(record.id)">
                发布
              </a-button>
              <a-divider type="vertical" class="action-divider" />
              <AppProfileChecker v-if="record.isConfig === CONFIG_STATUS.CONFIGURED.value" :ref="'profileChecker' + record.id">
                <template #trigger>
                  <span class="action-link">同步</span>
                </template>
                <template #footer>
                  <a-button type="primary" size="small" @click="sync(record.id)">确定</a-button>
                </template>
              </AppProfileChecker>
              <span v-else class="action-link-disabled">同步</span>
              <a-divider type="vertical" class="action-divider" />
              <a-dropdown>
                <a class="action-link">
                  更多
                  <a-icon type="down" />
                </a>
                <template #overlay>
                  <a-menu @click="menuHandler($event, record)">
                    <a-menu-item key="update">修改</a-menu-item>
                    <a-menu-item key="remove" class="danger-item">删除</a-menu-item>
                    <a-menu-item key="copy">复制</a-menu-item>
                    <a-menu-item key="openEnv">
                      <a :href="`#/app/env/${record.id}`">环境变量</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="app-info-event">
      <AddAppModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <AppBuildModal ref="buildModal" :visibleReselect="false"/>
      <AppReleaseModal ref="releaseModal" :visibleReselect="false"/>
      <ApplicationExportModal ref="export"/>
      <DataImportModal ref="import" :importType="importType"/>
    </div>
  </div>
</template>

<script>
import { defineArrayKey } from '@/lib/utils'
import { enumValueOf, CONFIG_STATUS, IMPORT_TYPE } from '@/lib/enum'
import AddAppModal from '@/components/app/AddAppModal'
import AppProfileChecker from '@/components/app/AppProfileChecker'
import AppBuildModal from '@/components/app/AppBuildModal'
import AppReleaseModal from '@/components/app/AppReleaseModal'
import ApplicationExportModal from '@/components/export/ApplicationExportModal'
import DataImportModal from '@/components/import/DataImportModal'
import AppAutoComplete from '@/components/app/AppAutoComplete'

const columns = [
  {
    title: '排序',
    key: 'sort',
    width: 70,
    align: 'center',
    scopedSlots: { customRender: 'sort' }
  },
  {
    title: '名称',
    key: 'name',
    dataIndex: 'name',
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: '唯一标识',
    key: 'tag',
    scopedSlots: { customRender: 'tag' },
    sorter: (a, b) => a.tag.localeCompare(b.tag)
  },
  {
    title: '配置状态',
    key: 'config',
    align: 'center',
    sorter: (a, b) => a.isConfig - b.isConfig,
    scopedSlots: { customRender: 'config' }
  },
  {
    title: '仓库名称',
    key: 'repoName',
    dataIndex: 'repoName',
    ellipsis: true
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
    align: 'center',
    width: 300,
    scopedSlots: { customRender: 'action' }
  }
]

const innerColumns = [
  {
    title: '机器名称',
    key: 'name',
    dataIndex: 'machineName',
    width: 230,
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: '机器主机',
    key: 'host',
    dataIndex: 'machineHost',
    width: 210,
    ellipsis: true,
    sorter: (a, b) => a.host.localeCompare(b.host)
  },
  {
    title: '当前构建版本',
    key: 'buildSeq',
    width: 160,
    align: 'center',
    sorter: (a, b) => (a.buildSeq || 0) - (b.buildSeq || 0),
    scopedSlots: { customRender: 'buildSeq' }
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

const moreMenuHandler = {
  update(record) {
    this.$refs.addModal.update(record.id)
  },
  remove(record) {
    this.$confirm({
      title: '确认删除',
      content: '是否删除当前应用? 将会删除应用相关联的所有数据!',
      okText: '确认',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        this.$api.deleteApp({ id: record.id }).then(() => {
          this.$message.success('删除成功')
          this.getList({})
        })
      }
    })
  },
  copy(record) {
    this.$confirm({
      title: '确认复制',
      content: `是否复制应用 ${record.name}?`,
      okText: '复制',
      cancelText: '取消',
      onOk: () => {
        this.$api.copyApp({ id: record.id }).then(() => {
          this.$message.success('复制成功')
          this.getList()
        })
      }
    })
  }
}

export default {
  name: 'AppList',
  components: {
    AppAutoComplete,
    DataImportModal,
    ApplicationExportModal,
    AppReleaseModal,
    AppBuildModal,
    AddAppModal,
    AppProfileChecker
  },
  data: function() {
    return {
      CONFIG_STATUS,
      query: {
        id: undefined,
        profileId: undefined,
        name: undefined,
        tag: undefined,
        username: undefined,
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
      expandedRowKeys: [],
      importType: IMPORT_TYPE.APPLICATION,
      columns,
      innerColumns
    }
  },
  methods: {
    chooseProfile(profile) {
      this.query.profileId = profile.id
      this.getList({})
    },
    getList(page = this.pagination) {
      this.loading = true
      this.expandedRowKeys = []
      this.$api.getAppList({
        ...this.query,
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
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    selectedApp(id, name) {
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
    expandMachine(expand, record) {
      if (!expand || record.machines.length) return
      record.loading = true
      this.$api.getAppMachineList({
        id: record.id,
        profileId: this.query.profileId
      }).then(({ data }) => {
        record.loading = false
        record.machines = data
      }).catch(() => {
        record.loading = false
      })
    },
    adjustSort(id, sortAdjust) {
      this.$api.adjustAppSort({ id, sortAdjust }).then(() => this.getList())
    },
    removeAppMachine(record, machineId) {
      this.$api.deleteAppMachine({ id: machineId }).then(() => {
        this.$message.success('已删除')
        for (let i = 0; i < record.machines.length; i++) {
          if (record.machines[i].id === machineId) {
            record.machines.splice(i, 1)
          }
        }
      })
    },
    add() {
      this.$refs.addModal.add()
    },
    buildApp(id) {
      this.$refs.buildModal.openBuild(this.query.profileId, id)
    },
    releaseApp(id) {
      this.$refs.releaseModal.openRelease(this.query.profileId, id)
    },
    sync(id) {
      const ref = this.$refs['profileChecker' + id]
      if (!ref.checkedList.length) {
        this.$message.warning('请先选择同步的环境')
        return
      }
      const targetProfileIdList = ref.checkedList
      ref.clear()
      ref.hide()
      this.$api.syncApp({
        appId: id,
        profileId: this.query.profileId,
        targetProfileIdList
      }).then(() => {
        this.$message.success('应用已同步成功')
      })
    },
    menuHandler({ key }, record) {
      const handler = moreMenuHandler[key]
      handler && handler.call(this, record)
    },
    openExport() {
      this.$refs.export.open()
    },
    openImport() {
      this.$refs.import.open()
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.$refs.appSelector.reset()
      this.query.id = undefined
      this.query.name = undefined
      this.getList({})
    }
  },
  filters: {
    formatConfigStatus(status, f) {
      return enumValueOf(CONFIG_STATUS, status)[f]
    }
  },
  mounted() {
    const activeProfile = this.$storage.get(this.$storage.keys.ACTIVE_PROFILE)
    if (activeProfile) {
      this.query.profileId = JSON.parse(activeProfile).id
    }
    this.getList({})
  }
}
</script>

<style scoped>
.app-info-container {
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

.sort-column {
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
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

.action-link-disabled {
  color: #94A3B8;
  cursor: not-allowed;
  font-size: 13px;
}

.action-divider {
  height: 12px;
  margin: 0;
}

.span-blue {
  color: #3B82F6;
}

::v-deep .danger-item {
  color: #EF4444;
}

::v-deep .danger-item:hover {
  background: #FEF2F2;
  color: #DC2626;
}

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>