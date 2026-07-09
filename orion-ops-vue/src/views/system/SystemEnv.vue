<template>
  <div class="system-env-container">
    <!-- 环境变量容器 -->
    <div class="system-env-wrapper">
      <!-- 环境变量筛选 -->
      <div class="search-columns-wrapper">
        <div class="table-search-columns">
          <a-form-model class="system-env-search-form" ref="query" :model="query">
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
            <a-button type="primary" icon="plus" @click="add" class="action-btn">添加</a-button>
            <a-divider type="vertical" class="divider-vertical" />
            <a-icon type="search" class="tools-icon" title="查询" @click="getSystemEnv({})"/>
            <a-icon type="reload" class="tools-icon" title="重置" @click="resetForm"/>
          </div>
        </div>
        <!-- 环境变量表格 -->
        <div class="table-main-container table-scroll-x-auto" v-if="viewType === VIEW_TYPE.TABLE.value">
          <a-table :columns="columns"
                   :dataSource="rows"
                   :pagination="pagination"
                   :rowSelection="rowSelection"
                   rowKey="id"
                   @change="getSystemEnv"
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
    <div class="system-env-event-container">
      <AddSystemEnvModal ref="addModal" @added="getSystemEnv({})" @updated="getSystemEnv({})"/>
      <EnvHistoryModal ref="historyModal" @rollback="getSystemEnv()"/>
      <TextPreview ref="preview"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { HISTORY_VALUE_TYPE, VIEW_TYPE } from '@/lib/enum'
import Editor from '@/components/editor/Editor'
import AddSystemEnvModal from '@/components/system/AddSystemEnvModal'
import EnvHistoryModal from '@/components/content/EnvHistoryModal'
import TextPreview from '@/components/preview/TextPreview'

const columns = [
  {
    title: 'key',
    key: 'key',
    width: 240,
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
    ellipsis: true,
    width: 180
  },
  {
    title: '修改时间',
    key: 'updateTime',
    align: 'center',
    width: 150,
    sorter: (a, b) => a.updateTime - b.updateTime,
    scopedSlots: { customRender: 'updateTime' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    align: 'center',
    width: 170,
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'SystemEnv',
  components: {
    AddSystemEnvModal,
    EnvHistoryModal,
    TextPreview,
    Editor
  },
  data: function() {
    return {
      VIEW_TYPE,
      rows: [],
      query: {
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
        columnWidth: '75px',
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
    changeView(view) {
      this.viewLang = view.lang
      this.viewType = view.value
      this.getSystemEnv({})
    },
    getSystemEnv(page = this.pagination) {
      this.loading = true
      if (this.viewType === VIEW_TYPE.TABLE.value) {
        this.$api.getSystemEnvList({
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
        this.$api.getSystemEnvView({
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
      this.$api.saveSystemEnvView({
        viewType: this.viewType,
        value
      }).then(({ data }) => {
        this.loading = false
        this.$message.info(`保存成功 ${data} 条数据`)
        this.getSystemEnv({})
      }).catch(() => {
        this.loading = false
        this.$message.error('解析失败, 请检查内容')
      })
    },
    add() {
      this.$refs.addModal.add()
    },
    update(id) {
      this.$refs.addModal.update(id)
    },
    remove(idList) {
      this.$api.deleteSystemEnv({ idList }).then(() => {
        this.$message.success('删除成功')
        this.getSystemEnv({})
      })
    },
    preview(value) {
      this.$refs.preview.preview(value)
    },
    history(record) {
      this.$refs.historyModal.open({
        key: record.key,
        valueId: record.id,
        valueType: HISTORY_VALUE_TYPE.SYSTEM_ENV.value
      })
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.getSystemEnv({})
    }
  },
  async mounted() {
    this.getSystemEnv({})
  },
  filters: {
    formatDate
  }
}
</script>

<style lang="less" scoped>
.system-env-container {
  background: white;
  border-radius: 16px;
  padding: 0;
  min-height: calc(100vh - 84px);
}

.system-env-wrapper {
  padding: 0;
}

/* 搜索列样式 */
.search-columns-wrapper {
  padding: 20px 20px 0;
}

.table-search-columns {
  margin-bottom: 0;
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

/* 视图单选组样式 */
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

/* 危险操作样式（删除） */
.action-danger {
  color: #EF4444;
}

.action-danger:hover {
  color: #DC2626;
}

/* 操作分隔线 */
.action-divider {
  height: 12px;
  margin: 0;
}

/* 禁用样式 */
.disabled {
  cursor: not-allowed;
  opacity: 0.5;
  pointer-events: none;
}

/* 编辑器容器 */
.env-editor-container {
  height: calc(100vh - 260px);
  padding-bottom: 16px;
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