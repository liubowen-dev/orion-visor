<template>
  <div class="user-detail-container">
    <a-tabs :defaultActiveKey="key" tabPosition="left" @change="changeTab" size="small">
      <!-- 基本信息 -->
      <a-tab-pane :key="1" tab="基本信息">
        <UserBasicForm/>
      </a-tab-pane>
      <!-- 操作日志 -->
      <a-tab-pane :key="2" tab="操作日志">
        <EventLogList/>
      </a-tab-pane>
      <!-- 登录历史 -->
      <a-tab-pane :key="3" tab="登录历史">
        <LoginHistory/>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import UserBasicForm from '@/components/user/UserBasicForm'

export default {
  name: 'UserDetail',
  data() {
    return {
      key: 1
    }
  },
  components: {
    UserBasicForm,
    EventLogList: () => import('@/components/user/EventLogList'),
    LoginHistory: () => import('@/components/user/LoginHistory')
  },
  methods: {
    changeTab(key) {
      switch (key) {
        case 1:
          document.title = '基本信息'
          break
        case 2:
          document.title = '操作日志'
          break
        case 3:
          document.title = '登录历史'
          break
        default:
          break
      }
    }
  },
  created() {
    const key = this.$route.query.key
    if (key) {
      this.key = parseInt(this.$route.query.key)
    }
    this.changeTab(this.key)
  }
}
</script>

<style lang="less" scoped>
.user-detail-container {
  background: white;
  padding: 0;
  border-radius: 16px;
  min-height: calc(100vh - 84px);
  
  ::v-deep .ant-tabs {
    background: white;
    border-radius: 16px;
  }
  
  ::v-deep .ant-tabs-bar {
    margin: 0;
    border-right: 1px solid #F1F5F9;
    padding: 16px 0;
  }
  
  ::v-deep .ant-tabs-tab {
    padding: 12px 24px;
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: #64748B;
    
    &:hover {
      color: #3B82F6;
    }
  }
  
  ::v-deep .ant-tabs-tab-active {
    color: #3B82F6;
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.08), transparent);
  }
  
  ::v-deep .ant-tabs-ink-bar {
    background: #3B82F6;
    width: 2px;
  }
  
  ::v-deep .ant-tabs-content {
    padding: 24px;
  }
}
</style>