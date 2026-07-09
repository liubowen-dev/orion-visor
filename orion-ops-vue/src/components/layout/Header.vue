<template>
  <a-layout-header class="header-main">
    <!-- 头部左侧 -->
    <div class="header-fixed-left">
      <!-- 折叠 -->
      <a-icon class="trigger-icon header-block-container header-block-fold"
              :type="fold ? 'menu-unfold' : 'menu-fold'"
              :title="fold ? '展开' : '折叠'"
              @click="changeFold"/>
      <!-- 左侧配置 -->
      <a-icon class="trigger-icon header-block-container"
              v-for="(prop, index) of leftProps"
              :key="index"
              :title="prop.title"
              :type="prop.icon"
              @click="handlerCall(prop)"/>
    </div>
    <!-- 头部右侧 -->
    <div class="header-fixed-right">
      <!-- 环境选择 -->
      <HeaderProfileSelect id="header-profile-selector"
                           class="header-block-container"
                           ref="profileSelect"
                           v-show="profileSelectorVisible"
                           @chooseProfile="chooseProfile"/>
      <!-- 站内信 -->
      <WebSideMessageDrawer id="web-side-message-drawer" class="header-block-container"/>
      <!-- 用户下拉 -->
      <HeaderUser id="header-user" class="header-block-container"/>
    </div>
  </a-layout-header>
</template>

<script>
import HeaderProfileSelect from './HeaderProfileSelect'
import HeaderUser from './HeaderUser'
import WebSideMessageDrawer from '@/components/layout/WebSideMessageDrawer'

export default {
  name: 'Header',
  components: {
    WebSideMessageDrawer,
    HeaderProfileSelect,
    HeaderUser
  },
  data: function() {
    return {
      fold: false,
      profileSelectorVisible: false,
      leftProps: []
    }
  },
  methods: {
    changeFold() {
      this.fold = !this.fold
      this.$emit('changeFoldStatus')
    },
    handlerCall(prop) {
      prop.call && this[prop.call] && this[prop.call]()
      prop.event && this.$emit('onHeaderEvent', prop.event)
    },
    back() {
      this.$router.back(-1)
    },
    checkVisible(e = this.$route) {
      this.profileSelectorVisible = e.meta.visibleProfile === true
      this.leftProps = e.meta.leftProps || []
    },
    chooseProfile(profile) {
      this.$emit('chooseProfile', profile)
    },
    reloadProfile() {
      this.$refs.profileSelect.loadProfile()
    }
  },
  created() {
    this.checkVisible()
  }
}
</script>

<style lang="less" scoped>
.header-main {
  background: transparent;
  padding: 0 10px 0 6px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-fixed-left {
    display: flex;

    .header-block-fold {
      margin-left: 2px;
    }

    .trigger-icon {
      font-size: 18px;
      line-height: 40px;
      padding: 0 14px;
      cursor: pointer;
      transition: color 0.3s, transform 0.2s ease;
      color: #64748B;
    }

    .trigger-icon:hover {
      color: #3B82F6;
      transform: translateY(-1px);
    }
  }

  .header-fixed-right {
    display: flex;
    align-items: center;

    #header-profile-selector {
      padding: 0 14px;
      height: 40px;
      display: flex;
      align-items: center;
      font-size: 14px;
      line-height: 18px;
      color: #475569;
      border-radius: 12px;

      ::v-deep i {
        padding-left: 4px;
        margin-top: 4px;
        color: #94A3B8;
      }
    }

    #web-side-message-drawer {
      padding: 0 16px;
      height: 40px;
      display: flex;
      align-items: center;
      margin: 0 4px 0 0;
    }

    #header-user {
      padding: 0 12px;
      margin-right: 2px;
      height: 40px;
      display: flex;
      align-items: center;
    }
  }
}

.header-block-container {
  transition: all 0.2s ease;
  border: 1px solid transparent;
  border-radius: 12px;
}

.header-block-container:hover {
  background-color: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.2);
  color: #3B82F6;
}
</style>