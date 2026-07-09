<template>
  <div class="web-side-message-container pointer" @click="onOpen" title="消息">
    <a class="icon-wrapper">
      <a-badge class="unread-message-dot" :dot="unreadCount > 0">
        <a-icon class="web-side-message-trigger" type="notification"/>
      </a-badge>
    </a>
    <a-drawer :title="null"
              placement="right"
              :closable="false"
              :width="400"
              :maskStyle="{opacity: 0, animation: 'none'}"
              :bodyStyle="{padding: 0}"
              :maskClosable="true"
              :visible="visible"
              :afterVisibleChange="visibleChange"
              @close="onClose">
      <div class="message-header-container">
        <div class="message-header-left">
          <a-dropdown :trigger="['click']">
            <a class="message-block-item message-status-trigger" @click="e => e.preventDefault()">
              <a-icon :type="messageStatus.icon"/>
              <span>{{ messageStatus.label }}</span>
              <a-icon type="down"/>
            </a>
            <a-menu slot="overlay">
              <a-menu-item v-for="item of messageStatusList" :key="item.label">
                <span class="header-dropdown-item message-type-dropdown-item" @click="changeStatus(item)">
                 <a-icon :type="item.icon"/>
                 <span>{{ item.label }}</span>
                </span>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </div>
        <div class="message-header-right">
          <a-dropdown :trigger="['click']">
            <a class="message-block-item" @click="e => e.preventDefault()">
              <a-icon class="message-action-trigger" type="ellipsis"/>
            </a>
            <a-menu slot="overlay">
              <a-menu-item key="1">
                <span class="header-dropdown-item message-action-dropdown-item" @click="readAllMessage()">
                 <a-icon type="check"/>
                 <span>标记所有消息为已读</span>
                </span>
              </a-menu-item>
              <a-menu-item key="2">
                <span class="header-dropdown-item message-action-dropdown-item" @click="deleteAllMessage()">
                 <a-icon type="delete"/>
                 <span>删除所有已读消息</span>
                </span>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <a class="message-block-item" @click="onClose" title="关闭">
            <a-icon class="message-action-trigger" type="close"/>
          </a>
        </div>
      </div>
      <div class="message-list-container">
        <a-spin :spinning="loading">
          <template v-if="rows.length">
            <div class="message-wrapper" v-for="row of rows"
                 :key="row.id"
                 @click="clickMessage(row)"
                 @mouseenter="addMessageActive(row)"
                 @mouseleave="removeMessageActive(row)">
              <div class="message-top-wrapper">
                <div class="message-top-left">
                  <a-icon class="message-classify-icon" :type="row.classify | formatMessageClassify('icon')"/>
                  <div class="message-classify-text-wrapper">
                    <span class="message-classify-label">{{ row.classify | formatMessageClassify('label') }}</span>
                    <span class="message-classify-divider">/</span>
                    <span class="message-type-label">{{ row.type | formatMessageType('label') }}</span>
                    <a-badge v-if="row.status === READ_STATUS.UNREAD.value" class="message-unread-dot" status="error"/>
                  </div>
                </div>
                <div class="message-top-right" v-show="row.visibleTools">
                  <a-icon v-if="row.status === READ_STATUS.UNREAD.value"
                          type="check"
                          title="已读"
                          @click.stop="readMessage(row)"/>
                  <a-icon type="close" title="删除" @click.stop="deleteMessage(row.id)"/>
                </div>
              </div>
              <div class="message-text-wrapper">
                <a-icon class="message-text-icon" type="bell" theme="twoTone"/>
                <span class="message-text" v-html="row.message"/>
              </div>
              <div class="message-date-wrapper">
                {{ row.createTime }}
              </div>
            </div>
          </template>
          <template v-else>
            <div class="message-empty-wrapper">
              <a-empty description="暂无消息"/>
            </div>
          </template>
          <div class="load-more-wrapper">
            <a-button v-if="hasMore" type="link" @click="loadMoreMessage">加载更多</a-button>
          </div>
        </a-spin>
      </div>
    </a-drawer>
  </div>
</template>

