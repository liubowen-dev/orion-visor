<!-- src/components/layout/CommandCenterLayout.vue -->
<template>
  <a-layout id="command-center-layout">
    <!-- 动态波纹背景 -->
    <div class="wave-bg"></div>
    
    <!-- 顶部状态栏 -->
    <div class="top-status-bar">
      <div class="status-left">
        <div class="system-logo">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="url(#gradient)" />
            <path d="M16 8 L20 12 L16 16 L12 12 L16 8Z" fill="white" />
            <path d="M16 16 L20 20 L16 24 L12 20 L16 16Z" fill="white" fill-opacity="0.8" />
            <defs>
              <linearGradient id="gradient" x1="0" y1="0" x2="32" y2="32">
                <stop stop-color="#3B82F6" />
                <stop offset="1" stop-color="#06B6D4" />
              </linearGradient>
            </defs>
          </svg>
          <span class="system-name">ORION OPS</span>
          <span class="system-badge">指挥中心</span>
        </div>
        <div class="time-display">{{ currentTime }}</div>
      </div>
      <div class="status-right">
        <div class="status-item health">
          <a-icon type="cloud-server" />
          <span>全域健康度: <strong :style="{color: healthColor}">{{ healthScore }}%</strong></span>
        </div>
        <div class="status-item alert">
          <a-icon type="bell" />
          <span>实时告警: <strong :style="{color: alertColor}">{{ alertCount }}</strong></span>
        </div>
        <HeaderProfileSelect ref="profileSelect" @chooseProfile="chooseProfile" />
        <HeaderUser />
      </div>
    </div>

    <!-- 主要内容区域 -->
    <a-layout class="command-layout-main">
      <!-- 左侧菜单栏 -->
      <a-layout-sider 
        v-model="collapsed" 
        :trigger="null"
        collapsible
        class="command-sider"
        :width="240"
      >
        <div class="sider-header">
          <div v-if="!collapsed" class="sider-title">
            <a-icon type="menu" />
            <span>导航菜单</span>
          </div>
          <a-icon 
            :type="collapsed ? 'menu-unfold' : 'menu-fold'" 
            class="collapse-trigger"
            @click="collapsed = !collapsed"
          />
        </div>
        <CommandMenu ref="menu" :collapsed="collapsed" />
      </a-layout-sider>

      <!-- 右侧内容区 -->
      <a-layout class="command-content-wrapper">
        <a-layout-content class="command-content">
          <router-view ref="route" :key="$route.fullPath" />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script>
import CommandMenu from './CommandMenu'
import HeaderProfileSelect from './HeaderProfileSelect'
import HeaderUser from './HeaderUser'
import dayjs from 'dayjs'

export default {
  name: 'CommandCenterLayout',
  components: {
    CommandMenu,
    HeaderProfileSelect,
    HeaderUser
  },
  data() {
    return {
      collapsed: false,
      currentTime: '',
      healthScore: 98.5,
      alertCount: 3,
      timer: null
    }
  },
  computed: {
    healthColor() {
      if (this.healthScore >= 95) return '#10B981'
      if (this.healthScore >= 80) return '#F59E0B'
      return '#EF4444'
    },
    alertColor() {
      return this.alertCount > 0 ? '#EF4444' : '#10B981'
    }
  },
  methods: {
    updateTime() {
      this.currentTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
    },
    chooseProfile(profile) {
      this.$refs.route && this.$refs.route.chooseProfile && this.$refs.route.chooseProfile(profile)
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  }
}
</script>

<style lang="less" scoped>
#command-center-layout {
  height: 100vh;
  background: linear-gradient(135deg, #F8FAFF 0%, #F0F4FE 100%);
  position: relative;
  overflow: hidden;
}

// 动态波纹背景
.wave-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background: 
    radial-gradient(circle at 10% 20%, rgba(59, 130, 246, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 90% 70%, rgba(6, 182, 212, 0.03) 0%, transparent 50%);
}

// 顶部状态栏
.top-status-bar {
  position: relative;
  z-index: 10;
  height: 64px;
  margin: 16px 20px 0;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02), 0 1px 0 rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.system-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .system-name {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, #1E293B 0%, #3B82F6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 1px;
  }
  
  .system-badge {
    font-size: 12px;
    padding: 2px 10px;
    background: linear-gradient(135deg, #3B82F6 0%, #06B6D4 100%);
    border-radius: 20px;
    color: white;
    font-weight: 500;
  }
}

.time-display {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 16px;
  font-weight: 500;
  color: #1E293B;
  background: #F1F5F9;
  padding: 6px 16px;
  border-radius: 24px;
}

.status-right {
  display: flex;
  align-items: center;
  gap: 16px;
  
  .status-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    background: #F8FAFF;
    border-radius: 24px;
    font-size: 13px;
    color: #475569;
    
    i {
      font-size: 16px;
    }
    
    &.health i {
      color: #3B82F6;
    }
    
    &.alert i {
      color: #EF4444;
    }
    
    strong {
      font-size: 16px;
      font-weight: 700;
      margin-left: 4px;
    }
  }
}

// 主布局
.command-layout-main {
  position: relative;
  z-index: 10;
  margin: 16px 20px 20px;
  height: calc(100vh - 100px);
  overflow: hidden;
}

// 侧边栏
.command-sider {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  margin-right: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  
  .sider-header {
    height: 56px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid rgba(59, 130, 246, 0.1);
    
    .sider-title {
      font-size: 14px;
      font-weight: 500;
      color: #1E293B;
      display: flex;
      align-items: center;
      gap: 8px;
      
      i {
        color: #3B82F6;
      }
    }
    
    .collapse-trigger {
      font-size: 18px;
      color: #64748B;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        color: #3B82F6;
      }
    }
  }
}

// 内容区
.command-content-wrapper {
  background: transparent;
  border-radius: 24px;
  overflow: hidden;
}

.command-content {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: #E2E8F0;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #94A3B8;
    border-radius: 3px;
    
    &:hover {
      background: #64748B;
    }
  }
}
</style>