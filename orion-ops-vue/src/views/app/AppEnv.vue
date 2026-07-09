<template>
  <div class="app-env-container">
    <!-- 应用容器 -->
    <div class="app-container gray-box-shadow">
      <div class="app-header">
        <a-page-header @back="() => {}">
          <template #title>
            <span class="ant-page-header-heading-title pointer" title="刷新" @click="getAppList">应用列表</span>
          </template>
          <template #backIcon>
            <a-icon type="appstore" title="刷新" @click="getAppList"/>
          </template>
        </a-page-header>
      </div>
      <a-spin :spinning="appLoading">
        <div class="app-list-wrapper">
          <a-menu mode="inline" :defaultSelectedKeys="defaultSelectedAppIds">
            <a-menu-item v-for="app in appList" :key="app.id" :title="app.host" @click="chooseApp(app.id)">
              <a-icon type="code-sandbox"/>
              {{ app.name }}
            </a-menu-item>
          </a-menu>
        </div>
      </a-spin>
    </div>
    <!-- 环境变量容器 -->
    <div class="env-container">
      <div class="search-columns-wrapper">
        <div class="table-search-columns">
          <a-form-model class="env-search-form" ref="query" :model="query">
            <a-row :gutter="16">
              <a-col :span="6">
                <a-form-model-item label="key" prop="key">
                  <a-input v-model="query.key" allowClear/>
                </a-form-model-item>
              </a-col>
              <a-col :span="6">
                <a-form-model-item label="value" prop="value">
                  <a-input v-model="query.value" allowClear/>
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
      </div>
      <!-- 表格 -->
      <div class="table-wrapper">
        <!-- 工具栏 - 修复布局 -->
        <div class="table-tools-bar">
          <!-- 左侧 -->
          <div class="tools-fixed-left">
            <span class="table-title">环境变量</span>
            <a-divider type="vertical" />
            <!-- 视图 -->
            <div class="view-radio-group">
              <a-radio-group v-model="viewType" buttonStyle="solid" size="small">
                <a-radio-button v-for="view in VIEW_TYPE"
                                :key="view.value"
                                :value="view.value"
                                @click="changeView(view)">
                  {{ view.name }}
                </a-radio-button>
              </a-radio-group>
            </div>
            <a-divider v-show="selectedRowKeys.length" type="vertical" />
            <!-- 删除 -->
            <a-popconfirm v-show="selectedRowKeys.length && viewType === VIEW_TYPE.TABLE.value"
                          placement="topRight"
                          title="是否删除选中环境变量?"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="remove(selectedRowKeys)">
              <a-button type="danger" icon="delete" size="small">删除</a-button>
            </a-popconfirm>
          </div>
          <!-- 右侧 -->
          <div class="tools-fixed-right">
            <a-button v-if="viewType !== VIEW_TYPE.TABLE.value"
                      type="primary"
                      icon="check"
                      :disabled="loading"
                      @click="save"
                      class="action-btn">保存</a-button>
            <a-divider v-if="viewType !== VIEW_TYPE.TABLE.value" type="vertical" class="divider-vertical" />
            <AppProfileChecker ref="profileChecker-1" v-if="query.profileId">
              <template #trigger>
                <a-button type="primary" icon="sync" class="action-btn">同步</a-button>
              </template>
              <template #footer>
                <a-button type="primary" size="small" @click="syncEnv(-1)">确定</a-button>
              </template>
            </AppProfileChecker>
            <a-button v-if="query.profileId" type="primary" icon="plus" @click="add" class="action-btn">添加</a-button>
            <a-divider type="vertical" class="divider-vertical" />
            <a-icon type="search" class="tools-icon" title="查询" @click="getAppEnv({})"/>
            <a-icon type="reload" class="tools-icon" title="重置" @click="resetForm"/>
          </div>
        </div>
        <!-- 环境变量表格 -->
        <div class="table-main-container table-scroll-x-auto"
             v-if="viewType === VIEW_TYPE.TABLE.value">
          <a-table :columns="columns"
                   :dataSource="rows"
                   :pagination="pagination"
                   :rowSelection="rowSelection"
                   rowKey="id"
                   @change="getAppEnv"
                   :scroll="{x: '100%'}"
                   :loading="loading"
                   size="middle">
            <!-- key -->
            <template #key="record">
              <div class="auto-ellipsis">
                <a class="copy-icon-left" @click="$copy(record.key)">
                  <a-icon type="copy"/>
                </a>
                <span class="pointer auto-ellipsis-item" title="预览" @click="preview(record.key)">
                  {{ record.key }}
                </span>
              </div>
            </template>
            <!-- value -->
            <template #value="record">
              <div class="auto-ellipsis">
                <a class="copy-icon-left" @click="$copy(record.value)">
                  <a-icon type="copy"/>
                </a>
                <span class="pointer auto-ellipsis-item" title="预览" @click="preview(record.value)">
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
                <AppProfileChecker :ref="'profileChecker' + record.id">
                  <template #trigger>
                    <span class="action-link">同步</span>
                  </template>
                  <template #footer>
                    <a-button type="primary" size="small" @click="syncEnv(record.id)">确定</a-button>
                  </template>
                </AppProfileChecker>
                <a-divider type="vertical" class="action-divider" />
                <a @click="history(record)" class="action-link">历史</a>
                <a-divider type="vertical" class="action-divider" />
                <a-popconfirm :disabled="record.forbidDelete !== 1"
                              placement="topRight"
                              title="是否删除当前变量?"
                              ok-text="确定"
                              cancel-text="取消"
                              @confirm="remove([record.id])">
                  <span class="action-link action-danger" :class="{'disabled': record.forbidDelete !== 1}">删除</span>
                </a-popconfirm>
              </div>
            </template>
          </a-table>
        </div>
        <!-- 环境变量视图 -->
        <div class="table-main-container env-editor-container" v-if="viewType !== VIEW_TYPE.TABLE.value">
          <a-spin class="editor-spin" style="height: 100%" :spinning="loading">
            <Editor ref="editor" :lang="viewLang"/>
          </a-spin>
        </div>
      </div>
    </div>
    <!-- 事件 -->
    <div class="app-env-event-container">
      <AddAppEnvModal ref="addModal" @added="getAppEnv({})" @updated="getAppEnv({})"/>
      <EnvHistoryModal ref="historyModal" @rollback="getAppEnv()"/>
      <TextPreview ref="preview"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { HISTORY_VALUE_TYPE, VIEW_TYPE } from '@/lib/enum'