<script>
import { getUUID, clearStainKeywords, dateFormat, replaceStainKeywords } from '@/lib/utils'
import { enumValueOf, MESSAGE_CLASSIFY, MESSAGE_TYPE, READ_STATUS } from '@/lib/enum'

const messageStatusList = [{
  status: READ_STATUS.UNREAD.value,
  label: '未读消息',
  icon: 'tag'
}, {
  status: undefined,
  label: '全部消息',
  icon: 'tags'
}]

export default {
  name: 'WebSideMessageDrawer',
  data() {
    return {
      visible: false,
      loading: false,
      unreadCount: 0,
      limit: 30,
      pollMaxId: null,
      minId: null,
      rows: [],
      pollId: null,
      messageStatus: messageStatusList[0],
      messageStatusList,
      READ_STATUS,
      hasMore: false
    }
  },
  methods: {
    onOpen() { this.visible = true },
    onClose() { this.visible = false },
    changeStatus(status) {
      this.messageStatus = status
      this.getMessages()
    },
    readAllMessage() {
      this.$api.setWebSideMessageAllRead().then(() => {
        this.rows.forEach(row => row.status = READ_STATUS.READ.value)
        this.$message.success('操作成功')
      })
    },
    deleteAllMessage() {
      this.$api.deleteAllReadMessage().then(() => {
        this.rows = this.rows.filter(row => row.status === READ_STATUS.UNREAD.value)
        this.$message.success('操作成功')
      })
    },
    readMessage(row) {
      this.$api.setMessageRead({ idList: [row.id] }).then(() => row.status = READ_STATUS.READ.value)
    },
    deleteMessage(id) {
      this.$api.deleteWebSideMessage({ idList: [id] }).then(() => {
        for (let i = 0; i < this.rows.length; i++) {
          if (this.rows[i].id === id) {
            this.rows.splice(i, 1)
            return
          }
        }
      })
    },
    clickMessage(row) {
      this.$router.push(enumValueOf(MESSAGE_TYPE, row.type).redirect(row))
      this.onClose()
      if (row.status === READ_STATUS.READ.value) return
      row.status = READ_STATUS.READ.value
      this.$api.setMessageRead({ idList: [row.id] })
    },
    addMessageActive(row) { row.visibleTools = true; this.$forceUpdate() },
    removeMessageActive(row) { row.visibleTools = false; this.$forceUpdate() },
    visibleChange(e) { if (e) this.loadNewMessage() },
    getMessages() {
      this.$api.getWebSideMessageList({ status: this.messageStatus.status, limit: this.limit }).then(({ data }) => {
        this.rows = data.rows || []
        this.rows.forEach(this.processMessage)
        this.hasMore = this.rows.length === this.limit
        this.loading = false
      }).then(() => this.loading = false)
    },
    loadNewMessage() {
      this.$api.getNewMessage({ maxId: this.rows.length ? this.rows[0].id : null, status: this.messageStatus.status })
        .then(({ data }) => {
          this.unreadCount = data.unreadCount
          const newMessages = data.newMessages
          if (newMessages && newMessages.length) {
            newMessages.forEach(this.processMessage)
            newMessages.forEach(row => this.rows.unshift(row))
          }
        })
    },
    loadMoreMessage() {
      this.loading = true
      this.$api.getMoreMessage({ maxId: this.rows[this.rows.length - 1].id, status: this.messageStatus.status, limit: this.limit })
        .then(({ data }) => {
          this.loading = false
          const length = data.length
          this.hasMore = length === this.limit
          if (!length) return
          data.forEach(this.processMessage)
          data.forEach(row => this.rows.push(row))
        }).then(() => this.loading = false)
    },
    processMessage(row) {
      row.createTime = dateFormat(new Date(row.createTime), 'MM月dd日 HH:mm:ss')
      row.message = replaceStainKeywords(row.message)
      row.visibleTools = false
    },
    pollWebSideMessage() {
      this.$api.pollWebSideMessage({ maxId: this.pollMaxId }).then(({ data }) => {
        this.unreadCount = data.unreadCount
        this.pollMaxId = data.maxId
        const newMessages = data.newMessages
        if (newMessages && newMessages.length) {
          for (const newMessage of newMessages) {
            setTimeout(() => {
              const messageType = enumValueOf(MESSAGE_TYPE, newMessage.type)
              const key = getUUID()
              this.$notification[messageType.notify]({
                key,
                message: messageType.label,
                description: clearStainKeywords(newMessage.message),
                duration: messageType.duration,
                onClick: () => {
                  this.$notification.close(key)
                  this.$router.push(messageType.redirect(newMessage))
                  this.$api.setMessageRead({ idList: [newMessage.id] })
                }
              })
            })
          }
        }
      })
    }
  },
  filters: {
    formatMessageClassify(classify, f) { return enumValueOf(MESSAGE_CLASSIFY, classify)[f] },
    formatMessageType(type, f) { return enumValueOf(MESSAGE_TYPE, type)[f] }
  },
  mounted() {
    this.pollId !== null && clearInterval(this.pollId)
    this.pollWebSideMessage()
    this.pollId = setInterval(this.pollWebSideMessage, 15000)
    this.getMessages()
  },
  beforeDestroy() {
    this.pollId !== null && clearInterval(this.pollId)
    this.pollId = null
  }
}
</script>

