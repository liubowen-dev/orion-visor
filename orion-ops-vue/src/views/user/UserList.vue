<template>
  <div class="user-list-container">
    <!-- 筛选列 -->
    <div class="table-search-columns">
      <a-form-model class="user-list-search-form" ref="query" :model="query">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-model-item label="用户名" prop="username">
              <a-input v-model="query.username" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="昵称" prop="nickname">
              <a-input v-model="query.nickname" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="手机号" prop="phone">
              <a-input v-model="query.phone" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="邮箱" prop="email">
              <a-input v-model="query.email" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="4">
            <a-form-model-item label="角色" prop="role">
              <a-select v-model="query.role" placeholder="全部" @change="getList({})" allowClear>
                <a-select-option v-for="role in ROLE_TYPE" :value="role.value" :key="role.value">
                  {{ role.label }}
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
          <span class="table-title">用户列表</span>
        </div>
        <!-- 右侧 -->
        <div class="tools-fixed-right">
          <!-- 状态单选 -->
          <div class="status-radio-group">
            <a-radio-group v-model="query.status" @change="getList({})" size="small">
              <a-radio-button :value="undefined">全部</a-radio-button>
              <a-radio-button :value="1">启用</a-radio-button>
              <a-radio-button :value="2">禁用</a-radio-button>
            </a-radio-group>
          </div>
          <a-divider type="vertical" class="divider-vertical" />
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
          <!-- 用户名 -->
          <template #username="record">
            <span class="pointer" @click="$copy(record.username)" title="复制">{{ record.username }}</span>
          </template>
          <!-- 昵称 -->
          <template #nickname="record">
            <span class="pointer" @click="$copy(record.nickname)" title="复制">{{ record.nickname }}</span>
          </template>
          <!-- 手机号 -->
          <template #phone="record">
            <span class="pointer span-blue" @click="$copy(record.phone)" title="复制">{{ record.phone }}</span>
          </template>
          <!-- 邮箱 -->
          <template #email="record">
            <span class="pointer span-blue" @click="$copy(record.email)" title="复制">{{ record.email }}</span>
          </template>
          <!-- 角色 -->
          <template #role="record">
            <span>{{ record.role | formatRoleType('label') }}</span>
          </template>
          <!-- 状态 -->
          <template #status="record">
            <a-badge :status="record.status | formatEnableStatus('status')"
                     :text="record.status | formatEnableStatus('label')"/>
          </template>
          <!-- 登录时间 -->
          <template #lastLoginTime="record">
            <span v-if="record.lastLoginTime">
              {{ record.lastLoginTime | formatDate }} ({{ record.lastLoginAgo }})
            </span>
          </template>
          <!-- 操作 -->
          <template #action="record">
            <div class="table-actions" v-if="record.id !== $getUserId()">
              <a-button :disabled="record.locked === 1" type="link" class="action-link" @click="unlock(record.id)">解锁</a-button>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm :title="`确认${record.status === 1 ? '禁用' : '启用'}当前用户?`"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="updateStatus(record)">
                <span class="action-link">{{ record.status === 1 ? '禁用' : '启用' }}</span>
              </a-popconfirm>
              <a-divider type="vertical" class="action-divider" />
              <span class="action-link" @click="update(record.id)">修改</span>
              <a-divider type="vertical" class="action-divider" />
              <a :href="`#/user/event/logs/${record.id}`" class="action-link">日志</a>
              <a-divider type="vertical" class="action-divider" />
              <a @click="resetPassword(record.id)" class="action-link">重置密码</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="是否要删除当前用户?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="remove(record.id)">
                <span class="action-link action-danger">删除</span>
              </a-popconfirm>
            </div>
            <div v-else class="table-actions">
              <a :href="`#/user/event/logs/${record.id}`" class="action-link">日志</a>
            </div>
          </template>
        </a-table>
      </div>
    </div>
    <!-- 事件 -->
    <div class="log-list-event">
      <AddUserModal ref="addModal" @added="getList({})" @updated="getList({})"/>
      <ResetPassword ref="reset" :visibleBeforePassword="false"/>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/lib/filters'
