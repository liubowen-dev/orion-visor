<template>
  <div class="batch-exec-task-container">
    <!-- 左侧 -->
    <div class="task-left-container gray-box-shadow" :style="leftContainerStyle">
      <!-- 执行配置 -->
      <transition name="fade">
        <div class="exec-config-container" v-show="visibleCommand">
          <!-- 执行机器 -->
          <div class="exec-machine-checker-container">
            <span class="normal-label exec-label">执行机器</span>
            <div class="machine-checker-wrapper">
              <MachineChecker ref="machineChecker" :query="machineQuery">
                <template #trigger>
                  <span class="span-blue pointer">已选择 {{ config.machineIdList.length }} 台机器</span>
                </template>
                <template #footer>
                  <a-button type="primary" size="small" @click="chooseMachines">确定</a-button>
                </template>
              </MachineChecker>
            </div>
          </div>
          <!-- 执行命令 -->
          <div class="exec-command-container">
            <!-- lable 头 -->
            <div class="command-label-wrapper">
              <span class="normal-label exec-label">执行命令</span>
              <span class="span-blue pointer mr8" title="选择模板" @click="openTemplate">选择模板</span>
            </div>
            <!-- 编辑器 -->
            <div class="editor-wrapper">
              <Editor ref="editor" :value="config.command"/>
            </div>
          </div>
          <!-- 执行描述 -->
          <div class="exex-description-container">
            <span class="normal-label exec-label">执行描述</span>
            <div class="exec-description-wrapper flex-1">
              <a-input v-model="config.description" allowClear/>
            </div>
          </div>
          <!-- 执行按钮 -->
          <div class="exec-button-wrapper">
            <a-button type="primary" icon="caret-right" @click="execCommand">执行</a-button>
          </div>
        </div>
      </transition>
      <!-- 执行机器 -->
      <transition name="fade">
        <div class="exec-machine-menu-container" v-show="!visibleCommand">
          <!-- 重置 -->
          <div class="reset-button-wrapper">
            <div class="reset-button" title="重置" @click="reset">
              <a-icon type="reload"/>
            </div>
          </div>
          <!-- 机器列表 -->
          <a-menu mode="inline" v-model="selectedMachineKeys">
            <a-menu-item v-for="item of execMachines"
                         :key="item.execId"
                         :title="item.machineName"
                         @click="chooseExecMachine(item.execId)">
              <div class="machine-menu-item">
                <!-- 文本 -->
                <a-icon type="desktop"/>
                <span class="machine-menu-item-text">{{ item.machineName }}</span>
                <!-- 状态 -->
                <a-tag class="m0" :color="item.status | formatExecStatus('color')">
                  {{ item.status | formatExecStatus('label') }}
                </a-tag>
              </div>
            </a-menu-item>
          </a-menu>
        </div>
      </transition>
    </div>
    <!-- 右侧 -->
    <div class="exec-logger-container gray-box-shadow">
      <!-- 日志 -->
      <div v-if="runnable" class="logger-appender-container">
        <div v-for="execMachine in execMachines"
             v-show="execMachine.execId === selectedMachineKeys[0]"
             :key="execMachine.execId">
          <LogAppender :ref="'appender' + execMachine.execId"
                       height="calc(100vh - 124px)"
                       size="default"
                       :relId="execMachine.execId"
                       :tailType="FILE_TAIL_TYPE.EXEC_LOG.value"
                       :downloadType="FILE_DOWNLOAD_TYPE.EXEC_LOG.value">
            <!-- 左侧工具栏 -->
            <template #left-tools>
              <div class="appender-left-tools">
                <!-- 命令输入 -->
                <a-input-search class="command-write-input"
                                v-if="BATCH_EXEC_STATUS.RUNNABLE.value === execMachine.status"
                                v-model="execMachine.inputCommand"
                                placeholder="输入"
                                @search="sendCommand(execMachine)">
                  <template #enterButton>
                    <a-icon type="forward"/>
                  </template>
                  <!-- 发送 lf -->
                  <template #suffix>
                    <a-icon :class="{'send-lf-trigger': true, 'send-lf-trigger-enable': config.sendLf}"
                            title="是否发送 \n"
                            type="pull-request"
                            @click="() => config.sendLf = !config.sendLf"/>
                  </template>
                </a-input-search>
                <!-- 停止 -->
                <a-popconfirm v-if="BATCH_EXEC_STATUS.RUNNABLE.value === execMachine.status"
                              title="是否要停止执行?"
                              placement="bottomLeft"
                              ok-text="确定"
                              cancel-text="取消"
                              @confirm="terminate(execMachine)">
                  <a-button icon="close">停止</a-button>
                </a-popconfirm>
                <!-- used -->
                <span class="mr8 nowrap" title="用时"
                      v-if="execMachine.keepTime && BATCH_EXEC_STATUS.COMPLETE.value === execMachine.status">
                  {{ `${execMachine.keepTime} (${execMachine.used}ms)` }}
                </span>
                <!-- exitCode -->
                <span class="mr8" title="退出码"
                      v-if="execMachine.exitCode !== null"
                      :style="{color: execMachine.exitCode === 0 ? '#4263EB' : '#E03131'}">
                  {{ execMachine.exitCode }}
                </span>
              </div>
            </template>
          </LogAppender>
        </div>
      </div>
      <!-- 空 -->
      <div v-else class="logger-empty-container">
        <a-empty description="执行命令以查看日志"/>
      </div>
    </div>
    <!-- 事件 -->
    <div class="exec-event-container">
      <TemplateSelector ref="templateSelector" @selected="selectedTemplate"/>
    </div>
  </div>