<style lang="less" scoped>
.web-side-message-trigger {
  font-size: 19px;
  color: #475569;
}

.icon-wrapper {
  display: inline-block;
  margin-top: 3px;
}

::v-deep .unread-message-dot .ant-badge-dot {
  margin: -2px;
}

.message-header-container {
  height: 48px;
  padding: 0 12px;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .message-header-right {
    display: flex;
  }
}

.message-block-item {
  padding-left: 4px;
  display: flex;
  align-items: center;
  color: #64748B !important;
}

.message-block-item:hover {
  color: #3B82F6 !important;
  transition: .2s;
}

.message-status-trigger {
  user-select: none;

  span {
    font-size: 14px;
    display: inline-block;
    padding: 0 5px 0 7px;
  }

  i {
    font-size: 16px !important;
  }
}

.header-dropdown-item {
  display: flex;
  align-items: center;
  user-select: none;

  i {
    font-size: 16px !important;
    margin-right: 8px !important;
  }

  span {
    font-size: 13px !important;
  }
}

.header-dropdown-item:hover {
  color: #3B82F6 !important;
}

.message-type-dropdown-item {
  width: 110px;
}

.message-action-dropdown-item {
  width: 170px;
}

.message-action-trigger {
  margin: 0 6px;
  font-size: 18px;
}

.message-list-container {
  display: flex;
  height: calc(100vh - 48px);
  overflow-y: auto;
}

.message-wrapper {
  padding: 14px 16px;
  border-bottom: 1px solid #F1F5F9;
  cursor: pointer;
}

.message-wrapper:hover {
  background: #F8FAFF;
}

.message-empty-wrapper {
  margin: 48px auto;
  text-align: center;
}

.load-more-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 8px;
  padding: 12px;
}

.message-top-wrapper, .message-text-wrapper {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 4px;
}

.message-date-wrapper {
  font-size: 11px;
  margin-left: 32px;
  color: #94A3B8;
}

.message-classify-icon, .message-text-icon {
  margin-right: 8px;
  font-size: 14px;
  color: #3B82F6;
  width: 24px;
  height: 24px;
  background: #EFF6FF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-top-wrapper {
  .message-top-left, .message-top-right {
    display: flex;
  }

  .message-top-right > i {
    width: 22px;
    font-size: 14px;
    color: #94A3B8;
  }

  .message-top-right > i:hover {
    color: #3B82F6 !important;
  }

  .message-classify-text-wrapper {
    margin-top: 2px;
    display: flex;
  }

  .message-classify-label, .message-type-label {
    color: #1E293B;
    font-size: 13px;
  }

  .message-classify-divider {
    color: #3B82F6;
    font-weight: 500;
    margin: 0 4px;
  }

  .message-unread-dot {
    display: flex;
    margin-left: 4px;
  }
}

.message-text {
  width: 320px;
  margin-top: 2px;
  color: #475569;
  font-size: 13px;
}
</style>