import { ENABLE_STATUS, enumValueOf, ROLE_TYPE } from '@/lib/enum'
import ResetPassword from '@/components/user/ResetPassword'
import AddUserModal from '@/components/user/AddUserModal'

function getColumns() {
  const columns = [
    { title: '序号', key: 'seq', width: 65, align: 'center', customRender: (text, record, index) => `${index + 1}` },
    { title: '用户名', key: 'username', ellipsis: true, sorter: (a, b) => a.username.localeCompare(b.username), scopedSlots: { customRender: 'username' } },
    { title: '昵称', key: 'nickname', ellipsis: true, sorter: (a, b) => a.nickname.localeCompare(b.nickname), scopedSlots: { customRender: 'nickname' } },
    { title: '联系电话', key: 'phone', ellipsis: true, align: 'center', scopedSlots: { customRender: 'phone' } },
    { title: '联系邮箱', key: 'email', ellipsis: true, scopedSlots: { customRender: 'email' } },
    { title: '角色', key: 'role', ellipsis: true, width: 100, sorter: (a, b) => a.role > b.role, scopedSlots: { customRender: 'role' } },
    { title: '状态', key: 'status', ellipsis: true, width: 100, sorter: (a, b) => a.status > b.status, scopedSlots: { customRender: 'status' } },
    { title: '最后登录时间', key: 'lastLoginTime', width: 220, ellipsis: true, align: 'center', sorter: (a, b) => (a.lastLoginTime || 0) - (b.lastLoginTime || 0), scopedSlots: { customRender: 'lastLoginTime' } },
    { title: '操作', key: 'action', fixed: 'right', width: 380, align: 'center', scopedSlots: { customRender: 'action' }, requireAdmin: true }
  ]
  if (this.$isAdmin()) {
    return columns
  } else {
    return columns.filter(s => !s.requireAdmin)
  }
}

export default {
  name: 'UserList',
  components: { ResetPassword, AddUserModal },
  data() {
    return {
      ROLE_TYPE,
      query: { username: undefined, nickname: undefined, phone: undefined, email: undefined, role: undefined, status: undefined },
      rows: [],
      pagination: { current: 1, pageSize: 10, total: 0, showTotal: total => `共 ${total} 条` },
      loading: false,
      columns: getColumns.call(this)
    }
  },
  methods: {
    getList(page = this.pagination) {
      this.loading = true
      this.$api.getUserList({ ...this.query, page: page.current, limit: page.pageSize })
        .then(({ data }) => {
          const pagination = { ...this.pagination }
          pagination.total = data.total
          pagination.current = data.page
          this.rows = data.rows || []
          this.pagination = pagination
          this.loading = false
        }).catch(() => { this.loading = false })
    },
    update(id) { this.$refs.addModal.update(id) },
    add() { this.$refs.addModal.add() },
    resetPassword(id) { this.$refs.reset.open(id) },
    unlock(id) { this.$api.unlockUser({ id }).then(() => { this.$message.success('已解锁'); this.getList() }) },
    remove(id) { this.$api.deleteUser({ id }).then(() => { this.$message.success('删除成功'); this.getList({}) }) },
    updateStatus(record) {
      const label = record.status === 1 ? '禁用' : '启用'
      const pending = this.$message.loading(`正在${label}...`)
      this.$api.updateUserStatus({ id: record.id, status: record.status === 1 ? 2 : 1 })
        .then(() => { pending(); this.$message.success(`${label}成功`); this.getList() })
        .catch(() => { pending() })
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.query.role = undefined
      this.query.status = undefined
      this.getList({})
    }
  },
  filters: {
    formatDate,
    formatEnableStatus(status, f) { return enumValueOf(ENABLE_STATUS, status)[f] },
    formatRoleType(type, f) { return enumValueOf(ROLE_TYPE, type)[f] }
  },
  mounted() { this.getList({}) }
}
</script>

<style scoped>
.user-list-container {
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