</template>

<script>
import { enumValueOf, BATCH_EXEC_STATUS, ENABLE_STATUS, FILE_DOWNLOAD_TYPE, FILE_TAIL_TYPE } from '@/lib/enum'
import MachineChecker from '@/components/machine/MachineChecker'
import Editor from '@/components/editor/Editor'
import TemplateSelector from '@/components/content/TemplateSelector'
import LogAppender from '@/components/log/LogAppender'

export default {
  name: 'BatchExecTask',
  components: {
    LogAppender,
    TemplateSelector,
    Editor,
    MachineChecker
  },
  computed: {
    leftContainerStyle() {
      return {
        width: this.runnable ? '240px' : '50%',
        padding: this.visibleCommand ? '16px 20px 20px 20px' : '0'
      }
    }
  },
  data() {
    return {
      BATCH_EXEC_STATUS,
      FILE_TAIL_TYPE,
      FILE_DOWNLOAD_TYPE,
      runnable: false,
      visibleCommand: true,
      pollId: null,
      config: {
        machineIdList: [],
        command: '',
        sendLf: true,
        description: undefined
      },
      machineQuery: {
        status: ENABLE_STATUS.ENABLE.value
      },
      execMachines: [],
      selectedMachineKeys: [-1]
    }
  },
  methods: {
    chooseMachines() {
      const ref = this.$refs.machineChecker
      if (!ref.checkedList.length) {
        this.$message.warning('请选择执行的机器')
        return
      }
      this.config.machineIdList = ref.checkedList
      ref.hide()
    },
    openTemplate() {
      this.$refs.templateSelector.open()
    },
    selectedTemplate(command) {
      this.config.command = command
      this.$refs.templateSelector.close()
    },
    execCommand() {
      if (!this.config.machineIdList.length) {
        this.$message.warn('请选择执行机器')
        return
      }
      const command = this.$refs.editor.getValue()
      if (!command.trim().length) {
        this.$message.warn('请输入执行命令')
        return
      }
      if (command.length > 2048) {
        this.$message.warn(`执行命令不能超过2048位 当前${command.length}位`)
        return
      }
      this.config.command = command
      // 执行
      this.$api.submitExecTask({
        ...this.config
      }).then(({ data }) => {
        for (const exec of data) {
          exec.inputCommand = null
          exec.status = BATCH_EXEC_STATUS.WAITING.value
          exec.exitCode = null
          exec.used = null
        }
        this.execMachines = data
        this.selectedMachineKeys[0] = this.execMachines[0].execId
        // 轮询状态
        this.pollId = setInterval(this.pollExecStatus, 2000)
      }).then(() => {
        // 打开日志
        setTimeout(() => {
          this.execMachines.forEach(m => {
            this.$refs['appender' + m.execId][0].openTail()
          })
        }, 300)
        this.runnable = true
        this.visibleCommand = false
      }).catch(() => {
        this.runnable = false
        this.visibleCommand = true
      })
    },
    chooseExecMachine(k) {
      setTimeout(() => {
        this.$nextTick(() => this.$refs['appender' + k][0].fitTerminal())
      }, 300)
    },
    pollExecStatus() {
      const idList = this.execMachines.filter(s =>
        s.status === BATCH_EXEC_STATUS.WAITING.value ||
        s.status === BATCH_EXEC_STATUS.RUNNABLE.value
      ).map(s => s.execId)
      if (!idList.length) {
        return
      }
      this.$api.getExecTaskStatus({
        idList
      }).then(({ data }) => {
        for (const status of data) {
          this.execMachines.filter(s => s.execId === status.id).forEach(s => {
            s.status = status.status
            s.exitCode = status.exitCode
            s.used = status.used
            s.keepTime = status.keepTime
          })
        }
      })
    },
    reset() {
      // 关闭轮询
      if (this.pollId) {
        clearInterval(this.pollId)
        this.pollId = null
      }
      // 关闭日志
      this.execMachines.forEach(m => {
        this.$refs['appender' + m.execId][0].dispose()
      })
      this.runnable = false
      this.visibleCommand = true
      this.execMachines = []
    },
    sendCommand(execMachine) {
      const id = execMachine.execId
      let command = execMachine.inputCommand || ''
      if (this.config.sendLf) {
        command += '\n'
      }
      if (!command) {
        return
      }
      this.$api.writeExecTask({
        id,
        command
      }).then(() => {
        execMachine.inputCommand = null
      })
    },
    terminate(execMachine) {
      execMachine.status = BATCH_EXEC_STATUS.TERMINATED.value
      this.$api.terminateExecTask({
        id: execMachine.execId
      }).then(() => {
        this.$message.success('已停止')
      })
    }
  },
  filters: {
    formatExecStatus(status, f) {
      return enumValueOf(BATCH_EXEC_STATUS, status)[f]
    }
  },
  beforeDestroy() {
    this.execMachines.forEach(m => {
      const refs = this.$refs['appender' + m.execId]
      refs && refs[0] && refs[0].dispose()
    })
    this.pollId !== null && clearInterval(this.pollId)
    this.pollId = null
  }
}
</script>

