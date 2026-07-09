<template>
  <div class="machine-key-container">
    <!-- 搜索框 -->
    <div class="table-search-columns">
      <a-form-model class="machine-key-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-model-item label="密钥名称" prop="name">
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
          <span class="table-title">机器密钥</span>
          <a-divider v-show="selectedRowKeys.length" type="vertical"/>
          <div v-show="selectedRowKeys.length">
            <!-- 删除 -->
            <a-popconfirm title="确认删除选中密钥?"
                          placement="topRight"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="remove(selectedRowKeys)">
              <a-button type="danger" icon="delete" size="small">删除</a-button>
            </a-popconfirm>
          </div>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
          <a-button type="primary" icon="plus" @click="add" class="action-btn">新建</a-button>
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
                 :rowSelection="{selectedRowKeys, onChange: e => selectedRowKeys = e}"
                 rowKey="id"
                 @change="getList"
                 :scroll="{x: '100%'}"
                 :loading="loading"
                 size="middle">
          <!-- 密钥路径 -->
          <template #path="record">
            <a @click="loadDownloadUrl(record)" title="获取下载链接">{{ record.path }}</a>
            <a v-if="record.downloadUrl"
               target="_blank"
               title="下载"
               style="margin-left: 10px"
               :href="record.downloadUrl"
               @click="clearDownloadUrl(record)">
              <a-icon type="download"/>
            </a>
          </template>
          <!-- 创建时间 -->
          <template #createTime="record">
            {{ record.createTime | formatDate }}
          </template>
          <!-- 操作 -->
          <template #action="record">
            <!-- 修改 -->
            <MachineChecker :ref="`machineChecker${record.id}`" placement="bottomRight">
              <template #trigger>
                <a>绑定机器</a>
              </template>
              <template #footer>
                <a-button type="primary" size="small" @click="chooseRelMachines(record.id)">确定</a-button>
              </template>
            </MachineChecker>
            <a-divider type="vertical"/>
            <!-- 修改 -->
            <a @click="update(record.id)">修改</a>
            <a-divider type="vertical"/>
            <!-- 删除 -->
            <a-popconfirm title="确认删除当前密钥?"
                          placement="topRight"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="remove([record.id])">
              <span class="span-blue pointer">删除</span>
            </a-popconfirm>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="machine-key-event">
      <!-- 新建模态框 -->
      <AddMachineKeyModal ref="addModal" @added="getList({})" @updated="getList({})"/>
    </div>
  </div>
</template>

<script>
import { defineArrayKey } from '@/lib/utils'
import { FILE_DOWNLOAD_TYPE } from '@/lib/enum'
import { formatDate } from '@/lib/filters'
import AddMachineKeyModal from '@/components/machine/AddMachineKeyModal'
import MachineChecker from '@/components/machine/MachineChecker'

/**
 * 列
 */
const columns = [
  {
    title: '密钥名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
    ellipsis: true,
    sorter: (a, b) => a.name.localeCompare(b.name)
  },
  {
    title: '密钥路径',
    key: 'path',
    width: 265,
    scopedSlots: { customRender: 'path' }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true,
    width: 200
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 150,
    align: 'center',
    sorter: (a, b) => a.createTime - b.createTime,
    scopedSlots: { customRender: 'createTime' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 190,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'MachineKey',
  components: {
    MachineChecker,
    AddMachineKeyModal
  },
  data: function() {
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
      selectedRowKeys: [],
      columns
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getMachineKeyList({
        ...this.query,
        page: page.current,
        limit: page.pageSize
      }).then(({ data }) => {
        const pagination = { ...this.pagination }
        pagination.total = data.total
        pagination.current = data.page
        // 定义下载路径
        defineArrayKey(data.rows, 'downloadUrl')
        this.rows = data.rows || []
        this.pagination = pagination
        this.loading = false
        this.selectedRowKeys = []
      }).catch(() => {
        this.loading = false
      })
    },
    remove(idList) {
      this.$api.removeMachineKey({
        idList
      }).then(() => {
        this.$message.success('删除成功')
        this.getList({})
      })
    },
    chooseRelMachines(id) {
      const ref = this.$refs[`machineChecker${id}`]
      const checkedList = ref.checkedList
      if (!checkedList || !checkedList.length) {
        this.$message.warning('请选择绑定密钥的机器')
        return
      }
      ref.hide()
      ref.clear()
      this.$api.bindMachineKey({
        id,
        machineIdList: checkedList
      }).then(() => {
        this.$message.success('绑定成功')
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
      this.getList({})
    },
    async loadDownloadUrl(record) {
      try {
        const downloadUrl = await this.$api.getFileDownloadToken({
          type: FILE_DOWNLOAD_TYPE.SECRET_KEY.value,
          id: record.id
        })
        record.downloadUrl = this.$api.fileDownloadExec({ token: downloadUrl.data })
      } catch (e) {
        // ignore
      }
    },
    clearDownloadUrl(record) {
      setTimeout(() => {
        record.downloadUrl = null
      })
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
.machine-key-container {
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

/* 移除按钮默认边距 */
.ml8 {
  margin-left: 0;
}

.mx8 {
  margin-left: 0;
  margin-right: 0;
}

/* 蓝色链接样式 */
.span-blue {
  color: #3B82F6;
  cursor: pointer;
}

.span-blue:hover {
  color: #2563EB;
  text-decoration: underline;
}
</style>