<template>
  <a-menu class="main-menu"
          theme="light"
          mode="inline"
          :selectedKeys="selectedKeys"
          :defaultOpenKeys="defaultOpenKeys">
    <template v-for="menuItem in filteredMenuList">
      <!-- 一级菜单 -->
      <a-menu-item v-if="!menuItem.children" :key="menuItem.id">
        <router-link :to="menuItem.path">
          <a-icon :type="menuItem.icon"/>
          <span>{{ menuItem.name }}</span>
        </router-link>
      </a-menu-item>
      <!-- 二级菜单 -->
      <a-sub-menu v-else :key="menuItem.id">
        <template #title>
          <a-icon :type="menuItem.icon"/>
          <span class="usn">{{ menuItem.name }}</span>
        </template>
        <a-menu-item v-for="subMenuItem in menuItem.children" :key="subMenuItem.id">
          <router-link :to="subMenuItem.path">
            <a-icon :type="subMenuItem.icon"/>
            <span>{{ subMenuItem.name }}</span>
          </router-link>
        </a-menu-item>
      </a-sub-menu>
    </template>
  </a-menu>
</template>

<script>
export default {
  name: 'Menu',
  data() {
    return {
      menuList: [],
      selectedKeys: [],
      defaultOpenKeys: []
    }
  },
  computed: {
    // 过滤菜单，移除"终端会话"
    filteredMenuList() {
      // 要过滤掉的菜单名称列表（可扩展）
      const excludeMenuNames = ['终端会话']
      
      return this.menuList
        .map(menu => {
          // 处理二级菜单
          if (menu.children) {
            const filteredChildren = menu.children.filter(
              child => !excludeMenuNames.includes(child.name)
            )
            // 如果过滤后还有子菜单，返回过滤后的菜单
            if (filteredChildren.length > 0) {
              return {
                ...menu,
                children: filteredChildren
              }
            }
            // 如果没有子菜单了，返回 null（后面会过滤掉）
            return null
          }
          // 一级菜单直接返回
          return menu
        })
        .filter(menu => menu !== null) // 移除为 null 的菜单
    }
  },
  methods: {
    chooseMenu(route = this.$route) {
      const routerPath = route.path
      for (const menu of this.filteredMenuList) {
        if (menu.path && routerPath.startsWith(menu.path)) {
          this.selectedKeys[0] = menu.id
          this.$forceUpdate()
          return
        }
        if (menu.children) {
          for (const child of menu.children) {
            if (child.path && routerPath.startsWith(child.path)) {
              let present = false
              for (const defaultOpenKey of this.defaultOpenKeys) {
                if (defaultOpenKey === menu.id) {
                  present = true
                  break
                }
              }
              this.selectedKeys[0] = child.id
              if (!present) {
                this.defaultOpenKeys.push(menu.id)
              }
              this.$forceUpdate()
              return
            }
          }
        }
      }
    }
  },
  async mounted() {
    this.$api.getMenu().then(({ data }) => {
      let id = 0
      for (const menu of data) {
        menu.id = ++id
        const children = menu.children
        if (children) {
          for (let i = 0; i < children.length; i++) {
            children[i].id = ++id
          }
        }
        this.menuList.push(menu)
      }
      this.chooseMenu()
    })
  }
}
</script>

<style scoped>
.main-menu {
  background: transparent;
  border-right: none;
  min-height: 100%;
  padding: 16px 10px 28px;
}

::v-deep .ant-menu-item,
::v-deep .ant-menu-submenu-title {
  margin: 4px 0 !important;
  border-radius: 12px;
  height: 44px;
  line-height: 44px;
}

::v-deep .ant-menu-item > a,
::v-deep .ant-menu-submenu-title {
  color: #475569;
  transition: all 0.2s;
}

::v-deep .ant-menu-inline .ant-menu-item::after {
  display: none;
}

::v-deep .ant-menu-item:hover {
  background: rgba(59, 130, 246, 0.08) !important;
}

::v-deep .ant-menu-item:hover > a {
  color: #3B82F6 !important;
}

::v-deep .ant-menu-submenu-title:hover {
  color: #3B82F6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
}

::v-deep .ant-menu-item-selected {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(6, 182, 212, 0.08)) !important;
  border-right: 2px solid #3B82F6;
}

::v-deep .ant-menu-item-selected > a,
::v-deep .ant-menu-item-selected > a:hover {
  color: #3B82F6 !important;
  font-weight: 500;
}

::v-deep .ant-menu-submenu-selected > .ant-menu-submenu-title {
  color: #3B82F6 !important;
  font-weight: 500;
}
</style>