<!-- src/components/command/AlertList.vue -->
<template>
  <div class="alert-list">
    <div class="alert-header">
      <span class="alert-title">
        <a-icon type="bell" />
        实时告警
      </span>
      <a-badge :count="alerts.length" :number-style="{ backgroundColor: '#EF4444' }" />
    </div>
    <div class="alert-content">
      <div 
        v-for="alert in displayAlerts" 
        :key="alert.id"
        class="alert-item"
        :class="alert.level"
      >
        <div class="alert-level">
          <a-icon :type="getAlertIcon(alert.level)" />
        </div>
        <div class="alert-info">
          <div class="alert-message">{{ alert.message }}</div>
          <div class="alert-meta">
            <span class="alert-source">{{ alert.source }}</span>
            <span class="alert-time">{{ alert.time }}</span>
          </div>
        </div>
      </div>
      <div v-if="alerts.length === 0" class="alert-empty">
        <a-icon type="check-circle" />
        <span>暂无告警，系统运行正常</span>
      </div>
      <div v-if="alerts.length > 3" class="alert-more">
        <a-button type="link" @click="$emit('viewMore')">
          查看全部 {{ alerts.length }} 条告警
        </a-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AlertList',
  props: {
    alerts: {
      type: Array,
      default: () => []
    },
    maxDisplay: {
      type: Number,
      default: 3
    }
  },
  computed: {
    displayAlerts() {
      return this.alerts.slice(0, this.maxDisplay)
    }
  },
  methods: {
    getAlertIcon(level) {
      const icons = {
        critical: 'close-circle',
        warning: 'exclamation-circle',
        info: 'info-circle'
      }
      return icons[level] || 'info-circle'
    }
  }
}
</script>

<style lang="less" scoped>
.alert-list {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.02);
  
  .alert-header {
    padding: 16px 20px;
    border-bottom: 1px solid #E2E8F0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .alert-title {
      font-size: 16px;
      font-weight: 600;
      color: #0F172A;
      
      i {
        margin-right: 8px;
        color: #F59E0B;
      }
    }
  }
  
  .alert-content {
    padding: 8px 0;
  }
  
  .alert-item {
    padding: 12px 20px;
    display: flex;
    gap: 12px;
    transition: all 0.2s;
    cursor: pointer;
    border-bottom: 1px solid #F1F5F9;
    
    &:hover {
      background: #F8FAFF;
    }
    
    &.critical .alert-level i {
      color: #EF4444;
    }
    
    &.warning .alert-level i {
      color: #F59E0B;
    }
    
    &.info .alert-level i {
      color: #3B82F6;
    }
    
    .alert-level {
      flex-shrink: 0;
      
      i {
        font-size: 18px;
      }
    }
    
    .alert-info {
      flex: 1;
      
      .alert-message {
        font-size: 14px;
        color: #1E293B;
        margin-bottom: 4px;
        font-weight: 500;
      }
      
      .alert-meta {
        font-size: 12px;
        color: #94A3B8;
        
        .alert-source {
          margin-right: 12px;
        }
        
        .alert-time {
          font-family: monospace;
        }
      }
    }
  }
  
  .alert-empty {
    padding: 48px 20px;
    text-align: center;
    
    i {
      font-size: 40px;
      color: #10B981;
      margin-bottom: 12px;
      display: block;
    }
    
    span {
      font-size: 14px;
      color: #64748B;
    }
  }
  
  .alert-more {
    padding: 12px 20px;
    text-align: center;
    border-top: 1px solid #F1F5F9;
    
    ::v-deep .ant-btn-link {
      color: #3B82F6;
      
      &:hover {
        color: #06B6D4;
      }
    }
  }
}
</style>