<style lang="less" scoped>
.batch-exec-task-container {
  display: flex;
  height: calc(100vh - 84px);

  .task-left-container {
    transition: width 0.3s ease, padding 0.3s ease;
    background: #FFFFFF;
    border-radius: 16px;
    margin-right: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

    .exec-config-container {
      .exec-machine-checker-container {
        display: flex;
        align-items: center;
        margin-bottom: 20px;
        height: auto;
        
        .machine-checker-wrapper {
          flex: 1;
          
          .span-blue {
            display: inline-flex;
            align-items: center;
            padding: 8px 16px;
            background: #EFF6FF;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 500;
            transition: all 0.2s ease;
            
            &:hover {
              background: #E0F2FE;
              transform: scale(1.02);
            }
          }
        }
      }

      .exec-command-container {
        margin-bottom: 20px;

        .command-label-wrapper {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
          
          .span-blue {
            padding: 4px 12px;
            background: #F1F5F9;
            border-radius: 16px;
            font-size: 12px;
            transition: all 0.2s;
            
            &:hover {
              background: #EFF6FF;
              color: #3B82F6;
            }
          }
        }

        .editor-wrapper {
          height: calc(100vh - 300px);
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          overflow: hidden;
          
          ::v-deep .monaco-editor {
            border-radius: 12px;
          }
        }
      }

      .exex-description-container {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 20px;
        
        .exec-label {
          white-space: nowrap;
        }
        
        .exec-description-wrapper {
          flex: 1;
          
          .ant-input {
            border-radius: 10px;
            border-color: #E2E8F0;
            transition: all 0.2s;
            
            &:focus {
              border-color: #3B82F6;
              box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
            }
          }
        }
      }

      .exec-button-wrapper {
        text-align: right;
        
        .ant-btn-primary {
          border-radius: 10px;
          padding: 6px 24px;
          height: auto;
          transition: all 0.2s;
          
          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
          }
        }
      }
    }

    .exec-machine-menu-container {
      .reset-button-wrapper {
        height: 40px;
        margin: 12px 12px 8px 12px;
        display: flex;
        background: linear-gradient(135deg, #3B82F6, #06B6D4);
        border-radius: 10px;
        overflow: hidden;

        .reset-button {
          width: 100%;
          height: 100%;
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.3s ease;
          cursor: pointer;
          gap: 8px;

          i {
            font-size: 18px;
            transition: transform 0.2s;
          }
          
          span {
            font-size: 13px;
            font-weight: 500;
          }
        }

        .reset-button:hover {
          background: linear-gradient(135deg, #2563EB, #0891B2);
          
          i {
            transform: rotate(180deg);
          }
        }
      }

      ::v-deep .ant-menu {
        background: transparent;
        border-right: none;
      }

      ::v-deep .ant-menu-item {
        margin: 4px 8px !important;
        border-radius: 12px;
        padding: 0 12px !important;
        transition: all 0.2s ease;
        
        &:hover {
          background: #F8FAFF;
          
          .machine-menu-item-text {
            color: #3B82F6;
          }
        }
      }
      
      ::v-deep .ant-menu-item-selected {
        background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(6, 182, 212, 0.08));
        
        .machine-menu-item-text {
          color: #3B82F6;
          font-weight: 500;
        }
      }

      .machine-menu-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 8px 0;

        i {
          font-size: 16px;
          color: #64748B;
        }

        .machine-menu-item-text {
          flex: 1;
          font-size: 14px;
          font-weight: 500;
          color: #1E293B;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .ant-tag {
          border-radius: 12px;
          padding: 2px 8px;
          font-size: 11px;
          font-weight: 500;
          border: none;
        }
      }
    }
  }

  .exec-logger-container {
    width: calc(100% - 260px);
    background: #FFFFFF;
    border-radius: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    overflow: hidden;

    .appender-left-tools {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .command-write-input {
        width: 280px;
        
        ::v-deep .ant-input {
          border-radius: 20px;
        }
        
        ::v-deep .ant-input-group-addon {
          border-radius: 0 20px 20px 0;
        }
      }
      
      .ant-btn {
        border-radius: 20px;
      }
      
      .send-lf-trigger {
        cursor: pointer;
        transition: all 0.2s;
        
        &.send-lf-trigger-enable {
          color: #3B82F6;
        }
      }
    }

    .logger-empty-container {
      margin-top: 25%;
    }
  }

  .exec-label {
    font-weight: 600;
    font-size: 14px;
    color: #1E293B;
  }
}

// 动画过渡
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}

// 滚动条样式
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #F1F5F9;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 3px;
  
  &:hover {
    background: #94A3B8;
  }
}
</style>