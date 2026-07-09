<!-- src/components/common/AiAssistant.vue -->
<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮 -->
    <div class="ai-float-btn" @click="openDrawer" :class="{ active: drawerVisible }">
      <a-icon type="robot" />
      <span class="pulse-ring"></span>
    </div>

    <!-- AI 助手抽屉 -->
    <a-drawer
      :visible="drawerVisible"
      :closable="false"
      placement="right"
      :width="420"
      :maskClosable="true"
      @close="closeDrawer"
      :bodyStyle="{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }"
    >
      <div class="ai-drawer">
        <!-- 头部 -->
        <div class="ai-header">
          <div class="ai-header-left">
            <div class="ai-logo">
              <a-icon type="robot" />
            </div>
            <div class="ai-info">
              <span class="ai-name">AI 运维助手</span>
              <span class="ai-status">在线</span>
            </div>
          </div>
          <div class="ai-header-right">
            <a-icon type="clear" class="clear-icon" title="清空对话" @click="clearMessages" />
            <a-icon type="close" class="close-icon" @click="closeDrawer" />
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="ai-messages" ref="messagesContainer">
          <div v-for="(msg, idx) in messages" :key="idx" class="message-item" :class="msg.role">
            <div class="message-avatar">
              <a-icon :type="msg.role === 'user' ? 'user' : 'robot'" />
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div class="message-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="loading" class="message-item assistant">
            <div class="message-avatar">
              <a-icon type="robot" />
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="ai-suggestions" v-if="suggestions.length && !loading">
          <div class="suggestions-title">快捷提问</div>
          <div class="suggestions-list">
            <a-tag 
              v-for="(s, idx) in suggestions" 
              :key="idx"
              class="suggestion-tag"
              @click="sendMessage(s)"
            >
              {{ s }}
            </a-tag>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="ai-input-area">
          <a-textarea
            v-model="inputMessage"
            :autoSize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入您的问题，AI 助手会帮您解答运维相关问题..."
            @pressEnter="handleSend"
            :disabled="loading"
          />
          <a-button 
            type="primary" 
            shape="circle" 
            :loading="loading"
            @click="sendMessage"
            class="send-btn"
          >
            <a-icon type="arrow-up" />
          </a-button>
        </div>

        <!-- 底部提示 -->
        <div class="ai-footer">
          <span>Powered by DeepSeek</span>
          <span>内容由 AI 生成，仅供参考</span>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script>
import dayjs from 'dayjs'

