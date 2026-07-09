<template>
  <div class="system-setting-container">
    <a-tabs defaultActiveKey="1" tabPosition="left" size="small">
      <!-- 统计分析 -->
      <a-tab-pane key="1" tab="统计分析">
        <SystemAnalysis/>
      </a-tab-pane>
      <!-- 安全设置 -->
      <a-tab-pane key="2" tab="安全设置">
        <SecurityConfig/>
      </a-tab-pane>
      <!-- IP 过滤器配置 -->
      <a-tab-pane key="3" tab="IP过滤器配置">
        <IpConfig/>
      </a-tab-pane>
      <!-- 其他设置 -->
      <a-tab-pane key="4" tab="其他设置">
        <OtherConfig/>
      </a-tab-pane>
      <!-- 线程池指标 -->
      <a-tab-pane key="5" tab="线程池指标">
        <ThreadMetrics/>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import SystemAnalysis from '@/components/system/SystemAnalysis'

export default {
  name: 'SystemSetting',
  components: {
    SystemAnalysis,
    SecurityConfig: () => import('@/components/system/SecurityConfig'),
    IpConfig: () => import('@/components/system/IpConfig'),
    OtherConfig: () => import('@/components/system/OtherConfig'),
    ThreadMetrics: () => import('@/components/system/ThreadMetrics')
  }
}
</script>

<style lang="less" scoped>
.system-setting-container {
  background: white;
  border-radius: 16px;
  padding: 0;
  min-height: calc(100vh - 84px);
  
  ::v-deep .ant-tabs {
    background: white;
    border-radius: 16px;
    height: 100%;
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