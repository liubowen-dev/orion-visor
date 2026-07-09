<template>
  <a-layout id="sftp-layout-container">
    <!-- 机器列表 -->
    <MachineListMenu ref="machineList"
                     theme="light"
                     :selectedMachine="selectedMachine"
                     :query="machineQuery"
                     @chooseMachine="changeSftpMain"/>
    <!-- main -->
    <a-layout>
      <a-layout-content id="sftp-content-fixed-right">
        <!-- 面包屑 -->
        <div class="sftp-header">
          <div class="sftp-breadcrumb">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <a-tag color="#3B82F6" class="machine-tag">
                  <a-icon type="cloud-server" />
                  {{ currentMachine.name }}
                </a-tag>
              </a-breadcrumb-item>
              <a-breadcrumb-item>
                <a-tag color="#06B6D4" class="host-tag">
                  <a-icon type="environment" />
                  {{ currentMachine.host }}
                </a-tag>
              </a-breadcrumb-item>
            </a-breadcrumb>
          </div>
          <div class="sftp-actions">
            <a-button size="small" @click="refreshCurrentPath" title="刷新">
              <a-icon type="reload" />
            </a-button>
            <a-button size="small" @click="goToParent" title="返回上级">
              <a-icon type="arrow-up" />
            </a-button>
          </div>
        </div>
        <!-- sftp -->
        <MachineSftpMain ref="sftpMain"
                         :machineId="machineId"
                         :leftFolderDefaultVisible="true"
                         :visibleRightMenu="true"
                         :setContainerShadow="true"
                         @opened="sftpOpened"/>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { defineArrayKey } from '@/lib/utils'
import { ENABLE_STATUS } from '@/lib/enum'
import MachineListMenu from '@/components/machine/MachineListMenu'
import MachineSftpMain from '@/components/sftp/MachineSftpMain'

export default {
  name: 'MachineSftp',
  components: {
    MachineSftpMain,
    MachineListMenu
  },
  data() {
    return {
      selectedMachine: [],
      currentMachine: {},
      initMachineId: null,
      machineId: null,
      machineQuery: {
        status: ENABLE_STATUS.ENABLE.value
      }
    }
  },
  methods: {
    changeSftpMain(id) {
      if (this.machineId === id) {
        return
      }
      const filterMachines = this.$refs.machineList.list.filter(m => m.id === id)
      if (filterMachines.length) {
        this.currentMachine = filterMachines[0]
      } else {
        this.$message.error('机器未启用或不存在')
        return
      }
      this.machineId = id
      this.$nextTick(() => {
        const session = this.currentMachine.session
        if (session) {
          this.$refs.sftpMain.changeToken(session)
        } else {
          this.$refs.sftpMain.openSftp(this.currentMachine.name)
        }
      })
    },
    refreshCurrentPath() {
      if (this.$refs.sftpMain && this.$refs.sftpMain.refreshCurrentPath) {
        this.$refs.sftpMain.refreshCurrentPath()
      }
    },
    goToParent() {
      if (this.$refs.sftpMain && this.$refs.sftpMain.goToParent) {
        this.$refs.sftpMain.goToParent()
      }
    },
    sftpOpened(machineId, session) {
      const filterMachines = this.$refs.machineList.list.filter(m => m.id === machineId)
      if (filterMachines.length) {
        filterMachines[0].session = session
      }
    }
  },
  created() {
    if (this.$route.params.id) {
      this.initMachineId = parseInt(this.$route.params.id)
    } else {
      this.initMachineId = 1
    }
    this.selectedMachine = [this.initMachineId]
  },
  async mounted() {
    await this.$refs.machineList.getMachineList()
    defineArrayKey(this.$refs.machineList.list, 'session')
    this.changeSftpMain(this.initMachineId)
  }
}
</script>

<style lang="less" scoped>
#sftp-layout-container {
  height: 100vh;
  background: #F5F7FA;
}

#sftp-content-fixed-right {
  overflow: auto;
  padding: 20px;
  
  .sftp-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    background: white;
    padding: 12px 20px;
    border-radius: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    
    .sftp-breadcrumb {
      flex: 1;
      
      .machine-tag,
      .host-tag {
        padding: 6px 14px;
        border-radius: 20px;
        font-size: 13px;
        font-weight: 500;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        
        i {
          font-size: 14px;
        }
      }
    }
    
    .sftp-actions {
      display: flex;
      gap: 8px;
      
      .ant-btn {
        border-radius: 8px;
        color: #64748B;
        
        &:hover {
          color: #3B82F6;
          border-color: #3B82F6;
        }
      }
    }
  }
}

::v-deep .sftp-folder-left-fixed {
  height: calc(100vh - 140px);
  max-height: calc(100vh - 140px);
}
</style>