import Editor from '@/components/editor/Editor'
import AddAppEnvModal from '@/components/app/AddAppEnvModal'
import EnvHistoryModal from '@/components/content/EnvHistoryModal'
import TextPreview from '@/components/preview/TextPreview'
import AppProfileChecker from '@/components/app/AppProfileChecker'

const columns = [
  {
    title: 'key',
    key: 'key',
    width: 220,
    ellipsis: true,
    sorter: (a, b) => a.key.localeCompare(b.key),
    scopedSlots: { customRender: 'key' }
  },
  {
    title: 'value',
    key: 'value',
    ellipsis: true,
    sorter: (a, b) => a.value.localeCompare(b.value),
    scopedSlots: { customRender: 'value' }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
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
    align: 'center',
    width: 200,
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'AppEnv',
  components: {
    AddAppEnvModal,
    EnvHistoryModal,
    TextPreview,
    Editor,
    AppProfileChecker
  },
  data: function() {
    return {
      VIEW_TYPE,
      defaultSelectedAppIds: [],
      appLoading: false,
      appList: [],
      rows: [],
      query: {
        appId: undefined,
        profileId: undefined,
        key: undefined,
        value: undefined,
        description: undefined
      },
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
      columns,
      viewType: VIEW_TYPE.TABLE.value,
      viewLang: null
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: e => {
          this.selectedRowKeys = e
        },
        getCheckboxProps: record => ({
          props: {
            disabled: record.forbidDelete !== 1
          }
        })
      }
    }
  },
  methods: {
    async getAppList() {
      this.appLoading = true
      const apps = await this.$api.getAppList({ limit: 10000 })
      this.appList = apps.data.rows.map(i => {
        return {
          id: i.id,
          name: i.name
        }
      })
      this.appLoading = false
    },
    chooseApp(id) {
      this.query.appId = id
      this.pagination.current = 1
      this.getAppEnv()
    },
    chooseProfile({ id }) {
      this.query.profileId = id
      this.pagination.current = 1
      this.getAppEnv({})
    },
    changeView(view) {
      this.viewLang = view.lang
      this.viewType = view.value
      this.getAppEnv({})
    },
    getAppEnv(page = this.pagination) {
      this.loading = true
      if (this.viewType === VIEW_TYPE.TABLE.value) {
        this.$api.getAppEnvList({
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
          this.selectedRowKeys = []
        }).catch(() => {
          this.loading = false
        })
      } else {
        this.$api.getAppEnvView({
          ...this.query,
          viewType: this.viewType
        }).then(({ data }) => {
          this.$refs.editor.setValue(data)
          this.loading = false
        }).catch(() => {
          this.loading = false
        })
      }
    },
    save() {
      const value = this.$refs.editor.getValue()
      if (!value || !value.trim().length) {
        this.$message.warn('请输入内容')
        return
      }
      this.loading = true
      this.$api.saveAppEnvView({
        appId: this.query.appId,
        profileId: this.query.profileId,
        viewType: this.viewType,
        value
      }).then(({ data }) => {
        this.loading = false
        this.$message.info(`保存成功 ${data} 条数据`)
        this.getAppEnv({})
      }).catch(() => {
        this.loading = false
        this.$message.error('解析失败, 请检查内容')
      })
    },
    add() {
      this.$refs.addModal.add(this.query.appId, this.query.profileId)
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(idList) {
      this.$api.deleteAppEnv({ idList }).then(() => {
        this.$message.success('删除成功')
        this.getAppEnv({})
      })
    },
    preview(value) {
      this.$refs.preview.preview(value)
    },
    history(record) {
      this.$refs.historyModal.open({
        key: record.key,
        valueId: record.id,
        valueType: HISTORY_VALUE_TYPE.APP_ENV.value
      })
    },
    syncEnv(id) {
      const ref = this.$refs['profileChecker' + id]
      if (!ref.checkedList.length) {
        this.$message.warning('请先选择同步的环境')
        return
      }
      const targetProfileIdList = ref.checkedList
      ref.clear()
      ref.hide()
      this.$api.syncAppEnv({
        id,
        appId: this.query.appId,
        profileId: this.query.profileId,
        targetProfileIdList
      }).then(() => {
        this.$message.success('同步成功')
      })
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.getAppEnv({})
    }
  },
  async mounted() {
    const activeProfile = this.$storage.get(this.$storage.keys.ACTIVE_PROFILE)
    if (!activeProfile) {
      this.$message.warning('请先维护应用环境')
      return
    }
    this.query.profileId = JSON.parse(activeProfile).id
    await this.getAppList()
    let chooseId
    if (this.$route.params.id) {
      chooseId = parseInt(this.$route.params.id)
    } else if (this.appList.length) {
      chooseId = this.appList[0].id
    } else {
      this.$message.warning('请先维护应用')
      return
    }
    this.chooseApp(chooseId)
    this.defaultSelectedAppIds.push(chooseId)
  },
  filters: {
    formatDate
  }
}
</script>

<style lang="less" scoped>
.app-env-container {
  display: flex;
  justify-content: flex-start;
  width: 100%;
  gap: 16px;

  .app-container {
    width: 240px;
    padding: 0 8px 8px 8px;
    background-color: white;
    border-radius: 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  }

  .env-container {
    flex: 1;
    background: white;
    border-radius: 16px;
    min-height: calc(100vh - 84px);
    overflow: hidden;

    .search-columns-wrapper {
      padding: 16px 20px 0;
      background: white;
      
      .table-search-columns {
        margin-bottom: 0;
      }
    }

    .table-wrapper {
      padding: 0 20px 20px;
    }

    .env-editor-container {
      height: calc(100% - 64px);
      padding-bottom: 6px;
    }
  }
}

.app-list-wrapper {
  height: calc(100vh - 180px);
  overflow-y: auto;
  
  ul {
    background-color: transparent;
  }
  
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

.view-radio-group {
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

.disabled {
  cursor: not-allowed;
  opacity: 0.5;
  pointer-events: none;
}

.span-blue {
  color: #3B82F6;
}

::v-deep .editor-spin .ant-spin-container {
  height: 100%;
}

::v-deep .ant-table-row-cell-ellipsis {
  padding: 12px 16px !important;
}

::v-deep .ant-table-tbody > tr > td {
  padding: 12px 16px !important;
}
</style>