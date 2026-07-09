<template>
  <a-layout id="common-layout" v-if="validToken" :class="{ 'fullscreen-layout': isFullscreenPage }">
    <!-- 左侧 - 全屏时隐藏 -->
    <a-layout-sider 
      v-if="!isFullscreenPage" 
      id="common-sider" 
      v-model="collapsed" 
      :trigger="null"
    >
      <Menu ref="menu"/>
    </a-layout-sider>
    <a-layout id="common-right" :class="{ 'fullscreen-right': isFullscreenPage }">
      <!-- 头部菜单 - 全屏时隐藏 -->
      <Header 
        v-if="!isFullscreenPage"
        id="common-header"
        ref="header"
        @changeFoldStatus="collapsed = !collapsed"
        @chooseProfile="chooseProfile"
        @onHeaderEvent="onHeaderEvent"
      />
      <!-- 主体部分 -->
      <a-layout-content id="common-content" :class="{ 'fullscreen-content': isFullscreenPage }">
        <a-spin :spinning="globalLoading" :tip="globalLoadingTip">
          <router-view 
            ref="route"
            :key="$route.fullPath"
            @reloadProfile="reloadProfile"
            @openLoading="openLoading"
            @closeLoading="closeLoading"
          />
        </a-spin>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import Menu from './Menu'
import Header from './Header'

export default {
  components: {
    Menu,
    Header
  },
  data() {
    return {
      collapsed: false,
      validToken: false,
      globalLoading: false,
      globalLoadingTip: null
    }
  },
  computed: {
    // 判断当前页面是否为全屏页面
    isFullscreenPage() {
      return this.$route.meta.fullscreen === true
    }
  },
  watch: {
    $route(e) {
      if (!this.isFullscreenPage) {
        this.$refs.menu?.chooseMenu(e)
        this.$refs.header?.checkVisible(e)
      }
    }
  },
  methods: {
    chooseProfile(e) {
      this.$refs.route && this.$refs.route.chooseProfile && this.$refs.route.chooseProfile(e)
    },
    onHeaderEvent(e) {
      this.$refs.route && this.$refs.route.onHeaderEvent && this.$refs.route.onHeaderEvent(e)
    },
    reloadProfile() {
      this.$refs.header?.reloadProfile()
    },
    openLoading(tip = null) {
      this.globalLoading = true
      this.globalLoadingTip = tip
    },
    closeLoading() {
      this.globalLoading = false
      this.globalLoadingTip = null
    }
  },
  async beforeCreate() {
    if (this.$getUserId()) {
      await this.$api.validToken().then(() => {
        this.validToken = true
      }).catch(() => {
        this.validToken = false
      })
    } else {
      this.validToken = false
    }
    if (!this.validToken) {
      this.$storage.clear()
      this.$storage.clearSession()
      this.$router.push('/login')
    }
  }
}
</script>

<style lang="less" scoped>
#common-layout {
  height: 100vh;
  background: linear-gradient(135deg, #F8FAFF 0%, #F0F4FE 100%);

  // 全屏模式样式
  &.fullscreen-layout {
    background: transparent;
  }

  #common-sider {
    overflow: auto;
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(20px);
    border-right: 1px solid rgba(59, 130, 246, 0.12);
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.02);
  }
}

#common-right {
  overflow: hidden;
  
  // 全屏时右侧区域占满整个视口
  &.fullscreen-right {
    width: 100% !important;
    max-width: 100% !important;
    overflow: auto;  // 👈 添加这一行，让右侧区域可以滚动
  }

  #common-content {
    padding: 18px 20px 24px;
    overflow: auto;
    flex: auto;
    background: transparent;
    
    // 全屏时内容区域占满且无内边距
    &.fullscreen-content {
      padding: 0;
      margin: 0;
      overflow: auto;  // 👈 改为 auto，允许滚动
      height: 100vh;   // 👈 添加高度，确保占满视口
    }
  }

  #common-header {
    z-index: 10;
    margin: 12px 16px 0;
    padding-right: 8px;
    height: 56px;
    border: 1px solid rgba(59, 130, 246, 0.12);
    border-radius: 18px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(14px);
  }
}
</style>