export default {
  name: 'AiAssistant',
  data() {
    return {
      drawerVisible: false,
      loading: false,
      inputMessage: '',
      messages: [],
      suggestions: [
        '如何查看服务器状态？',
        '怎么批量执行命令？',
        '如何配置告警规则？',
        '帮我分析一下系统负载',
        '最近有哪些异常告警？',
        '如何优化部署流程？'
      ]
    }
  },
  methods: {
    openDrawer() {
      this.drawerVisible = true
      if (this.messages.length === 0) {
        this.addWelcomeMessage()
      }
      this.scrollToBottom()
    },
    closeDrawer() {
      this.drawerVisible = false
    },
    addWelcomeMessage() {
      this.messages.push({
        role: 'assistant',
        content: '你好！我是 AI 运维助手，基于 DeepSeek 模型。我可以帮助你：<br><br>📊 查询系统状态<br>🔧 执行运维操作<br>📝 分析日志问题<br>💡 提供优化建议<br><br>有什么我可以帮你的吗？',
        time: dayjs().format('HH:mm')
      })
    },
    async sendMessage(text) {
      const message = text || this.inputMessage
      if (!message.trim() || this.loading) return
      
      // 添加用户消息
      this.messages.push({
        role: 'user',
        content: message,
        time: dayjs().format('HH:mm')
      })
      this.inputMessage = ''
      this.scrollToBottom()
      
      // 调用 API
      this.loading = true
      try {
        const response = await this.callDeepSeek(message)
        this.messages.push({
          role: 'assistant',
          content: this.formatResponse(response),
          time: dayjs().format('HH:mm')
        })
      } catch (error) {
        console.error('API Error:', error)
        this.messages.push({
          role: 'assistant',
          content: '抱歉，我遇到了一些问题，请稍后再试。',
          time: dayjs().format('HH:mm')
        })
      } finally {
        this.loading = false
        this.scrollToBottom()
      }
    },
    async callDeepSeek(userMessage) {
      // ========== 获取场景信息 ==========
      const currentRoute = this.$route
      const currentPath = currentRoute.path
      const currentRouteName = currentRoute.name
      
      // 获取机器 ID（从路由参数中获取）
      let currentMachineId = null
      if (currentRoute.params.machineId) {
        currentMachineId = currentRoute.params.machineId
      } else if (currentRoute.params.id) {
        currentMachineId = currentRoute.params.id
      }
      
      // 构建消息历史（只保留用户和助手的对话，去掉欢迎消息）
      const historyMessages = this.messages
        .filter(m => m.content !== this.messages[0]?.content)
        .map(m => ({
          role: m.role,
          content: m.content
        }))
      
      // ========== 调用后端 API（把场景字段单独传） ==========
      const response = await this.$api.aiChat({
        messages: historyMessages,
        currentRouteName: currentRouteName,
        currentPath: currentPath,
        currentMachineId: currentMachineId
      })
      
      // 处理响应
      if (response.data && response.data.content) {
        return response.data.content
      } else if (response.content) {
        return response.content
      } else {
        console.error('Unexpected response format:', response)
        return '抱歉，响应格式异常。'
      }
    },
    formatResponse(content) {
      if (!content) return ''
      return content
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br>')
    },
    formatMessage(content) {
      return content
    },
    clearMessages() {
      this.messages = []
      this.addWelcomeMessage()
      this.scrollToBottom()
    },
    handleSend(e) {
      if (e.shiftKey) return
      e.preventDefault()
      this.sendMessage()
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.ai-assistant {
  position: fixed;
  bottom: 80px;
  right: 24px;
  z-index: 1000;
}

.ai-float-btn {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #3B82F6, #06B6D4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
  position: relative;
  
  i {
    font-size: 28px;
    color: white;
  }
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
  }
  
  &.active {
    background: linear-gradient(135deg, #EF4444, #F97316);
  }
  
  .pulse-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: rgba(59, 130, 246, 0.4);
    animation: pulse 1.5s infinite;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

.ai-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #F8FAFF;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #F1F5F9;
  
  .ai-header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .ai-logo {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #3B82F6, #06B6D4);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      i {
        font-size: 22px;
        color: white;
      }
    }
    
    .ai-info {
      .ai-name {
        font-size: 16px;
        font-weight: 600;
        color: #0F172A;
        display: block;
      }
      
      .ai-status {
        font-size: 11px;
        color: #10B981;
        display: flex;
        align-items: center;
        gap: 4px;
        
        &::before {
          content: '';
          width: 6px;
          height: 6px;
          background: #10B981;
          border-radius: 50%;
          display: inline-block;
        }
      }
    }
  }
  
  .ai-header-right {
    display: flex;
    gap: 16px;
    
    i {
      font-size: 18px;
      color: #94A3B8;
      cursor: pointer;
      transition: color 0.2s;
      
      &:hover {
        color: #3B82F6;
      }
    }
  }
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  
  .message-item {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    
    &.user {
      flex-direction: row-reverse;
      
      .message-avatar {
        background: #3B82F6;
      }
      
      .message-content {
        background: #EFF6FF;
        border-radius: 18px 18px 4px 18px;
      }
    }
    
    &.assistant {
      .message-avatar {
        background: #F1F5F9;
        
        i {
          color: #3B82F6;
        }
      }
      
      .message-content {
        background: white;
        border-radius: 18px 18px 18px 4px;
      }
    }
    
    .message-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      
      i {
        font-size: 16px;
        color: white;
      }
    }
    
    .message-content {
      max-width: 80%;
      padding: 12px 16px;
      
      .message-text {
        font-size: 14px;
        line-height: 1.6;
        color: #1E293B;
        
        ::v-deep code {
          background: #F1F5F9;
          padding: 2px 6px;
          border-radius: 4px;
          font-family: monospace;
          font-size: 12px;
        }
        
        ::v-deep strong {
          color: #3B82F6;
        }
      }
      
      .message-time {
        font-size: 10px;
        color: #94A3B8;
        margin-top: 6px;
      }
    }
  }
  
  .typing-indicator {
    display: flex;
    gap: 4px;
    padding: 8px 0;
    
    span {
      width: 8px;
      height: 8px;
      background: #94A3B8;
      border-radius: 50%;
      animation: typing 1.4s infinite ease-in-out;
      
      &:nth-child(1) { animation-delay: 0s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.ai-suggestions {
  padding: 12px 20px;
  border-top: 1px solid #F1F5F9;
  background: white;
  
  .suggestions-title {
    font-size: 12px;
    color: #64748B;
    margin-bottom: 10px;
  }
  
  .suggestions-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .suggestion-tag {
      cursor: pointer;
      background: #F8FAFF;
      border: 1px solid #E2E8F0;
      border-radius: 20px;
      padding: 6px 14px;
      font-size: 12px;
      transition: all 0.2s;
      
      &:hover {
        background: #EFF6FF;
        border-color: #3B82F6;
        color: #3B82F6;
      }
    }
  }
}

.ai-input-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #F1F5F9;
  
  ::v-deep .ant-input {
    flex: 1;
    border-radius: 24px;
    padding: 10px 16px;
    border-color: #E2E8F0;
    
    &:focus {
      border-color: #3B82F6;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
    }
  }
  
  .send-btn {
    width: 40px;
    height: 40px;
    background: #3B82F6;
    border: none;
    box-shadow: none;
    
    &:hover {
      background: #2563EB;
      transform: scale(1.02);
    }
  }
}

.ai-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 12px;
  background: #F8FAFF;
  font-size: 10px;
  color: #94A3B8;
  border-top: 1px solid #F1F5F9;
}
</style>