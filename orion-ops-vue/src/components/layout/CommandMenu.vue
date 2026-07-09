<!-- src/components/layout/CommandMenu.vue -->
<template>
  <a-menu
    class="command-menu"
    :theme="menuTheme"
    mode="inline"
    :inline-collapsed="collapsed"
    :selectedKeys="selectedKeys"
    :defaultOpenKeys="defaultOpenKeys"
  >
    <template v-for="menuItem in menuList">
      <a-menu-item v-if="!menuItem.children" :key="menuItem.id">
        <router-link :to="menuItem.path">
          <a-icon :type="menuItem.icon" />
          <span>{{ menuItem.name }}</span>
        </router-link>
      </a-menu-item>
      <a-sub-menu v-else :key="menuItem.id">
        <template #title>
          <a-icon :type="menuItem.icon" />
          <span>{{ menuItem.name }}</span>
        </template>
        <a-menu-item v-for="subMenuItem in menuItem.children" :key="subMenuItem.id">
          <router-link :to="subMenuItem.path">
            <a-icon :type="subMenuItem.icon" />
            <span>{{ subMenuItem.name }}</span>
          </router-link>
        </a-menu-item>
      </a-sub-menu>
    </template>
  </a-menu>
</template>

<script>
export default {
  name: 'CommandMenu',
  props: {
    collapsed: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      menuList: [],
      selectedKeys: [],
      defaultOpenKeys: [],
      menuTheme: 'light'
    }
  },
  methods: {
    chooseMenu(route = this.$route) {
      const routerPath = route.path
      for (const menu of this.menuList) {
        if (menu.path && routerPath.startsWith(menu.path)) {
          this.selectedKeys = [menu.id]
          return
        }
        if (menu.children) {
          for (const child of menu.children) {
            if (child.path && routerPath.startsWith(child.path)) {
              this.selectedKeys = [child.id]
              if (!this.defaultOpenKeys.includes(menu.id)) {
                this.defaultOpenKeys.push(menu.id)
              }
              return
            }
          }
        }
      }
    }
  },
  watch: {
    $route(route) {
      this.chooseMenu(route)
    }
  },
  async mounted() {
    const { data } = await this.$api.getMenu()
    let id = 0
    for (const menu of data) {
      menu.id = ++id
      if (menu.children) {
        for (let i = 0; i < menu.children.length; i++) {
          menu.children[i].id = ++id
        }
      }
      this.menuList.push(menu)
    }
    this.chooseMenu()
  }
}
</script>

<style lang="less" scoped>
.command-menu {
  background: transparent;
  border-right: none;
  padding: 12px 8px;
  
  ::v-deep .ant-menu-item,
  ::v-deep .ant-menu-submenu-title {
    margin: 4px 0 !important;
    border-radius: 12px;
    height: 44px;
    line-height: 44px;
    transition: all 0.3s ease;
    color: #475569;
  }
  
  ::v-deep .ant-menu-item {
    padding-left: 24px !important;
    
    a {
      color: #475569;
      transition: color 0.3s;
    }
    
    &:hover {
      background: rgba(59, 130, 246, 0.08) !important;
      
      a {
        color: #3B82F6 !important;
      }
    }
  }
  
  ::v-deep .ant-menu-submenu-title {
    color: #475569;
    
    &:hover {
      color: #3B82F6 !important;
      background: rgba(59, 130, 246, 0.05) !important;
    }
  }
  
  ::v-deep .ant-menu-item-selected {
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(6, 182, 212, 0.08)) !important;
    border-right: 2px solid #3B82F6;
    
    a {
      color: #3B82F6 !important;
      font-weight: 500;
    }
  }
  
  ::v-deep .ant-menu-submenu-selected > .ant-menu-submenu-title {
    color: #3B82F6 !important;
    font-weight: 500;
  }
  
  ::v-deep .ant-menu-inline-collapsed {
    .ant-menu-item {
      padding: 0 !important;
      text-align: center;
      
      i {
        font-size: 20px;
      }
    }
    
    .ant-menu-submenu-title {
      padding: 0 !important;
      text-align: center;
      
      i {
        font-size: 20px;
      }
    }
  }
}
</style>